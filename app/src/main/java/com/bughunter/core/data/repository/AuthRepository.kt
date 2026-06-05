package com.bughunter.core.data.repository

import com.bughunter.core.data.local.AppPrefs
import com.bughunter.core.data.local.AuthPrefs
import com.bughunter.core.network.AuthEvent
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.EncryptedCookieJar
import com.bughunter.core.network.ErrorMapper
import com.bughunter.core.network.Result2
import com.bughunter.core.network.api.AuthApi
import com.bughunter.core.network.api.MetaApi
import com.bughunter.core.network.api.TotpApi
import com.bughunter.feature.auth.AuthStateHolder
import com.bughunter.core.network.dto.ChangePasswordIn
import com.bughunter.core.network.dto.DeleteAccountIn
import com.bughunter.core.network.dto.EmailChangeConfirmIn
import com.bughunter.core.network.dto.EmailChangeRequestIn
import com.bughunter.core.network.dto.ForgotPasswordIn
import com.bughunter.core.network.dto.LoginIn
import com.bughunter.core.network.dto.LoginResponse
import com.bughunter.core.network.dto.LoginTotpStepIn
import com.bughunter.core.network.dto.MeOut
import com.bughunter.core.network.dto.ProfileUpdateIn
import com.bughunter.core.network.dto.ResetPasswordIn
import com.bughunter.core.network.dto.SignupIn
import com.bughunter.core.network.dto.TotpBeginOut
import com.bughunter.core.network.dto.TotpConfirmIn
import com.bughunter.core.network.dto.TotpConfirmOut
import com.bughunter.core.network.dto.TotpDisableIn
import com.bughunter.core.network.dto.TotpStatus
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

internal sealed interface AuthState {
    data object Unknown : AuthState
    data object Unauthenticated : AuthState
    data class AwaitingTotp(val pendingToken: String, val email: String) : AuthState
    data class Authenticated(val me: MeOut) : AuthState
    data class LockedOut(val until: Instant) : AuthState
}

