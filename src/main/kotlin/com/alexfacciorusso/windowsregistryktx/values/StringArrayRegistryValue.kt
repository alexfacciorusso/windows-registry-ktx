package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.ReadableRegistryValue
import com.alexfacciorusso.windowsregistryktx.Writable
import com.sun.jna.platform.win32.Advapi32Util

class StringArrayRegistryValue internal constructor(parentKey: RegistryKey, name: String) :
    ReadableRegistryValue<List<String>>(parentKey, name), Writable<List<String>> {
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
