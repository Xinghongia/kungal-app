package com.kungal.app.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object AndroidActions {

    fun openInBrowser(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(context, "没有可用的浏览器", Toast.LENGTH_SHORT).show()
        }
    }
}
