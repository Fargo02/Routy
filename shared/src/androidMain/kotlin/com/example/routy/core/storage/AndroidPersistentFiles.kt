package com.example.routy.core.storage

import android.content.Context
import android.util.AtomicFile
import com.example.routy.core.transport.data.PersistentFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AndroidPersistentFiles(context: Context) : PersistentFiles {
    private val directory = File(context.applicationContext.filesDir, "routy").apply { mkdirs() }
    override suspend fun read(name: String): String? = withContext(Dispatchers.IO) {
        val file = AtomicFile(File(directory, name))
        try { file.openRead().bufferedReader().use { it.readText() } } catch (_: java.io.FileNotFoundException) { null }
    }
    override suspend fun write(name: String, content: String) = withContext(Dispatchers.IO) {
        val file = AtomicFile(File(directory, name))
        val output = file.startWrite()
        try {
            output.write(content.toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }
}
