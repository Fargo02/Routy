package com.example.routy.core.preferences.data

import com.example.routy.core.preferences.domain.*
import com.example.routy.core.logging.AppLogger
import com.example.routy.core.logging.LogEvent
import com.example.routy.core.logging.SilentLogger
import com.example.routy.core.transport.data.PersistentFiles
import com.example.routy.core.transport.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class PreferencesRecord(
    val language: String = "English",
    val appearance: String = "System",
    val colorTheme: String = "Ocean",
    val routeIds: Set<String> = emptySet(),
    val stopIds: Set<String> = emptySet(),
) {
    fun domain() =
        Preferences(
            language = Language.entries.firstOrNull { it.name == language } ?: Language.English,
            appearance = Appearance.entries.firstOrNull { it.name == appearance } ?: Appearance.System,
            colorTheme = ColorTheme.entries.firstOrNull { it.name == colorTheme } ?: ColorTheme.Ocean,
            routeIds = routeIds,
            stopIds = stopIds,
        )
}

class FilePreferencesRepository(
    private val files: PersistentFiles,
    private val logger: AppLogger = SilentLogger,
) : PreferencesRepository {
    private val mutableState = MutableStateFlow(Preferences())
    override val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private var loaded = false
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun readOnce() {
        if (loaded) return
        files.read("preferences-v1.json")?.let { mutableState.value = json.decodeFromString<PreferencesRecord>(it).domain() }
        loaded = true
    }

    private suspend fun operation(block: suspend () -> Unit): Outcome<Unit> =
        try {
            mutex.withLock {
                readOnce()
                block()
            }
            Outcome.Success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Outcome.Failure(AppError.StorageUnavailable)
        }

    override suspend fun load() = operation { }

    private suspend fun update(transform: (Preferences) -> Preferences) =
        operation {
            val next = transform(state.value)
            files.write(
                "preferences-v1.json",
                json.encodeToString(
                    PreferencesRecord(
                        language = next.language.name,
                        appearance = next.appearance.name,
                        colorTheme = next.colorTheme.name,
                        routeIds = next.routeIds,
                        stopIds = next.stopIds,
                    ),
                ),
            )
            mutableState.value = next
        }

    override suspend fun setLanguage(language: Language) = update { it.copy(language = language) }

    override suspend fun setAppearance(appearance: Appearance) = update { it.copy(appearance = appearance) }

    override suspend fun setColorTheme(colorTheme: ColorTheme) = update { it.copy(colorTheme = colorTheme) }

    override suspend fun toggleRoute(id: String) =
        update {
            it.copy(
                routeIds =
                    if (id in
                        it.routeIds
                    ) {
                        it.routeIds - id
                    } else {
                        it.routeIds + id
                    },
            )
        }

    override suspend fun toggleStop(id: String): Outcome<Unit> {
        val result = update { it.copy(stopIds = if (id in it.stopIds) it.stopIds - id else it.stopIds + id) }
        if (result is Outcome.Success) logger.diagnostic(LogEvent.FavoriteStopsChanged, "savedCount=${state.value.stopIds.size}")
        return result
    }
}
