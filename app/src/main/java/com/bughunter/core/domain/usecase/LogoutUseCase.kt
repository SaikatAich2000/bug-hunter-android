package com.bughunter.core.domain.usecase

import com.bughunter.core.data.repository.AuthRepository
import com.bughunter.core.data.repository.ChatRepository
import com.bughunter.core.data.repository.MetaRepository
import com.bughunter.core.network.Result2
import javax.inject.Inject

internal class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val metaRepository: MetaRepository,
) {

    suspend operator fun invoke(): Result2<Unit> {
        val result = authRepository.logout()
        chatRepository.clear()
        metaRepository.clear()
        return result
    }
}
