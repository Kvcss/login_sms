package com.login.model

import kotlinx.serialization.Serializable

// Corpos de requisicao.
@Serializable
data class LoginRequest(val phone: String? = null, val uuid: String? = null)

@Serializable
data class ConfirmRequest(
    val phone: String? = null,
    val uuid: String? = null,
    val code: String? = null,
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val description: String? = null,
    val email: String? = null,
)

// Representacao publica do usuario (sem expor o uuid do dispositivo).
@Serializable
data class UserResponse(
    val id: Int,
    val phone: String,
    val name: String? = null,
    val description: String? = null,
    val email: String? = null,
    val active: Boolean,
)

// Resposta do 202 (codigo enviado). devCode so aparece em modo dev.
@Serializable
data class PendingResponse(val message: String, val devCode: String? = null)

@Serializable
data class ErrorResponse(val error: String)
