package windowsregistryktx

import com.alexfacciorusso.windowsregistryktx.Registry
import com.alexfacciorusso.windowsregistryktx.RegistryKey
import com.alexfacciorusso.windowsregistryktx.RegistryValue
import com.alexfacciorusso.windowsregistryktx.values.booleanValue
import com.alexfacciorusso.windowsregistryktx.values.intValue
import com.alexfacciorusso.windowsregistryktx.values.longValue
import com.alexfacciorusso.windowsregistryktx.values.stringArrayValue
import com.alexfacciorusso.windowsregistryktx.values.stringValue
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RegistryTestImplementation {
    private val testRoot = WinReg.HKEY_CURRENT_USER
    private val testKey = "TestRegistryKTX"

    val testPath = Registry.currentUser[testKey].fullPath

    fun init() {
        Advapi32Util.registryCreateKey(testRoot, testKey)
    }

    fun writeString(valueName: String, value: String) {
        Advapi32Util.registrySetStringValue(testRoot, testKey, valueName, value)
    }

    fun writeLong(valueName: String, valueContent: Long) {
        Advapi32Util.registrySetLongValue(testRoot, testKey, valueName, valueContent)
    }

    fun readString(valueName: String): String =
        Advapi32Util.registryGetStringValue(testRoot, testKey, valueName)

    fun readLong(valueName: String): Long =
        Advapi32Util.registryGetLongValue(testRoot, testKey, valueName)

    fun readInt(valueName: String): Int =
        Advapi32Util.registryGetIntValue(testRoot, testKey, valueName)

    fun writeInt(valueName: String, valueContent: Int) {
        Advapi32Util.registrySetIntValue(testRoot, testKey, valueName, valueContent)
    }

    fun readBinary(valueName: String): ByteArray =
        Advapi32Util.registryGetBinaryValue(testRoot, testKey, valueName)

    fun writeBinary(valueName: String, valueContent: ByteArray) {
        Advapi32Util.registrySetBinaryValue(testRoot, testKey, valueName, valueContent)
    }

    fun readStringArray(valueName: String): Array<String> =
        Advapi32Util.registryGetStringArray(testRoot, testKey, valueName)

    fun writeStringArray(valueName: String, valueContent: Array<String>) {
        Advapi32Util.registrySetStringArray(testRoot, testKey, valueName, valueContent)
    }

    fun cleanup() {
        Advapi32Util.registryDeleteKey(testRoot, testKey)
    }
}

private const val SLEEP_FOR_REGISTRY_UPDATE_MS = 10L

@OptIn(ExperimentalCoroutinesApi::class)
class RegistryTest {
    private val tester = RegistryTestImplementation()

    @BeforeTest
    fun setup() {
        tester.init()
    }

    @AfterTest
    fun cleanup() {
        tester.cleanup()
    }

    private fun <T> readingTest(
        typeName: String,
        valueContent: T,
        writePrepData: RegistryTestImplementation.(valueName: String, value: T) -> Unit,
        getValue: RegistryKey.(valueName: String) -> RegistryValue<T>,
    ) {
        val valueName = "TestRegistryKTX_${typeName}"

        tester.writePrepData(valueName, valueContent)

        assertEquals(
            expected = valueContent,
            actual = Registry[tester.testPath].getValue(valueName).read()
        )
    }

    private fun <T> writingTest(
        typeName: String,
        valueContent: T,
        getValue: RegistryKey.(valueName: String) -> RegistryValue<T>,
        readResultData: RegistryTestImplementation.(valueName: String) -> T,
    ) {
        val valueName = "TestRegistryKTX_${typeName}"

        Registry[tester.testPath].getValue(valueName).write(valueContent)

        assertEquals(
            expected = valueContent,
            actual = tester.readResultData(valueName)
        )
    }

    private fun <T> flowTest(
        typeName: String,
        valueContents: List<T>,
        getValue: RegistryKey.(valueName: String) -> RegistryValue<T>,
        writePrepData: RegistryTestImplementation.(valueName: String, value: T) -> Unit,
    ) = runTest(UnconfinedTestDispatcher()) {
        val valueName = "TestRegistryKTX_${typeName}"
        val flowValues = mutableListOf<T?>()

        backgroundScope.launch {
            Registry[tester.testPath].getValue(valueName).flowChanges()
                .toList(flowValues)
        }

        Thread.sleep(SLEEP_FOR_REGISTRY_UPDATE_MS) // needed for the registry to trigger first update (null)

        valueContents.forEach {
            tester.writePrepData(valueName, it)
            Thread.sleep(SLEEP_FOR_REGISTRY_UPDATE_MS) // needed for the registry to trigger updates
        }

        assertEquals(
            expected = listOf(null) + valueContents,
            actual = flowValues,
        )
    }

