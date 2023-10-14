package com.alexfacciorusso.windowsregistryktx.values

//class BinaryRegistryValue(parentKey: RegistryKey, name: String) : RegistryValue<ByteArray>(parentKey, name) {
//    override val typeName: String = "Binary"
//
//    override fun retrieveValue(): ByteArray =
//        Advapi32Util.registryGetBinaryValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name)
//
//    override fun write(value: ByteArray) =
//        Advapi32Util.registrySetBinaryValue(parentKey.rootHandle, parentKey.pathWithoutRoot.toString(), name, value)
//}
//
//fun RegistryKey.binaryValue(name: String) = BinaryRegistryValue(this, name)
