package com.kungal.app

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
}
