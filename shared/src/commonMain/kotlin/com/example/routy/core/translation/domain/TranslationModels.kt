package com.example.routy.core.translation.domain

import com.example.routy.core.transport.domain.Language
import com.example.routy.core.transport.domain.TransportNetwork

interface TranslationRemoteDataSource {
    suspend fun translate(
        texts: List<String>,
        language: Language,
    ): List<String>
}

val Language.translationCode: String
    get() =
        when (this) {
            Language.English -> "en"
            Language.Georgian -> "ka"
            Language.Russian -> "ru"
        }

fun TransportNetwork.untranslatedStopNames(language: Language): Set<String> =
    stops.mapNotNullTo(mutableSetOf()) { it.name.translationSource(language) }

fun TransportNetwork.withTranslatedStopNames(
    language: Language,
    translations: Map<String, String>,
): TransportNetwork {
    if (translations.isEmpty()) return this
    return copy(
        stops =
            stops.map { stop ->
                val source = stop.name.translationSource(language) ?: return@map stop
                val translation = translations[source]?.takeIf { it.isNotBlank() && it != source } ?: return@map stop
                stop.copy(name = stop.name.copy(translated = translation))
            },
    )
}

fun tidyTranslation(
    source: String,
    translation: String,
): String {
    val tidied = translation.trim()
    return if (source.trim().endsWith(".") || !tidied.endsWith(".")) tidied else tidied.dropLast(1).trimEnd()
}

fun translationBatches(
    texts: List<String>,
    maxTexts: Int,
    maxCharacters: Int,
): List<List<String>> {
    val batches = mutableListOf<List<String>>()
    var batch = mutableListOf<String>()
    var characters = 0
    texts.forEach { text ->
        if (batch.isNotEmpty() && (batch.size >= maxTexts || characters + text.length > maxCharacters)) {
            batches += batch
            batch = mutableListOf()
            characters = 0
        }
        batch += text
        characters += text.length
    }
    if (batch.isNotEmpty()) batches += batch
    return batches
}
