package com.login.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

// Um usuario e identificado pelo telefone. O uuid e o identificador do
// dispositivo (gerado e guardado pelo app Android, ou identifierForVendor no iOS).
object Users : IntIdTable("users") {
    val phone = varchar("phone", 32).uniqueIndex()
    val uuid = varchar("uuid", 128)
    val name = varchar("name", 255).nullable()
    val description = varchar("description", 1024).nullable()
    val email = varchar("email", 255).nullable()
    // Usuario so fica ativo apos confirmar o codigo recebido por SMS.
    val active = bool("active").default(false)
}

// Codigos de confirmacao enviados por SMS para um par telefone+uuid.
// Quando o usuario confirma, a linha correspondente e consumida (removida).
object Confirmations : IntIdTable("confirmations") {
    val phone = varchar("phone", 32)
    val uuid = varchar("uuid", 128)
    val code = varchar("code", 8)
    val expiresAt = datetime("expires_at")
}
