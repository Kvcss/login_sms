package com.login.db

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object Users : IntIdTable("users") {
    val phone = varchar("phone", 32).uniqueIndex()
    val uuid = varchar("uuid", 128)
    val name = varchar("name", 255).nullable()
    val description = varchar("description", 1024).nullable()
    val email = varchar("email", 255).nullable()
    val active = bool("active").default(false)
}

object Confirmations : IntIdTable("confirmations") {
    val phone = varchar("phone", 32)
    val uuid = varchar("uuid", 128)
    val code = varchar("code", 8)
    val expiresAt = datetime("expires_at")
}
