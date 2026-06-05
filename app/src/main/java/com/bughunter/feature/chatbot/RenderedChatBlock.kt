package com.bughunter.feature.chatbot

import com.bughunter.core.network.dto.ChatBlock

internal sealed interface RenderedChatBlock {

    data class Text(
        val text: String,
        val format: String?,
    ) : RenderedChatBlock

    data class Table(
        val columns: List<String>,
        val rows: List<List<String>>,
        val rowKeys: List<Int?>,
    ) : RenderedChatBlock

    data class File(
        val filename: String,
        val token: String,
        val sizeBytes: Long?,
        val mimeType: String?,
    ) : RenderedChatBlock

    data class Suggestions(
        val items: List<String>,
    ) : RenderedChatBlock

    data class Confirm(
        val prompt: String,
        val confirmLabel: String,
        val cancelLabel: String,
        val payload: Map<String, Any?>,
        val resolved: Resolution = Resolution.PENDING,
    ) : RenderedChatBlock {
        enum class Resolution { PENDING, APPROVED, REJECTED }
    }

    data class Unknown(val raw: Map<String, Any?>) : RenderedChatBlock
}

internal fun ChatBlock.toRendered(): RenderedChatBlock = when (this) {
    is ChatBlock.Text -> RenderedChatBlock.Text(text = text, format = format)
    is ChatBlock.Table -> RenderedChatBlock.Table(
        columns = columns,
        rows = rows.map { row -> row.map { it?.toString().orEmpty() } },
        rowKeys = rowKeys,
    )
    is ChatBlock.File -> RenderedChatBlock.File(
        filename = filename,
        token = token,
        sizeBytes = sizeBytes,
        mimeType = mimeType,
    )
    is ChatBlock.Suggestions -> RenderedChatBlock.Suggestions(items = items)
    is ChatBlock.Confirm -> RenderedChatBlock.Confirm(
        prompt = prompt,
        confirmLabel = confirmLabel?.takeIf { it.isNotBlank() } ?: "Yes",
        cancelLabel = cancelLabel?.takeIf { it.isNotBlank() } ?: "No",
        payload = payload,
    )
    is ChatBlock.Unknown -> RenderedChatBlock.Unknown(raw = raw)
}
