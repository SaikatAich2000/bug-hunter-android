package com.bughunter.core.network

import com.bughunter.core.data.repository.AuthRepoTestFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class BaseUrlProviderTest {

    private fun newProvider() = BaseUrlProvider(AuthRepoTestFactory.appPrefs())

    @Test
    fun defaults_setNormalisation_andFlow() = runBlocking {
        val provider = newProvider()

        // Default value before any set() mirrors the baked-in default.
        assertThat(provider.current()).isEqualTo(BaseUrlProvider.DEFAULT_BASE_URL)
        assertThat(provider.currentBlocking()).isEqualTo(BaseUrlProvider.DEFAULT_BASE_URL)
        assertThat(provider.baseUrlFlow.first()).isEqualTo(BaseUrlProvider.DEFAULT_BASE_URL)

        // https without trailing slash gets one appended.
        provider.set("https://api.example.com")
        assertThat(provider.current()).isEqualTo("https://api.example.com/")
        assertThat(provider.currentBlocking()).isEqualTo("https://api.example.com/")
        assertThat(provider.baseUrlFlow.first()).isEqualTo("https://api.example.com/")

        // https with an existing trailing slash stays single-slash.
        provider.set("https://api.example.com/")
        assertThat(provider.current()).isEqualTo("https://api.example.com/")
    }

    @Test
    fun debugAllowsHttp_butRejectsOtherSchemes() = runBlocking {
        val provider = newProvider()

        // DEBUG unit-test build -> http allowed for emulator/dev hosts.
        provider.set("http://10.0.2.2:8000")
        assertThat(provider.current()).isEqualTo("http://10.0.2.2:8000/")

        // Non-HTTP(S) schemes are rejected with IllegalArgumentException.
        val ftp = runCatching { provider.set("ftp://bad") }
        assertThat(ftp.isFailure).isTrue()
        assertThat(ftp.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)

        val notUrl = runCatching { provider.set("notaurl") }
        assertThat(notUrl.isFailure).isTrue()
        assertThat(notUrl.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)

        // Rejected writes did not change the persisted value.
        assertThat(provider.current()).isEqualTo("http://10.0.2.2:8000/")
    }
}
