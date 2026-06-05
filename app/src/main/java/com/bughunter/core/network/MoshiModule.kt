package com.bughunter.core.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.lang.reflect.Type
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Singleton

class InstantAdapter {
    @ToJson fun toJson(instant: Instant): String = DateTimeFormatter.ISO_INSTANT.format(instant)

    @FromJson fun fromJson(raw: String): Instant {
        return try {
            Instant.parse(raw)
        } catch (_: Exception) {
            // Fallback: tolerate dates without milliseconds and offsets like "+00:00".
            Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(raw))
        }
    }
}

class LocalDateAdapter {
    @ToJson fun toJson(date: LocalDate): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)

    @FromJson fun fromJson(raw: String): LocalDate = LocalDate.parse(raw, DateTimeFormatter.ISO_LOCAL_DATE)
}

/**
 * Marker annotation indicating a DTO whose null fields must be elided from the JSON output.
 * Used on `*Update` payloads where omission means "do not change" but explicit null means
 * "set to null" — except for fields we always want to drop when null.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class OmitNull

class OmitNullJsonAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        val rawType = Types.getRawType(type)
        if (!shouldApply(rawType.simpleName)) return null
        val delegate: JsonAdapter<Any> = moshi.nextAdapter(this, type, annotations)
        return object : JsonAdapter<Any>() {
            override fun fromJson(reader: JsonReader): Any? = delegate.fromJson(reader)
            override fun toJson(writer: JsonWriter, value: Any?) {
                val previous = writer.serializeNulls
                writer.serializeNulls = false
                try {
                    delegate.toJson(writer, value)
                } finally {
                    writer.serializeNulls = previous
                }
            }
        }
    }

    private fun shouldApply(simpleName: String?): Boolean {
        if (simpleName == null) return false
        return simpleName.endsWith("Update") || simpleName.endsWith("UpdateIn")
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object MoshiModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .add(OmitNullJsonAdapterFactory())
        .add(InstantAdapter())
        .add(LocalDateAdapter())
        .add(java.util.Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
        .add(LoginResponseAdapterFactory)
        .add(ChatBlockAdapterFactory)
        .build()
}

private object LoginResponseAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != com.bughunter.core.network.dto.LoginResponse::class.java) return null
        return com.bughunter.core.network.dto.LoginResponseAdapter(moshi)
    }
}

private object ChatBlockAdapterFactory : JsonAdapter.Factory {
    override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
        if (Types.getRawType(type) != com.bughunter.core.network.dto.ChatBlock::class.java) return null
        return com.bughunter.core.network.dto.ChatBlockAdapter(moshi)
    }
}
