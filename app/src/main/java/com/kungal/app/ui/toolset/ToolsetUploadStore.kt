package com.kungal.app.ui.toolset

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kungal.core.KungalJson
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

private val Context.toolsetUploadDataStore by preferencesDataStore(name = "toolset_uploads")

/**
 * 未完成 / 已完成的上传记录（对应网页 localStorage 的 `kungal:toolset-upload-resume:{id}`）。
 * 存 SAF Uri：选择文件时已申请持久读权限，应用重启后仍可直接续传。
 */
@Serializable
data class ToolsetUploadRecord(
    val id: String,
    val name: String,
    val size: Long,
    val uri: String = "",
    val progress: Int = 0,
    val updatedAt: Long = 0,
    val completed: Boolean = false,
)

class ToolsetUploadStore(private val context: Context) {

    private val serializer = ListSerializer(ToolsetUploadRecord.serializer())

    private fun key(toolsetId: String) = stringPreferencesKey("toolset_$toolsetId")

    private fun decode(raw: String?): List<ToolsetUploadRecord> =
        raw?.let {
            runCatching { KungalJson.decodeFromString(serializer, it) }.getOrNull()
        }.orEmpty()

    private fun encode(records: List<ToolsetUploadRecord>): String =
        KungalJson.encodeToString(serializer, records)

    suspend fun list(toolsetId: String): List<ToolsetUploadRecord> {
        val raw = context.toolsetUploadDataStore.data.first()[key(toolsetId)] ?: return emptyList()
        return decode(raw)
    }

    suspend fun upsert(toolsetId: String, record: ToolsetUploadRecord) {
        context.toolsetUploadDataStore.edit { prefs ->
            val current = decode(prefs[key(toolsetId)])
            val next = (current.filterNot { it.id == record.id } + record)
                .sortedByDescending { it.updatedAt }
            prefs[key(toolsetId)] = encode(next)
        }
    }

    suspend fun remove(toolsetId: String, uploadId: String) {
        context.toolsetUploadDataStore.edit { prefs ->
            val current = decode(prefs[key(toolsetId)])
            prefs[key(toolsetId)] = encode(current.filterNot { it.id == uploadId })
        }
    }
}
