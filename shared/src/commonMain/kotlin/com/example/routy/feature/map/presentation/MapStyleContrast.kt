package com.example.routy.feature.map.presentation

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.roundToInt

private const val ContrastPivot = 0.05
private const val ContrastGain = 2.4

fun contrastBoostedStyle(style: String): String {
    val root = runCatching { Json.parseToJsonElement(style) }.getOrNull() as? JsonObject ?: return style
    val layers = root["layers"] as? JsonArray ?: return style
    return JsonObject(root + ("layers" to JsonArray(layers.map(::boostedLayer)))).toString()
}

private fun boostedLayer(layer: JsonElement): JsonElement {
    val properties = layer as? JsonObject ?: return layer
    val paint = properties["paint"] as? JsonObject ?: return layer
    return JsonObject(properties + ("paint" to boostedColors(paint)))
}

private fun boostedColors(element: JsonElement): JsonElement =
    when (element) {
        is JsonObject -> JsonObject(element.mapValues { boostedColors(it.value) })
        is JsonArray -> JsonArray(element.map(::boostedColors))
        is JsonPrimitive -> if (element.isString) JsonPrimitive(boostedColor(element.content)) else element
    }

private fun boostedColor(value: String): String {
    val color = value.trim().lowercase()
    return when {
        color.startsWith("#") -> boostedHex(color)
        color.startsWith("rgb") -> boostedRgb(color)
        color.startsWith("hsl") -> boostedHsl(color)
        else -> null
    } ?: value
}

private fun boostedHex(color: String): String? {
    val digits = color.removePrefix("#")
    val pairs =
        when (digits.length) {
            3, 4 -> digits.map { "$it$it" }
            6, 8 -> digits.chunked(2)
            else -> return null
        }
    val channels = pairs.map { it.toIntOrNull(16) ?: return null }
    val boosted = channels.take(3).map { boosted(it / 255.0) }
    return if (pairs.size == 3) rgb(boosted) else rgba(boosted, (channels[3] / 255.0).toString())
}

private fun boostedRgb(color: String): String? {
    val parts = components(color) ?: return null
    val channels = parts.take(3).map { (it.toDoubleOrNull() ?: return null) / 255.0 }
    if (channels.size != 3) return null
    val boosted = channels.map(::boosted)
    return if (parts.size == 3) rgb(boosted) else rgba(boosted, parts[3])
}

private fun boostedHsl(color: String): String? {
    val parts = components(color) ?: return null
    val lightness = parts.getOrNull(2)?.removeSuffix("%")?.toDoubleOrNull() ?: return null
    val boosted = "${parts[0]},${parts[1]},${(boosted(lightness / 100) * 100).roundToInt()}%"
    return if (parts.size == 3) "hsl($boosted)" else "hsla($boosted,${parts[3]})"
}

private fun components(color: String): List<String>? =
    color
        .substringAfter('(', "")
        .substringBefore(')', "")
        .split(',')
        .map { it.trim() }
        .takeIf { it.size == 3 || it.size == 4 }

private fun boosted(channel: Double): Double = (ContrastPivot + (channel - ContrastPivot) * ContrastGain).coerceIn(0.0, 1.0)

private fun rgb(channels: List<Double>): String = "rgb(${channels.joinToString(",") { (it * 255).roundToInt().toString() }})"

private fun rgba(channels: List<Double>, alpha: String): String =
    "rgba(${channels.joinToString(",") { (it * 255).roundToInt().toString() }},$alpha)"
