package com.alexfacciorusso.windowsregistryktx

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg

@JvmInline
value class CloseableHkey(val hkey: WinReg.HKEY) : AutoCloseable {
    override fun close() {
        Advapi32Util.registryCloseKey(hkey)
    }
}