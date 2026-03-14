package dev.deviceai.demo

import platform.Foundation.NSUserDefaults

internal actual object AppPrefs {
    actual fun getString(key: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(key)

    actual fun putString(key: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
    }
}
