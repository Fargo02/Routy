@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.example.routy.transport

import com.example.routy.core.preferences.domain.*
import com.example.routy.core.translation.data.GoogleTranslateRemoteDataSource
import com.example.routy.core.translation.data.StopNameTranslations
import com.example.routy.core.translation.data.TranslatedTransportRepository
import com.example.routy.core.translation.data.parseTranslations
import com.example.routy.core.translation.domain.TranslationRemoteDataSource
import com.example.routy.core.translation.domain.tidyTranslation
import com.example.routy.core.translation.domain.translationBatches
import com.example.routy.core.translation.domain.untranslatedStopNames
import com.example.routy.core.translation.domain.withTranslatedStopNames
import com.example.routy.core.transport.data.PersistentFiles
import com.example.routy.core.transport.data.TransportParser
import com.example.routy.core.transport.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class TranslationTest {
    private class Remote(
        private val failure: Boolean = false,
    ) : TranslationRemoteDataSource {
        val batches = mutableListOf<List<String>>()

        override suspend fun translate(
            texts: List<String>,
            language: Language,
        ): List<String> {
            batches += texts
            if (failure) error("offline")
            return texts.map { "${language.name}:$it" }
        }
    }

    private class Files : PersistentFiles {
        val contents = mutableMapOf<String, String>()

        override suspend fun read(name: String) = contents[name]

        override suspend fun write(
            name: String,
            content: String,
        ) {
            contents[name] = content
        }
    }

    private class Preferences(
        language: Language,
    ) : PreferencesRepository {
        override val state =
            MutableStateFlow(
                com.example.routy.core.preferences.domain
                    .Preferences(language = language),
            )

        override suspend fun load() = Outcome.Success(Unit)

        override suspend fun setLanguage(language: Language) = Outcome.Success(Unit)

        override suspend fun setAppearance(appearance: Appearance) = Outcome.Success(Unit)

        override suspend fun setColorTheme(colorTheme: ColorTheme) = Outcome.Success(Unit)

        override suspend fun toggleRoute(id: String) = Outcome.Success(Unit)

        override suspend fun toggleStop(id: String) = Outcome.Success(Unit)
    }

    private val georgianName = LocalizedName(english = null, georgian = "ვოკზალი", original = "Vokzali")

    @Test fun readsTheTranslationOfEveryRequestedName() {
        assertEquals(
            listOf("Горгасали", "Вокзал"),
            parseTranslations("""[["Горгасали","ka"],["Вокзал","ka"]]""", expected = 2),
        )
    }

    @Test fun readsATranslationReturnedWithoutItsDetectedLanguage() {
        assertEquals(listOf("Gorgasali"), parseTranslations("""["Gorgasali"]""", expected = 1))
    }

    @Test fun rejectsAPayloadThatDoesNotAnswerEveryName() {
        assertFailsWith<IllegalArgumentException> { parseTranslations("""[["Вокзал","ka"]]""", expected = 2) }
        assertFailsWith<IllegalArgumentException> { parseTranslations("""{"error":429}""", expected = 1) }
    }

    @Test fun requiresASecureEndpoint() {
        assertFailsWith<IllegalArgumentException> {
            GoogleTranslateRemoteDataSource(io.ktor.client.HttpClient(), endpoint = "http://clients5.google.com/translate_a/t")
        }
    }

    @Test fun splitsNamesIntoBatchesTheUrlCanCarry() {
        assertEquals(
            listOf(listOf("aa", "bb"), listOf("cc")),
            translationBatches(listOf("aa", "bb", "cc"), maxTexts = 2, maxCharacters = 100),
        )
        assertEquals(
            listOf(listOf("aaaa"), listOf("bb", "cc")),
            translationBatches(listOf("aaaa", "bb", "cc"), maxTexts = 9, maxCharacters = 5),
        )
        assertEquals(listOf(listOf("aaaaaaa")), translationBatches(listOf("aaaaaaa"), maxTexts = 9, maxCharacters = 5))
    }

    @Test fun dropsTheSentencePeriodTheTranslatorAddsToAName() {
        assertEquals("Ледовая арена Батуми", tidyTranslation("ბათუმის ყინულის არენა", "Ледовая арена Батуми."))
        assertEquals("Улица Гришашвили №14", tidyTranslation("გრიშაშვილის ქუჩა #14", " Улица Гришашвили №14. "))
        assertEquals("Проспект Руставели.", tidyTranslation("რუსთაველის გამზირი.", "Проспект Руставели."))
    }

    @Test fun translatesOnlyTheNamesTheLanguageIsMissing() {
        assertEquals("ვოკზალი", georgianName.translationSource(Language.Russian))
        assertEquals("ვოკზალი", georgianName.translationSource(Language.English))
        assertNull(georgianName.translationSource(Language.Georgian))
        assertNull(LocalizedName("Station", "ვოკზალი", null).translationSource(Language.English))
    }

    @Test fun translatesANameStoredInTheWrongScript() {
        val georgianEnglish = LocalizedName(english = "ბათუმის ყინულის არენა", georgian = "ბათუმის ყინულის არენა", original = null)
        assertEquals("ბათუმის ყინულის არენა", georgianEnglish.translationSource(Language.English))
        assertEquals("Batumi Ice Arena", georgianEnglish.copy(translated = "Batumi Ice Arena").resolve(Language.English, "id"))
    }

    @Test fun prefersTheOfficialNameOverTheTranslation() {
        val name = LocalizedName("Station", "ვოკზალი", null, translated = "Вокзал")
        assertEquals("Station", name.resolve(Language.English, "id"))
        assertEquals("ვოკზალი", name.resolve(Language.Georgian, "id"))
        assertEquals("Вокзал", name.resolve(Language.Russian, "id"))
    }

    @Test fun findsAStopByItsTranslatedName() {
        assertTrue(georgianName.copy(translated = "Вокзал").matches("вокз"))
        assertFalse(georgianName.matches("вокз"))
    }

    @Test fun translatesEveryStopOfTheNetworkOnce() =
        runTest {
            val remote = Remote()
            val files = Files()
            val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val translations = StopNameTranslations(remote, files, scope, dispatcher = StandardTestDispatcher(testScheduler))
            translations.request(setOf("ვოკზალი", "გორგასალი"), Language.Russian)
            advanceUntilIdle()
            translations.request(setOf("ვოკზალი", "გორგასალი"), Language.Russian)
            advanceUntilIdle()
            assertEquals(1, remote.batches.size)
            assertEquals(
                mapOf("ვოკზალი" to "Russian:ვოკზალი", "გორგასალი" to "Russian:გორგასალი"),
                translations.state.value[Language.Russian],
            )
            scope.cancel()
        }

    @Test fun keepsTranslationsBetweenLaunches() =
        runTest {
            val files = Files()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            StopNameTranslations(Remote(), files, scope, dispatcher = dispatcher).request(setOf("ვოკზალი"), Language.Russian)
            advanceUntilIdle()
            val restarted = Remote()
            val translations = StopNameTranslations(restarted, files, scope, dispatcher = dispatcher)
            translations.request(setOf("ვოკზალი"), Language.Russian)
            advanceUntilIdle()
            assertTrue(restarted.batches.isEmpty())
            assertEquals("Russian:ვოკზალი", translations.state.value[Language.Russian]?.get("ვოკზალი"))
            scope.cancel()
        }

    @Test fun stopsRequestingAfterAFailureInsteadOfLooping() =
        runTest {
            val remote = Remote(failure = true)
            val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
            val translations = StopNameTranslations(remote, Files(), scope, dispatcher = StandardTestDispatcher(testScheduler))
            translations.request(setOf("ვოკზალი"), Language.Russian)
            advanceUntilIdle()
            assertEquals(1, remote.batches.size)
            assertNull(translations.state.value[Language.Russian])
            scope.cancel()
        }

    @Test fun showsTranslatedStopNamesToTheScreens() =
        runTest {
            val network = TransportParser().network(TransportParserTest.fixture)
            val remote = Remote()
            val dispatcher = StandardTestDispatcher(testScheduler)
            val scope = CoroutineScope(dispatcher)
            val translations = StopNameTranslations(remote, Files(), scope, dispatcher = dispatcher)
            val repository =
                TranslatedTransportRepository(
                    object : TransportRepository {
                        override val state = MutableStateFlow(NetworkState(network, isStale = false))

                        override suspend fun refresh() = Unit

                        override fun observeNetwork() = state
                    },
                    translations,
                    Preferences(Language.Russian),
                    scope,
                )
            advanceUntilIdle()
            val sources = network.untranslatedStopNames(Language.Russian)
            assertTrue(sources.isNotEmpty())
            assertEquals(sources, remote.batches.flatten().toSet())
            val stop =
                repository.state.value.network
                    ?.stops
                    ?.first()
            assertEquals("Russian:${stop?.name?.translationSource(Language.Russian)}", stop?.name?.translated)
            scope.cancel()
        }

    @Test fun leavesTheNetworkAloneWhileNothingIsTranslated() {
        val network = TransportParser().network(TransportParserTest.fixture)
        assertSame(network, network.withTranslatedStopNames(Language.Russian, emptyMap()))
    }
}
