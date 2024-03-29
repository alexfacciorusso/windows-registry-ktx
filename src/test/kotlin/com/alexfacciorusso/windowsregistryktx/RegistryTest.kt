package com.alexfacciorusso.windowsregistryktx

import com.alexfacciorusso.windowsregistryktx.values.stringValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

//    @Test
//    fun `binary reading`() = readingTest(
//        typeName = "Binary",
//        valueContent = byteArrayOf(0x01, 0x02, 0x03),
//        writePrepData = { name, content ->
//            writeBinary(name, content)
//        },
//        getValue = { binaryValue(it) }
//    )
//
//    @Test
//    fun `binary writing`() = writingTest(
//        typeName = "Binary",
//        valueContent = byteArrayOf(0x01, 0x02, 0x03),
//        getValue = { binaryValue(it) },
//        readResultData = {
//            readBinary(it)
//        },
//    )
//
//    @Test
//    fun `binary flow`() = flowTest(
//        typeName = "Binary",
//        valueContents = listOf(byteArrayOf(0x01, 0x02, 0x03), byteArrayOf(0x04, 0x05, 0x06)),
//        getValue = { binaryValue(it) },
//        writePrepData = { valueName, value ->
//            writeBinary(valueName, value)
//        },
//    )

@OptIn(ExperimentalCoroutinesApi::class)
class RegistryTest {
    protected val testHelper = RegistryTestHelper()

    @BeforeTest
    fun setup() {
        testHelper.init()
    }

    @AfterTest
    fun cleanup() {
        testHelper.cleanup()
    }

    @Test
    fun deleting() = runTest(UnconfinedTestDispatcher()) {
        val valueName = "TestRegistryKTX_Deleting"
        val valueContent = "test"

        testHelper.writeString(valueName, valueContent)

        assertEquals(
            expected = valueContent,
            actual = Registry.subKey(testHelper.testPath).stringValue(valueName).read()
        )

        Registry.subKey(testHelper.testPath).stringValue(valueName).delete()

        assertEquals(
            expected = null,
            actual = Registry.subKey(testHelper.testPath).stringValue(valueName).read()
        )
    }

    @Test
    fun `registry key parent`() {
        val parentKey = Registry.currentUser
        val key = parentKey.subKey("TestRegistryKTX_KeyParent")

        assertEquals(
            expected = parentKey,
            actual = key.parent(),
        )
    }

    @Test
    fun `pathable varargs`() {
        assertEquals(
            expected = Registry.currentUser.subKey("TestRegistryKTX_PathableVarargs\\TestRegistryKTX_PathableVarargs2"),
            actual = Registry.currentUser.subKey("TestRegistryKTX_PathableVarargs", "TestRegistryKTX_PathableVarargs2")
        )
    }

    @Test
    fun `getter delegate`() {
        val valueName = "TestRegistryKTX_GetterDelegate"
        val valueContent = "test"

        val value by Registry.subKey(testHelper.testPath).stringValue(valueName)

        assertNull(value)

        testHelper.writeString(valueName, valueContent)

        assertEquals(
            expected = valueContent,
            actual = value
        )
    }

    @Test
    fun `setter delegate`() {
        val valueName = "TestRegistryKTX_SetterDelegate"
        val valueContent = "test"

        var value by Registry.subKey(testHelper.testPath).stringValue(valueName)

        // set
        value = valueContent

        assertEquals(
            expected = valueContent,
            actual = testHelper.readString(valueName)
        )

        // delete value
        value = null

        assertFalse {
            testHelper.valueExists(valueName)
        }
    }
}