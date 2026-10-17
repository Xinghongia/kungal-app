package com.kungal.core

import com.kungal.core.api.EditApi
import com.kungal.core.api.MeApi
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EditApiTest {

    private lateinit var server: MockWebServer
    private lateinit var edit: EditApi
    private lateinit var me: MeApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        val client = KungalClient(baseUrl = baseUrl, session = session)
        edit = EditApi(client)
        me = MeApi(client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String, code: Int = 200, etag: String? = null) = MockResponse.Builder()
        .code(code)
        .addHeader("Content-Type", "application/json")
        .apply { etag?.let { addHeader("ETag", it) } }
        .body(body)
        .build()

    @Test
    fun `编辑表单解析字段模式词汇表与当前值`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_edit_form.json")))
        val form = edit.form("229")
        assertEquals("229", form.workId)
        assertEquals(18, form.fields.size)

        val titles = form.fields.first { it.key == "catalog.work.titles" }
        assertEquals("list", titles.fieldType)
        assertEquals("items", titles.diffHint)
        assertEquals("object", titles.element?.elementType)
        assertEquals(
            listOf("lang", "title", "kind", "latin"),
            titles.element?.members?.map { it.key },
        )
        assertEquals("enum", titles.element?.members?.first { it.key == "kind" }?.memberType)

        val cover = form.fields.first { it.key == "catalog.work.covers" }
        assertEquals("image", cover.diffHint)
        assertEquals(
            "imagehash",
            cover.element?.members?.first { it.key == "image_hash" }?.memberType,
        )

        val rating = form.fields.first { it.key == "catalog.work.content_rating" }
        assertEquals("int", rating.encoding)
        assertEquals(
            listOf("all_ages", "sensitive", "r18"),
            form.vocabularies.first { it.vocabulary == "content_rating" }.values.map { it.value },
        )

        assertEquals("示例作品", form.fieldValues["catalog.work.display_name"]?.jsonPrimitive?.content)
        assertEquals(2, form.fieldValues["catalog.work.content_rating"]?.jsonPrimitive?.content?.toInt())
        assertEquals(
            "1333",
            (form.fieldValues["catalog.work.tag_ids"] as JsonArray)[0].jsonPrimitive.content,
        )
        assertEquals("/api/v1/works/229/edit-form", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `作品提案列表解析摘要与游标`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_edit_proposals.json")))
        val page = edit.proposals("229", state = "open", limit = 20)
        assertEquals(2, page.items.size)
        assertEquals("cursor-2", page.nextCursor)
        assertEquals("1207", page.items[0].id)
        assertEquals("补充系列信息", page.items[0].note)
        assertTrue(page.items[0].viewer?.canWithdraw == true)
        assertEquals("示例作品", page.items[0].workSummary.localized["zh-Hans"]?.value)
        assertTrue(page.items[1].viewer?.canDecide == true)
        assertFalse(page.items[1].viewer?.isProposer == true)

        val request = server.takeRequest()
        assertEquals("/api/v1/works/229/edit-proposals", request.url.encodedPath)
        assertEquals("open", request.url.queryParameter("state"))
        assertEquals("20", request.url.queryParameter("limit"))
    }

    @Test
    fun `我的提案与审阅队列传筛选参数`() = runBlocking {
        server.enqueue(ok("""{"object":"list","items":[]}"""))
        val mine = edit.myProposals(workId = "229", state = "merged", limit = 10)
        assertTrue(mine.items.isEmpty())
        val mineRequest = server.takeRequest()
        assertEquals("/api/v1/me/edit-proposals", mineRequest.url.encodedPath)
        assertEquals("229", mineRequest.url.queryParameter("work_id"))
        assertEquals("merged", mineRequest.url.queryParameter("state"))

        server.enqueue(ok("""{"object":"list","items":[]}"""))
        edit.reviewQueue(state = "open", cursor = "c1")
        val queue = server.takeRequest()
        assertEquals("/api/v1/edit-proposals", queue.url.encodedPath)
        assertEquals("c1", queue.url.queryParameter("cursor"))
    }

    @Test
    fun `提交提案带幂等键并解析合并结果`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_edit_merged_result.json"), code = 201))
        val patch = buildJsonObject {
            put("catalog.work.release_date", "2024-06-28")
        }
        val result = edit.createProposal("229", patch, note = "修正发售日", idempotencyKey = "ik-edit")
        assertEquals("merged", result.proposal.state)
        assertEquals("1209", result.proposal.id)
        assertEquals(3, result.revision?.seq)
        assertEquals("merged", result.revision?.revisionAction)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/works/229/edit-proposals", request.url.encodedPath)
        assertEquals("ik-edit", request.headers["Idempotency-Key"])
        val body = request.body!!.utf8()
        assertTrue(body.contains("\"catalog.work.release_date\""))
        assertTrue(body.contains("修正发售日"))
    }

    @Test
    fun `提案详情读取 ETag 并用 If-Match 审阅`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_edit_proposal.json"), etag = "\"p1207.1\""))
        val detail = edit.proposal("1207")
        assertEquals("\"p1207.1\"", detail.etag)
        assertEquals("1207", detail.body.id)
        assertEquals(2, detail.body.effectivePatch["catalog.work.series_ids"]?.jsonArraySize())
        assertEquals(1, detail.body.amendments.size)
        assertEquals("系列补全", detail.body.amendments[0].note)
        assertTrue(detail.body.viewer?.canAmend == true)
        assertEquals(
            "/api/v1/edit-proposals/1207",
            server.takeRequest().url.encodedPath,
        )

        server.enqueue(ok(Fixtures.text("galgame_edit_proposal.json"), etag = "\"p1207.2\""))
        val merged = edit.updateProposal("1207", "merged", note = "通过", ifMatch = "\"p1207.1\"")
        assertEquals("\"p1207.2\"", merged.etag)
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        assertEquals("\"p1207.1\"", patch.headers["If-Match"])
        assertTrue(patch.body!!.utf8().contains("\"state\":\"merged\""))
        assertTrue(patch.body!!.utf8().contains("通过"))
    }

    @Test
    fun `修正提案发送 set 与 unset 并读取新 ETag`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_edit_amendment.json"), code = 201, etag = "\"p1207.3\""))
        val result = edit.amendProposal(
            proposalId = "1207",
            set = buildJsonObject { put("catalog.work.display_name", "示例作品（终）") },
            unset = listOf("catalog.work.series_ids"),
            note = "去掉多余字段",
            ifMatch = "\"p1207.2\"",
            idempotencyKey = "ik-amend",
        )
        assertEquals("\"p1207.3\"", result.etag)
        assertEquals(2, result.body.seq)
        assertEquals("去掉多余字段", result.body.note)

        val request = server.takeRequest()
        assertEquals("/api/v1/edit-proposals/1207/amendments", request.url.encodedPath)
        assertEquals("ik-amend", request.headers["Idempotency-Key"])
        assertEquals("\"p1207.2\"", request.headers["If-Match"])
        val body = request.body!!.utf8()
        assertTrue(body.contains("\"catalog.work.display_name\""))
        assertTrue(body.contains("\"catalog.work.series_ids\""))
        assertTrue(body.contains("\"unset\""))
    }

    @Test
    fun `修订列表差异与回滚`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_edit_revisions.json")))
        val revisions = edit.revisions("229", page = 1, limit = 100)
        assertEquals(2, revisions.total)
        assertEquals(2, revisions.items[0].seq)
        assertEquals("merged", revisions.items[0].revisionAction)
        assertEquals(listOf("galgame.game.series_id"), revisions.items[0].changedFields)
        assertEquals("Sakurakouji", revisions.items[0].actor.name)
        assertEquals("/api/v1/works/229/edit-revisions", server.takeRequest().url.encodedPath)

        server.enqueue(ok(Fixtures.text("galgame_edit_diff.json")))
        val diff = edit.diff("229", fromSeq = 1, toSeq = 2)
        assertEquals(2, diff.fieldChanges.size)
        assertEquals("catalog.work.covers", diff.fieldChanges[0].key)
        val from = diff.fieldChanges[0].from as JsonArray
        assertEquals(1, from.size)
        assertEquals(
            "64f904b81dcf9c4a24e235466be8cdc3cd7eab31b91bfe29c5d6f1bdb95af4e3",
            from[0].jsonObject["image_hash"]?.jsonPrimitive?.content,
        )
        val diffRequest = server.takeRequest()
        assertEquals("1", diffRequest.url.queryParameter("from_seq"))
        assertEquals("2", diffRequest.url.queryParameter("to_seq"))

        server.enqueue(ok(Fixtures.text("galgame_edit_revert.json"), code = 201))
        val revert = edit.revert("229", toSeq = 1, note = "回滚到版本 1", idempotencyKey = "ik-revert")
        assertEquals(4, revert.revision?.seq)
        assertEquals("reverted", revert.revision?.revisionAction)
        val revertRequest = server.takeRequest()
        assertEquals("/api/v1/works/229/edit-reverts", revertRequest.url.encodedPath)
        assertEquals("ik-revert", revertRequest.headers["Idempotency-Key"])
        assertTrue(revertRequest.body!!.utf8().contains("\"to_seq\":1"))
    }

    @Test
    fun `编辑图片上传带 preset 并返回哈希`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_edit_image.json"), code = 201))
        val image = edit.uploadImage(
            bytes = byteArrayOf(1, 2, 3),
            fileName = "cover.png",
            contentType = "image/png",
            preset = "cover",
            idempotencyKey = "ik-image",
        )
        assertEquals(1200, image.width)
        assertTrue(image.hash.isNotBlank())

        val request = server.takeRequest()
        assertEquals("/api/v1/work-edit-images", request.url.encodedPath)
        assertEquals("ik-image", request.headers["Idempotency-Key"])
        val body = request.body!!.utf8()
        assertTrue(body.contains("name=\"preset\""))
        assertTrue(body.contains("cover"))
        assertTrue(body.contains("name=\"file\""))
    }

    @Test
    fun `我的权限列表`() = runBlocking {
        server.enqueue(
            ok("""{"permissions":["galgame.edit_proposal.review","topic.edit"]}"""),
        )
        val permissions = me.permissions()
        assertEquals(
            listOf("galgame.edit_proposal.review", "topic.edit"),
            permissions.permissions,
        )
        assertEquals("/api/v1/me/permissions", server.takeRequest().url.encodedPath)
    }
}

private fun JsonElement.jsonArraySize(): Int = (this as? JsonArray)?.size ?: -1
