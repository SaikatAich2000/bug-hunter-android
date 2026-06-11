package com.bughunter.core.network.dto

import com.bughunter.core.data.repository.RepoTestSupport
import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.junit.Before
import org.junit.Test

class PolymorphicTest {

    private lateinit var moshi: Moshi
    private lateinit var chatAdapter: JsonAdapter<ChatBlock>
    private lateinit var loginAdapter: JsonAdapter<LoginResponse>

    @Before
    fun setUp() {
        moshi = RepoTestSupport.moshi()
        chatAdapter = moshi.adapter(ChatBlock::class.java)
        loginAdapter = moshi.adapter(LoginResponse::class.java)
    }

    // ---------------- ChatBlock.fromJson ----------------

    @Test
    fun `chat text block parses text and format`() {
        val parsed = chatAdapter.fromJson(
            """{"type":"text","text":"hello","format":"markdown"}""",
        )
        assertThat(parsed).isInstanceOf(ChatBlock.Text::class.java)
        val text = parsed as ChatBlock.Text
        assertThat(text.text).isEqualTo("hello")
        assertThat(text.format).isEqualTo("markdown")
        assertThat(text.type).isEqualTo("text")
    }

    @Test
    fun `chat text block defaults missing text to empty and null format`() {
        val parsed = chatAdapter.fromJson("""{"type":"text"}""") as ChatBlock.Text
        assertThat(parsed.text).isEmpty()
        assertThat(parsed.format).isNull()
    }

    @Test
    fun `chat table block parses columns rows and row_keys`() {
        val parsed = chatAdapter.fromJson(
            """{"type":"table","columns":["a","b"],"rows":[["1","2"],["3","4"]],"row_keys":[10,20]}""",
        )
        assertThat(parsed).isInstanceOf(ChatBlock.Table::class.java)
        val table = parsed as ChatBlock.Table
        assertThat(table.columns).containsExactly("a", "b").inOrder()
        assertThat(table.rows).hasSize(2)
        assertThat(table.rows[0]).containsExactly("1", "2").inOrder()
        assertThat(table.rowKeys).containsExactly(10, 20).inOrder()
        assertThat(table.type).isEqualTo("table")
    }

    @Test
    fun `chat table block defaults to empty lists when fields absent`() {
        val parsed = chatAdapter.fromJson("""{"type":"table"}""") as ChatBlock.Table
        assertThat(parsed.columns).isEmpty()
        assertThat(parsed.rows).isEmpty()
        assertThat(parsed.rowKeys).isEmpty()
    }

    @Test
    fun `chat file block parses all fields including size and mime`() {
        val parsed = chatAdapter.fromJson(
            """{"type":"file","filename":"report.pdf","token":"tok-1","size_bytes":2048,"mime_type":"application/pdf"}""",
        )
        assertThat(parsed).isInstanceOf(ChatBlock.File::class.java)
        val file = parsed as ChatBlock.File
        assertThat(file.filename).isEqualTo("report.pdf")
        assertThat(file.token).isEqualTo("tok-1")
        assertThat(file.sizeBytes).isEqualTo(2048L)
        assertThat(file.mimeType).isEqualTo("application/pdf")
        assertThat(file.type).isEqualTo("file")
    }

    @Test
    fun `chat file block defaults optional size and mime to null`() {
        val parsed = chatAdapter.fromJson(
            """{"type":"file","filename":"a.txt","token":"t"}""",
        ) as ChatBlock.File
        assertThat(parsed.sizeBytes).isNull()
        assertThat(parsed.mimeType).isNull()
    }

    @Test
    fun `chat suggestions block parses items`() {
        val parsed = chatAdapter.fromJson(
            """{"type":"suggestions","items":["one","two","three"]}""",
        )
        assertThat(parsed).isInstanceOf(ChatBlock.Suggestions::class.java)
        val suggestions = parsed as ChatBlock.Suggestions
        assertThat(suggestions.items).containsExactly("one", "two", "three").inOrder()
        assertThat(suggestions.type).isEqualTo("suggestions")
    }

