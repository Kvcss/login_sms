package com.login.service

import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlin.random.Random

// Servico de envio de SMS.
//
// Dois modos, controlados pela variavel de ambiente SMS_PROVIDER:
//   - "mock"   -> nao envia nada de verdade, apenas imprime o codigo no console.
//                 Ideal para desenvolvimento, testes e gravacao do video.
//   - "twilio" -> envia um SMS real chamando a API REST da Twilio.
object SmsService {

    private fun env(key: String, default: String = "") = System.getenv(key) ?: default

    val provider: String get() = env("SMS_PROVIDER", "mock").lowercase()

    // Gera um codigo numerico de 6 digitos.
    fun generateCode(): String = Random.nextInt(100000, 1000000).toString()

    fun sendCode(phone: String, code: String) {
        val message = "Seu codigo de confirmacao e: $code"
        if (provider == "twilio") {
            sendViaTwilio(phone, message)
            println("[sms] SMS enviado via Twilio para $phone")
        } else {
            // Modo mock: apenas loga. Assim da pra ver o codigo durante o video/testes.
            println("[sms:mock] Para $phone -> \"$message\"")
        }
    }

    private fun sendViaTwilio(to: String, body: String) {
        val sid = env("TWILIO_ACCOUNT_SID")
        val token = env("TWILIO_AUTH_TOKEN")
        val from = env("TWILIO_FROM_NUMBER")
        val url = "https://api.twilio.com/2010-04-01/Accounts/$sid/Messages.json"

        fun enc(v: String) = URLEncoder.encode(v, StandardCharsets.UTF_8)
        val form = "To=${enc(to)}&From=${enc(from)}&Body=${enc(body)}"
        val auth = Base64.getEncoder()
            .encodeToString("$sid:$token".toByteArray(StandardCharsets.UTF_8))

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Basic $auth")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build()

        val response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw RuntimeException("Falha ao enviar SMS via Twilio: ${response.statusCode()} ${response.body()}")
        }
    }
}
