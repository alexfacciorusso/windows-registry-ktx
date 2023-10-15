package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.ReadableRegistryValue
import com.sun.jna.platform.win32.Advapi32Util

class AnyRegistryValue internal constructor(parentKey: RegistryKey, name: String) :
    ReadableRegistryValue<Any>(parentKey, name) {
    override val typeName: String = "Any"

    override fun retrieveValue(): Any =
        Advapi32Util.registryGetValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)
}

fun RegistryKey.anyValue(name: String) = AnyRegistryValue(this, name)
