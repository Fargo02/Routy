package com.example.routy.core.di

import com.example.routy.core.logging.AppLogger
import com.example.routy.core.logging.SilentLogger
import com.example.routy.core.map.data.CachingMapStyleSource
import com.example.routy.core.preferences.data.FilePreferencesRepository
import com.example.routy.core.transport.data.*
import com.example.routy.core.transport.domain.*
import com.example.routy.feature.favorites.domain.FavoritesUseCase
import com.example.routy.feature.settings.domain.SettingsUseCase
import io.ktor.client.HttpClient
import kotlin.time.Clock

class AppGraph(
    private val client: HttpClient,
    files: PersistentFiles,
    config: TransportConfig = TransportConfig(),
    val logger: AppLogger = SilentLogger,
) {
    private val http = configuredHttpClient(client)
    private val parser = TransportParser()
    private val remote = ThetaMapsRemoteDataSource(http, config)
    private val clock = EpochClock { Clock.System.now().toEpochMilliseconds() }
    private val preferences = FilePreferencesRepository(files, logger)
    val transport =
        ObserveTransportUseCase(
            OfflineTransportRepository(remote, FileTransportLocalDataSource(files), parser, clock, config, logger = logger),
        )
    val vehicles = ObserveRouteVehiclesUseCase(PollingVehicleRepository(remote, parser, clock, config, logger = logger))
    val mapStyle = CachingMapStyleSource(http, files)
    val settings = SettingsUseCase(preferences)
    val favorites = FavoritesUseCase(preferences)

    fun close() {
        http.close()
        client.close()
    }
}
