package com.alexfacciorusso.windowsregistryktx

import com.sun.jna.platform.win32.Advapi32
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinNT.KEY_ALL_ACCESS
import com.sun.jna.platform.win32.WinNT.KEY_NOTIFY
import com.sun.jna.platform.win32.WinNT.REG_NOTIFY_CHANGE_LAST_SET
import com.sun.jna.platform.win32.WinReg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private enum class Hkeys(private val alternateName: String) {
    HKEY_CURRENT_USER(alternateName = "HKCU"),
    HKEY_LOCAL_MACHINE(alternateName = "HKLM"),
    HKEY_CLASSES_ROOT(alternateName = "HKCR"),
    HKEY_USERS(alternateName = "HKU"),
    HKEY_CURRENT_CONFIG(alternateName = "HKCC"),
    HKEY_PERFORMANCE_DATA(alternateName = "HKPD")
    ;

    companion object {
        fun valueByName(value: String) = entries.find { it.name == value || it.alternateName == value }
    }
}

private fun sanitisePath(path: String) = path.replace("/".toRegex(), Regex.escapeReplacement("\\"))

@JvmInline
value class RegistryPath internal constructor(val components: List<String> = emptyList()) {
    constructor(path: String) : this(sanitisePath(path).split("\\").filter { it.isNotEmpty() })
    constructor(vararg path: String) : this(path.toList())

    override fun toString(): String = components.joinToString("\\")

    operator fun plus(name: String): RegistryPath = RegistryPath(components + name)
    operator fun plus(path: RegistryPath): RegistryPath = RegistryPath(components + path.components)

    fun parent(): RegistryPath? =
        if (components.count() <= 1) null // Root key
        else RegistryPath(components.dropLast(1))
}

interface RegistryPathable {
    operator fun get(path: RegistryPath): RegistryKey

    operator fun get(vararg path: String): RegistryKey = this[RegistryPath(*path)]
    operator fun get(path: String): RegistryKey = this[RegistryPath(path)]
}

private operator fun RegistryPathable.get(hkey: Hkeys): RegistryKey = this[hkey.name]

class RegistryKey internal constructor(path: RegistryPath) : RegistryPathable {
    val fullPath = path
    val pathWithoutRoot = RegistryPath(fullPath.components.drop(1))

    val rootHandle = when (Hkeys.valueByName(path.components.first())) {
        Hkeys.HKEY_CURRENT_USER -> WinReg.HKEY_CURRENT_USER
        Hkeys.HKEY_LOCAL_MACHINE -> WinReg.HKEY_LOCAL_MACHINE
        Hkeys.HKEY_CLASSES_ROOT -> WinReg.HKEY_CLASSES_ROOT
        Hkeys.HKEY_USERS -> WinReg.HKEY_USERS
        Hkeys.HKEY_CURRENT_CONFIG -> WinReg.HKEY_CURRENT_CONFIG
        Hkeys.HKEY_PERFORMANCE_DATA -> WinReg.HKEY_PERFORMANCE_DATA
        null -> error("Invalid root key")
    }

    override operator fun get(path: RegistryPath): RegistryKey = RegistryKey(this.fullPath + path)

    fun parent(): RegistryKey? = fullPath.parent()?.let { RegistryKey(it) }

    fun openHandle(accessLevel: Int = KEY_ALL_ACCESS): CloseableHkey =
        CloseableHkey(Advapi32Util.registryGetKey(rootHandle, pathWithoutRoot.toString(), accessLevel).value)

    fun flowChanges(
        includingKeySubtree: Boolean = false,
        emitOnStart: Boolean = true,
    ): Flow<Unit> = flow {
        val hkeyContainer = openHandle(KEY_NOTIFY)

        if (emitOnStart) emit(Unit)

        hkeyContainer.use {
            while (true) {
                val result = Advapi32.INSTANCE.RegNotifyChangeKeyValue(
                    hkeyContainer.hkey,
                    includingKeySubtree,
                    REG_NOTIFY_CHANGE_LAST_SET,
                    null,
                    false,
                )

                if (result == 0) { // WAIT_OBJECT_0
                    emit(Unit)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    fun delete() = Advapi32Util.registryDeleteKey(rootHandle, pathWithoutRoot.toString())

    fun exists(): Boolean = Advapi32Util.registryKeyExists(rootHandle, pathWithoutRoot.toString())

    override fun toString(): String = "RegistryKey($fullPath)"
}

abstract class RegistryValue<T : Any?> internal constructor(val parentKey: RegistryKey, val name: String) {
    abstract val typeName: String

    private fun errorForValue(type: String): Nothing =
        error("$type value is not existing for key ${parentKey.pathWithoutRoot}")

    private fun <T> flowInternal(
        emitOnStart: Boolean,
        block: () -> T?,
    ): Flow<T?> = parentKey.flowChanges(emitOnStart).map { block() }.distinctUntilChanged()

    protected abstract fun retrieveValue(): T

    fun exists(): Boolean =
        Advapi32Util.registryValueExists(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)

    fun read(): T? =
        if (exists()) retrieveValue() else null

    fun readOrThrow(): T =
        read() ?: errorForValue(typeName)

    abstract fun write(value: T)

    fun delete() = Advapi32Util.registryDeleteValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)

    fun flowChanges(emitFirstValue: Boolean = true): Flow<T?> =
        flowInternal(emitOnStart = emitFirstValue) { read() }
}

object Registry : RegistryPathable {
    override fun get(path: RegistryPath): RegistryKey = RegistryKey(path)

    val currentUser = this[Hkeys.HKEY_CURRENT_USER]
    val localMachine = this[Hkeys.HKEY_LOCAL_MACHINE]
    val classesRoot = this[Hkeys.HKEY_CLASSES_ROOT]
    val users = this[Hkeys.HKEY_USERS]
    val currentConfig = this[Hkeys.HKEY_CURRENT_CONFIG]
    val performanceData = this[Hkeys.HKEY_PERFORMANCE_DATA]
}
