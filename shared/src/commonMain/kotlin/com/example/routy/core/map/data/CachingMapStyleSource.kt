package com.example.routy.core.map.data

import com.example.routy.core.map.domain.MapStyleSource
import com.example.routy.core.transport.data.PersistentFiles
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException

class CachingMapStyleSource(
    private val client: HttpClient,
    private val files: PersistentFiles,
) : MapStyleSource {
    override suspend fun style(uri: String): String? {
        val name = "map-style-${uri.substringAfterLast('/').filter(Char::isLetterOrDigit)}-v1.json"
        val downloaded = attempt { client.get(uri).bodyAsText() }
        if (downloaded != null) attempt { files.write(name, downloaded) }
        return downloaded ?: attempt { files.read(name) }
    }

    private suspend fun <T> attempt(block: suspend () -> T): T? =
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            null
        }
}
