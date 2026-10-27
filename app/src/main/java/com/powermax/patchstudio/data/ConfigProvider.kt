package com.powermax.patchstudio.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.powermax.patchstudio.core.TargetApps

/** Expose en lecture seule la config : content://com.powermax.patchstudio.config/<appId>. */
class ConfigProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val appId = uri.lastPathSegment ?: return null
        if (TargetApps.byId(appId) == null) return null
        val ctx = context ?: return null
        val cursor = MatrixCursor(arrayOf("key", "value"))
        for ((k, v) in ConfigRepository.get(ctx).current(appId).toMap()) cursor.addRow(arrayOf<Any>(k, v))
        return cursor
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
