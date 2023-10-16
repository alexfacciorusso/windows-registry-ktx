package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.RegistryTestHelper
import com.alexfacciorusso.windowsregistryktx.ReadableRegistryValue
import com.alexfacciorusso.windowsregistryktx.WritableRegistryValue

class StringArrayRegistryValueTest : BaseRegistryValueTest<List<String>>("StringArray") {
    override val testContents: List<List<String>> = listOf(listOf("test1", "test2", "test3"))

    override fun RegistryKey.getValue(valueName: String): ReadableRegistryValue<List<String>> =
        stringArrayValue(valueName)

    override fun RegistryTestHelper.readResultData(valueName: String): List<String> =
        readStringArray(valueName).toList()

    override fun RegistryKey.getWritable(valueName: String): WritableRegistryValue<List<String>> = stringArrayValue(valueName)

    override fun RegistryTestHelper.writeDataToKey(valueName: String, value: List<String>) {
        writeStringArray(valueName, value.toTypedArray())
    }
}