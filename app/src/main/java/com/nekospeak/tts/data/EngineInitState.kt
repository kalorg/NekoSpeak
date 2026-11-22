package com.nekospeak.tts.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * État de chargement du moteur TTS, partagé entre le service (producteur)
 * et l'interface (observateur du bandeau d'état).
 *
 * Le service met à jour cet état au fil de reloadEngine() : l'UI peut ainsi
 * montrer « initialisation en cours » puis « moteur prêt » sans être liée au
 * service, même quand l'init est déclenché par un client externe.
 *
 * IDLE = le service n'a pas encore tourné depuis le lancement de l'app :
 * rien à afficher (aucune initialisation n'est en attente).
 */
object EngineInitState {

    enum class Status { IDLE, INITIALIZING, READY, FAILED }

    data class State(
        val status: Status = Status.IDLE,
        val model: String = "",
        val message: String? = null,
        val changedAt: Long = 0L
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun initializing(model: String) {
        _state.value = State(
            status = Status.INITIALIZING,
            model = model,
            changedAt = System.currentTimeMillis()
        )
    }

    fun ready(model: String) {
        _state.value = State(
            status = Status.READY,
            model = model,
            changedAt = System.currentTimeMillis()
        )
    }

    fun failed(model: String, message: String? = null) {
        _state.value = State(
            status = Status.FAILED,
            model = model,
            message = message,
            changedAt = System.currentTimeMillis()
        )
    }
}
