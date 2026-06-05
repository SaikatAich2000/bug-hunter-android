package com.bughunter.core.domain.usecase

import com.bughunter.core.network.DomainError
import com.bughunter.core.network.Result2
import javax.inject.Inject

internal class OpenBugDeepLinkUseCase @Inject constructor() {

    operator fun invoke(raw: String?): Result2<Int> {
        if (raw.isNullOrBlank()) return Result2.Err(DomainError.NotFound)
        val trimmed = raw.trim()

        APP_REGEX.matchEntire(trimmed)?.let { match ->
            val id = match.groupValues[1].toIntOrNull()
            if (id != null) return Result2.Ok(id)
        }
        SLEUTH_REGEX.matchEntire(trimmed)?.let { match ->
            val id = match.groupValues[1].toIntOrNull()
            if (id != null) return Result2.Ok(id)
        }
        HASH_REGEX.matchEntire(trimmed)?.let { match ->
            val id = match.groupValues[1].toIntOrNull()
            if (id != null) return Result2.Ok(id)
        }

        return Result2.Err(DomainError.NotFound)
    }

    companion object {
        private val APP_REGEX = Regex("""app://bughunter/bug/(\d+)""")
        private val SLEUTH_REGEX = Regex("""sleuth:open-bug\?id=(\d+)""")
        private val HASH_REGEX = Regex("""#?bug-(\d+)""")
    }
}
