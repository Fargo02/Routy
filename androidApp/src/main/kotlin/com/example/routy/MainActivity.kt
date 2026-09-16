package com.example.routy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.routy.core.di.AppGraph
import com.example.routy.core.logging.PlatformLogger
import com.example.routy.core.storage.AndroidPersistentFiles
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val model =
            ViewModelProvider(
                this,
                object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T =
                        GraphModel(AppGraph(HttpClient(OkHttp), AndroidPersistentFiles(applicationContext), logger = PlatformLogger())) as T
                },
            )[GraphModel::class.java]
        setContent { App(model.graph) }
    }
}

class GraphModel(
    val graph: AppGraph,
) : ViewModel() {
    override fun onCleared() {
        graph.close()
    }
}
