package com.kungal.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kungal.core.model.GitHubContributions
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.githubProfileDataStore by preferencesDataStore(name = "github_profile")

/** GitHub 公开资料缓存：贡献日历 + 各仓库 star 数，24 小时内不重复请求。 */
@Serializable
data class GitHubProfileSnapshot(
    val fetchedAt: Long = 0L,
    val contributions: GitHubContributions? = null,
    val stars: Map<String, Int> = emptyMap(),
)

const val GITHUB_CACHE_TTL_MS = 24L * 60 * 60 * 1000

fun isGitHubCacheFresh(fetchedAt: Long, now: Long): Boolean =
    fetchedAt > 0 && now >= fetchedAt && now - fetchedAt < GITHUB_CACHE_TTL_MS

class GitHubProfileStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val keySnapshot = stringPreferencesKey("snapshot")

    suspend fun load(): GitHubProfileSnapshot? {
        val raw = context.githubProfileDataStore.data.first()[keySnapshot] ?: return null
        return runCatching { json.decodeFromString(GitHubProfileSnapshot.serializer(), raw) }.getOrNull()
    }

    suspend fun save(snapshot: GitHubProfileSnapshot) {
        context.githubProfileDataStore.edit { prefs ->
            prefs[keySnapshot] = json.encodeToString(GitHubProfileSnapshot.serializer(), snapshot)
        }
    }

    suspend fun clear() {
        context.githubProfileDataStore.edit { it.clear() }
    }
}
