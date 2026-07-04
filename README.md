# Login por Telefone (Tema 1) — Servidor em Kotlin/Ktor

Servidor que implementa **login por telefone com confirmação via SMS**, conforme o
Tema 1 do trabalho. Feito em **Kotlin** com **Ktor** (HTTP) + **Exposed** (ORM) +
**SQLite** (banco zero-config). O envio de SMS suporta um modo *mock* (para
desenvolvimento/vídeo) e o **Twilio** (SMS real).

## 👥 Integrantes

- Nome: Kaio Vinicius Corredor da Silva

## 🎥 Vídeo de demonstração

🔗 **Link do vídeo:** https://www.youtube.com/watch?v=Wae5_yMgzoY

---

## ✅ O que foi implementado

O Tema 1 pede alterações em três endpoints. Todos foram implementados e testados:

| Endpoint | Comportamento |
|---|---|
| `POST /users/login` | Recebe `phone` + `uuid`. Se existirem para um usuário **ativo**, faz login e retorna o usuário (`200`). Caso o telefone não exista, ou exista com **outro uuid**, envia um SMS de confirmação e retorna `202`. |
| `POST /users/confirm` | Recebe `phone`, `uuid` e `code`. Se nenhum código foi enviado para esse par telefone+uuid, retorna `404`. Se o código estiver correto, **ativa o novo usuário** ou **substitui o uuid** do usuário antigo e retorna `200`. |
| `PUT /users/{id}` | Permite preencher os demais dados do usuário (`name`, `description`, `email`). |

Endpoints auxiliares: `GET /` (healthcheck) e `GET /users/{id}`.

### Regras de status HTTP

- `200` — login bem-sucedido / confirmação ok / atualização ok
- `202` — telefone novo (ou uuid diferente / usuário inativo): SMS enviado, aguardando confirmação
- `400` — corpo inválido, código incorreto ou código expirado
- `404` — nenhum código de confirmação para o par telefone+uuid (no `confirm`), ou usuário inexistente

---

## 🏗️ Como funciona

1. **Login** (`/users/login`): busca o usuário pelo telefone.
   - Usuário existe, está **ativo** e o **uuid bate** → login direto (`200`).
   - Qualquer outro caso → gera um código de 6 dígitos, salva na tabela
     `confirmations` (com validade) e envia por SMS → `202`.
2. **Confirmação** (`/users/confirm`): valida o código mais recente do par
   telefone+uuid.
   - Sem código → `404`. Expirado/errado → `400`.
   - Correto → consome o código e:
     - cria um novo usuário **ativo**, **ou**
     - atualiza o `uuid` de um usuário já existente (troca de aparelho) e o mantém ativo.
3. **Dados do perfil** (`PUT /users/{id}`): atualiza nome, descrição e e-mail.
   Telefone, uuid e status `active` **não** são alterados por aqui.

### O `uuid`

O `uuid` é o identificador do **dispositivo**. No Android ele pode ser gerado uma
vez e guardado no app (ex.: `UUID.randomUUID()` salvo em `SharedPreferences`). No
iOS pode-se usar o `identifierForVendor`. Assim, o par **telefone + uuid** garante
que aquele número está logado naquele aparelho específico.

---

## 📁 Estrutura do projeto

```
src/main/kotlin/com/login/
  Application.kt              # main() + módulo Ktor (JSON + rotas)
  Routing.kt                 # definição das rotas e tradução para status HTTP
  db/
    DatabaseFactory.kt       # conexão e criação das tabelas (SQLite)
    Tables.kt                # tabelas Users e Confirmations (Exposed)
  model/Dtos.kt              # DTOs de request/response (kotlinx.serialization)
  service/
    SmsService.kt            # envio de SMS (mock ou Twilio)
    UserService.kt           # regra de negócio do login/confirm/update
src/test/kotlin/com/login/
  ApiTest.kt                 # testes de integração (testApplication)
requests.http                # exemplos de requisição (REST Client / referência)
```

---

## ▶️ Como rodar

Pré-requisitos: **JDK 17+** (o Gradle vem embutido via *wrapper*, não precisa instalar).

```bash
# Linux/macOS
./gradlew run

# Windows
.\gradlew.bat run
```

O servidor sobe em `http://localhost:3000`. No modo padrão (`SMS_PROVIDER=mock`),
o código de confirmação é **impresso no console** e também devolvido no corpo do
`202` (campo `devCode`), o que facilita testar e gravar o vídeo.

### Rodar os testes

```bash
./gradlew test        # (ou .\gradlew.bat test no Windows)
```

São 8 testes de integração cobrindo todos os cenários (202, 404, 400, login direto,
troca de uuid, atualização de perfil).

