package com.alexfacciorusso.windowsregistryktx

import com.sun.jna.platform.win32.Advapi32
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.WinNT.INFINITE
import com.sun.jna.platform.win32.WinNT.KEY_ALL_ACCESS
import com.sun.jna.platform.win32.WinNT.KEY_NOTIFY
import com.sun.jna.platform.win32.WinNT.REG_NOTIFY_CHANGE_LAST_SET
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.platform.win32.WinReg.HKEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.reflect.KProperty

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

private fun sanitisePath(path: String) = path.replace("/", "\\")

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

interface Pathable {
    fun subKey(path: RegistryPath): RegistryKey

    fun subKey(vararg path: String): RegistryKey = this.subKey(RegistryPath(*path))
    fun subKey(path: String): RegistryKey = this.subKey(RegistryPath(path))
}

private fun Pathable.subKey(hkey: Hkeys): RegistryKey = this.subKey(hkey.name)

interface Deleteable {
    fun delete()
}

interface Existable {
    fun exists(): Boolean
}

interface Creatable {
    fun createIfNotExisting(): RegistryKey
}

class RegistryKey internal constructor(path: RegistryPath) : Pathable, Deleteable, Existable, Creatable {
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

    override fun subKey(path: RegistryPath): RegistryKey = RegistryKey(this.fullPath + path)

    fun parent(): RegistryKey? = fullPath.parent()?.let { RegistryKey(it) }

    private fun openHandle(accessLevel: Int = KEY_ALL_ACCESS) =
        Advapi32Util.registryGetKey(rootHandle, pathWithoutRoot.toString(), accessLevel).asClosable()

    @Suppress("SameParameterValue")
    private inline fun <T> openHandleAndThenDispose(accessLevel: Int = KEY_ALL_ACCESS, block: (HKEY?) -> T) =
        openHandle(accessLevel).use {
            block(it.hkeyReference.value ?: error("Cannot open key"))
        }

    private suspend fun notifyChangeInKeyValue(
        includingKeySubtree: Boolean = false,
    ) = suspendCancellableCoroutine {
        val event = Kernel32.INSTANCE.CreateEvent(
            /* lpEventAttributes = */ null,
            /* bManualReset = */ true,
            /* bInitialState = */ false,
            /* lpName = */ null
        )

        val keyHandle = openHandle(KEY_NOTIFY)

        Advapi32.INSTANCE.RegNotifyChangeKeyValue(
            keyHandle.hkeyReference.value,
            includingKeySubtree,
            REG_NOTIFY_CHANGE_LAST_SET,
            event,
            true,
        )

        it.invokeOnCancellation {
            Kernel32.INSTANCE.CloseHandle(event)
            keyHandle.close()
        }

        val value = Kernel32.INSTANCE.WaitForSingleObject(event, INFINITE)

        it.resume(value)
    }

    fun flowChanges(
        includingKeySubtree: Boolean = false,
        emitOnStart: Boolean = true,
        coroutineContext: CoroutineContext = Dispatchers.IO,
    ): Flow<Unit> = flow {
        if (emitOnStart) emit(Unit)

        while (true) {
            val result = withContext(coroutineContext) { notifyChangeInKeyValue(includingKeySubtree) }

            if (result == 0) { // WAIT_OBJECT_0
                emit(Unit)
            }
        }
    }

    override fun delete() = Advapi32Util.registryDeleteKey(rootHandle, pathWithoutRoot.toString())

    override fun exists(): Boolean = Advapi32Util.registryKeyExists(rootHandle, pathWithoutRoot.toString())

    override fun toString(): String = "RegistryKey($fullPath)"

    override fun createIfNotExisting(): RegistryKey {
        if (!exists()) Advapi32Util.registryCreateKey(rootHandle, pathWithoutRoot.toString())
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RegistryKey) return false

        if (fullPath != other.fullPath) return false

        return true
    }

    override fun hashCode(): Int = fullPath.hashCode()
}

abstract class ReadableRegistryValue<T> internal constructor(val parentKey: RegistryKey, val name: String) :
    Existable,
    Deleteable {
    abstract val typeName: String

    private fun errorForValue(type: String): Nothing =
        error("$type value is not existing for key ${parentKey.pathWithoutRoot}")

    protected abstract fun retrieveValue(): T

    override fun exists(): Boolean =
        Advapi32Util.registryValueExists(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)

    override fun delete() =
        Advapi32Util.registryDeleteValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)

    fun read(): T? =
        if (exists()) retrieveValue() else null

    fun readOrThrow(): T =
        read() ?: errorForValue(typeName)

    fun flowChanges(
        emitCurrentValue: Boolean = true,
        coroutineContext: CoroutineContext = Dispatchers.IO,
    ): Flow<T?> =
        parentKey.flowChanges(
            emitOnStart = emitCurrentValue,
            coroutineContext = coroutineContext,
        ).map { read() }.distinctUntilChanged()
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> ReadableRegistryValue<T>.getValue(thisObj: Any?, property: KProperty<*>): T? = read()

abstract class WritableRegistryValue<T : Any?>(
    parentKey: RegistryKey,
    name: String,
) : ReadableRegistryValue<T>(parentKey, name) {
    abstract fun write(value: T)
}

@Suppress("NOTHING_TO_INLINE")
inline operator fun <T> WritableRegistryValue<T>.setValue(thisObj: Any?, property: KProperty<*>, value: T?) =
    if (value == null) delete() else write(value)

object Registry : Pathable {
    override fun subKey(path: RegistryPath): RegistryKey = RegistryKey(path)

    val currentUser = subKey(Hkeys.HKEY_CURRENT_USER)
    val localMachine = subKey(Hkeys.HKEY_LOCAL_MACHINE)
    val classesRoot = subKey(Hkeys.HKEY_CLASSES_ROOT)
    val users = subKey(Hkeys.HKEY_USERS)
    val currentConfig = subKey(Hkeys.HKEY_CURRENT_CONFIG)
    val performanceData = subKey(Hkeys.HKEY_PERFORMANCE_DATA)
}
