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

sealed class LoginResult {
    data class LoggedIn(val user: UserResponse) : LoginResult()
    data class CodeSent(val devCode: String) : LoginResult()
    data class SmsError(val message: String) : LoginResult()
}

sealed class ConfirmResult {
    data class Confirmed(val user: UserResponse) : ConfirmResult()
    object NotFound : ConfirmResult()
    object Invalid : ConfirmResult()
    object Expired : ConfirmResult()
}

object UserService {

    private val ttlMinutes: Long
        get() = (System.getenv("CONFIRMATION_TTL_MINUTES") ?: "10").toLong()

    private fun normalizePhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.isEmpty()) raw.trim() else "+$digits"
    }

    private fun rowToUser(row: ResultRow) = UserResponse(
        id = row[Users.id].value,
        phone = row[Users.phone],
        name = row[Users.name],
        description = row[Users.description],
        email = row[Users.email],
        active = row[Users.active],
    )

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

    fun login(rawPhone: String, uuid: String): LoginResult {
        val phone = normalizePhone(rawPhone)
        val user = transaction {
            Users.selectAll().where { Users.phone eq phone }.firstOrNull()
        }

        if (user != null && user[Users.active] && user[Users.uuid] == uuid) {
            return LoginResult.LoggedIn(rowToUser(user))
        }

        return try {
            val code = createAndSendConfirmation(phone, uuid)
            LoginResult.CodeSent(code)
        } catch (e: Exception) {
            LoginResult.SmsError(e.message ?: "Falha ao enviar SMS")
        }
    }

    fun confirm(rawPhone: String, uuid: String, code: String): ConfirmResult = transaction {
        val phone = normalizePhone(rawPhone)
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

        Confirmations.deleteWhere { (Confirmations.phone eq phone) and (Confirmations.uuid eq uuid) }

        val existing = Users.selectAll().where { Users.phone eq phone }.firstOrNull()

        val userId = if (existing != null) {
            Users.update({ Users.phone eq phone }) {
                it[Users.uuid] = uuid
                it[active] = true
            }
            existing[Users.id].value
        } else {
            Users.insertAndGetId {
                it[Users.phone] = phone
                it[Users.uuid] = uuid
                it[active] = true
            }.value
        }

        val row = Users.selectAll().where { Users.id eq userId }.first()
        ConfirmResult.Confirmed(rowToUser(row))
    }

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
