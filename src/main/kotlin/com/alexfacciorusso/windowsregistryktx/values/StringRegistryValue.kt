package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.RegistryValue
import com.sun.jna.platform.win32.Advapi32Util

class StringRegistryValue(parentKey: RegistryKey, name: String) : RegistryValue<String>(parentKey, name) {
    override val typeName: String = "String"

    override fun retrieveValue(): String =
        Advapi32Util.registryGetStringValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)

    override fun write(value: String) =
        Advapi32Util.registrySetStringValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name, value)
}

fun RegistryKey.stringValue(name: String) = StringRegistryValue(this, name)
