package com.bughunter.core.domain.usecase

import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.network.EncryptedCookieJar
import com.bughunter.core.network.Result2
import com.bughunter.core.network.dto.LoginIn
import com.bughunter.core.network.dto.LoginResponse
import javax.inject.Inject

internal class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val cookieJar: EncryptedCookieJar,
) {

    suspend operator fun invoke(email: String, password: String): Result2<LoginResponse> {
        if (cookieJar.csrfToken() == null) {
            // Seed CSRF via cheap GET so the next mutating call carries the header.
            authRepository.seedCsrf()
        }
        return authRepository.login(LoginIn(email = email, password = password))
    }
}
