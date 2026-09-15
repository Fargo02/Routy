@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.example.routy.core.storage

import com.example.routy.core.transport.data.PersistentFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

class IosPersistentFiles : PersistentFiles {
    private val directory =
        (NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true).first() as String) + "/Routy"

    init {
        check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null))
    }

    override suspend fun read(name: String): String? =
        withContext(Dispatchers.Default) {
            val path = "$directory/$name"
            if (!NSFileManager.defaultManager.fileExistsAtPath(path)) {
                null
            } else {
                requireNotNull(NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null))
            }
        }

    override suspend fun write(
        name: String,
        content: String,
    ): Unit =
        withContext(Dispatchers.Default) {
            check(NSString.create(string = content).writeToFile("$directory/$name", true, NSUTF8StringEncoding, null))
        }
}
