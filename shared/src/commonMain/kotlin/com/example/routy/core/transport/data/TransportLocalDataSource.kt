package com.example.routy.core.transport.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Platform implementation performs atomic replacement away from the UI thread. */
interface PersistentFiles {
    suspend fun read(name: String): String?

    suspend fun write(
        name: String,
        content: String,
    )
}

@Serializable
data class CachedDatabase(
    val payload: String,
    val updatedAtMillis: Long,
    val schemaVersion: Int = 1,
)

interface TransportLocalDataSource {
    suspend fun read(): CachedDatabase?

    suspend fun write(database: CachedDatabase)
}

class FileTransportLocalDataSource(
    private val files: PersistentFiles,
) : TransportLocalDataSource {
    override suspend fun read(): CachedDatabase? =
        files.read("transport-v1.json")?.let {
            Json.decodeFromString<CachedDatabase>(it).takeIf { cache -> cache.schemaVersion == 1 }
        }

    override suspend fun write(database: CachedDatabase) = files.write("transport-v1.json", Json.encodeToString(database))
}
