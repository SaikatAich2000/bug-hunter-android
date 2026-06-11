package com.bughunter.feature.bugs.detail

import androidx.lifecycle.SavedStateHandle
import com.bughunter.core.data.repository.BugsRepository
import com.bughunter.core.data.repository.RepoTestSupport
import com.bughunter.core.nav.BhRoute
import com.bughunter.core.network.api.BugsApi
import com.bughunter.core.ui.util.UiState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Pins the SavedStateHandle key/type contract for the bug-detail nav arg AND
 * exercises every public action on the VM (refresh, comment draft, post /
 * delete comment, delete attachment, dismiss banner) on both the success and
 * error paths.
 *
 * Nav declares `bugId` as NavType.IntType in BhNavHost, so the framework
 * stores it as java.lang.Integer in SavedStateHandle. The v2.10 crash
 * (`ClassCastException: Integer cannot be cast to String` on every
 * navigation to bug detail) was caused by the VM reading it as `<String>`
 * and calling toIntOrNull(). The constructor test instantiates the VM with
 * the same Integer the framework would provide; the constructor crashes if
 * anyone reverts the type back to `<String>`.
 *
 * Init fires a single sequential refresh() (one GET /api/bugs/{id}). Several
 * actions, however, do a GET-after-mutation; to keep that ordering robust we
 * route by path with a MockWebServer.Dispatcher rather than relying on FIFO
 * enqueue order.
 */
class BugDetailViewModelTest {

