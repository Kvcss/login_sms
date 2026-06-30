# Login por Telefone (Tema 1) — Servidor em Kotlin/Ktor

Servidor que implementa **login por telefone com confirmação via SMS**, conforme o
Tema 1 do trabalho. Feito em **Kotlin** com **Ktor** (HTTP) + **Exposed** (ORM) +
**SQLite** (banco zero-config). O envio de SMS suporta um modo *mock* (para
desenvolvimento/vídeo) e o **Twilio** (SMS real).

## 👥 Integrantes

- Nome do integrante 1 — matrícula
- Nome do integrante 2 — matrícula

> ⚠️ Preencha com os nomes de **todos** os integrantes da dupla.

## 🎥 Vídeo de demonstração

🔗 **Link do vídeo:** _COLE AQUI O LINK_

> ⚠️ O vídeo deve mostrar o servidor funcionando e explicar os principais pontos do
> código. Garanta que esteja **público ou não listado** (vídeos privados não podem
> ser acessados). Cada integrante deve gravar uma parte explicando um trecho do
> servidor que desenvolveu.

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

## 📱 App (opcional)

O servidor já está pronto para um app Android. O fluxo no cliente seria:

1. Gerar/recuperar um `uuid` salvo localmente (`SharedPreferences`).
2. `POST /users/login` com `phone` + `uuid`.
3. Se vier `202`, abrir tela para digitar o código e chamar `POST /users/confirm`.
4. Se vier `200`, guardar o usuário e ir para a tela principal.
5. Completar o perfil com `PUT /users/{id}`.

---

## 🧰 Tecnologias

- **Kotlin** 1.9
- **Ktor** 2.3 (Netty)
- **Exposed** 0.53 (ORM) + **SQLite**
- **kotlinx.serialization** (JSON)
- **Twilio** (SMS real, via API REST)
- **JUnit / ktor-server-test-host** (testes)
