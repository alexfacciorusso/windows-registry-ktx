package com.alexfacciorusso.windowsregistryktx

import com.alexfacciorusso.windowsregistryktx.values.stringValue

suspend fun main() {
    println("Value of CurrentBuild in HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion:")

    // Regedit path style
    println(
        Registry["HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion"]
            .stringValue("CurrentBuild").read()
    )

    // Regedit path with raw string (no need for escape)
    println(
        Registry.localMachine["""SOFTWARE\Microsoft\Windows NT\CurrentVersion"""].stringValue("CurrentBuild").read()
    )

    // Slash-separated path (it will get normalised to backslash-separated internally)
    println(
        Registry.localMachine["SOFTWARE/Microsoft/Windows NT/CurrentVersion"].stringValue("CurrentBuild").read()
    )

    // get operator access
    println(
        Registry.localMachine["SOFTWARE"]["Microsoft"]["Windows NT"]["CurrentVersion"]
            .stringValue("CurrentBuild").read()
    )

    // get operator access with list of keys (it can be intermixed with slash-separated path)
    println(
        Registry.localMachine["SOFTWARE", "Microsoft", "Windows NT", "CurrentVersion"]
            .stringValue("CurrentBuild").read()
    )

    println("Collecting changes on the string value TestRegistryKTX in CURRENT_USER...")

    Registry.currentUser.stringValue("TestRegistryKTX_String").flowChanges().collect {
        println("New value: $it")
    }
}
