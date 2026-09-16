@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.routy

import com.example.routy.core.storage.IosPersistentFiles
import kotlinx.coroutines.test.runTest
import platform.Foundation.*
import kotlin.test.*

class PersistentFilesIosTest {
    @Test fun atomicFileRoundTripPreservesGeorgianAcrossInstances() =
        runTest {
            val name = "test-${NSUUID().UUIDString}.json"
            val files = IosPersistentFiles()
            try {
                assertNull(files.read(name))
                files.write(name, "ბათუმი")
                assertEquals("ბათუმი", IosPersistentFiles().read(name))
                files.write(name, "updated")
                assertEquals("updated", IosPersistentFiles().read(name))
            } finally {
                val directory = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String
                NSFileManager.defaultManager.removeItemAtPath("$directory/Routy/$name", null)
            }
        }
}
