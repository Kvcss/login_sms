package com.login.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.login.app.data.AuthRepository
import com.login.app.data.ConfirmResult
import com.login.app.data.LoginResult
import com.login.app.data.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Telas do fluxo.
enum class Step { PHONE, CODE, PROFILE }

data class UiState(
    val step: Step = Step.PHONE,
    val loading: Boolean = false,
    val phone: String = "",
    val message: String? = null,     // erro/aviso para o usuario
    val devCode: String? = null,     // dica do codigo em modo mock (dev)
    val user: UserResponse? = null,
)

class LoginViewModel(
    private val repo: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // uuid do dispositivo, injetado pela Activity (vem do SharedPreferences).
    var uuid: String = ""

    // PASSO 1: enviar telefone -> POST /users/login
    fun submitPhone(phone: String) {
        if (phone.isBlank()) {
            _state.update { it.copy(message = "Informe o telefone") }
            return
        }
        _state.update { it.copy(loading = true, message = null, phone = phone) }
        viewModelScope.launch {
            when (val r = repo.login(phone, uuid)) {
                is LoginResult.LoggedIn ->
                    _state.update { it.copy(loading = false, step = Step.PROFILE, user = r.user) }
                is LoginResult.CodeSent ->
                    _state.update {
                        it.copy(
                            loading = false,
                            step = Step.CODE,
                            devCode = r.devCode,
                            message = "Codigo enviado por SMS.",
                        )
                    }
                is LoginResult.Error ->
                    _state.update { it.copy(loading = false, message = r.message) }
            }
        }
    }

    // PASSO 2: confirmar codigo -> POST /users/confirm
    fun submitCode(code: String) {
        if (code.isBlank()) {
            _state.update { it.copy(message = "Informe o codigo") }
            return
        }
        _state.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            when (val r = repo.confirm(_state.value.phone, uuid, code)) {
                is ConfirmResult.Confirmed ->
                    _state.update { it.copy(loading = false, step = Step.PROFILE, user = r.user, devCode = null) }
                ConfirmResult.NotFound ->
                    _state.update { it.copy(loading = false, message = "Nenhum codigo enviado para este numero.") }
                is ConfirmResult.Invalid ->
                    _state.update { it.copy(loading = false, message = r.message) }
                is ConfirmResult.Error ->
                    _state.update { it.copy(loading = false, message = r.message) }
            }
        }
    }

    // PASSO 3: salvar dados do perfil -> PUT /users/{id}
    fun saveProfile(name: String, description: String, email: String) {
        val user = _state.value.user ?: return
        _state.update { it.copy(loading = true, message = null) }
        viewModelScope.launch {
            val result = repo.updateUser(
                id = user.id,
                name = name.ifBlank { null },
                description = description.ifBlank { null },
                email = email.ifBlank { null },
            )
            result
                .onSuccess { updated ->
                    _state.update { it.copy(loading = false, user = updated, message = "Perfil salvo!") }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, message = e.message ?: "Falha ao salvar") }
                }
        }
    }

    fun logout() {
        _state.value = UiState()
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
