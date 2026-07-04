package com.login

import com.login.db.DatabaseFactory
import com.login.model.PendingResponse
import com.login.model.UserResponse
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ApiTest {

    private val phone = "+5511999999999"
    private val uuidA = "device-aaa"
    private val uuidB = "device-bbb"

    @BeforeTest
    fun setup() {
        System.setProperty("DB_URL", "jdbc:sqlite:build/test.db")
        DatabaseFactory.init()
        DatabaseFactory.clear()
    }

    @AfterTest
    fun teardown() {
        DatabaseFactory.clear()
    }

    private fun runApi(block: suspend (io.ktor.client.HttpClient) -> Unit) = testApplication {
        application { module() }
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        block(client)
    }

    private suspend fun login(client: io.ktor.client.HttpClient, phone: String, uuid: String) =
        client.post("/users/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("phone" to phone, "uuid" to uuid))
        }

    private suspend fun confirm(client: io.ktor.client.HttpClient, phone: String, uuid: String, code: String) =
        client.post("/users/confirm") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("phone" to phone, "uuid" to uuid, "code" to code))
        }

    @Test
    fun `login com telefone novo retorna 202 e envia codigo`() = runApi { client ->
        val res = login(client, phone, uuidA)
        assertEquals(HttpStatusCode.Accepted, res.status)
        val body: PendingResponse = res.body()
        assertNotNull(body.devCode, "deveria devolver o codigo em modo dev")
    }

    @Test
    fun `confirm sem codigo enviado retorna 404`() = runApi { client ->
        val res = confirm(client, "+5511888888888", "qualquer", "123456")
        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    @Test
    fun `confirm com codigo errado retorna 400`() = runApi { client ->
        login(client, phone, uuidA)
        val res = confirm(client, phone, uuidA, "000000")
        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `fluxo completo login confirm cria e ativa usuario`() = runApi { client ->
        val loginRes = login(client, phone, uuidA)
        assertEquals(HttpStatusCode.Accepted, loginRes.status)
        val code = loginRes.body<PendingResponse>().devCode!!

        val confirmRes = confirm(client, phone, uuidA, code)
        assertEquals(HttpStatusCode.OK, confirmRes.status)
        val user: UserResponse = confirmRes.body()
        assertTrue(user.active)
        assertEquals(phone, user.phone)
    }

    @Test
    fun `login de usuario ativo com mesmo uuid retorna 200`() = runApi { client ->
        val code = login(client, phone, uuidA).body<PendingResponse>().devCode!!
        confirm(client, phone, uuidA, code)

        val res = login(client, phone, uuidA)
        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.body<UserResponse>().active)
    }

    @Test
    fun `login com uuid diferente exige nova confirmacao 202`() = runApi { client ->
        val code = login(client, phone, uuidA).body<PendingResponse>().devCode!!
        confirm(client, phone, uuidA, code)

        val res = login(client, phone, uuidB)
        assertEquals(HttpStatusCode.Accepted, res.status)
    }

    @Test
    fun `confirm com novo uuid substitui o uuid do usuario existente`() = runApi { client ->
        val codeA = login(client, phone, uuidA).body<PendingResponse>().devCode!!
        confirm(client, phone, uuidA, codeA)

        val codeB = login(client, phone, uuidB).body<PendingResponse>().devCode!!
        val confirmB = confirm(client, phone, uuidB, codeB)
        assertEquals(HttpStatusCode.OK, confirmB.status)

        assertEquals(HttpStatusCode.Accepted, login(client, phone, uuidA).status)
        assertEquals(HttpStatusCode.OK, login(client, phone, uuidB).status)
    }

    @Test
    fun `PUT users id atualiza nome descricao e email`() = runApi { client ->
        val code = login(client, phone, uuidA).body<PendingResponse>().devCode!!
        val user = confirm(client, phone, uuidA, code).body<UserResponse>()

        val res = client.put("/users/${user.id}") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("name" to "Kaio", "description" to "Aluno de mobile", "email" to "kaio@example.com"))
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val updated: UserResponse = res.body()
        assertEquals("Kaio", updated.name)
        assertEquals("Aluno de mobile", updated.description)
        assertEquals("kaio@example.com", updated.email)
    }
}
