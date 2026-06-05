package com.bughunter.core.network.api

import com.bughunter.core.network.InstantAdapter
import com.bughunter.core.network.LocalDateAdapter
import com.bughunter.core.network.OmitNullJsonAdapterFactory
import com.bughunter.core.network.dto.ChangePasswordIn
import com.bughunter.core.network.dto.LoginIn
import com.bughunter.core.network.dto.LoginResponse
import com.bughunter.core.network.dto.LoginResponseAdapter
import com.bughunter.core.network.dto.Role
import com.bughunter.core.network.dto.registerEnumAdapters
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class AuthApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AuthApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val builder = registerEnumAdapters(
            Moshi.Builder()
                .add(OmitNullJsonAdapterFactory())
                .add(InstantAdapter())
                .add(LocalDateAdapter()),
        )
        val moshi = builder.build().newBuilder()
            .add(LoginResponse::class.java, LoginResponseAdapter(builder.build()))
            .build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuthApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `login posts to api auth login`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "{\"pending_2fa\":true,\"pending_token\":\"opaque\"}",
            ),
        )
        val out = api.login(LoginIn(email = "a@b.com", password = "p"))
        val recorded = server.takeRequest()
        assertThat(recorded.path).isEqualTo("/api/auth/login")
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(out).isInstanceOf(LoginResponse.AwaitingTotp::class.java)
        assertThat((out as LoginResponse.AwaitingTotp).pendingToken).isEqualTo("opaque")
    }

    @Test
    fun `login parses Authenticated MeOut variant`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "ok": true,
                  "user": {
                    "id": 1, "name": "Test User", "email": "user@example.com", "role": "admin",
                    "is_active": true, "org_id": 7, "organization_name": "Acme",
                    "organization_slug": "acme", "totp_enabled": false
                  }
                }
                """.trimIndent(),
            ),
        )
        val out = api.login(LoginIn(email = "s@b.com", password = "x"))
        assertThat(out).isInstanceOf(LoginResponse.Authenticated::class.java)
        val me = (out as LoginResponse.Authenticated).me
        assertThat(me.role).isEqualTo(Role.ADMIN)
        assertThat(me.orgId).isEqualTo(7)
    }

    @Test
    fun `me hits api auth me with GET`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "id": 1, "name": "S", "email": "s@b.com", "role": "member",
                  "is_active": true, "org_id": 1, "organization_name": "x",
                  "organization_slug": "x", "totp_enabled": false
                }
                """.trimIndent(),
            ),
        )
        api.me()
        val recorded = server.takeRequest()
        assertThat(recorded.path).isEqualTo("/api/auth/me")
        assertThat(recorded.method).isEqualTo("GET")
    }

    @Test
    fun `changePassword posts JSON body`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        api.changePassword(ChangePasswordIn(currentPassword = "old", newPassword = "newpw1"))
        val recorded = server.takeRequest()
        assertThat(recorded.path).isEqualTo("/api/auth/change-password")
        assertThat(recorded.method).isEqualTo("POST")
        val body = recorded.body.readUtf8()
        assertThat(body).contains("current_password")
        assertThat(body).contains("new_password")
    }
}
