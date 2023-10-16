package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.WritableRegistryValue
import com.sun.jna.platform.win32.Advapi32Util

class LongRegistryValue internal constructor(parentKey: RegistryKey, name: String) :
    WritableRegistryValue<Long>(parentKey, name) {
    override val typeName: String = "String"

    override fun retrieveValue(): Long =
        Advapi32Util.registryGetLongValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)

    override fun write(value: Long) =
        Advapi32Util.registrySetLongValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name, value)
}

fun RegistryKey.longValue(name: String) = LongRegistryValue(this, name)
