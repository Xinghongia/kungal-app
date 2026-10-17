package com.kungal.app.ui.toolset

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolsetUploadPolicyTest {

    @Test
    fun `每日额度与单文件上限按萌萌点计算`() {
        val quota = ToolsetUploadQuota(moemoepoint = 250, usedToday = 30 * UPLOAD_MB, bypass = false)
        assertEquals(350 * UPLOAD_MB, quota.dailyBudget)
        assertEquals(320 * UPLOAD_MB, quota.dailyRemaining)
        assertEquals(100 * UPLOAD_MB, quota.maxSingleFile)
    }

    @Test
    fun `萌萌点足够高时单文件上限提升但不超2GB`() {
        val rich = ToolsetUploadQuota(moemoepoint = 3000, usedToday = 0, bypass = false)
        assertEquals(300 * UPLOAD_MB, rich.maxSingleFile)
        val huge = ToolsetUploadQuota(moemoepoint = 1_000_000, usedToday = 0, bypass = false)
        assertEquals(MAX_LARGE_FILE_SIZE, huge.maxSingleFile)
    }

    @Test
    fun `白名单账号不受每日用量限制`() {
        val quota = ToolsetUploadQuota(moemoepoint = 0, usedToday = 500 * UPLOAD_MB, bypass = true)
        assertEquals(MAX_LARGE_FILE_SIZE, quota.dailyRemaining)
        assertEquals(MAX_LARGE_FILE_SIZE, quota.maxSingleFile)
        assertNull(checkToolsetUploadFile("tool.zip", 1024 * UPLOAD_MB, quota))
    }

    @Test
    fun `压缩格式不区分大小写`() {
        assertTrue(isValidToolsetArchiveName("tool.zip"))
        assertTrue(isValidToolsetArchiveName("TOOL.7Z"))
        assertTrue(isValidToolsetArchiveName("a.b.RaR"))
        assertFalse(isValidToolsetArchiveName("tool.tar.gz"))
        assertFalse(isValidToolsetArchiveName("tool.zip.exe"))
    }

    @Test
    fun `前置校验按服务端规则给出中文提示`() {
        val quota = ToolsetUploadQuota(moemoepoint = 100, usedToday = 0, bypass = false)
        assertEquals("仅支持 .7z、.zip、.rar 压缩格式", checkToolsetUploadFile("a.tar", 10, quota))
        assertEquals("文件为空，无法上传", checkToolsetUploadFile("a.zip", 0, quota))
        assertEquals(
            "单文件超过限制，最大 100 MB",
            checkToolsetUploadFile("a.zip", 150 * UPLOAD_MB, quota),
        )
        assertNull(checkToolsetUploadFile("a.zip", MAX_SMALL_FILE_SIZE, quota))
    }

    @Test
    fun `超出今日剩余额度时优先提示额度`() {
        val quota = ToolsetUploadQuota(moemoepoint = 0, usedToday = 90 * UPLOAD_MB, bypass = false)
        assertEquals(
            "超出今日可用上传额度，剩余 10 MB",
            checkToolsetUploadFile("a.zip", 20 * UPLOAD_MB, quota),
        )
    }

    @Test
    fun `超过2GB给最大限制提示`() {
        val quota = ToolsetUploadQuota(moemoepoint = 5000, usedToday = 0, bypass = false)
        assertEquals(
            "文件超过最大限制 2048 MB",
            checkToolsetUploadFile("a.zip", MAX_LARGE_FILE_SIZE + 1, quota),
        )
    }

    @Test
    fun `MB 文案去掉多余小数`() {
        assertEquals("100", formatUploadMb(100 * UPLOAD_MB))
        assertEquals("0.5", formatUploadMb(UPLOAD_MB / 2))
        assertEquals("0", formatUploadMb(0))
    }
}
