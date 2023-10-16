# Windows Registry KTX

[![](https://jitpack.io/v/alexfacciorusso/windows-registry-ktx.svg)](https://jitpack.io/#alexfacciorusso/windows-registry-ktx)

## Description

This repository contains the Windows Registry KTX, a set of extensions to use the
registry with Kotlin.

It uses JNA to access the Windows API (mostly Advapi32) and provide a Kotlin-friendly API for the developers.

## Usage

```kotlin
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
```

## Installation

### Gradle (snapshot via JitPack)

**Step 1.** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

	dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
**Step 2.** Add the dependency

	dependencies {
        implementation 'com.github.alexfacciorusso:windows-registry-ktx:main-SNAPSHOT'
	}

