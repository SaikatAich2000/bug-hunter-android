package com.bughunter.core.network

import com.google.common.truth.Truth.assertThat
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class MoshiAdaptersTest {

    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .add(OmitNullJsonAdapterFactory())
            .add(InstantAdapter())
            .add(LocalDateAdapter())
            .build()
    }

    @Test
    fun `InstantAdapter roundtrips ISO-8601`() {
        val adapter = moshi.adapter(Instant::class.java)
        val raw = "\"2026-01-15T10:22:00Z\""
        val parsed = adapter.fromJson(raw)!!
        assertThat(parsed).isEqualTo(Instant.parse("2026-01-15T10:22:00Z"))
        assertThat(adapter.toJson(parsed)).isEqualTo(raw)
    }

    @Test
    fun `LocalDateAdapter roundtrips YYYY-MM-DD`() {
        val adapter = moshi.adapter(LocalDate::class.java)
        val raw = "\"2026-01-15\""
        val parsed = adapter.fromJson(raw)!!
        assertThat(parsed).isEqualTo(LocalDate.of(2026, 1, 15))
        assertThat(adapter.toJson(parsed)).isEqualTo(raw)
    }

    @Test
    fun `OmitNullJsonAdapterFactory drops nulls for Update DTOs`() {
        val adapter = moshi.adapter(SampleUpdate::class.java)
        val json = adapter.toJson(SampleUpdate(name = "x", description = null))
        assertThat(json).doesNotContain("description")
        assertThat(json).contains("\"name\":\"x\"")
    }

    @Test
    fun `OmitNullJsonAdapterFactory does not drop nulls for non-Update DTOs`() {
        val adapter = moshi.adapter(SampleIn::class.java).serializeNulls()
        val json = adapter.toJson(SampleIn(name = "x", description = null))
        assertThat(json).contains("description")
    }

    @JsonClass(generateAdapter = true)
    data class SampleUpdate(val name: String?, val description: String?)

    @JsonClass(generateAdapter = true)
    data class SampleIn(val name: String?, val description: String?)
}
