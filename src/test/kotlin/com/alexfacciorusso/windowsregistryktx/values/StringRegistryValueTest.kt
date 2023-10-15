package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.RegistryTestHelper
import com.alexfacciorusso.windowsregistryktx.ReadableRegistryValue
import com.alexfacciorusso.windowsregistryktx.Writable

class StringRegistryValueTest : BaseRegistryValueTest<String>("String") {
    override val testContents: List<String> = listOf("test1", "test2", "test3")

    override fun RegistryKey.getValue(valueName: String): ReadableRegistryValue<String> =
        stringValue(valueName)

    override fun RegistryTestHelper.readResultData(valueName: String): String = readString(valueName)

    override fun RegistryKey.getWritable(valueName: String): Writable<String> = stringValue(valueName)

    override fun RegistryTestHelper.writeDataToKey(valueName: String, value: String) {
        writeString(valueName, value)
    }
}