    @Test
    fun `string reading`() = readingTest(
        typeName = "String",
        valueContent = "test",
        writePrepData = { name, content ->
            writeString(name, content)
        },
        getValue = { stringValue(it) },
    )

    @Test
    fun `string writing`() = writingTest(
        typeName = "String",
        valueContent = "test",
        getValue = { stringValue(it) },
        readResultData = {
            readString(it)
        },
    )

    @Test
    fun `string flow`() = flowTest(
        typeName = "String",
        valueContents = listOf("test1", "test2", "test3"),
        getValue = RegistryKey::stringValue,
        writePrepData = { valueName, value ->
            writeString(valueName, value)
        },
    )

    @Test
    fun `long reading`() = readingTest(
        typeName = "Long",
        valueContent = 1L,
        writePrepData = { name, content ->
            writeLong(name, content)
        },
        getValue = { longValue(it) }
    )

    @Test
    fun `long writing`() = writingTest(
        typeName = "Long",
        valueContent = 10L,
        getValue = { longValue(it) },
        readResultData = {
            readLong(it)
        },
    )

    @Test
    fun `long flow`() = flowTest(
        typeName = "Long",
        valueContents = listOf(2L),
        getValue = { longValue(it) },
        writePrepData = { valueName, value ->
            writeLong(valueName, value)
        },
    )

    @Test
    fun `int reading`() = readingTest(
        typeName = "Int",
        valueContent = 1,
        writePrepData = { name, content ->
            writeInt(name, content)
        },
        getValue = { intValue(it) }
    )

    @Test
    fun `int writing`() = writingTest(
        typeName = "Int",
        valueContent = 1,
        getValue = { intValue(it) },
        readResultData = {
            readInt(it)
        },
    )

    @Test
    fun `int flow`() = flowTest(
        typeName = "Int",
        valueContents = listOf(2),

        getValue = { intValue(it) },
        writePrepData = { valueName, value ->
            writeInt(valueName, value)
        },
    )

    @Test
    fun `boolean reading`() = readingTest(
        typeName = "Boolean",
        valueContent = true,
        writePrepData = { name, content ->
            writeInt(name, if (content) 1 else 0)
        },
        getValue = { booleanValue(it) }
    )

    @Test
    fun `boolean writing`() = writingTest(
        typeName = "Boolean",
        valueContent = true,
        getValue = { booleanValue(it) },
        readResultData = {
            readInt(it) != 0
        },
    )

    @Test
    fun `boolean flow`() = flowTest(
        typeName = "Boolean",
        valueContents = listOf(true, false),
        getValue = { booleanValue(it) },
        writePrepData = { valueName, value ->
            writeInt(valueName, if (value) 1 else 0)
        },
    )

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

    @Test
    fun `string array reading`() = readingTest(
        typeName = "StringArray",
        valueContent = listOf("test1", "test2", "test3"),
        writePrepData = { name, content ->
            writeStringArray(name, content.toTypedArray())
        },
        getValue = { stringArrayValue(it) }
    )

    @Test
    fun `string array writing`() = writingTest(
        typeName = "StringArray",
        valueContent = listOf("test1", "test2", "test3"),
        getValue = { stringArrayValue(it) },
        readResultData = {
            readStringArray(it).toList()
        },
    )

    @Test
    fun `string array flow`() = flowTest(
        typeName = "StringArray",
        valueContents = listOf(listOf("test1", "test2", "test3")),
        getValue = { stringArrayValue(it) },
        writePrepData = { valueName, value ->
            writeStringArray(valueName, value.toTypedArray())
        },
    )

    @Test
    fun deleting() = runTest(UnconfinedTestDispatcher()) {
        val valueName = "TestRegistryKTX_Deleting"
        val valueContent = "test"

        tester.writeString(valueName, valueContent)

        assertEquals(
            expected = valueContent,
            actual = Registry[tester.testPath].stringValue(valueName).read()
        )

        Registry[tester.testPath].stringValue(valueName).delete()

        assertEquals(
            expected = null,
            actual = Registry[tester.testPath].stringValue(valueName).read()
        )
    }
}