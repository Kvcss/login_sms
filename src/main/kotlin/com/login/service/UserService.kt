package com.login.service

import com.login.db.Confirmations
import com.login.db.Users
import com.login.model.UpdateUserRequest
import com.login.model.UserResponse
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

// Resultado possivel das operacoes de login/confirm, para o roteador traduzir
// no codigo HTTP correto.
sealed class LoginResult {
    data class LoggedIn(val user: UserResponse) : LoginResult()      // 200
    data class CodeSent(val devCode: String) : LoginResult()         // 202
}

sealed class ConfirmResult {
    data class Confirmed(val user: UserResponse) : ConfirmResult()   // 200
    object NotFound : ConfirmResult()                                // 404
    object Invalid : ConfirmResult()                                 // 400 (codigo errado)
    object Expired : ConfirmResult()                                 // 400 (expirado)
}

object UserService {

    private val ttlMinutes: Long
        get() = (System.getenv("CONFIRMATION_TTL_MINUTES") ?: "10").toLong()

    private fun rowToUser(row: ResultRow) = UserResponse(
        id = row[Users.id].value,
        phone = row[Users.phone],
        name = row[Users.name],
        description = row[Users.description],
        email = row[Users.email],
        active = row[Users.active],
    )

    // Cria (ou renova) um codigo de confirmacao para o par telefone+uuid e envia
    // por SMS. Apaga codigos antigos do mesmo par para nao acumular lixo.
    private fun createAndSendConfirmation(phone: String, uuid: String): String = transaction {
        Confirmations.deleteWhere { (Confirmations.phone eq phone) and (Confirmations.uuid eq uuid) }
        val code = SmsService.generateCode()
        Confirmations.insert {
            it[Confirmations.phone] = phone
            it[Confirmations.uuid] = uuid
            it[Confirmations.code] = code
            it[expiresAt] = LocalDateTime.now().plusMinutes(ttlMinutes)
        }
        SmsService.sendCode(phone, code)
        code
    }

    // POST /users/login
    //  - telefone + uuid existem para um usuario ATIVO -> login (200)
    //  - telefone nao existe, ou existe com outro uuid -> envia SMS (202)
    fun login(phone: String, uuid: String): LoginResult {
        val user = transaction {
            Users.selectAll().where { Users.phone eq phone }.firstOrNull()
        }

        if (user != null && user[Users.active] && user[Users.uuid] == uuid) {
            return LoginResult.LoggedIn(rowToUser(user))
        }

        // Telefone novo, ou com uuid diferente, ou ainda nao ativo: confirma via SMS.
        val code = createAndSendConfirmation(phone, uuid)
        return LoginResult.CodeSent(code)
    }

    // POST /users/confirm
    //  - nenhum codigo enviado para telefone+uuid -> NotFound (404)
    //  - codigo incorreto/expirado -> Invalid/Expired (400)
    //  - correto -> ativa novo usuario OU substitui o uuid do antigo (200)
    fun confirm(phone: String, uuid: String, code: String): ConfirmResult = transaction {
        val confirmation = Confirmations
            .selectAll().where { (Confirmations.phone eq phone) and (Confirmations.uuid eq uuid) }
            .orderBy(Confirmations.id, SortOrder.DESC)
            .firstOrNull()
            ?: return@transaction ConfirmResult.NotFound

        if (confirmation[Confirmations.expiresAt].isBefore(LocalDateTime.now())) {
            Confirmations.deleteWhere { (Confirmations.phone eq phone) and (Confirmations.uuid eq uuid) }
            return@transaction ConfirmResult.Expired
        }

        if (confirmation[Confirmations.code] != code) {
            return@transaction ConfirmResult.Invalid
        }

        // Codigo correto: consome todos os codigos desse par.
        Confirmations.deleteWhere { (Confirmations.phone eq phone) and (Confirmations.uuid eq uuid) }

        val existing = Users.selectAll().where { Users.phone eq phone }.firstOrNull()

        val userId = if (existing != null) {
            // Usuario ja existe: substitui o uuid (novo dispositivo) e garante ativo.
            Users.update({ Users.phone eq phone }) {
                it[Users.uuid] = uuid
                it[active] = true
            }
            existing[Users.id].value
        } else {
            // Novo usuario.
            Users.insertAndGetId {
                it[Users.phone] = phone
                it[Users.uuid] = uuid
                it[active] = true
            }.value
        }

        val row = Users.selectAll().where { Users.id eq userId }.first()
        ConfirmResult.Confirmed(rowToUser(row))
    }

    // PUT /users/{id}: preenche os demais dados (nome, descricao, email).
    fun update(id: Int, data: UpdateUserRequest): UserResponse? = transaction {
        Users.selectAll().where { Users.id eq id }.firstOrNull() ?: return@transaction null
        Users.update({ Users.id eq id }) {
            if (data.name != null) it[name] = data.name
            if (data.description != null) it[description] = data.description
            if (data.email != null) it[email] = data.email
        }
        rowToUser(Users.selectAll().where { Users.id eq id }.first())
    }

    fun getById(id: Int): UserResponse? = transaction {
        Users.selectAll().where { Users.id eq id }.firstOrNull()?.let(::rowToUser)
    }
}
