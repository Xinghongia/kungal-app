package com.kungal.app.ui.activity

import com.kungal.core.model.ActivityRating
import com.kungal.core.model.FeedActivity
import com.kungal.core.model.LocalizedText
import com.kungal.core.model.WorkRef
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityUiTest {

    private val work = WorkRef(
        id = "31",
        displayName = "恋×シンアイ彼女",
        localized = mapOf("zh-Hans" to LocalizedText("想要传达给你的爱恋")),
    )

    @Test
    fun `动态类型中文标签与兜底`() {
        assertEquals("新话题", activityTypeLabel("topic_creation"))
        assertEquals("Galgame 资源", activityTypeLabel("galgame_resource_creation"))
        assertEquals("工具资源", activityTypeLabel("toolset_resource_creation"))
        assertEquals("future_type", activityTypeLabel("future_type"))
        assertEquals(22, ActivityTypeOptions.size)
    }

    @Test
    fun `摘要压缩为单行并去掉 Markdown 标记`() {
        assertEquals(
            "第一段 第二段",
            activityInlineText("**第一段**\n\n[第二段](https://example.com)"),
        )
        assertEquals(
            "第一段\n\n第二段",
            activityBodyText("**第一段**\n\n第二段"),
        )
    }

    @Test
    fun `作品动态摘要使用中文名`() {
        assertEquals(
            "在《想要传达给你的爱恋》发布了下载资源",
            activitySummaryText(
                FeedActivity(activityType = "galgame_resource_creation", work = work),
            ),
        )
        assertEquals(
            "编辑了《想要传达给你的爱恋》",
            activitySummaryText(FeedActivity(activityType = "galgame_edit", work = work)),
        )
        assertEquals(
            "对《想要传达给你的爱恋》提出了更新请求",
            activitySummaryText(FeedActivity(activityType = "galgame_pr_creation", work = work)),
        )
    }

    @Test
    fun `评分摘要有短评时带在作品名后`() {
        assertEquals(
            "想要传达给你的爱恋 · 很推荐",
            activitySummaryText(
                FeedActivity(
                    activityType = "galgame_rating_creation",
                    work = work,
                    galgameRating = ActivityRating(shortSummary = "很推荐"),
                ),
            ),
        )
        assertEquals(
            "想要传达给你的爱恋",
            activitySummaryText(
                FeedActivity(
                    activityType = "galgame_rating_creation",
                    work = work,
                    galgameRating = ActivityRating(shortSummary = ""),
                ),
            ),
        )
    }

    @Test
    fun `其他动态回退到服务端摘录`() {
        assertEquals(
            "大学怎么比高中还难受",
            activitySummaryText(
                FeedActivity(activityType = "topic_creation", excerptMarkdown = "**大学**怎么比高中还难受"),
            ),
        )
    }

    @Test
    fun `更新日志与待办文案`() {
        assertEquals("增加功能", updateLogChangeTypeLabel("feat"))
        assertEquals("错误修复", updateLogChangeTypeLabel("fix"))
        assertEquals("mystery", updateLogChangeTypeLabel("mystery"))
        assertEquals("待处理", todoStateLabel("pending"))
        assertEquals("进行中", todoStateLabel("in_progress"))
        assertEquals("补丁站", todoProjectLabel("patch"))
    }
}
