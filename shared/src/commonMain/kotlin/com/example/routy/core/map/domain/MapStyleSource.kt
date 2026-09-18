package com.example.routy.core.map.domain

interface MapStyleSource {
    suspend fun style(uri: String): String?
}
