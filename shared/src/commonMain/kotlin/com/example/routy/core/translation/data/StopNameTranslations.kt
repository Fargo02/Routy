package com.example.routy.core.translation.data

import com.example.routy.core.logging.AppLogger
import com.example.routy.core.logging.LogEvent
import com.example.routy.core.logging.LogLevel
import com.example.routy.core.logging.SilentLogger
import com.example.routy.core.translation.domain.TranslationRemoteDataSource
import com.example.routy.core.translation.domain.tidyTranslation
import com.example.routy.core.translation.domain.translationBatches
import com.example.routy.core.translation.domain.translationCode
import com.example.routy.core.transport.data.PersistentFiles
import com.example.routy.core.transport.data.transportOperation
import com.example.routy.core.transport.domain.AppError
import com.example.routy.core.transport.domain.Language
import com.example.routy.core.transport.domain.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class StopNameTranslations(
    private val remote: TranslationRemoteDataSource,
    private val files: PersistentFiles,
    private val scope: CoroutineScope,
    private val logger: AppLogger = SilentLogger,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val maxTexts: Int = 30,
    private val maxCharacters: Int = 400,
) {
    private val mutableState = MutableStateFlow(emptyMap<Language, Map<String, String>>())
    val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private val queued = mutableMapOf<Language, MutableSet<String>>()
    private val working = mutableSetOf<Language>()
    private val restored = mutableSetOf<Language>()
    private val json = Json { ignoreUnknownKeys = true }

    fun request(
        texts: Set<String>,
        language: Language,
    ) {
        if (texts.isEmpty()) return
        scope.launch {
            mutex.withLock {
                restoreOnce(language)
                val known = mutableState.value[language].orEmpty()
                val missing = texts.filterNot { it.isBlank() || it in known }
                if (missing.isEmpty()) return@withLock
                queued.getOrPut(language) { linkedSetOf() } += missing
                if (!working.add(language)) return@withLock
                scope.launch { translateQueued(language) }
            }
        }
    }

    private suspend fun translateQueued(language: Language) {
        while (true) {
            val batch =
                mutex.withLock {
                    translationBatches(queued[language].orEmpty().toList(), maxTexts, maxCharacters)
                        .firstOrNull()
                        .also { if (it.isNullOrEmpty()) working.remove(language) }
                }
            if (batch.isNullOrEmpty()) return
            when (val result = transportOperation(dispatcher) { remote.translate(batch, language) }) {
                is Outcome.Success ->
                    mutex.withLock {
                        store(language, batch.zip(result.value) { source, text -> source to tidyTranslation(source, text) }.toMap())
                    }
                is Outcome.Failure -> {
                    logger.log(LogLevel.Warning, LogEvent.StopNamesTranslationFailed, result.error)
                    mutex.withLock {
                        queued.remove(language)
                        working.remove(language)
                    }
                    return
                }
            }
        }
    }

    private suspend fun store(
        language: Language,
        translations: Map<String, String>,
    ) {
        queued[language]?.removeAll(translations.keys)
        val merged = mutableState.value[language].orEmpty() + translations.filterValues { it.isNotBlank() }
        mutableState.value = mutableState.value + (language to merged)
        logger.diagnostic(LogEvent.StopNamesTranslated, "language=${language.name}, count=${merged.size}")
        persist(language, merged)
    }

    private suspend fun restoreOnce(language: Language) {
        if (!restored.add(language)) return
        try {
            val content = files.read(fileName(language)) ?: return
            val cached = json.decodeFromString<Map<String, String>>(content)
            mutableState.value = mutableState.value + (language to (cached + mutableState.value[language].orEmpty()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            logger.log(LogLevel.Warning, LogEvent.CacheReadFailed, AppError.StorageUnavailable)
        }
    }

    private suspend fun persist(
        language: Language,
        translations: Map<String, String>,
    ) {
        try {
            files.write(fileName(language), json.encodeToString(translations))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            logger.log(LogLevel.Warning, LogEvent.CacheWriteFailed, AppError.StorageUnavailable)
        }
    }

    private fun fileName(language: Language) = "translations-${language.translationCode}-v1.json"
}
