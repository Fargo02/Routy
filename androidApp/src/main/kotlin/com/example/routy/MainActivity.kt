package ge.routy.transport

import android.os.Bundle
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.routy.App
import com.example.routy.core.di.AppGraph
import com.example.routy.core.logging.PlatformLogger
import com.example.routy.core.preferences.domain.Appearance
import com.example.routy.core.storage.AndroidPersistentFiles
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
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
        splash.setKeepOnScreenCondition { !model.graph.settings.state.value.loaded }
        setContent {
            val preferences by model.graph.settings.state.collectAsStateWithLifecycle()
            val dark =
                preferences.appearance == Appearance.Dark ||
                    (
                        preferences.appearance == Appearance.System &&
                            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                    )
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !dark
            }
            App(model.graph)
        }
    }
}

class GraphModel(
    val graph: AppGraph,
) : ViewModel() {
    override fun onCleared() {
        graph.close()
    }
}
