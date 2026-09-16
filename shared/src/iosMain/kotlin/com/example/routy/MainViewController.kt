package com.example.routy

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.example.routy.core.di.AppGraph
import com.example.routy.core.logging.PlatformLogger
import com.example.routy.core.storage.IosPersistentFiles
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

fun mainViewController() =
    ComposeUIViewController {
        val graph = remember { AppGraph(HttpClient(Darwin), IosPersistentFiles(), logger = PlatformLogger()) }
        DisposableEffect(graph) { onDispose { graph.close() } }
        App(graph)
    }
