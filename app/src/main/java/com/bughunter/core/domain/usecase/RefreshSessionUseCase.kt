package com.bughunter.core.domain.usecase

import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.network.AuthEvent
import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.MeOut
import javax.inject.Inject

internal class RefreshSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {

    suspend operator fun invoke(): Result2<MeOut> {
        val result = authRepository.me()
        if (result is Result2.Err && result.error == DomainError.Unauthorized) {
            authRepository.onAuthEvent(AuthEvent.LoggedOut(AuthEvent.LoggedOut.Reason.ServerExpired))
            authRepository.clearLocalSession()
        }
        return result
    }
}