@Singleton
internal class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val totpApi: TotpApi,
    private val metaApi: MetaApi,
    private val errorMapper: ErrorMapper,
    private val authPrefs: AuthPrefs,
    private val appPrefs: AppPrefs,
    private val cookieJar: EncryptedCookieJar,
    private val stateHolder: AuthStateHolder,
    moshi: Moshi,
) {

    private val meAdapter: JsonAdapter<MeOut> = moshi.adapter(MeOut::class.java)

    private val _state = MutableStateFlow<AuthState>(AuthState.Unknown)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    suspend fun bootstrap() {
        val cached = readCachedMe()
        if (cached != null) {
            _state.value = AuthState.Authenticated(cached)
            stateHolder.setAuthenticated(cached)
        } else {
            _state.value = AuthState.Unauthenticated
            stateHolder.setUnauthenticated()
        }
    }

    suspend fun signup(body: SignupIn): Result2<MeOut> {
        val result = runResult(errorMapper) { authApi.signup(body) }
        if (result is Result2.Ok) onAuthenticated(result.value)
        return result
    }

    suspend fun login(body: LoginIn): Result2<LoginResponse> {
        val result = runResult(errorMapper) { authApi.login(body) }
        when (result) {
            is Result2.Ok -> when (val payload = result.value) {
                is LoginResponse.Authenticated -> onAuthenticated(payload.me)
                is LoginResponse.AwaitingTotp -> {
                    _state.value = AuthState.AwaitingTotp(
                        pendingToken = payload.pendingToken,
                        email = body.email,
                    )
                    stateHolder.setAwaitingTotp(payload.pendingToken, body.email)
                }
            }
            is Result2.Err -> handleLoginError(result.error)
        }
        return result
    }

    suspend fun loginTotp(body: LoginTotpStepIn): Result2<MeOut> {
        val result = runResult(errorMapper) { authApi.loginTotp(body) }
        when (result) {
            is Result2.Ok -> onAuthenticated(result.value)
            is Result2.Err -> handleLoginError(result.error)
        }
        return result
    }

    suspend fun logout(): Result2<Unit> {
        val result = runResult(errorMapper) { authApi.logout() }
        // Best-effort: clear locally even when the network call fails.
        clearLocalSession()
        return result
    }

    suspend fun me(): Result2<MeOut> {
        val result = runResult(errorMapper) { authApi.me() }
        if (result is Result2.Ok) onAuthenticated(result.value)
        return result
    }

    suspend fun changePassword(body: ChangePasswordIn): Result2<Unit> =
        runResult(errorMapper) { authApi.changePassword(body) }

    suspend fun forgotPassword(body: ForgotPasswordIn): Result2<Unit> =
        runResult(errorMapper) { authApi.forgotPassword(body) }

    suspend fun resetPassword(body: ResetPasswordIn): Result2<Unit> =
        runResult(errorMapper) { authApi.resetPassword(body) }

    suspend fun updateProfile(body: ProfileUpdateIn): Result2<MeOut> {
        val result = runResult(errorMapper) { authApi.updateProfile(body) }
        if (result is Result2.Ok) onAuthenticated(result.value)
        return result
    }

    suspend fun requestEmailChange(body: EmailChangeRequestIn): Result2<Map<String, String>> =
        runResult(errorMapper) { authApi.requestEmailChange(body) }

    suspend fun confirmEmailChange(body: EmailChangeConfirmIn): Result2<MeOut> {
        val result = runResult(errorMapper) { authApi.confirmEmailChange(body) }
        if (result is Result2.Ok) onAuthenticated(result.value)
        return result
    }

    suspend fun dataExport(): Result2<Map<String, Any?>> =
        runResult(errorMapper) { authApi.dataExport() }

    suspend fun deleteAccount(body: DeleteAccountIn): Result2<Unit> {
        val result = runResult(errorMapper) { authApi.deleteAccount(body) }
        if (result is Result2.Ok) clearLocalSession()
        return result
    }

    suspend fun onInviteAccepted(me: MeOut) {
        onAuthenticated(me)
    }

    suspend fun totpStatus(): Result2<TotpStatus> = runResult(errorMapper) { totpApi.status() }

    suspend fun totpBegin(): Result2<TotpBeginOut> = runResult(errorMapper) { totpApi.begin() }

    suspend fun totpConfirm(body: TotpConfirmIn): Result2<TotpConfirmOut> {
        val result = runResult(errorMapper) { totpApi.confirm(body) }
        if (result is Result2.Ok) authPrefs.setTotpEnabledCached(true)
        return result
    }

    suspend fun totpDisable(body: TotpDisableIn): Result2<Unit> {
        val result = runResult(errorMapper) { totpApi.disable(body) }
        if (result is Result2.Ok) authPrefs.setTotpEnabledCached(false)
        return result
    }

    suspend fun totpRegenerateRecoveryCodes(): Result2<TotpConfirmOut> =
        runResult(errorMapper) { totpApi.regenerateRecoveryCodes() }

    suspend fun seedCsrf(): Result2<Unit> = runResult(errorMapper) { metaApi.health() }

    fun onAuthEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.LoggedOut -> {
                _state.value = AuthState.Unauthenticated
                stateHolder.emitSessionExpired()
            }
            is AuthEvent.LockedOut -> {
                _state.value = AuthState.LockedOut(event.until)
                val now = Instant.now()
                val duration = if (event.until.isAfter(now)) {
                    Duration.between(now, event.until)
                } else {
                    DEFAULT_LOCKOUT
                }
                stateHolder.setLockedOut(duration)
            }
            AuthEvent.CsrfReseedNeeded -> Unit
        }
    }

    suspend fun clearLocalSession() {
        cookieJar.clear()
        authPrefs.clear()
        appPrefs.setLastKnownOrgId(null)
        _state.value = AuthState.Unauthenticated
        stateHolder.emitLoggedOut()
    }

    private suspend fun onAuthenticated(me: MeOut) {
        authPrefs.setLastKnownMe(meAdapter.toJson(me))
        authPrefs.setTotpEnabledCached(me.totpEnabled)
        appPrefs.setLastKnownOrgId(me.orgId)
        _state.value = AuthState.Authenticated(me)
        stateHolder.setAuthenticated(me)
    }

    private fun handleLoginError(error: DomainError) {
        when (error) {
            is DomainError.RateLimited -> {
                val duration = error.retryAfter ?: DEFAULT_LOCKOUT
                val until = Instant.now().plus(duration)
                _state.value = AuthState.LockedOut(until)
                stateHolder.setLockedOut(duration)
            }
            is DomainError.Validation -> {
                val msg = error.message.orEmpty()
                if (msg.contains("breach", ignoreCase = true) || msg.contains("known breach")) {
                    stateHolder.emitBreachRejected()
                }
            }
            else -> Unit
        }
    }

    private suspend fun readCachedMe(): MeOut? {
        val json = authPrefs.lastKnownMe.firstOrNull() ?: return null
        return try {
            meAdapter.fromJson(json)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val DEFAULT_LOCKOUT: Duration = Duration.ofMinutes(15)
    }
}
