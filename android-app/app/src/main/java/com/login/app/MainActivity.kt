package com.login.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.login.app.data.DeviceId
import com.login.app.ui.LoginViewModel
import com.login.app.ui.Step
import com.login.app.ui.UiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deviceUuid = DeviceId.get(this)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: LoginViewModel = viewModel()
                    vm.uuid = deviceUuid
                    AppScreen(vm, deviceUuid)
                }
            }
        }
    }
}

@Composable
fun AppScreen(vm: LoginViewModel, deviceUuid: String) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Login por Telefone", style = MaterialTheme.typography.headlineSmall)
        Text(
            "uuid do aparelho: ${deviceUuid.take(8)}...",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))

        when (state.step) {
            Step.PHONE -> PhoneStep(state, vm)
            Step.CODE -> CodeStep(state, vm)
            Step.PROFILE -> ProfileStep(state, vm)
        }

        if (state.loading) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator()
        }

        state.message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PhoneStep(state: UiState, vm: LoginViewModel) {
    var phone by remember { mutableStateOf(state.phone) }
    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Telefone (ex.: +5511999999999)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { vm.submitPhone(phone.trim()) },
        enabled = !state.loading,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Entrar") }
}

@Composable
private fun CodeStep(state: UiState, vm: LoginViewModel) {
    var code by remember { mutableStateOf("") }
    Text("Enviamos um codigo para ${state.phone}.")
    state.devCode?.let {
        // Em modo mock o servidor devolve o codigo; mostramos como dica.
        Text("Codigo (modo dev): $it", style = MaterialTheme.typography.bodyMedium)
    }
    OutlinedTextField(
        value = code,
        onValueChange = { code = it },
        label = { Text("Codigo de confirmacao") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { vm.submitCode(code.trim()) },
        enabled = !state.loading,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Confirmar") }
    TextButton(onClick = { vm.logout() }) { Text("Voltar") }
}

@Composable
private fun ProfileStep(state: UiState, vm: LoginViewModel) {
    val user = state.user ?: return
    var name by remember { mutableStateOf(user.name ?: "") }
    var description by remember { mutableStateOf(user.description ?: "") }
    var email by remember { mutableStateOf(user.email ?: "") }

    Text("Logado! id=${user.id} | ${user.phone}", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))

    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nome") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Descricao") },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("E-mail") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { vm.saveProfile(name.trim(), description.trim(), email.trim()) },
        enabled = !state.loading,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Salvar perfil (PUT /users/{id})") }
    TextButton(onClick = { vm.logout() }) { Text("Sair") }
}
