package com.login.db

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    @Volatile
    private var initialized = false

    // Conecta no banco e cria as tabelas. SQLite por padrao (zero-config);
    // o caminho pode ser trocado pela variavel de ambiente DB_URL.
    fun init(jdbcUrl: String = System.getProperty("DB_URL") ?: System.getenv("DB_URL") ?: "jdbc:sqlite:database.db") {
        if (initialized) return
        Database.connect(jdbcUrl, driver = "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(Users, Confirmations)
        }
        initialized = true
    }

    // Util para os testes: limpa todos os dados entre cenarios.
    fun clear() = transaction {
        SchemaUtils.drop(Users, Confirmations)
        SchemaUtils.create(Users, Confirmations)
    }
}
