package com.kungal.app.ui.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.kungal.app.ui.components.AppMessages

object AndroidActions {

    fun openInBrowser(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            AppMessages.show("没有可用的浏览器")
        }
    }

    fun copyText(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        AppMessages.show("已复制")
    }

    /** 返回是否成功拉起分享面板（失败时已通过 AppMessages 提示）。 */
    fun shareText(context: Context, text: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "分享"))
            true
        } catch (_: Exception) {
            AppMessages.show("没有可用的分享应用")
            false
        }
    }
}
