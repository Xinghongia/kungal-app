package com.kungal.app.ui.util

/**
 * 站点分类/版块原始键 → 中文显示名。
 * 与官方前端 apps/web/app/constants/topic.ts 的 KUN_TOPIC_CATEGORY / KUN_TOPIC_SECTION 一致；
 * 服务端返回英文键（如 t-practical），界面必须显示站点网页上的中文名。
 */
object TopicLabels {

    private val categories = mapOf(
        "galgame" to "Galgame",
        "technique" to "技术交流",
        "others" to "其它话题",
    )

    private val sections = mapOf(
        "g-walkthrough" to "攻略",
        "g-chatting" to "闲聊",
        "g-article" to "文章",
        "g-seeking" to "寻求资源",
        "g-news" to "资讯",
        "g-releases" to "新作消息",
        "g-other" to "其它",
        "t-crack" to "逆向工程",
        "t-web" to "Web",
        "t-languages" to "编程语言",
        "t-help" to "请求帮助",
        "t-linux" to "Linux",
        "t-practical" to "实用技术",
        "t-ai" to "AI",
        "t-android" to "Android",
        "t-adobe" to "Adobe",
        "t-algorithm" to "算法",
        "t-other" to "其它",
        "o-anime" to "动漫",
        "o-comics" to "漫画",
        "o-music" to "音乐",
        "o-novel" to "轻小说",
        "o-daily" to "日常",
        "o-essay" to "个人随笔",
        "o-forum" to "论坛相关",
        "o-patch" to "补丁网站",
        "o-other" to "其它",
    )

    /** 未知键按原样显示（站点可能新增版块，不吞内容）。 */
    fun category(key: String): String = categories[key] ?: key

    fun section(key: String): String = sections[key] ?: key

    /** 话题列表的分类筛选选项（null = 全部）。 */
    val filterOptions: List<Pair<String?, String>> = listOf(
        null to "全部话题",
        "galgame" to "Galgame",
        "technique" to "技术交流",
        "others" to "其它话题",
    )
}
