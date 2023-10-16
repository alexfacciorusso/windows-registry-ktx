package com.alexfacciorusso.windowsregistryktx

import com.alexfacciorusso.windowsregistryktx.values.stringValue

suspend fun main() {
    println("Value of CurrentBuild in HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion:")

    // Regedit path style
    println(
        Registry.subKey("HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion")
            .stringValue("CurrentBuild").read()
    )

    // Regedit path with raw string (no need for escape)
    println(
        Registry.localMachine.subKey("""SOFTWARE\Microsoft\Windows NT\CurrentVersion""").stringValue("CurrentBuild")
            .read()
    )

    // Slash-separated path (it will get normalised to backslash-separated internally)
    println(
        Registry.localMachine.subKey("SOFTWARE/Microsoft/Windows NT/CurrentVersion").stringValue("CurrentBuild").read()
    )

    // get operator access
    println(
        Registry.localMachine.subKey("SOFTWARE").subKey("Microsoft").subKey("Windows NT").subKey("CurrentVersion")
            .stringValue("CurrentBuild").read()
    )

    // get operator access with list of keys (it can be intermixed with slash-separated path)
    println(
        Registry.localMachine.subKey("SOFTWARE", "Microsoft", "Windows NT", "CurrentVersion")
            .stringValue("CurrentBuild").read()
    )

    // Delegate access
    val delegatedValue by Registry.localMachine.subKey("SOFTWARE", "Microsoft", "Windows NT", "CurrentVersion")
        .stringValue("CurrentBuild")

    println(delegatedValue)

    // Create key if not exists
    val testKey = Registry.currentUser.subKey("TestRegistry").createIfNotExisting()

    // Delegate setter
    var delegatedTestValue by testKey.stringValue("MyTestStringValue")

    delegatedTestValue = "Test value"

    println(delegatedTestValue)

    // Remove delegated value
    delegatedTestValue = null

    // Flow changes
    println("Collecting changes on the string value...")

    testKey.stringValue("MyTestStringValue").flowChanges().collect {
        println("New value: $it")
    }
}
