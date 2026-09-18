package com.example.routy.transport

import com.example.routy.feature.map.presentation.contrastBoostedStyle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class MapStyleContrastTest {
    private val style =
        """
        {
          "version": 8,
          "sources": {"openmaptiles": {"type": "vector", "url": "https://tiles.openfreemap.org/planet"}},
          "layers": [
            {"id": "background", "type": "background", "paint": {"background-color": "rgb(12,12,12)"}},
            {"id": "water", "type": "fill", "paint": {"fill-color": "rgb(27 ,27 ,29)"}},
            {"id": "road", "type": "line", "paint": {"line-color": "#181818", "line-width": 2}},
            {"id": "tunnel", "type": "line", "paint": {"line-color": "hsl(0,0%,23%)"}},
            {
              "id": "label",
              "type": "symbol",
              "layout": {"text-font": ["Noto Sans Regular"]},
              "paint": {
                "text-color": "rgb(101,101,101)",
                "text-halo-color": "rgba(0,0,0,0.7)",
                "text-halo-blur": ["interpolate", ["linear"], ["zoom"], 8, 0, 12, 1]
              }
            },
            {"id": "glacier", "type": "fill", "paint": {"fill-color": "hsla(0,0%,85%,0.53)"}}
          ]
        }
        """.trimIndent()

    private fun paint(
        boosted: String,
        id: String,
    ): JsonObject =
        (Json.parseToJsonElement(boosted).jsonObject["layers"] as JsonArray)
            .map { it.jsonObject }
            .first { it["id"]?.jsonPrimitive?.content == id }["paint"]!!
            .jsonObject

    @Test fun brightensColorsAwayFromTheBackground() {
        val boosted = contrastBoostedStyle(style)
        assertEquals("rgb(11,11,11)", paint(boosted, "background")["background-color"]?.jsonPrimitive?.content)
        assertEquals("rgb(47,47,52)", paint(boosted, "water")["fill-color"]?.jsonPrimitive?.content)
        assertEquals("rgb(40,40,40)", paint(boosted, "road")["line-color"]?.jsonPrimitive?.content)
        assertEquals("hsl(0,0%,48%)", paint(boosted, "tunnel")["line-color"]?.jsonPrimitive?.content)
        assertEquals("rgb(225,225,225)", paint(boosted, "label")["text-color"]?.jsonPrimitive?.content)
        assertEquals("hsla(0,0%,100%,0.53)", paint(boosted, "glacier")["fill-color"]?.jsonPrimitive?.content)
    }

    @Test fun keepsHalosDarkAndLeavesEverythingElseIntact() {
        val boosted = contrastBoostedStyle(style)
        val label = paint(boosted, "label")
        assertEquals("rgba(0,0,0,0.7)", label["text-halo-color"]?.jsonPrimitive?.content)
        assertEquals(
            listOf("interpolate", "linear", "zoom"),
            label["text-halo-blur"]!!
                .jsonArray
                .flatMap { if (it is JsonArray) it.map { token -> token.jsonPrimitive.content } else listOf(it.jsonPrimitive.content) }
                .filter { it.toDoubleOrNull() == null },
        )
        assertEquals(2, paint(boosted, "road")["line-width"]?.jsonPrimitive?.content?.toInt())
        val root = Json.parseToJsonElement(boosted).jsonObject
        assertEquals(Json.parseToJsonElement(style).jsonObject["sources"], root["sources"])
        assertEquals(8, root["version"]?.jsonPrimitive?.content?.toInt())
    }

    @Test fun returnsInputWhenItIsNotAStyle() {
        assertEquals("not json", contrastBoostedStyle("not json"))
        assertEquals("""{"version":8}""", contrastBoostedStyle("""{"version":8}"""))
    }
}
