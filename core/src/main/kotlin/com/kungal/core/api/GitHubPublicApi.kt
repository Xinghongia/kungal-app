package com.kungal.core.api

import com.kungal.core.model.GitHubContributionDay
import com.kungal.core.model.GitHubContributions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * GitHub 公开页面读取（关于页用）：贡献日历与仓库 star 数。
 *
 * 不用 GitHub REST API：匿名额度是**每 IP 每小时 60 次**，移动网络多为共享出口，
 * 经常已经被别人用光（实测 2026-10-18 本机出口就是 0/60）。这里改为读取
 * 公开网页，每次打开关于页最多 6 个请求，并由调用方做 24 小时本地缓存。
 */
class GitHubPublicApi(
    private val http: OkHttpClient = defaultHttp(),
    private val webBase: String = DEFAULT_WEB_BASE,
) {
    companion object {
        const val DEFAULT_WEB_BASE = "https://github.com"
        const val USER_AGENT = "kungal-app/0.8.17 (Android)"

        fun defaultHttp(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        private val DAY_PATTERN = Regex("""data-date="(\d{4}-\d{2}-\d{2})"[^>]*data-level="(\d)"""")
        private val TOTAL_PATTERN = Regex("""([\d,]+)\s+contributions?\s+in the last year""")
        private val STAR_PATTERN = Regex(""""stargazerCount":(\d+)""")
    }

    /** 公开贡献日历；未登录视角只包含公开贡献。 */
    suspend fun contributions(user: String): GitHubContributions = withContext(Dispatchers.IO) {
        val html = fetchHtml("/users/$user/contributions")
        parseContributions(html)
    }

    /** 仓库 star 数；仓库不存在或不可见时返回 null。 */
    suspend fun repoStars(owner: String, repo: String): Int? = withContext(Dispatchers.IO) {
        val response = execute("/$owner/$repo")
        response.use { r ->
            if (r.code == 404) return@withContext null
            if (!r.isSuccessful) {
                throw IOException("GitHub 请求失败（HTTP ${r.code}）")
            }
            STAR_PATTERN.find(r.body.string())?.groupValues?.get(1)?.toIntOrNull()
        }
    }

    private fun fetchHtml(path: String): String = execute(path).use { r ->
        if (!r.isSuccessful) {
            throw IOException("GitHub 请求失败（HTTP ${r.code}）")
        }
        r.body.string()
    }

    private fun execute(path: String) = http.newCall(
        Request.Builder()
            .url("$webBase$path")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html")
            .build(),
    ).execute()

    private fun parseContributions(html: String): GitHubContributions {
        val total = TOTAL_PATTERN.find(html)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toIntOrNull()
            ?: 0
        val days = DAY_PATTERN.findAll(html)
            .map { match ->
                GitHubContributionDay(date = match.groupValues[1], level = match.groupValues[2].toInt())
            }
            .sortedBy { it.date }
            .mapNotNull { day ->
                runCatching { LocalDate.parse(day.date) }.getOrNull()?.let { day }
            }
            .toList()
        if (days.isEmpty()) return GitHubContributions(total = total)
        val firstDate = LocalDate.parse(days.first().date)
        val firstSunday = firstDate.minusDays((firstDate.dayOfWeek.value % 7).toLong())
        val weeks = mutableListOf<MutableList<GitHubContributionDay?>>()
        for (day in days) {
            val date = LocalDate.parse(day.date)
            val index = (ChronoUnit.DAYS.between(firstSunday, date) / 7).toInt()
            while (weeks.size <= index) {
                weeks += MutableList<GitHubContributionDay?>(7) { null }
            }
            val row = date.dayOfWeek.value % 7
            weeks[index][row] = day
        }
        return GitHubContributions(total = total, days = days, weeks = weeks)
    }
}
