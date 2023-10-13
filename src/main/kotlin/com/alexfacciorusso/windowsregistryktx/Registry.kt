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
}

operator fun RegistryPathable.get(vararg path: String): RegistryKey = this[RegistryPath(*path)]
operator fun RegistryPathable.get(path: String): RegistryKey = this[RegistryPath(path)]

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

    fun value(name: String): RegistryValue = RegistryValue(this, name)

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

    override fun toString(): String = "RegistryKey($fullPath)"
}

@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is a delicate API and its use requires care, and it may be even removed in future."
)
public annotation class DelicateRegistryApi

@Suppress("MemberVisibilityCanBePrivate")
class RegistryValue internal constructor(val parentKey: RegistryKey, val name: String) {
    private fun <T> getOrNull(block: () -> T?): T? = try {
        block()
    } catch (e: Exception) {
        null
    }

    private fun errorForValue(type: String): Nothing =
        error("$type value is not existing for key ${parentKey.pathWithoutRoot}")

    private fun <T> flowInternal(
        emitOnStart: Boolean = true,
        block: () -> T?,
    ): Flow<T?> = parentKey.flowChanges(emitOnStart).map { block() }.distinctUntilChanged()

    // region Generic
    @DelicateRegistryApi
    fun getOrNull(): Any? = getOrNull {
        Advapi32Util.registryGetValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)
    }

    @DelicateRegistryApi
    fun get(): Any = getOrNull() ?: errorForValue("")

    @DelicateRegistryApi
    fun <T : Any> getAs(
        clazz: Class<T>,
    ): T? = getOrNull()?.let { if (clazz.isInstance(it)) clazz.cast(it) else error("Type not respected") }

    @DelicateRegistryApi
    inline fun <reified T : Any> getAs(): T? = getAs(T::class.java)

    @DelicateRegistryApi
    fun flow(
        emitOnStart: Boolean = true,
    ): Flow<Any?> = parentKey.flowChanges(emitOnStart).map { getOrNull() }.distinctUntilChanged()

    @DelicateRegistryApi
    fun <T : Any> flowAs(
        emitOnStart: Boolean = true,
        clazz: Class<T>,
    ): Flow<T?> = flowInternal(emitOnStart) { getAs(clazz) }

    @DelicateRegistryApi
    inline fun <reified T : Any> flowAs(
        emitOnStart: Boolean = true,
    ): Flow<T?> = flowAs(emitOnStart, T::class.java)
    // endregion

    // region String
    fun asStringOrNull(): String? =
        getOrNull {
            Advapi32Util.registryGetStringValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)
        }

    fun asString(): String = asStringOrNull() ?: errorForValue("String")

    fun flowString(
        emitCurrentValue: Boolean = true,
    ): Flow<String?> = flowInternal(emitCurrentValue) { asStringOrNull() }
    // endregion

    // region Long
    fun asLongOrNull(): Long? = getOrNull {
        Advapi32Util.registryGetLongValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)
    }

    fun asLong(): Long = asLongOrNull() ?: errorForValue("Long")
    fun flowLong(
        emitOnStart: Boolean = true,
    ): Flow<Long?> = flowInternal(emitOnStart) { asLongOrNull() }
    // endregion

    // region Int
    fun asIntOrNull(): Int? = getOrNull {
        Advapi32Util.registryGetIntValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)
    }

    fun asInt(): Int = asIntOrNull() ?: errorForValue("Int")
    fun flowInt(
        emitOnStart: Boolean = true,
    ): Flow<Int?> = flowInternal(emitOnStart) { asIntOrNull() }
    // endregion

    // region Boolean
    fun asBooleanOrNull(): Boolean? = asIntOrNull()?.let { it != 0 }

    fun asBoolean(): Boolean = asBooleanOrNull() ?: errorForValue("Boolean")
    fun flowBoolean(
        emitOnStart: Boolean = true,
    ): Flow<Boolean?> = flowInternal(emitOnStart) { asBooleanOrNull() }
    // endregion

    // region Binary
    fun asBinary(): ByteArray = asBinaryOrNull() ?: errorForValue("Binary")
    fun asBinaryOrNull(): ByteArray? = getOrNull {
        Advapi32Util.registryGetBinaryValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)
    }

    fun flowBinary(
        emitOnStart: Boolean = true,
    ): Flow<ByteArray?> = flowInternal(emitOnStart) { asBinaryOrNull() }
    // endregion

    // region String list
    fun asStringListOrNull(): List<String>? = getOrNull {
        Advapi32Util.registryGetStringArray(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)?.toList()
    }

    fun asStringList(): List<String> =
        asStringListOrNull() ?: errorForValue("String list")

    fun flowStringList(
        emitOnStart: Boolean = true,
    ): Flow<List<String>?> = flowInternal(emitOnStart) { asStringListOrNull() }
    // endregion

    // region Expandable string
    fun asExpandableStringOrNull(): String? = getOrNull {
        Advapi32Util.registryGetExpandableStringValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)
    }

    fun asExpandableString(): String = asExpandableStringOrNull() ?: errorForValue("Expandable string")

    fun flowExpandableString(
        emitOnStart: Boolean = true,
    ): Flow<String?> = flowInternal(emitOnStart) { asExpandableStringOrNull() }
    // endregion

    override fun toString(): String = "RegistryValue(parent=$parentKey, name=$name)"
    override fun equals(other: Any?): Boolean =
        other is RegistryValue && other.parentKey == parentKey && other.name == name

    override fun hashCode(): Int {
        var result = parentKey.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}

object Registry : RegistryPathable {
    override fun get(path: RegistryPath): RegistryKey = RegistryKey(path)

    val currentUser = this[Hkeys.HKEY_CURRENT_USER.name]
    val localMachine = this[Hkeys.HKEY_LOCAL_MACHINE.name]
    val classesRoot = this[Hkeys.HKEY_CLASSES_ROOT.name]
    val users = this[Hkeys.HKEY_USERS.name]
    val currentConfig = this[Hkeys.HKEY_CURRENT_CONFIG.name]
    val performanceData = this[Hkeys.HKEY_PERFORMANCE_DATA.name]
}
