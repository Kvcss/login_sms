package com.login.app.data

sealed interface LoginResult {
    data class LoggedIn(val user: UserResponse) : LoginResult
    data class CodeSent(val devCode: String?) : LoginResult
    data class Error(val message: String) : LoginResult
}

sealed interface ConfirmResult {
    data class Confirmed(val user: UserResponse) : ConfirmResult
    object NotFound : ConfirmResult
    data class Invalid(val message: String) : ConfirmResult
    data class Error(val message: String) : ConfirmResult
}

class AuthRepository(private val api: LoginApi = Network.api) {

    private val json = Network.jsonParser

    suspend fun login(phone: String, uuid: String): LoginResult = try {
        val res = api.login(LoginRequest(phone, uuid))
        val raw = res.body()?.string() ?: res.errorBody()?.string() ?: ""
        when (res.code()) {
            200 -> LoginResult.LoggedIn(json.decodeFromString(UserResponse.serializer(), raw))
            202 -> {
                val pending = json.decodeFromString(PendingResponse.serializer(), raw)
                LoginResult.CodeSent(pending.devCode)
            }
            else -> LoginResult.Error(parseError(raw, "Erro ${res.code()}"))
        }
    } catch (e: Exception) {
        LoginResult.Error(e.message ?: "Falha de conexao")
    }

    suspend fun confirm(phone: String, uuid: String, code: String): ConfirmResult = try {
        val res = api.confirm(ConfirmRequest(phone, uuid, code))
        val raw = res.body()?.string() ?: res.errorBody()?.string() ?: ""
        when (res.code()) {
            200 -> ConfirmResult.Confirmed(json.decodeFromString(UserResponse.serializer(), raw))
            404 -> ConfirmResult.NotFound
            400 -> ConfirmResult.Invalid(parseError(raw, "Codigo invalido"))
            else -> ConfirmResult.Error(parseError(raw, "Erro ${res.code()}"))
        }
    } catch (e: Exception) {
        ConfirmResult.Error(e.message ?: "Falha de conexao")
    }

    suspend fun updateUser(
        id: Int,
        name: String?,
        description: String?,
        email: String?,
    ): Result<UserResponse> = try {
        val res = api.updateUser(id, UpdateUserRequest(name, description, email))
        val user = res.body()
        if (res.isSuccessful && user != null) Result.success(user)
        else Result.failure(Exception("Erro ${res.code()}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun parseError(raw: String, fallback: String): String = try {
        json.decodeFromString(ErrorResponse.serializer(), raw).error ?: fallback
    } catch (_: Exception) {
        fallback
    }
}