    @Test
    fun `chat suggestions block defaults to empty items`() {
        val parsed = chatAdapter.fromJson("""{"type":"suggestions"}""") as ChatBlock.Suggestions
        assertThat(parsed.items).isEmpty()
    }

    @Test
    fun `chat confirm block parses prompt labels and payload`() {
        val parsed = chatAdapter.fromJson(
            """{"type":"confirm","prompt":"Sure?","confirm_label":"Yes","cancel_label":"No","payload":{"id":7}}""",
        )
        assertThat(parsed).isInstanceOf(ChatBlock.Confirm::class.java)
        val confirm = parsed as ChatBlock.Confirm
        assertThat(confirm.prompt).isEqualTo("Sure?")
        assertThat(confirm.confirmLabel).isEqualTo("Yes")
        assertThat(confirm.cancelLabel).isEqualTo("No")
        assertThat(confirm.payload).containsKey("id")
        assertThat(confirm.type).isEqualTo("confirm")
    }

    @Test
    fun `chat confirm block defaults labels null and empty payload`() {
        val parsed = chatAdapter.fromJson(
            """{"type":"confirm","prompt":"Go?"}""",
        ) as ChatBlock.Confirm
        assertThat(parsed.confirmLabel).isNull()
        assertThat(parsed.cancelLabel).isNull()
        assertThat(parsed.payload).isEmpty()
    }

    @Test
    fun `chat unknown type falls back to Unknown with raw map`() {
        val parsed = chatAdapter.fromJson(
            """{"type":"mystery","foo":"bar","n":3}""",
        )
        assertThat(parsed).isInstanceOf(ChatBlock.Unknown::class.java)
        val unknown = parsed as ChatBlock.Unknown
        assertThat(unknown.raw).containsEntry("type", "mystery")
        assertThat(unknown.raw).containsEntry("foo", "bar")
        assertThat(unknown.type).isEqualTo("unknown")
    }

    @Test
    fun `chat missing type falls back to Unknown`() {
        val parsed = chatAdapter.fromJson("""{"foo":"bar"}""")
        assertThat(parsed).isInstanceOf(ChatBlock.Unknown::class.java)
    }

    @Test
    fun `chat null json yields null`() {
        assertThat(chatAdapter.fromJson("null")).isNull()
    }

    // ---------------- ChatBlock.toJson ----------------

    @Test
    fun `chat text block serializes`() {
        val json = chatAdapter.toJson(ChatBlock.Text(text = "hi", format = "plain"))
        assertThat(json).contains("\"type\":\"text\"")
        assertThat(json).contains("\"text\":\"hi\"")
        assertThat(json).contains("\"format\":\"plain\"")
    }

    @Test
    fun `chat table block round-trips`() {
        val original = ChatBlock.Table(
            columns = listOf("c1", "c2"),
            rows = listOf(listOf("x", "y")),
            rowKeys = listOf(5),
        )
        val json = chatAdapter.toJson(original)
        val back = chatAdapter.fromJson(json) as ChatBlock.Table
        assertThat(back.columns).isEqualTo(original.columns)
        assertThat(back.rowKeys).isEqualTo(original.rowKeys)
        assertThat(json).contains("\"row_keys\"")
    }

    @Test
    fun `chat file block round-trips`() {
        val original = ChatBlock.File(
            filename = "f.bin",
            token = "tk",
            sizeBytes = 99L,
            mimeType = "application/octet-stream",
        )
        val json = chatAdapter.toJson(original)
        val back = chatAdapter.fromJson(json) as ChatBlock.File
        assertThat(back).isEqualTo(original)
        assertThat(json).contains("\"size_bytes\"")
        assertThat(json).contains("\"mime_type\"")
    }

    @Test
    fun `chat suggestions block round-trips`() {
        val original = ChatBlock.Suggestions(items = listOf("a", "b"))
        val json = chatAdapter.toJson(original)
        val back = chatAdapter.fromJson(json) as ChatBlock.Suggestions
        assertThat(back.items).isEqualTo(original.items)
    }

