package com.kungal.app

import com.kungal.core.model.GitHubContributions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubProfileStoreTest {

    private val hour = 60L * 60 * 1000

    @Test
    fun `24 小时内的缓存有效`() {
        val now = 1_000_000_000L
        assertTrue(isGitHubCacheFresh(now, now + 23 * hour))
    }

    @Test
    fun `超过 24 小时的缓存失效`() {
        val now = 1_000_000_000L
        assertFalse(isGitHubCacheFresh(now, now + 25 * hour))
    }

    @Test
    fun `没有抓取时间视为失效`() {
        assertFalse(isGitHubCacheFresh(0L, 1_000_000_000L))
    }

    @Test
    fun `缓存缺少项目 star 时不可用`() {
        val now = 1_000_000_000L
        val snapshot = GitHubProfileSnapshot(
            fetchedAt = now,
            contributions = GitHubContributions(total = 10),
            stars = mapOf("Kirameku" to 248),
        )
        assertFalse(isGitHubCacheUsable(snapshot, now + hour, listOf("Kirameku", "kungal-app")))
        assertTrue(isGitHubCacheUsable(snapshot, now + hour, listOf("Kirameku")))
    }

    @Test
    fun `缓存缺少贡献图时不可用`() {
        val now = 1_000_000_000L
        val snapshot = GitHubProfileSnapshot(
            fetchedAt = now,
            contributions = null,
            stars = mapOf("Kirameku" to 248),
        )
        assertFalse(isGitHubCacheUsable(snapshot, now + hour, listOf("Kirameku")))
    }
}
