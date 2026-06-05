package com.bughunter.core.network.api

import com.bughunter.core.network.InstantAdapter
import com.bughunter.core.network.LocalDateAdapter
import com.bughunter.core.network.OmitNullJsonAdapterFactory
import com.bughunter.core.network.dto.registerEnumAdapters
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class BugsApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: BugsApi

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        val moshi = registerEnumAdapters(
            Moshi.Builder()
                .add(OmitNullJsonAdapterFactory())
                .add(InstantAdapter())
                .add(LocalDateAdapter()),
        ).build()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BugsApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `list builds repeated query keys for arrays`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"items":[],"page":1,"page_size":50,"total":0,"pages":0}""",
            ),
        )
        api.list(
            projectId = listOf(1, 2),
            status = listOf("New", "In Progress"),
            priority = null,
            page = 1,
            pageSize = 50,
        )
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("GET")
        val path = recorded.path.orEmpty()
        assertThat(path).startsWith("/api/bugs?")
        assertThat(path).contains("project_id=1")
        assertThat(path).contains("project_id=2")
        assertThat(path).contains("status=New")
        // Spaces should be url-encoded as +/%20 — accept either form.
        assertThat(path).matches(".*status=In(\\+|%20)Progress.*")
        assertThat(path).contains("page=1")
        assertThat(path).contains("page_size=50")
    }

    @Test
    fun `exportCsv streams response with Accept text csv header`() = runBlocking {
        val csv = "id,title\n1,first\n2,second\n"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/csv")
                .setBody(csv),
        )
        val body = api.exportCsv(projectId = listOf(7))
        val recorded = server.takeRequest()
        assertThat(recorded.path.orEmpty()).startsWith("/api/bugs/export.csv")
        assertThat(recorded.path.orEmpty()).contains("project_id=7")
        assertThat(recorded.getHeader("Accept")).isEqualTo("text/csv")
        // Reads source bytes; @Streaming means body is not buffered into memory.
        val bytes = body.byteStream().readBytes()
        body.close()
        assertThat(String(bytes)).isEqualTo(csv)
    }

    @Test
    fun `delete returns map and hits bug id path`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"deleted\":\"42\"}"))
        val out = api.delete(42)
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("DELETE")
        assertThat(recorded.path).isEqualTo("/api/bugs/42")
        assertThat(out["deleted"]).isEqualTo("42")
    }

    @Test
    fun `uploadAttachment posts multipart with file part`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {
                  "id": 9, "filename": "shot.png", "content_type": "image/png",
                  "size_bytes": 13, "uploader_user_id": 1, "uploader_name": "S",
                  "comment_id": null, "created_at": "2026-06-04T12:00:00Z"
                }
                """.trimIndent(),
            ),
        )
        val file = "hello-content".toRequestBody("image/png".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "shot.png", file)
        api.uploadAttachment(bugId = 7, file = part, commentId = null)
        val recorded = server.takeRequest()
        assertThat(recorded.method).isEqualTo("POST")
        assertThat(recorded.path).isEqualTo("/api/bugs/7/attachments")
        assertThat(recorded.getHeader("Content-Type").orEmpty()).startsWith("multipart/form-data")
        assertThat(recorded.body.readUtf8()).contains("hello-content")
    }
}
