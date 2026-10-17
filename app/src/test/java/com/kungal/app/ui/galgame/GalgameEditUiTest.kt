package com.kungal.app.ui.galgame

import com.kungal.core.model.proposalStateLabel
import com.kungal.core.model.revisionActionLabel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

class GalgameEditUiTest {

    @Test
    fun `编辑字段标签与分组`() {
        assertEquals("标题与别名", editFieldLabel("catalog.work.titles"))
        assertEquals("隐藏的角色", editFieldLabel("catalog.work.roster.suppressed"))
        assertEquals("相关链接", editFieldLabel("catalog.work.links"))
        assertEquals(EditGroup.TITLES, editGroupOf("catalog.work.display_name"))
        assertEquals(EditGroup.RELATIONS, editGroupOf("catalog.work.tag_ids"))
        assertEquals(EditGroup.CAST, editGroupOf("catalog.work.credits"))
        assertEquals(EditGroup.IMAGES, editGroupOf("catalog.work.covers"))
        assertEquals(EditGroup.OTHER, editGroupOf("galgame.game.unknown"))
    }

    @Test
    fun `字段值摘要覆盖标题列表开关与分级`() {
        val titles = Json.parseToJsonElement(
            """[{"lang":"ja","title":"サンプル","kind":0},{"lang":"zh-Hans","title":"示例","kind":1}]""",
        )
        assertEquals(
            "サンプル（日语·官方名）、示例（简体中文·别名）",
            editValueSummary("catalog.work.titles", titles),
        )
        assertEquals("是", editValueSummary("catalog.work.display_nsfw", JsonPrimitive(true)))
        assertEquals("否", editValueSummary("catalog.work.display_nsfw", JsonPrimitive("false")))
        assertEquals("R18", editValueSummary("catalog.work.content_rating", JsonPrimitive(2)))
        assertEquals("敏感", editValueSummary("catalog.work.content_rating", JsonPrimitive("sensitive")))
        assertEquals("日语", editValueSummary("catalog.work.olang", JsonPrimitive("ja")))
        assertEquals(
            "a、b",
            editValueSummary(
                "catalog.work.links",
                Json.parseToJsonElement("""["a","b"]"""),
            ),
        )
        assertEquals("（未设置）", editValueSummary("catalog.work.display_name", null))
        assertEquals("（空）", editValueSummary("catalog.work.links", Json.parseToJsonElement("[]")))
    }

    @Test
    fun `图片地址与枚举标签`() {
        val hash = "64f904b81dcf9c4a24e235466be8cdc3cd7eab31b91bfe29c5d6f1bdb95af4e3"
        assertEquals(
            "https://image.kungal.iloveren.link/64/f9/$hash.webp",
            editImageUrl(hash),
        )
        assertEquals("简体中文", editLanguageLabel("zh-Hans"))
        assertEquals("开发商", editLabelKindLabel(2))
        assertEquals("主角", editRosterKindLabel(1))
        assertEquals("严重", editSpoilerLevelLabel(2))
        assertEquals("脚本", editCreditRoleLabel(247))
        assertEquals("露骨", editSexualLabel(2))
        assertEquals("血腥", editViolenceLabel(2))
        assertEquals(2, contentRatingCode(JsonPrimitive("r18")))
        assertEquals(1, contentRatingCode(JsonPrimitive(1)))
    }

    @Test
    fun `提案状态与修订动作中文映射`() {
        assertEquals("待审核", proposalStateLabel("open"))
        assertEquals("已合并", proposalStateLabel("merged"))
        assertEquals("已拒绝", proposalStateLabel("declined"))
        assertEquals("已撤回", proposalStateLabel("withdrawn"))
        assertEquals("回滚", revisionActionLabel("reverted"))
        assertEquals("直接编辑", revisionActionLabel("direct"))
    }
}