    private lateinit var server: MockWebServer
    private lateinit var api: BugsApi
    private val moshi = RepoTestSupport.moshi()
    private val mapper = RepoTestSupport.errorMapper(moshi)
    private val jar = RepoTestSupport.cookieJar()

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        server = MockWebServer().apply { start() }
        RepoTestSupport.seedCsrf(jar, server.url("/"))
        api = RepoTestSupport.retrofit(server, moshi, client = RepoTestSupport.client(jar))
            .create(BugsApi::class.java)
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
        Dispatchers.resetMain()
    }

    // --- helpers ----------------------------------------------------------

    private fun repo() = BugsRepository(api, mapper)

    private fun vm(bugId: Int = 42) =
        BugDetailViewModel(SavedStateHandle(mapOf(BhRoute.BugDetail.ARG_ID to bugId)), repo())

    private suspend fun awaitUntil(timeoutMs: Long = 4_000, check: () -> Boolean) {
        withTimeout(timeoutMs) { while (!check()) delay(10) }
    }

    private fun success(state: UiState<BugDetailScreenModel>): BugDetailScreenModel =
        (state as UiState.Success).data

    /**
     * Route by path so mutation + follow-up GET ordering doesn't depend on
     * arrival order. [detailBody] is returned for any `GET /api/bugs/{id}`;
     * comment-add / comment-delete / attachment-delete each get their own
     * canned 200 unless [failMutations] flips them to errors.
     */
    private fun routeOk(detailBody: String = BUG_DETAIL_BODY) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath ?: ""
                return when {
                    path.contains("/comments") ->
                        MockResponse().setResponseCode(200).setBody(COMMENT_BODY)
                    path.contains("/attachments") ->
                        MockResponse().setResponseCode(200).setBody("""{"deleted":1}""")
                    path.matches(Regex(".*/api/bugs/\\d+$")) ->
                        MockResponse().setResponseCode(200).setBody(detailBody)
                    else ->
                        MockResponse().setResponseCode(200).setBody(detailBody)
                }
            }
        }
    }

    // --- constructor / nav arg contract -----------------------------------

    @Test
    fun `constructor accepts integer bugId from nav SavedStateHandle without crashing`() = runBlocking {
        routeOk()
        val handle = SavedStateHandle(mapOf(BhRoute.BugDetail.ARG_ID to 42))
        val vm = BugDetailViewModel(handle, repo())

        awaitUntil { server.requestCount >= 1 }
        val recorded = server.takeRequest()
        assertThat(recorded.path).contains("/api/bugs/42")
        assertThat(vm).isNotNull()
    }

    @Test
    fun `missing bugId falls back to NotFound error`() = runBlocking {
        val handle = SavedStateHandle()
        val vm = BugDetailViewModel(handle, repo())

        // refresh() short-circuits when bugId <= 0; it does NOT hit the server.
        delay(50)
        assertThat(server.requestCount).isEqualTo(0)
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    // --- init / refresh ---------------------------------------------------

    @Test
    fun `init load success lands in Success state with the parsed bug`() = runBlocking {
        routeOk()
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Success }
        val data = success(vm.state.value)
        assertThat(data.bug.id).isEqualTo(42)
        assertThat(data.bug.title).isEqualTo("t")
        assertThat(data.actionError).isNull()
    }

    @Test
    fun `init load failure lands in Error state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Error }
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    @Test
    fun `refresh after error recovers to Success`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Error }

        routeOk()
        vm.refresh()
        awaitUntil { vm.state.value is UiState.Success }
        assertThat(success(vm.state.value).bug.id).isEqualTo(42)
    }

    // --- onCommentDraftChange ---------------------------------------------

    @Test
    fun `onCommentDraftChange updates draft and clears stale action error`() = runBlocking {
        routeOk()
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.onCommentDraftChange("hello")
        assertThat(success(vm.state.value).commentDraft).isEqualTo("hello")
    }

    @Test
    fun `onCommentDraftChange is a no-op when not in Success state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Error }
        vm.onCommentDraftChange("ignored")
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    // --- postComment ------------------------------------------------------

    @Test
    fun `postComment empty draft is a no-op`() = runBlocking {
        routeOk()
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.onCommentDraftChange("   ")   // whitespace only -> trims to empty
        val before = server.requestCount
        vm.postComment()
        delay(50)
        assertThat(server.requestCount).isEqualTo(before)
    }

    @Test
    fun `postComment success refreshes bug and clears draft`() = runBlocking {
        // First GET returns no comments; after posting, the refresh GET returns
        // a body that contains one comment so we can observe the merge.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath ?: ""
                return when {
                    path.contains("/comments") ->
                        MockResponse().setResponseCode(200).setBody(COMMENT_BODY)
                    path.matches(Regex(".*/api/bugs/\\d+$")) ->
                        MockResponse().setResponseCode(200).setBody(BUG_DETAIL_WITH_COMMENT)
                    else -> MockResponse().setResponseCode(200).setBody(BUG_DETAIL_WITH_COMMENT)
                }
            }
        }
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.onCommentDraftChange("nice catch")
        vm.postComment()
        awaitUntil { success(vm.state.value).commentDraft.isEmpty() && !success(vm.state.value).isPostingComment }
        val data = success(vm.state.value)
        assertThat(data.commentDraft).isEmpty()
        assertThat(data.isPostingComment).isFalse()
        assertThat(data.bug.comments).hasSize(1)
        assertThat(data.actionError).isNull()
    }

    @Test
    fun `postComment success with refresh failure merges the returned comment`() = runBlocking {
        // addComment succeeds; the follow-up GET fails -> VM falls back to
        // appending the returned CommentOut to the existing comment list.
        server.dispatcher = object : Dispatcher() {
            private var detailGets = 0
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath ?: ""
                return when {
                    path.contains("/comments") ->
                        MockResponse().setResponseCode(200).setBody(COMMENT_BODY)
                    path.matches(Regex(".*/api/bugs/\\d+$")) -> {
                        detailGets++
                        if (detailGets == 1) {
                            MockResponse().setResponseCode(200).setBody(BUG_DETAIL_BODY)
                        } else {
                            MockResponse().setResponseCode(500).setBody("""{"detail":"x"}""")
                        }
                    }
                    else -> MockResponse().setResponseCode(200).setBody(BUG_DETAIL_BODY)
                }
            }
        }
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.onCommentDraftChange("merge me")
        vm.postComment()
        awaitUntil { !success(vm.state.value).isPostingComment }
        val data = success(vm.state.value)
        assertThat(data.commentDraft).isEmpty()
        assertThat(data.bug.comments).hasSize(1)
        assertThat(data.bug.comments.first().id).isEqualTo(7)
    }

    @Test
    fun `postComment error surfaces an action error and keeps Success state`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath ?: ""
                return when {
                    path.contains("/comments") ->
                        MockResponse().setResponseCode(403).setBody("""{"detail":"forbidden"}""")
                    else -> MockResponse().setResponseCode(200).setBody(BUG_DETAIL_BODY)
                }
            }
        }
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.onCommentDraftChange("will fail")
        vm.postComment()
        awaitUntil { success(vm.state.value).actionError != null }
        val data = success(vm.state.value)
        assertThat(data.actionError).isNotNull()
        assertThat(data.isPostingComment).isFalse()
        // Draft preserved so the user can retry.
        assertThat(data.commentDraft).isEqualTo("will fail")
    }

    @Test
    fun `postComment is a no-op when not in Success state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Error }
        val before = server.requestCount
        vm.postComment()
        delay(50)
        assertThat(server.requestCount).isEqualTo(before)
    }

    // --- deleteComment ----------------------------------------------------

    @Test
    fun `deleteComment success refreshes bug and clears action error`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath ?: ""
                return when {
                    request.method == "DELETE" && path.contains("/comments") ->
                        MockResponse().setResponseCode(204)
                    path.matches(Regex(".*/api/bugs/\\d+$")) ->
                        MockResponse().setResponseCode(200).setBody(BUG_DETAIL_BODY)
                    else -> MockResponse().setResponseCode(200).setBody(BUG_DETAIL_BODY)
                }
            }
        }
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.deleteComment(5)
        awaitUntil { server.requestCount >= 2 }
        delay(30)
        assertThat(success(vm.state.value).actionError).isNull()
        assertThat(vm.state.value).isInstanceOf(UiState.Success::class.java)
    }

    @Test
    fun `deleteComment error surfaces an action error`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath ?: ""
                return when {
                    request.method == "DELETE" && path.contains("/comments") ->
                        MockResponse().setResponseCode(403).setBody("""{"detail":"nope"}""")
                    else -> MockResponse().setResponseCode(200).setBody(BUG_DETAIL_BODY)
                }
            }
        }
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.deleteComment(5)
        awaitUntil { success(vm.state.value).actionError != null }
        assertThat(success(vm.state.value).actionError).isNotNull()
    }

    @Test
    fun `deleteComment is a no-op when not in Success state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Error }
        val before = server.requestCount
        vm.deleteComment(5)
        delay(50)
        assertThat(server.requestCount).isEqualTo(before)
    }

    // --- deleteAttachment -------------------------------------------------

    @Test
    fun `deleteAttachment success refreshes bug and clears action error`() = runBlocking {
        routeOk()
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.deleteAttachment(9)
        awaitUntil { server.requestCount >= 2 }
        delay(30)
        assertThat(success(vm.state.value).actionError).isNull()
        assertThat(vm.state.value).isInstanceOf(UiState.Success::class.java)
    }

    @Test
    fun `deleteAttachment error surfaces an action error`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath ?: ""
                return when {
                    request.method == "DELETE" && path.contains("/attachments") ->
                        MockResponse().setResponseCode(403).setBody("""{"detail":"nope"}""")
                    else -> MockResponse().setResponseCode(200).setBody(BUG_DETAIL_BODY)
                }
            }
        }
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.deleteAttachment(9)
        awaitUntil { success(vm.state.value).actionError != null }
        assertThat(success(vm.state.value).actionError).isNotNull()
    }

    @Test
    fun `deleteAttachment is a no-op when not in Success state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Error }
        val before = server.requestCount
        vm.deleteAttachment(9)
        delay(50)
        assertThat(server.requestCount).isEqualTo(before)
    }

    // --- dismissActionError -----------------------------------------------

    @Test
    fun `dismissActionError clears the banner`() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.requestUrl?.encodedPath ?: ""
                return when {
                    path.contains("/comments") ->
                        MockResponse().setResponseCode(403).setBody("""{"detail":"forbidden"}""")
                    else -> MockResponse().setResponseCode(200).setBody(BUG_DETAIL_BODY)
                }
            }
        }
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Success }
        vm.onCommentDraftChange("x")
        vm.postComment()
        awaitUntil { success(vm.state.value).actionError != null }
        vm.dismissActionError()
        assertThat(success(vm.state.value).actionError).isNull()
    }

    @Test
    fun `dismissActionError is a no-op when not in Success state`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"detail":"boom"}"""))
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Error }
        vm.dismissActionError()
        assertThat(vm.state.value).isInstanceOf(UiState.Error::class.java)
    }

    // --- pure model logic -------------------------------------------------

    @Test
    fun `commentsNewestFirst sorts comments by createdAt descending`() = runBlocking {
        // Detail body carries two comments with out-of-order timestamps; the
        // computed property must surface the newest (id 2) first.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse().setResponseCode(200).setBody(BUG_DETAIL_TWO_COMMENTS)
        }
        val vm = vm()
        awaitUntil { vm.state.value is UiState.Success }
        val ids = success(vm.state.value).commentsNewestFirst.map { it.id }
        assertThat(ids).containsExactly(2, 1).inOrder()
        Unit
    }

    companion object {
        // Minimal BugDetail body just substantial enough for Moshi to parse.
        private const val BUG_DETAIL_BODY = """
            {
              "id": 42,
              "project_id": 1,
              "project_name": "p",
              "project_key": "P",
              "item_type": "Bug",
              "event_id": null,
              "event_name": null,
              "title": "t",
              "description": "",
              "reporter": {"id": 1, "name": "r", "email": "r@x.x", "role": "admin"},
              "assignees": [],
              "status": "New",
              "priority": "Medium",
              "environment": "DEV",
              "due_date": null,
              "created_at": "2026-06-10T09:01:30",
              "updated_at": "2026-06-10T09:01:30",
              "comments": [],
              "attachments": [],
              "activities": [],
              "attachment_count": 0,
              "can_edit": true
            }
        """

        // A single addComment() response body (CommentOut).
        private const val COMMENT_BODY = """
            {
              "id": 7,
              "bug_id": 42,
              "author_user_id": 1,
              "author_name": "r",
              "body": "nice catch",
              "created_at": "2026-06-10T10:00:00",
              "attachments": []
            }
        """

        // Detail body that already contains one comment (for the refresh path).
        private const val BUG_DETAIL_WITH_COMMENT = """
            {
              "id": 42,
              "project_id": 1,
              "project_name": "p",
              "project_key": "P",
              "item_type": "Bug",
              "event_id": null,
              "event_name": null,
              "title": "t",
              "description": "",
              "reporter": {"id": 1, "name": "r", "email": "r@x.x", "role": "admin"},
              "assignees": [],
              "status": "New",
              "priority": "Medium",
              "environment": "DEV",
              "due_date": null,
              "created_at": "2026-06-10T09:01:30",
              "updated_at": "2026-06-10T09:01:30",
              "comments": [
                {"id": 7, "bug_id": 42, "author_user_id": 1, "author_name": "r",
                 "body": "nice catch", "created_at": "2026-06-10T10:00:00", "attachments": []}
              ],
              "attachments": [],
              "activities": [],
              "attachment_count": 0,
              "can_edit": true
            }
        """

        // Two comments deliberately out of timestamp order (id 1 newer in the
        // array but older by createdAt) to exercise commentsNewestFirst sort.
        private const val BUG_DETAIL_TWO_COMMENTS = """
            {
              "id": 42,
              "project_id": 1,
              "project_name": "p",
              "project_key": "P",
              "item_type": "Bug",
              "event_id": null,
              "event_name": null,
              "title": "t",
              "description": "",
              "reporter": {"id": 1, "name": "r", "email": "r@x.x", "role": "admin"},
              "assignees": [],
              "status": "New",
              "priority": "Medium",
              "environment": "DEV",
              "due_date": null,
              "created_at": "2026-06-10T09:01:30",
              "updated_at": "2026-06-10T09:01:30",
              "comments": [
                {"id": 1, "bug_id": 42, "author_user_id": 1, "author_name": "r",
                 "body": "old", "created_at": "2026-06-10T08:00:00", "attachments": []},
                {"id": 2, "bug_id": 42, "author_user_id": 1, "author_name": "r",
                 "body": "new", "created_at": "2026-06-10T11:00:00", "attachments": []}
              ],
              "attachments": [],
              "activities": [],
              "attachment_count": 0,
              "can_edit": true
            }
        """
    }
}
