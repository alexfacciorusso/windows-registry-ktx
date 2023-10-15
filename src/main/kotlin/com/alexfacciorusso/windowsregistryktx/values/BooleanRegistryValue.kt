package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.ReadableRegistryValue
import com.alexfacciorusso.windowsregistryktx.Writable

class BooleanRegistryValue internal constructor(parentKey: RegistryKey, name: String) : ReadableRegistryValue<Boolean>(parentKey, name),
    Writable<Boolean> {

    private val intRegistryValue = IntRegistryValue(parentKey, name)

    override val typeName: String = "Boolean"

    override fun retrieveValue(): Boolean =
        intRegistryValue.readOrThrow() != 0

    override fun write(value: Boolean) =
        intRegistryValue.write(if (value) 1 else 0)
}

fun RegistryKey.booleanValue(name: String) = BooleanRegistryValue(this, name)
