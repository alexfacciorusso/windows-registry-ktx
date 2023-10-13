package com.alexfacciorusso.windowsregistryktx

suspend fun main() {
    println("Value of CurrentBuild in HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion:")

    // Regedit path style
    println(
        Registry["HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion"]
            .value("CurrentBuild").asStringOrNull()
    )
    println(
        Registry.localMachine["""SOFTWARE\Microsoft\Windows NT\CurrentVersion"""].value("CurrentBuild").asStringOrNull()
    )
    println(
        Registry.localMachine["SOFTWARE/Microsoft/Windows NT/CurrentVersion"].value("CurrentBuild").asStringOrNull()
    )
    println(
        Registry.localMachine["SOFTWARE"]["Microsoft"]["Windows NT"]["CurrentVersion"]
            .value("CurrentBuild").asStringOrNull()
    )
    println(
        Registry.localMachine["SOFTWARE", "Microsoft", "Windows NT", "CurrentVersion"]
            .value("CurrentBuild").asStringOrNull()
    )

    println("Collecting changes on the string value TestRegistryKTX in CURRENT_USER...")

    Registry.currentUser.value("TestRegistryKTX_String").flowString().collect {
        println("New value: $it")
    }
}
