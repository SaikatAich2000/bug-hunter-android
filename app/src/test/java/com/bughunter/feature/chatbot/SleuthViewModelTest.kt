package com.bughunter.feature.chatbot

import com.bughunter.core.data.repository.ChatRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.network.api.ChatApi
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleuthViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: ChatRepository
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val clock: () -> Long = { 1_000L }

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        // Pre-seeded client => the CsrfInterceptor doesn't fire an inline GET /api/health
        // on the first POST, so we don't accidentally consume a queued MockResponse.
        val api = RepoTestSupport.retrofit(server, moshi).create(ChatApi::class.java)
        repo = ChatRepository(api, mapper, clock = clock)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun vm() = SleuthViewModel(repo, OpenBugBus(), clock = clock)

    private fun textReply(text: String = "hello") =
        MockResponse().setResponseCode(200).setBody("""{"blocks":[{"type":"text","text":"$text"}]}""")

    private fun confirmReply() = MockResponse().setResponseCode(200).setBody(
        """{"blocks":[{"type":"confirm","prompt":"Delete?","confirm_label":"Do it","cancel_label":"Stop"}]}""",
    )

    // --- initial state -----------------------------------------------------
    @Test
    fun `initial state is closed and empty`() {
        val vm = vm()
        val s = vm.state.value
        assertThat(s.isPanelOpen).isFalse()
        assertThat(s.isTyping).isFalse()
        assertThat(s.turns).isEmpty()
        assertThat(s.input).isEmpty()
        assertThat(s.selectedTab).isEqualTo(SleuthTab.CHAT)
        assertThat(s.unread).isEqualTo(0)
        assertThat(s.errorMessage).isNull()
        assertThat(s.history).isEmpty()
        assertThat(s.settings).isEqualTo(SleuthSettings())
    }

    // --- open / close / toggle panel --------------------------------------
    @Test
    fun `openPanel opens, resets unread and seeds welcome`() {
        val vm = vm()
        vm.openPanel()
        val s = vm.state.value
        assertThat(s.isPanelOpen).isTrue()
        assertThat(s.unread).isEqualTo(0)
        assertThat(s.turns).hasSize(1)
        assertThat(s.turns.first()).isInstanceOf(ChatTurn.SystemSaid::class.java)
    }

    @Test
    fun `openPanel a second time does not re-seed when turns exist`() {
        val vm = vm()
        vm.openPanel()
        vm.closePanel()
        vm.openPanel()
        assertThat(vm.state.value.turns).hasSize(1)
    }

    @Test
    fun `closePanel closes the panel`() {
        val vm = vm()
        vm.openPanel()
        vm.closePanel()
        assertThat(vm.state.value.isPanelOpen).isFalse()
    }

    @Test
    fun `togglePanel flips open then closed`() {
        val vm = vm()
        vm.togglePanel()
        assertThat(vm.state.value.isPanelOpen).isTrue()
        vm.togglePanel()
        assertThat(vm.state.value.isPanelOpen).isFalse()
    }

    // --- simple state setters ---------------------------------------------
    @Test
    fun `selectTab updates selected tab`() {
        val vm = vm()
        vm.selectTab(SleuthTab.HISTORY)
        assertThat(vm.state.value.selectedTab).isEqualTo(SleuthTab.HISTORY)
        vm.selectTab(SleuthTab.SETTINGS)
        assertThat(vm.state.value.selectedTab).isEqualTo(SleuthTab.SETTINGS)
    }

    @Test
    fun `onInputChange updates input`() {
        val vm = vm()
        vm.onInputChange("typing")
        assertThat(vm.state.value.input).isEqualTo("typing")
    }

    @Test
    fun `dismissError clears error message`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val vm = vm()
        vm.send("oops")
        awaitUntil { vm.state.value.errorMessage != null }
        vm.dismissError()
        assertThat(vm.state.value.errorMessage).isNull()
    }

    @Test
    fun `toggle settings flags update settings`() {
        val vm = vm()
        vm.toggleAutoOpen(true)
        assertThat(vm.state.value.settings.autoOpenOnFirstLaunch).isTrue()
        vm.toggleShowTyping(false)
        assertThat(vm.state.value.settings.showTypingIndicator).isFalse()
    }

    // --- send (success / error) -------------------------------------------
    @Test
    fun `send success appends user and bot turns and ends typing`() = runBlocking {
        server.enqueue(textReply("hi there"))
        val vm = vm()
        vm.openPanel()
        vm.send("show bugs")
        awaitUntil { vm.state.value.turns.any { it is ChatTurn.BotSaid } }
        val turns = vm.state.value.turns
        assertThat(turns.any { it is ChatTurn.UserSaid && it.text == "show bugs" }).isTrue()
        assertThat(turns.any { it is ChatTurn.BotSaid }).isTrue()
        assertThat(vm.state.value.isTyping).isFalse()
        assertThat(vm.state.value.input).isEmpty()
    }

    @Test
    fun `send uses input when no override given`() = runBlocking {
        server.enqueue(textReply())
        val vm = vm()
        vm.onInputChange("from input")
        vm.send()
        awaitUntil { vm.state.value.turns.any { it is ChatTurn.UserSaid } }
        assertThat(vm.state.value.turns.any { it is ChatTurn.UserSaid && it.text == "from input" }).isTrue()
    }

    @Test
    fun `send ignores blank text`() {
        val vm = vm()
        vm.send("   ")
        assertThat(vm.state.value.turns).isEmpty()
        assertThat(vm.state.value.history).isEmpty()
    }

    @Test
    fun `send error records errorMessage and appends system turn`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val vm = vm()
        vm.send("trigger error")
        awaitUntil { vm.state.value.errorMessage != null }
        assertThat(vm.state.value.errorMessage).isNotNull()
        awaitUntil {
            vm.state.value.turns.any {
                it is ChatTurn.SystemSaid && it.text.contains("Could not reach Sleuth")
            }
        }
        assertThat(vm.state.value.isTyping).isFalse()
    }

    @Test
    fun `onSuggestionTap sends the suggestion`() = runBlocking {
        server.enqueue(textReply())
        val vm = vm()
        vm.onSuggestionTap("bug 42")
        awaitUntil { vm.state.value.turns.any { it is ChatTurn.UserSaid && it.text == "bug 42" } }
        assertThat(vm.state.value.turns.any { it is ChatTurn.UserSaid && it.text == "bug 42" }).isTrue()
    }

    // --- typing indicator toggles during send -----------------------------
    @Test
    fun `isTyping toggles true during an in-flight ask`() = runBlocking {
        server.enqueue(textReply().setBodyDelay(200, java.util.concurrent.TimeUnit.MILLISECONDS))
        val vm = vm()
        vm.send("slow")
        awaitUntil { vm.state.value.isTyping }
        assertThat(vm.state.value.isTyping).isTrue()
        awaitUntil { !vm.state.value.isTyping && vm.state.value.turns.any { it is ChatTurn.BotSaid } }
        assertThat(vm.state.value.isTyping).isFalse()
    }

    // --- unread counter ----------------------------------------------------
    @Test
    fun `unread increments when bot replies while panel closed`() = runBlocking {
        server.enqueue(textReply())
        val vm = vm()
        // panel stays closed
        vm.send("ask while closed")
        awaitUntil { vm.state.value.unread >= 1 }
        assertThat(vm.state.value.unread).isAtLeast(1)
    }

    @Test
    fun `openPanel resets unread to zero`() = runBlocking {
        server.enqueue(textReply())
        val vm = vm()
        vm.send("ask while closed")
        awaitUntil { vm.state.value.unread >= 1 }
        vm.openPanel()
        assertThat(vm.state.value.unread).isEqualTo(0)
    }

    @Test
    fun `unread stays zero when panel is open`() = runBlocking {
        server.enqueue(textReply())
        val vm = vm()
        vm.openPanel()
        vm.send("ask while open")
        awaitUntil { vm.state.value.turns.any { it is ChatTurn.BotSaid } }
        assertThat(vm.state.value.unread).isEqualTo(0)
    }

    // --- mergeRepoTurns preserves local welcome ---------------------------
    @Test
    fun `welcome system message survives repo turn merge`() = runBlocking {
        server.enqueue(textReply())
        val vm = vm()
        vm.openPanel() // seeds welcome (local-only system message)
        vm.send("hello")
        awaitUntil { vm.state.value.turns.any { it is ChatTurn.BotSaid } }
        val systemTexts = vm.state.value.turns.filterIsInstance<ChatTurn.SystemSaid>().map { it.text }
        assertThat(systemTexts.any { it.startsWith("Hi! I'm Sleuth") }).isTrue()
    }

    // --- history accumulation ---------------------------------------------
    @Test
    fun `send records a history entry`() = runBlocking {
        server.enqueue(textReply())
        val vm = vm()
        vm.send("first prompt")
        awaitUntil { vm.state.value.history.isNotEmpty() }
        val entry = vm.state.value.history.first()
        assertThat(entry.firstPrompt).isEqualTo("first prompt")
        assertThat(entry.turnCount).isEqualTo(1)
        assertThat(entry.startedAtEpochMs).isEqualTo(1_000L)
    }

    @Test
    fun `subsequent sends increment turnCount of the same session`() = runBlocking {
        server.enqueue(textReply())
        server.enqueue(textReply())
        val vm = vm()
        vm.send("one")
        awaitUntil { vm.state.value.turns.any { it is ChatTurn.BotSaid } }
        vm.send("two")
        awaitUntil { vm.state.value.history.firstOrNull()?.turnCount == 2 }
        assertThat(vm.state.value.history).hasSize(1)
        assertThat(vm.state.value.history.first().turnCount).isEqualTo(2)
    }

    // --- onConfirm + send flow --------------------------------------------
    // onConfirm marks the block resolved (APPROVED/REJECTED) and then sends the
    // confirm/cancel label. That follow-up send re-merges the repo turns, so the
    // local resolution is transient — the stable, observable outcome is that the
    // label was sent as a user turn. (The resolution code path still runs.)
    @Test
    fun `onConfirm approve sends the confirm label`() = runBlocking {
        server.enqueue(confirmReply())
        val vm = vm()
        vm.openPanel()
        vm.send("delete bugs")
        awaitUntil { vm.state.value.turns.any { it is ChatTurn.BotSaid } }
        val botIndex = vm.state.value.turns.indexOfFirst { it is ChatTurn.BotSaid }
        // The reply to the confirm-label send.
        server.enqueue(textReply("done"))
        vm.onConfirm(botIndex, 0, approved = true)
        // The confirm label "Do it" was sent as a user turn.
        awaitUntil { vm.state.value.turns.any { it is ChatTurn.UserSaid && it.text == "Do it" } }
        assertThat(vm.state.value.turns.any { it is ChatTurn.UserSaid && it.text == "Do it" }).isTrue()
    }

    @Test
    fun `onConfirm reject sends the cancel label`() = runBlocking {
        server.enqueue(confirmReply())
        val vm = vm()
        vm.openPanel()
        vm.send("delete bugs")
        awaitUntil { vm.state.value.turns.any { it is ChatTurn.BotSaid } }
        val botIndex = vm.state.value.turns.indexOfFirst { it is ChatTurn.BotSaid }
        server.enqueue(textReply("ok"))
        vm.onConfirm(botIndex, 0, approved = false)
        awaitUntil { vm.state.value.turns.any { it is ChatTurn.UserSaid && it.text == "Stop" } }
        assertThat(vm.state.value.turns.any { it is ChatTurn.UserSaid && it.text == "Stop" }).isTrue()
    }

    @Test
    fun `onConfirm is a no-op for a non-bot turn`() {
        val vm = vm()
        vm.openPanel() // turn 0 is a SystemSaid (welcome)
        vm.onConfirm(0, 0, approved = true)
        // No user turn produced; welcome unchanged.
        assertThat(vm.state.value.turns.any { it is ChatTurn.UserSaid }).isFalse()
    }

    @Test
    fun `onConfirm is a no-op for a non-confirm block index`() = runBlocking {
        server.enqueue(textReply()) // a Text block, not Confirm
        val vm = vm()
        vm.openPanel()
        vm.send("hello")
        awaitUntil { vm.state.value.turns.any { it is ChatTurn.BotSaid } }
        val botIndex = vm.state.value.turns.indexOfFirst { it is ChatTurn.BotSaid }
        val before = vm.state.value.turns
        vm.onConfirm(botIndex, 0, approved = true)
        assertThat(vm.state.value.turns).isEqualTo(before)
    }

    // --- onTableRowTap + OpenBugBus ---------------------------------------
    @Test
    fun `onTableRowTap emits bug id on the bus and closes the panel`() = runBlocking {
        val bus = OpenBugBus()
        val vm = SleuthViewModel(repo, bus, clock = clock)
        vm.openPanel()
        val received = mutableListOf<Int>()
        // Collect on the same runBlocking scope (Unconfined main) so the
        // collector is active before onTableRowTap's emit fires.
        val job = launch(Dispatchers.Unconfined) {
            bus.events.collect { received += it }
        }
        vm.onTableRowTap(99)
        awaitUntil { received.contains(99) }
        assertThat(received).contains(99)
        assertThat(vm.state.value.isPanelOpen).isFalse()
        job.cancel()
    }

    // --- clearHistory ------------------------------------------------------
    @Test
    fun `clearHistory empties turns and history`() = runBlocking {
        server.enqueue(textReply())
        val vm = vm()
        vm.openPanel()
        vm.send("something")
        awaitUntil { vm.state.value.history.isNotEmpty() }
        vm.clearHistory()
        awaitUntil { vm.state.value.turns.isEmpty() }
        assertThat(vm.state.value.turns).isEmpty()
        assertThat(vm.state.value.history).isEmpty()
    }
}
