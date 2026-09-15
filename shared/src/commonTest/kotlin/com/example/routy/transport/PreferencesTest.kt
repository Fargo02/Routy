package com.example.routy.transport

import com.example.routy.core.localization.*
import com.example.routy.core.preferences.data.FilePreferencesRepository
import com.example.routy.core.preferences.domain.Appearance
import com.example.routy.core.transport.data.PersistentFiles
import com.example.routy.core.transport.domain.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class PreferencesTest {
    private class Files : PersistentFiles {
        val records = mutableMapOf<String, String>()
        var fails = false

        override suspend fun read(name: String) = records[name]

        override suspend fun write(
            name: String,
            content: String,
        ) {
            if (fails) error("full")
            records[name] = content
        }
    }

    @Test fun favoritesAndSettingsSurviveRepositoryRecreation() =
        runTest {
            val files = Files()
            val repository = FilePreferencesRepository(files)
            repository.toggleRoute("r")
            repository.toggleStop("s")
            repository.setLanguage(Language.Georgian)
            repository.setAppearance(Appearance.Dark)
            val restored = FilePreferencesRepository(files)
            restored.load()
            assertEquals(repository.state.value, restored.state.value)
            restored.toggleRoute("r")
            assertTrue(
                restored.state.value.routeIds
                    .isEmpty(),
            )
        }

    @Test fun failedWriteDoesNotPretendFavoriteWasSaved() =
        runTest {
            val files = Files()
            val repository = FilePreferencesRepository(files)
            files.fails = true
            assertEquals(Outcome.Failure(AppError.StorageUnavailable), repository.toggleRoute("r"))
            assertTrue(
                repository.state.value.routeIds
                    .isEmpty(),
            )
        }

    @Test fun bothLocalizationCatalogsAreComplete() {
        for (language in Language.entries) for (key in TextKey.entries) assertTrue(Strings(language)[key].isNotBlank())
    }
}
