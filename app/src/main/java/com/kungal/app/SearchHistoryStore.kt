package com.kungal.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.searchHistoryDataStore by preferencesDataStore(name = "search_history")

/**
 * 搜索历史：与网页一致，去重后置、最多 20 条，可单条删除/清空。
 * 只存搜索词，不含结果或个人信息。
 */
class SearchHistoryStore(private val context: Context) {

    private val key = stringPreferencesKey("queries")

    val flow: Flow<List<String>> = context.searchHistoryDataStore.data.map { prefs ->
        prefs[key]?.let { raw ->
            runCatching {
                Json.decodeFromString(ListSerializer(String.serializer()), raw)
            }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    suspend fun add(query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        context.searchHistoryDataStore.edit { prefs ->
            val current = prefs[key]?.let { raw ->
                runCatching {
                    Json.decodeFromString(ListSerializer(String.serializer()), raw)
                }.getOrDefault(emptyList())
            } ?: emptyList()
            val next = (current.filterNot { it == q } + q).takeLast(20)
            prefs[key] = Json.encodeToString(ListSerializer(String.serializer()), next)
        }
    }

    suspend fun remove(query: String) {
        context.searchHistoryDataStore.edit { prefs ->
            val current = prefs[key]?.let { raw ->
                runCatching {
                    Json.decodeFromString(ListSerializer(String.serializer()), raw)
                }.getOrDefault(emptyList())
            } ?: emptyList()
            prefs[key] = Json.encodeToString(
                ListSerializer(String.serializer()),
                current.filterNot { it == query },
            )
        }
    }

    suspend fun clear() {
        context.searchHistoryDataStore.edit { it.remove(key) }
    }
}
