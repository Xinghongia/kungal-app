package com.kungal.core

import com.kungal.core.net.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProblemParseTest {

    @Test
    fun `401 无凭证样本`() {
        val p = KungalJson.decodeFromString<Problem>(Fixtures.text("problem_401.json"))
        assertEquals(401, p.status)
        assertEquals("MISSING_CREDENTIAL", p.code)
        assertEquals("Missing credential", p.title)
        assertEquals("The request has no credentials.", p.detail)
        assertTrue(p.requestId.startsWith("req_"))
        assertTrue(p.requiresLogin)
    }

    @Test
    fun `404 样本`() {
        val p = KungalJson.decodeFromString<Problem>(Fixtures.text("topic_404.json"))
        assertEquals(404, p.status)
        assertEquals("NOT_FOUND", p.code)
        assertFalse(p.requiresLogin)
    }
}
