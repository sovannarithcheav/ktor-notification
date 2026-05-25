# ktor-notification

A reusable Kotlin library for building notification services on top of [Ktor](https://ktor.io). Provides a server-side plugin, pluggable channel abstraction (Email, WebSocket, and custom), template variable resolution, and a lightweight HTTP client for other services to trigger notifications.

## Features

- **Channel abstraction** — implement one interface to add Email, WebSocket, Telegram, SMS, or any custom channel
- **WebSocket push** — built-in multi-session manager per user; route installed automatically at `/ws/{userId}`
- **Email dispatch** — SMTP-based with a `UserEmailResolver` hook so the service provides the email lookup
- **Template resolution** — pure `${variable}` substitution with no DB or framework dependency
- **Notification client** — one-line HTTP call for other services to trigger notifications by event code

## Installation

> Requires access to the GitHub Package Registry. Add your credentials to `~/.gradle/gradle.properties`:
> ```properties
> gpr.user=YOUR_GITHUB_USERNAME
> gpr.key=YOUR_GITHUB_PAT
> ```

Add the repository and dependency to your service:

```kotlin
// build.gradle.kts
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/sovannarithcheav/ktor-notification")
        credentials {
            username = System.getenv("GIT_PUBLISH_USER")
            password = System.getenv("GIT_PUBLISH_PASSWORD")
        }
    }
}

dependencies {
    implementation("kh.com.ktor:notification:0.0.1")
}
```

## Usage

### Server — notification service

Install the plugin in your `Application.kt`:

```kotlin
install(NotificationServer) {
    channel(EmailChannel(
        config = EmailConfig(
            host     = "smtp.example.com",
            port     = 587,
            from     = "no-reply@example.com",
            username = "user",
            password = "secret",
        ),
        userEmailResolver = { userId -> userRepository.findEmailById(userId) },
    ))
    channel(WebSocketChannel())
}
```

The plugin automatically registers a WebSocket route at `/ws/{userId}`. Clients connect to receive push notifications in real time.

#### Adding a custom channel

Implement `NotificationChannel` and register it:

```kotlin
class TelegramChannel(private val botToken: String) : NotificationChannel {
    override val id   = 3L
    override val name = "TELEGRAM"

    override suspend fun dispatch(
        userId: Long,
        title: String,
        body: String,
        mergeFields: Map<String, String>,
    ): DispatchResult {
        // send via Telegram Bot API
        return DispatchResult(success = true, message = "Sent to Telegram")
    }
}

// register
install(NotificationServer) {
    channel(TelegramChannel(botToken = "..."))
}
```

Seed the matching row in your `channels` table so the subscription system can route to it.

#### Dispatching programmatically inside the service

```kotlin
val result = NotificationServerDefaults.dispatcher?.dispatch(
    DispatchRequest(
        userId      = 42L,
        title       = "Salary Paid",
        subject     = "salary_paid",
        body        = "Dear \${employee_name}, your salary has been processed.",
        mergeFields = mapOf("employee_name" to "Dara"),
        channelId   = WebSocketChannel.CHANNEL_ID,
    )
)
```

### Client — any other service

Configure once at startup, then call anywhere:

```kotlin
// Application.kt
NotificationClient.configure("http://localhost:8088")

// anywhere in the service
NotificationClient.send(
    userId      = employeeId,
    eventCode   = "salary_paid",
    mergeFields = mapOf("employee_name" to "Dara", "amount" to "1500"),
)
```

`send()` is fire-and-forget — it does not throw on non-2xx responses.

## Database Migrations

The lib ships Liquibase changesets inside the JAR. Add this as the **first entry** in your service's `db.changelog-master.yaml`:

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/ktor-notification/db.changelog-lib.yaml
  # then your service-owned seeds
  - include:
      file: db/changelog/changes/001-seed-channels.yaml
  - include:
      file: db/changelog/changes/002-seed-events.yaml
```

**The lib creates all tables. The service provides all seeds** (channels, categories, event codes, templates). This split means each project defines its own notification events without touching the schema.

Tables created by the lib:

| Table | Purpose |
|---|---|
| `categories` | Groups events (e.g. Payroll, Leave) |
| `channels` | Email, Push, Telegram, etc. |
| `variables` | Registered merge field definitions |
| `event_notifications` | Event registry with code, type, required flag |
| `event_variables` | Join table — which variables belong to which event |
| `notification_templates` | Per-event per-channel subject + body |
| `users` | Minimal user copy (id, email, full_name) |
| `user_subscriptions` | Per-user channel preference per event |
| `user_web_notifications` | Notification history (inbox) |
| `audit_log` | Dispatch audit trail |

## Architecture

```
server/
├── NotificationServer.kt       Ktor plugin — installs WS route, wires dispatcher
├── channel/
│   ├── NotificationChannel.kt  Interface every channel implements
│   ├── EmailChannel.kt         SMTP dispatch
│   └── WebSocketChannel.kt     Real-time push + WebSocketSessionManager
├── template/
│   └── TemplateResolver.kt     Pure ${var} substitution
└── dispatch/
    └── NotificationDispatcher.kt  Resolves template → routes to channel

client/
└── NotificationClient.kt       HTTP client for consumer services
```

## Publishing

```bash
# local Maven (for development)
./gradlew publishToMavenLocal

# GitHub Package Registry
GIT_PUBLISH_USER=username GIT_PUBLISH_PASSWORD=token ./gradlew publish
```

Bump `version` in `build.gradle.kts` before each release.
