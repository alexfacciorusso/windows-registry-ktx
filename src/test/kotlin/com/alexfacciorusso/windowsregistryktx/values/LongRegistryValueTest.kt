package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.RegistryTestHelper
import com.alexfacciorusso.windowsregistryktx.ReadableRegistryValue
import com.alexfacciorusso.windowsregistryktx.WritableRegistryValue

class LongRegistryValueTest : BaseRegistryValueTest<Long>("Long") {
    override val testContents: List<Long> = listOf(1L, 2L, 3L)

    override fun RegistryKey.getValue(valueName: String): ReadableRegistryValue<Long> =
        longValue(valueName)

    override fun RegistryTestHelper.readResultData(valueName: String): Long = readLong(valueName)

    override fun RegistryKey.getWritable(valueName: String): WritableRegistryValue<Long> = longValue(valueName)

    override fun RegistryTestHelper.writeDataToKey(valueName: String, value: Long) {
        writeLong(valueName, value)
    }
}