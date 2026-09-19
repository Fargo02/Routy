package com.example.routy.core.translation.data

import com.example.routy.core.preferences.domain.PreferencesRepository
import com.example.routy.core.translation.domain.untranslatedStopNames
import com.example.routy.core.translation.domain.withTranslatedStopNames
import com.example.routy.core.transport.domain.NetworkState
import com.example.routy.core.transport.domain.TransportRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TranslatedTransportRepository(
    private val delegate: TransportRepository,
    translations: StopNameTranslations,
    preferences: PreferencesRepository,
    scope: CoroutineScope,
) : TransportRepository {
    private val language = preferences.state.map { it.language }.distinctUntilChanged()

    override val state: StateFlow<NetworkState> =
        combine(delegate.state, language, translations.state) { network, language, known ->
            network.copy(network = network.network?.withTranslatedStopNames(language, known[language].orEmpty()))
        }.stateIn(scope, SharingStarted.Eagerly, delegate.state.value)

    init {
        scope.launch {
            combine(delegate.state, language) { network, language -> network.network to language }
                .collect { (network, language) ->
                    translations.request(network?.untranslatedStopNames(language).orEmpty(), language)
                }
        }
    }

    override suspend fun refresh() = delegate.refresh()

    override fun observeNetwork() = delegate.observeNetwork()
}
