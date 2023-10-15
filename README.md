# Windows Registry KTX

[![](https://jitpack.io/v/alexfacciorusso/windows-registry-ktx.svg)](https://jitpack.io/#alexfacciorusso/windows-registry-ktx)

## Description

This repository contains the Windows Registry KTX, a set of extensions to use the
registry with Kotlin.

It uses JNA to access the Windows API (mostly Advapi32) and provide a Kotlin-friendly API for the developers.

## Usage

```kotlin
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

