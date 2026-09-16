package com.example.routy.transport

import com.example.routy.core.transport.data.*
import com.example.routy.core.transport.domain.AppError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.*

class HttpDataSourceTest {
    @Test fun routeIdIsEncodedAsQueryParameter() =
        runTest {
            val engine =
                MockEngine { request ->
                    assertEquals("/api/getBusLocsOnRoute", request.url.encodedPath)
                    assertEquals("r&other=value", request.url.parameters["routeId"])
                    assertNull(request.url.parameters["other"])
                    respond("{\"data\":[]}")
                }
            val original = HttpClient(engine)
            val client = configuredHttpClient(original)
            try {
                ThetaMapsRemoteDataSource(client, TransportConfig()).vehicles("r&other=value")
            } finally {
                client.close()
                original.close()
            }
        }

    @Test fun serverFailureMapsToDomainError() =
        runTest {
            val original = HttpClient(MockEngine { respond("unavailable", HttpStatusCode.ServiceUnavailable) })
            val client = configuredHttpClient(original)
            try {
                val failure = assertFails { ThetaMapsRemoteDataSource(client, TransportConfig()).database() }
                assertEquals(AppError.ServerUnavailable, mapTransportError(failure as Exception))
            } finally {
                client.close()
                original.close()
            }
        }

    @Test fun timeoutAndConnectivityMapping() {
        assertEquals(AppError.NoInternet, mapTransportError(IOException("network failure")))
        assertEquals(AppError.Timeout, mapTransportError(HttpRequestTimeoutException("https://example.invalid", 1L)))
    }
}
