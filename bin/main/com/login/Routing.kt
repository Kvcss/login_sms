package com.login

import com.login.model.ConfirmRequest
import com.login.model.ErrorResponse
import com.login.model.LoginRequest
import com.login.model.PendingResponse
import com.login.model.UpdateUserRequest
import com.login.service.ConfirmResult
import com.login.service.LoginResult
import com.login.service.SmsService
import com.login.service.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing

private val returnCode: Boolean
    get() = (System.getenv("SMS_RETURN_CODE_IN_RESPONSE") ?: "true").equals("true", true)

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respond(mapOf("status" to "ok", "service" to "login-sms-server"))
        }

        post("/users/login") {
            val body = call.receive<LoginRequest>()
            val phone = body.phone
            val uuid = body.uuid
            if (phone.isNullOrBlank() || uuid.isNullOrBlank()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("phone e uuid sao obrigatorios"),
                )
            }

            when (val result = UserService.login(phone, uuid)) {
                is LoginResult.LoggedIn -> call.respond(HttpStatusCode.OK, result.user)
                is LoginResult.CodeSent -> call.respond(
                    HttpStatusCode.Accepted,
                    PendingResponse(
                        message = "Codigo de confirmacao enviado por SMS. Confirme em /users/confirm.",
                        devCode = if (returnCode) result.devCode else null,
                    ),
                )
                is LoginResult.SmsError -> call.respond(
                    HttpStatusCode.BadGateway,
                    ErrorResponse("Nao foi possivel enviar o SMS: ${result.message}"),
                )
            }
        }

        post("/users/confirm") {
            val body = call.receive<ConfirmRequest>()
            val phone = body.phone
            val uuid = body.uuid
            val code = body.code
            if (phone.isNullOrBlank() || uuid.isNullOrBlank() || code.isNullOrBlank()) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("phone, uuid e code sao obrigatorios"),
                )
            }

            when (val result = UserService.confirm(phone, uuid, code)) {
                is ConfirmResult.Confirmed -> call.respond(HttpStatusCode.OK, result.user)
                ConfirmResult.NotFound -> call.respond(
                    HttpStatusCode.NotFound,
                    ErrorResponse("Nenhum codigo de confirmacao para esse telefone e uuid"),
                )
                ConfirmResult.Invalid -> call.respond(
                    HttpStatusCode.BadRequest, ErrorResponse("Codigo incorreto"),
                )
                ConfirmResult.Expired -> call.respond(
                    HttpStatusCode.BadRequest, ErrorResponse("Codigo expirado. Solicite um novo."),
                )
            }
        }

        put("/users/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("id invalido"))
            val body = call.receive<UpdateUserRequest>()
            val user = UserService.update(id, body)
                ?: return@put call.respond(HttpStatusCode.NotFound, ErrorResponse("Usuario nao encontrado"))
            call.respond(HttpStatusCode.OK, user)
        }

        get("/users/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("id invalido"))
            val user = UserService.getById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("Usuario nao encontrado"))
            call.respond(HttpStatusCode.OK, user)
        }
    }
}