### Exemplo rápido (curl)

```bash
# 1) Login (telefone novo) -> 202 + devCode no corpo (modo mock)
curl -X POST http://localhost:3000/users/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"+5511999999999","uuid":"meu-device-123"}'

# 2) Confirmar -> 200 (use o código recebido)
curl -X POST http://localhost:3000/users/confirm \
  -H "Content-Type: application/json" \
  -d '{"phone":"+5511999999999","uuid":"meu-device-123","code":"123456"}'

# 3) Completar o perfil -> 200
curl -X PUT http://localhost:3000/users/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Kaio","description":"Aluno","email":"kaio@example.com"}'
```

---

## ⚙️ Configuração (variáveis de ambiente)

Veja `.env.example`. As principais:

| Variável | Padrão | Descrição |
|---|---|---|
| `PORT` | `3000` | Porta do servidor |
| `SMS_PROVIDER` | `mock` | `mock` (loga o código) ou `twilio` (SMS real) |
| `SMS_RETURN_CODE_IN_RESPONSE` | `true` | Em dev, devolve o `devCode` no `202` |
| `CONFIRMATION_TTL_MINUTES` | `10` | Validade do código |
| `DB_URL` | `jdbc:sqlite:database.db` | URL JDBC do banco |
| `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN` / `TWILIO_FROM_NUMBER` | — | Credenciais Twilio (só se `SMS_PROVIDER=twilio`) |

### Enviando SMS real (Twilio)

```bash
# Linux/macOS
export SMS_PROVIDER=twilio
export SMS_RETURN_CODE_IN_RESPONSE=false
export TWILIO_ACCOUNT_SID=ACxxxxxxxx
export TWILIO_AUTH_TOKEN=xxxxxxxx
export TWILIO_FROM_NUMBER=+1xxxxxxxxxx
./gradlew run
```

A integração com a Twilio é feita por uma chamada HTTP direta à API REST
(`SmsService.kt`), sem SDK pesado.

---

## 📱 App Android (Kotlin + Jetpack Compose)

Além do servidor, foi feito o **app Android** (parte opcional do enunciado) que
realiza o login de verdade. Fica na pasta [`android-app/`](android-app/).

### Fluxo do app

1. Na primeira execução, gera um `uuid` com `UUID.randomUUID()` e guarda em
   `SharedPreferences` (`DeviceId.kt`) — reutilizado em todos os logins do aparelho.
2. Tela **Telefone** → `POST /users/login` com `phone` + `uuid`.
3. Se vier `202`, vai para a tela **Código** e chama `POST /users/confirm`.
   (Em modo mock o app mostra o código como dica, para facilitar o teste.)
4. Se vier `200`, vai para a tela **Perfil** com os dados do usuário.
5. Na tela **Perfil**, os campos nome/descrição/e-mail salvam via `PUT /users/{id}`.

### Telas (execução real no emulador)

| 1. Telefone | 2. Código (SMS) | 3. Perfil (logado) |
|---|---|---|
| ![](docs/screenshots/1-telefone.png) | ![](docs/screenshots/2-codigo.png) | ![](docs/screenshots/3-perfil.png) |

### Como rodar o app

1. Suba o servidor (`.\gradlew.bat run` na raiz).
2. Abra a pasta `android-app/` no **Android Studio** (ou use a linha de comando).
3. Rode em um **emulador** (o app já aponta para `http://10.0.2.2:3000`, que é
   como o emulador enxerga o `localhost` da máquina).

```bash
# Build/instalação por linha de comando (dentro de android-app/)
.\gradlew.bat :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.login.app/.MainActivity
```

> **Dispositivo físico:** troque `BASE_URL` em
> `android-app/app/src/main/java/com/login/app/data/Network.kt` pelo IP da sua
> máquina na rede local (ex.: `http://192.168.0.10:3000/`).

### Estrutura do app

```
android-app/app/src/main/java/com/login/app/
  MainActivity.kt            # UI em Compose (telas telefone/codigo/perfil)
  data/
    Models.kt                # DTOs (espelham os do servidor)
    DeviceId.kt              # gera/guarda o uuid do aparelho
    Network.kt               # Retrofit + base URL
    AuthRepository.kt        # chamadas e traducao dos status HTTP
  ui/LoginViewModel.kt       # estado das telas + coroutines
```

---

## 🧰 Tecnologias

- **Kotlin** 1.9
- **Ktor** 2.3 (Netty)
- **Exposed** 0.53 (ORM) + **SQLite**
- **kotlinx.serialization** (JSON)
- **Twilio** (SMS real, via API REST)
- **JUnit / ktor-server-test-host** (testes)
