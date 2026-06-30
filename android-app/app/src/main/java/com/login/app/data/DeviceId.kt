package com.login.app.data

import android.content.Context
import java.util.UUID

// Gera e guarda um uuid do dispositivo. Conforme o enunciado, no Android o uuid
// pode ser gerado e armazenado no app. Aqui usamos SharedPreferences: o mesmo
// uuid e reutilizado em todos os logins deste aparelho.
object DeviceId {
    private const val PREFS = "login_sms_prefs"
    private const val KEY_UUID = "device_uuid"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_UUID, null)
        if (existing != null) return existing
        val novo = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_UUID, novo).apply()
        return novo
    }
}
