package com.alexfacciorusso.windowsregistryktx

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg

class RegistryTestHelper {
    private val testRoot = WinReg.HKEY_CURRENT_USER
    private val testKeyName = "TestRegistryKTX"

    val testPath = Registry.currentUser[testKeyName].fullPath
    val testKey = Registry[testPath]

    fun init() {
        Advapi32Util.registryCreateKey(testRoot, testKeyName)
    }

    fun writeString(valueName: String, value: String) {
        Advapi32Util.registrySetStringValue(testRoot, testKeyName, valueName, value)
    }

    fun writeLong(valueName: String, valueContent: Long) {
        Advapi32Util.registrySetLongValue(testRoot, testKeyName, valueName, valueContent)
    }

    fun readString(valueName: String): String =
        Advapi32Util.registryGetStringValue(testRoot, testKeyName, valueName)

    fun readLong(valueName: String): Long =
        Advapi32Util.registryGetLongValue(testRoot, testKeyName, valueName)

    fun readInt(valueName: String): Int =
        Advapi32Util.registryGetIntValue(testRoot, testKeyName, valueName)

    fun writeInt(valueName: String, valueContent: Int) {
        Advapi32Util.registrySetIntValue(testRoot, testKeyName, valueName, valueContent)
    }

    fun readBinary(valueName: String): ByteArray =
        Advapi32Util.registryGetBinaryValue(testRoot, testKeyName, valueName)

    fun writeBinary(valueName: String, valueContent: ByteArray) {
        Advapi32Util.registrySetBinaryValue(testRoot, testKeyName, valueName, valueContent)
    }

    fun readStringArray(valueName: String): Array<String> =
        Advapi32Util.registryGetStringArray(testRoot, testKeyName, valueName)

    fun writeStringArray(valueName: String, valueContent: Array<String>) {
        Advapi32Util.registrySetStringArray(testRoot, testKeyName, valueName, valueContent)
    }

    fun cleanup() {
        Advapi32Util.registryDeleteKey(testRoot, testKeyName)
    }
}

const val SLEEP_FOR_REGISTRY_UPDATE_MS = 10L
