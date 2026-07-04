package com.login.model

import kotlinx.serialization.Serializable

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
data class ErrorResponse(val error: String)