    @Test
    fun `chat confirm block round-trips`() {
        val original = ChatBlock.Confirm(
            prompt = "p",
            confirmLabel = "ok",
            cancelLabel = "no",
            payload = mapOf("k" to "v"),
        )
        val json = chatAdapter.toJson(original)
        val back = chatAdapter.fromJson(json) as ChatBlock.Confirm
        assertThat(back.prompt).isEqualTo("p")
        assertThat(back.confirmLabel).isEqualTo("ok")
        assertThat(json).contains("\"confirm_label\"")
    }

    @Test
    fun `chat unknown block serializes its raw map`() {
        val json = chatAdapter.toJson(ChatBlock.Unknown(raw = mapOf("type" to "x", "extra" to "y")))
        assertThat(json).contains("\"extra\":\"y\"")
    }

    @Test
    fun `chat null value serializes to null`() {
        assertThat(chatAdapter.toJson(null)).isEqualTo("null")
    }

    // ---------------- LoginResponse.fromJson ----------------

    @Test
    fun `login awaiting totp parsed from pending_token`() {
        val parsed = loginAdapter.fromJson(
            """{"pending_2fa":true,"pending_token":"pt-123"}""",
        )
        assertThat(parsed).isInstanceOf(LoginResponse.AwaitingTotp::class.java)
        assertThat((parsed as LoginResponse.AwaitingTotp).pendingToken).isEqualTo("pt-123")
    }

    @Test
    fun `login authenticated parsed from flat MeOut object`() {
        val parsed = loginAdapter.fromJson(ME_JSON)
        assertThat(parsed).isInstanceOf(LoginResponse.Authenticated::class.java)
        val me = (parsed as LoginResponse.Authenticated).me
        assertThat(me.id).isEqualTo(42)
        assertThat(me.email).isEqualTo("a@b.com")
        assertThat(me.role).isEqualTo(Role.ADMIN)
        assertThat(me.totpEnabled).isTrue()
    }

    @Test
    fun `login authenticated unwraps nested user object`() {
        val parsed = loginAdapter.fromJson("""{"user":$ME_JSON}""")
        assertThat(parsed).isInstanceOf(LoginResponse.Authenticated::class.java)
        val me = (parsed as LoginResponse.Authenticated).me
        assertThat(me.id).isEqualTo(42)
        assertThat(me.organizationSlug).isEqualTo("acme")
    }

    @Test
    fun `login null json yields null`() {
        assertThat(loginAdapter.fromJson("null")).isNull()
    }

    // ---------------- LoginResponse.toJson ----------------

    @Test
    fun `login awaiting totp serializes pending fields`() {
        val json = loginAdapter.toJson(LoginResponse.AwaitingTotp("pt-9"))
        assertThat(json).contains("\"pending_2fa\":true")
        assertThat(json).contains("\"pending_token\":\"pt-9\"")
    }

    @Test
    fun `login awaiting totp round-trips`() {
        val original = LoginResponse.AwaitingTotp("rt-1")
        val back = loginAdapter.fromJson(loginAdapter.toJson(original))
        assertThat(back).isEqualTo(original)
    }

    @Test
    fun `login authenticated serializes as MeOut and round-trips`() {
        val me = MeOut(
            id = 1,
            name = "Neo",
            email = "neo@m.com",
            role = Role.MANAGER,
            isActive = true,
            orgId = 9,
            organizationName = "Matrix",
            organizationSlug = "matrix",
            totpEnabled = false,
            branding = null,
        )
        val json = loginAdapter.toJson(LoginResponse.Authenticated(me))
        assertThat(json).contains("\"organization_slug\":\"matrix\"")
        assertThat(json).contains("\"role\":\"manager\"")
        val back = loginAdapter.fromJson(json) as LoginResponse.Authenticated
        assertThat(back.me).isEqualTo(me)
    }

    @Test
    fun `login null value serializes to null`() {
        assertThat(loginAdapter.toJson(null)).isEqualTo("null")
    }

    private companion object {
        const val ME_JSON =
            """{"id":42,"name":"Alice","email":"a@b.com","role":"admin","is_active":true,""" +
                """"org_id":3,"organization_name":"Acme","organization_slug":"acme","totp_enabled":true}"""
    }
}
