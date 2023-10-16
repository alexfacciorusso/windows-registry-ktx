package com.alexfacciorusso.windowsregistryktx.values

import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.RegistryTestHelper
import com.alexfacciorusso.windowsregistryktx.ReadableRegistryValue
import com.alexfacciorusso.windowsregistryktx.SLEEP_FOR_REGISTRY_UPDATE_MS
import com.alexfacciorusso.windowsregistryktx.WritableRegistryValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseRegistryValueTest<T>(private val typeName: String) {
    private val testHelper = RegistryTestHelper()

    @BeforeTest
    fun setup() {
        testHelper.init()
    }

    @AfterTest
    fun cleanup() {
        testHelper.cleanup()
    }

    abstract val testContents: List<T>
    abstract fun RegistryKey.getValue(valueName: String): ReadableRegistryValue<T>
    abstract fun RegistryKey.getWritable(valueName: String): WritableRegistryValue<T>
    abstract fun RegistryTestHelper.readResultData(valueName: String): T
    abstract fun RegistryTestHelper.writeDataToKey(valueName: String, value: T)

    @Test
    fun reading() {
        val valueName = "TestRegistryKTX_${typeName}"

        testHelper.writeDataToKey(valueName, testContents.first())

        assertEquals(
            expected = testContents.first(),
            testHelper.testKey.getValue(valueName).read(),
        )
    }

    @Test
    fun writing() {
        val valueName = "TestRegistryKTX_${typeName}"

        testHelper.testKey.getWritable(valueName).write(testContents.first())

        assertEquals(
            expected = testContents.first(),
            actual = testHelper.readResultData(valueName)
        )
    }

    @Test
    fun flow() = runTest(UnconfinedTestDispatcher()) {
        val valueName = "TestRegistryKTX_${typeName}"
        val flowValues = mutableListOf<T?>()

        backgroundScope.launch {
            testHelper.testKey.getValue(valueName).flowChanges()
                .toList(flowValues)
        }

        Thread.sleep(SLEEP_FOR_REGISTRY_UPDATE_MS) // needed for the registry to trigger first update (null)

        testContents.forEach {
            testHelper.writeDataToKey(valueName, it)
            Thread.sleep(SLEEP_FOR_REGISTRY_UPDATE_MS) // needed for the registry to trigger updates
        }

        assertEquals(
            expected = listOf(null) + testContents,
            actual = flowValues,
        )
    }
}