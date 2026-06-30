package com.login

import com.login.db.DatabaseFactory
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

fun main() {
    val port = (System.getenv("PORT") ?: "3000").toInt()
    DatabaseFactory.init()
    println("Servidor login-sms iniciando em http://localhost:$port")
    println("Provedor de SMS: ${com.login.service.SmsService.provider}")
    embeddedServer(Netty, port = port) {
        module()
    }.start(wait = true)
}

// Modulo da aplicacao: registra serializacao JSON e as rotas.
// Separado para ser reutilizado nos testes (testApplication).
fun Application.module() {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    configureRouting()
}
