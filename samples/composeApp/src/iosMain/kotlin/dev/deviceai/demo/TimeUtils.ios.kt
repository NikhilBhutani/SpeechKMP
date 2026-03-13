package dev.deviceai.demo

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun currentTimeMs(): Long =
    (NSDate().timeIntervalSince1970() * 1000.0).toLong()
