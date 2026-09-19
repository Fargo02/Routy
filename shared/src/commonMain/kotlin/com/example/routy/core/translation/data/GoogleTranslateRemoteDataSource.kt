package com.example.routy.core.translation.data

import com.example.routy.core.translation.domain.TranslationRemoteDataSource
import com.example.routy.core.translation.domain.translationCode
import com.example.routy.core.transport.domain.Language
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

class GoogleTranslateRemoteDataSource(
    private val client: HttpClient,
    private val endpoint: String = "https://clients5.google.com/translate_a/t",
) : TranslationRemoteDataSource {
    init {
        require(endpoint.startsWith("https://"))
    }

    override suspend fun translate(
        texts: List<String>,
        language: Language,
    ): List<String> {
        if (texts.isEmpty()) return emptyList()
        val payload =
            client
                .get(endpoint) {
                    parameter("client", "dict-chrome-ex")
                    parameter("sl", "auto")
                    parameter("tl", language.translationCode)
                    texts.forEach { parameter("q", it) }
                }.bodyAsText()
        return parseTranslations(payload, texts.size)
    }
}

fun parseTranslations(
    payload: String,
    expected: Int,
): List<String> {
    val root = Json.parseToJsonElement(payload)
    require(root is JsonArray && root.size == expected) { "Unexpected translation payload" }
    return root.map { entry ->
        when (entry) {
            is JsonPrimitive -> entry.content
            is JsonArray -> (entry.firstOrNull() as? JsonPrimitive)?.content.orEmpty()
            else -> ""
        }
    }
}
