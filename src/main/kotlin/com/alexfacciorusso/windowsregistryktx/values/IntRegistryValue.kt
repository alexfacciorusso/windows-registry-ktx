package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.WritableRegistryValue
import com.sun.jna.platform.win32.Advapi32Util

class IntRegistryValue internal constructor(parentKey: RegistryKey, name: String) :
    WritableRegistryValue<Int>(parentKey, name) {
    override val typeName: String = "Int"

    override fun retrieveValue(): Int =
        Advapi32Util.registryGetIntValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)

    override fun write(value: Int) =
        Advapi32Util.registrySetIntValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name, value)
}

fun RegistryKey.intValue(name: String) = IntRegistryValue(this, name)
