package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.RegistryTestHelper
import com.alexfacciorusso.windowsregistryktx.ReadableRegistryValue
import com.alexfacciorusso.windowsregistryktx.WritableRegistryValue

class BooleanRegistryValueTest : BaseRegistryValueTest<Boolean>("Boolean") {
    override val testContents: List<Boolean> = listOf(true, false)

    override fun RegistryKey.getValue(valueName: String): ReadableRegistryValue<Boolean> =
        booleanValue(valueName)

    override fun RegistryTestHelper.readResultData(valueName: String): Boolean = readInt(valueName) != 0

    override fun RegistryKey.getWritable(valueName: String): WritableRegistryValue<Boolean> = booleanValue(valueName)

    override fun RegistryTestHelper.writeDataToKey(valueName: String, value: Boolean) {
        writeInt(valueName, if (value) 1 else 0)
    }
}