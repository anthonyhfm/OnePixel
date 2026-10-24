package org.onepixel.compat

import android.content.ContentResolver
import android.provider.Settings

object SettingsCompat {
    @JvmStatic
    fun secureInt(
        resolver: ContentResolver,
        name: String,
        default: Int
    ): Int {
        return try {
            Settings.Secure.getInt(resolver, name, default)
        } catch (_: SecurityException) {
            0
        }
    }

    @JvmStatic
    fun systemInt(
        resolver: ContentResolver,
        name: String,
        default: Int
    ): Int {
        return try {
            Settings.System.getInt(resolver, name, default)
        } catch (_: SecurityException) {
            0
        }
    }

    @JvmStatic
    fun globalInt(
        resolver: ContentResolver,
        name: String,
        default: Int
    ): Int {
        return try {
            Settings.Global.getInt(resolver, name, default)
        } catch (_: SecurityException) {
            0
        }
    }
}
