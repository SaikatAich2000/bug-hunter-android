package com.bughunter.core.network.dto

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson

internal sealed interface LoginResponse {
    data class AwaitingTotp(val pendingToken: String) : LoginResponse
    data class Authenticated(val me: MeOut) : LoginResponse
}

// Discriminator: presence of "pending_token"/"pending_2fa" => AwaitingTotp; otherwise treat as MeOut/Authenticated.
internal class LoginResponseAdapter(moshi: Moshi) : JsonAdapter<LoginResponse>() {
    private val mapAdapter: JsonAdapter<Map<String, Any?>> = moshi.adapter(
        com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )
    private val meAdapter: JsonAdapter<MeOut> = moshi.adapter(MeOut::class.java)

    override fun fromJson(reader: JsonReader): LoginResponse? {
        val raw = mapAdapter.fromJson(reader) ?: return null
        val pendingToken = raw["pending_token"] as? String
        if (pendingToken != null) return LoginResponse.AwaitingTotp(pendingToken)
        val nested = raw["user"]
        @Suppress("UNCHECKED_CAST")
        val source: Map<String, Any?> = if (nested is Map<*, *>) nested as Map<String, Any?> else raw
        val json = mapAdapter.toJson(source)
        val me = meAdapter.fromJson(json) ?: return null
        return LoginResponse.Authenticated(me)
    }

    override fun toJson(writer: JsonWriter, value: LoginResponse?) {
        when (value) {
            null -> writer.nullValue()
            is LoginResponse.AwaitingTotp -> {
                writer.beginObject()
                writer.name("pending_2fa").value(true)
                writer.name("pending_token").value(value.pendingToken)
                writer.endObject()
            }
            is LoginResponse.Authenticated -> meAdapter.toJson(writer, value.me)
        }
    }
}

internal sealed interface ChatBlock {
    val type: String

    data class Text(
        val text: String,
        val format: String? = null,
    ) : ChatBlock { override val type: String = "text" }

    data class Table(
        val columns: List<String> = emptyList(),
        val rows: List<List<Any?>> = emptyList(),
        val rowKeys: List<Int?> = emptyList(),
    ) : ChatBlock { override val type: String = "table" }

    data class File(
        val filename: String,
        val token: String,
        val sizeBytes: Long? = null,
        val mimeType: String? = null,
    ) : ChatBlock { override val type: String = "file" }

    data class Suggestions(
        val items: List<String> = emptyList(),
    ) : ChatBlock { override val type: String = "suggestions" }

    data class Confirm(
        val prompt: String,
        val confirmLabel: String? = null,
        val cancelLabel: String? = null,
        val payload: Map<String, Any?> = emptyMap(),
    ) : ChatBlock { override val type: String = "confirm" }

    data class Unknown(
        val raw: Map<String, Any?> = emptyMap(),
    ) : ChatBlock { override val type: String = "unknown" }
}

internal class ChatBlockAdapter(moshi: Moshi) : JsonAdapter<ChatBlock>() {
    private val mapAdapter: JsonAdapter<Map<String, Any?>> = moshi.adapter(
        com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java),
    )

    override fun fromJson(reader: JsonReader): ChatBlock? {
        val raw = mapAdapter.fromJson(reader) ?: return null
        return when (raw["type"] as? String) {
            "text" -> ChatBlock.Text(
                text = (raw["text"] as? String).orEmpty(),
                format = raw["format"] as? String,
            )
            "table" -> {
                @Suppress("UNCHECKED_CAST")
                val cols = (raw["columns"] as? List<Any?>)?.map { it?.toString().orEmpty() } ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val rows = (raw["rows"] as? List<List<Any?>>) ?: emptyList()
                @Suppress("UNCHECKED_CAST")
                val keys = (raw["row_keys"] as? List<Any?>)?.map { (it as? Number)?.toInt() } ?: emptyList()
                ChatBlock.Table(columns = cols, rows = rows, rowKeys = keys)
            }
            "file" -> ChatBlock.File(
                filename = (raw["filename"] as? String).orEmpty(),
                token = (raw["token"] as? String).orEmpty(),
                sizeBytes = (raw["size_bytes"] as? Number)?.toLong(),
                mimeType = raw["mime_type"] as? String,
            )
            "suggestions" -> {
                @Suppress("UNCHECKED_CAST")
                val items = (raw["items"] as? List<Any?>)?.map { it?.toString().orEmpty() } ?: emptyList()
                ChatBlock.Suggestions(items = items)
            }
            "confirm" -> {
                @Suppress("UNCHECKED_CAST")
                val payload = (raw["payload"] as? Map<String, Any?>) ?: emptyMap()
                ChatBlock.Confirm(
                    prompt = (raw["prompt"] as? String).orEmpty(),
                    confirmLabel = raw["confirm_label"] as? String,
                    cancelLabel = raw["cancel_label"] as? String,
                    payload = payload,
                )
            }
            else -> ChatBlock.Unknown(raw = raw)
        }
    }

    override fun toJson(writer: JsonWriter, value: ChatBlock?) {
        if (value == null) { writer.nullValue(); return }
        val map: Map<String, Any?> = when (value) {
            is ChatBlock.Text -> mapOf("type" to "text", "text" to value.text, "format" to value.format)
            is ChatBlock.Table -> mapOf(
                "type" to "table",
                "columns" to value.columns,
                "rows" to value.rows,
                "row_keys" to value.rowKeys,
            )
            is ChatBlock.File -> mapOf(
                "type" to "file",
                "filename" to value.filename,
                "token" to value.token,
                "size_bytes" to value.sizeBytes,
                "mime_type" to value.mimeType,
            )
            is ChatBlock.Suggestions -> mapOf("type" to "suggestions", "items" to value.items)
            is ChatBlock.Confirm -> mapOf(
                "type" to "confirm",
                "prompt" to value.prompt,
                "confirm_label" to value.confirmLabel,
                "cancel_label" to value.cancelLabel,
                "payload" to value.payload,
            )
            is ChatBlock.Unknown -> value.raw
        }
        mapAdapter.toJson(writer, map)
    }
}

internal object PolymorphicAdapters {
    @FromJson fun loginFromJson(reader: JsonReader): Nothing = error("Use LoginResponseAdapter registered in MoshiModule")
    @ToJson fun loginToJson(writer: JsonWriter, value: LoginResponse): Nothing = error("Use LoginResponseAdapter registered in MoshiModule")
}
