package com.alexfacciorusso.windowsregistryktx

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg.HKEYByReference

@JvmInline
value class CloseableHkeyByReference internal constructor(val hkeyReference: HKEYByReference) : AutoCloseable {

    override fun close() {
        Advapi32Util.registryCloseKey(hkeyReference.value)
    }
}

fun HKEYByReference.asClosable() = CloseableHkeyByReference(this)
