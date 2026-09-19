package com.example.routy.core.transport.data

import com.example.routy.core.transport.domain.*
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

class TransportConfig(
    val baseUrl: String = "https://thetamaps.site:54321",
    val databaseRefreshMillis: Long = 600_000,
    val databaseRetryMillis: Long = 15_000,
    val vehicleRefreshMillis: Long = 5_000,
    val vehicleRetentionMillis: Long = 30_000,
) {
    init {
        require(baseUrl.startsWith("https://"))
        require(databaseRefreshMillis >= 600_000 && vehicleRefreshMillis >= 4_000)
        require(databaseRetryMillis in 5_000..databaseRefreshMillis)
        require(vehicleRetentionMillis >= vehicleRefreshMillis)
    }
}

interface TransportRemoteDataSource {
    suspend fun database(): String

    suspend fun vehicles(routeId: String): String
}

class ThetaMapsRemoteDataSource(
    private val client: HttpClient,
    private val config: TransportConfig,
) : TransportRemoteDataSource {
    override suspend fun database(): String = client.get("${config.baseUrl.trimEnd('/')}/api/getDbData").bodyAsText()

    override suspend fun vehicles(routeId: String): String =
        client.get("${config.baseUrl.trimEnd('/')}/api/getBusLocsOnRoute") { parameter("routeId", routeId) }.bodyAsText()
}

fun configuredHttpClient(client: HttpClient): HttpClient =
    client.config {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
    }

fun mapTransportError(error: Exception): AppError =
    when (error) {
        is CancellationException -> throw error
        is HttpRequestTimeoutException, is ConnectTimeoutException, is SocketTimeoutException -> AppError.Timeout
        is ResponseException -> AppError.ServerUnavailable
        is SerializationException, is IllegalArgumentException -> AppError.InvalidData
        is IOException -> AppError.NoInternet
        else -> AppError.Unknown
    }

suspend fun <T> transportOperation(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    block: suspend () -> T,
): Outcome<T> =
    try {
        Outcome.Success(withContext(dispatcher) { block() })
    } catch (error: Exception) {
        Outcome.Failure(mapTransportError(error))
    }
