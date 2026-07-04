package com.login.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    @Volatile
    private var initialized = false

    fun init(jdbcUrl: String = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:sqlite:database.db") {
        if (initialized) return
        Database.connect(jdbcUrl, driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(Users, Confirmations)
        }
        initialized = true
    }

    fun clear() = transaction {
        SchemaUtils.drop(Users, Confirmations)
        SchemaUtils.create(Users, Confirmations)
    }
}
