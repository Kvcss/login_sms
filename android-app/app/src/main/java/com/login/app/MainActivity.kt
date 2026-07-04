package com.login.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.login.app.data.DeviceId
import com.login.app.ui.GradientBottom
import com.login.app.ui.GradientTop
import com.login.app.ui.LoginSmsTheme
import com.login.app.ui.LoginViewModel
import com.login.app.ui.Step
import com.login.app.ui.UiState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val deviceUuid = DeviceId.get(this)
        setContent {
            LoginSmsTheme {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(GradientTop, GradientBottom))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(state.step, deviceUuid)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    StepDots(state.step)

                    when (state.step) {
                        Step.PHONE -> PhoneStep(state, vm)
                        Step.CODE -> CodeStep(state, vm)
                        Step.PROFILE -> ProfileStep(state, vm)
                    }

                    AnimatedVisibility(visible = state.loading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
                        }
                    }

                    state.message?.let { MessageBanner(it) }
                }
            }
        }
    }
}

@Composable
private fun Header(step: Step, deviceUuid: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 56.dp, bottom = 8.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(color = Color.White.copy(alpha = 0.18f), shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("📱", fontSize = 34.sp)
        }
        Text(
            "Login por Telefone",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Confirmacao por SMS",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 14.sp,
        )
        Text(
            "aparelho ${deviceUuid.take(8)}…",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun StepDots(step: Step) {
    val current = when (step) {
        Step.PHONE -> 0
        Step.CODE -> 1
        Step.PROFILE -> 2
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(3) { i ->
            val active = i <= current
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 6.dp)
                    .background(
                        color = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(3.dp),
                    ),
            )
        }
    }
}

@Composable
private fun StepTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun PhoneStep(state: UiState, vm: LoginViewModel) {
    var phone by remember { mutableStateOf(state.phone) }
    StepTitle("Qual o seu telefone?", "Enviaremos um codigo de confirmacao por SMS.")
    OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Telefone") },
        placeholder = { Text("+5511999999999") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryButton("Entrar", enabled = !state.loading) { vm.submitPhone(phone.trim()) }
}

@Composable
private fun CodeStep(state: UiState, vm: LoginViewModel) {
    var code by remember { mutableStateOf("") }
    StepTitle("Digite o codigo", "Enviamos um SMS para ${state.phone}.")
    state.devCode?.let {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Text(
                "modo dev — codigo: $it",
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium,
            )
        }
    }
    OutlinedTextField(
        value = code,
        onValueChange = { code = it },
        label = { Text("Codigo de confirmacao") },
        placeholder = { Text("000000") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryButton("Confirmar", enabled = !state.loading) { vm.submitCode(code.trim()) }
    TextButton(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
}

@Composable
private fun ProfileStep(state: UiState, vm: LoginViewModel) {
    val user = state.user ?: return
    var name by remember { mutableStateOf(user.name ?: "") }
    var description by remember { mutableStateOf(user.description ?: "") }
    var email by remember { mutableStateOf(user.email ?: "") }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials(name.ifBlank { user.phone }),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column {
            Text("Logado ✓", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("id ${user.id} · ${user.phone}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    Text(
        "Complete seus dados (PUT /users/${user.id}):",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nome") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Descricao") },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("E-mail") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        modifier = Modifier.fillMaxWidth(),
    )
    PrimaryButton("Salvar perfil", enabled = !state.loading) {
        vm.saveProfile(name.trim(), description.trim(), email.trim())
    }
    TextButton(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth()) { Text("Sair") }
}

@Composable
private fun MessageBanner(msg: String) {
    val isError = !(msg.endsWith("!") || msg.contains("enviado", true) || msg.contains("salvo", true))
    val container = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
    else MaterialTheme.colorScheme.primaryContainer
    val content = if (isError) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Text(
            msg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            color = content,
            textAlign = TextAlign.Center,
            fontSize = 14.sp,
        )
    }
}

private fun initials(source: String): String {
    val parts = source.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}
