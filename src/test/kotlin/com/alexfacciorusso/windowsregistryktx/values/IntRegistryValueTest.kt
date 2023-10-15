package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.RegistryTestHelper
import com.alexfacciorusso.windowsregistryktx.ReadableRegistryValue
import com.alexfacciorusso.windowsregistryktx.Writable

class IntRegistryValueTest : BaseRegistryValueTest<Int>("Int") {
    override val testContents: List<Int> = listOf(1, 2, 3)

    override fun RegistryKey.getValue(valueName: String): ReadableRegistryValue<Int> =
        intValue(valueName)

    override fun RegistryTestHelper.readResultData(valueName: String): Int = readInt(valueName)

    override fun RegistryKey.getWritable(valueName: String): Writable<Int> = intValue(valueName)

    override fun RegistryTestHelper.writeDataToKey(valueName: String, value: Int) {
        writeInt(valueName, value)
    }
}