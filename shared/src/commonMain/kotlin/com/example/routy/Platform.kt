package com.example.routy

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform