package com.login.app.data

import kotlinx.serialization.Serializable

// DTOs que espelham os do servidor (com.login.model).

@Serializable
data class LoginRequest(val phone: String, val uuid: String)

@Serializable
data class ConfirmRequest(val phone: String, val uuid: String, val code: String)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val description: String? = null,
    val email: String? = null,
)

@Serializable
data class UserResponse(
    val id: Int,
    val phone: String,
    val name: String? = null,
    val description: String? = null,
    val email: String? = null,
    val active: Boolean,
)

@Serializable
data class PendingResponse(val message: String, val devCode: String? = null)

@Serializable
data class ErrorResponse(val error: String? = null)
