package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.WritableRegistryValue
import com.sun.jna.platform.win32.Advapi32Util

class StringArrayRegistryValue internal constructor(parentKey: RegistryKey, name: String) :
    WritableRegistryValue<List<String>>(parentKey, name) {
    override val typeName: String = "StringArray"

    override fun retrieveValue(): List<String> =
        Advapi32Util.registryGetStringArray(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name).toList()

    override fun write(value: List<String>) =
        Advapi32Util.registrySetStringArray(
            parentKey.rootHandle,
            parentKey.pathWithoutRoot.toString(),
            name,
            value.toTypedArray()
        )
}

fun RegistryKey.stringArrayValue(name: String) = StringArrayRegistryValue(this, name)
