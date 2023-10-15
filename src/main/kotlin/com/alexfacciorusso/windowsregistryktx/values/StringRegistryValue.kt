package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.ReadableRegistryValue
import com.alexfacciorusso.windowsregistryktx.Writable
import com.sun.jna.platform.win32.Advapi32Util

class StringRegistryValue internal constructor(parentKey: RegistryKey, name: String) :
    ReadableRegistryValue<String>(parentKey, name),
    Writable<String> {
    override val typeName: String = "String"

    override fun retrieveValue(): String =
        Advapi32Util.registryGetStringValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)

    override fun write(value: String) =
        Advapi32Util.registrySetStringValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name, value)
}

fun RegistryKey.stringValue(name: String) = StringRegistryValue(this, name)
