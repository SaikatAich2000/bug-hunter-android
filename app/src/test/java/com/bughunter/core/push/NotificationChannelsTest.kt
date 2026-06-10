package com.bughunter.core.push

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pure-data tests for the push subsystem — no Android framework needed,
 * so they run on the JVM in milliseconds. We verify:
 *
 *  - the wire-format channel string ("mentions", "assignments",
 *    "activity", null, garbage) resolves to the right system channel id.
 *  - unknown/missing channel keys fall back to ACTIVITY (lowest
 *    importance) — never silently drop the push.
 */
class NotificationChannelsTest {

    @Test
    fun `resolve maps mentions variants to MENTIONS`() {
        assertThat(NotificationChannels.resolve("mention")).isEqualTo(NotificationChannels.MENTIONS)
        assertThat(NotificationChannels.resolve("mentions")).isEqualTo(NotificationChannels.MENTIONS)
        assertThat(NotificationChannels.resolve("MENTIONS")).isEqualTo(NotificationChannels.MENTIONS)
        assertThat(NotificationChannels.resolve(NotificationChannels.MENTIONS))
            .isEqualTo(NotificationChannels.MENTIONS)
    }

    @Test
    fun `resolve maps assignment variants to ASSIGNMENTS`() {
        assertThat(NotificationChannels.resolve("assignment")).isEqualTo(NotificationChannels.ASSIGNMENTS)
        assertThat(NotificationChannels.resolve("assignments")).isEqualTo(NotificationChannels.ASSIGNMENTS)
        assertThat(NotificationChannels.resolve(NotificationChannels.ASSIGNMENTS))
            .isEqualTo(NotificationChannels.ASSIGNMENTS)
    }

    @Test
    fun `unknown channel falls back to ACTIVITY, never null`() {
        assertThat(NotificationChannels.resolve(null)).isEqualTo(NotificationChannels.ACTIVITY)
        assertThat(NotificationChannels.resolve("")).isEqualTo(NotificationChannels.ACTIVITY)
        assertThat(NotificationChannels.resolve("garbage")).isEqualTo(NotificationChannels.ACTIVITY)
        assertThat(NotificationChannels.resolve("activity")).isEqualTo(NotificationChannels.ACTIVITY)
    }
}
