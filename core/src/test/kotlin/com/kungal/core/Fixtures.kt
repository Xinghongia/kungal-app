package com.kungal.core

/** 读取 core/src/test/resources/fixtures 下的真实响应样本（2026-10-17 抓取自 kungal.com）。 */
object Fixtures {
    fun text(name: String): String =
        Fixtures::class.java.classLoader.getResourceAsStream("fixtures/$name")
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            ?: error("fixture 不存在: $name")
}
