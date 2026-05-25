# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`ktor-notification` is a reusable Kotlin library (`kh.com.ktor:notification`) for building notification services on top of Ktor. It provides the server-side plugin, channel abstraction, WebSocket session management, template resolution, and an HTTP client for other services to trigger notifications.

- **Group / artifact**: `kh.com.ktor:notification`
- **Version**: `0.0.1`
- **Stack**: Kotlin 2.2.21, Ktor 3.1.1, kotlinx-serialization
- **Published to**: GitHub Package Registry (`https://maven.pkg.github.com/sovannarithcheav/ktor-notification`)

## Common Commands

```bash
# Compile
./gradlew compileKotlin

# Publish to local Maven (~/.m2) for local testing in a service
./gradlew publishToMavenLocal

# Publish to GitHub Package Registry (requires GIT_PUBLISH_USER + GIT_PUBLISH_PASSWORD env vars)
./gradlew publish
```

## Architecture

The lib is split into two halves:

### Server (installed in the notification service)

```
server/
├── NotificationServer.kt          ← Ktor plugin; installs /ws/{userId} WebSocket route
├── channel/
│   ├── NotificationChannel.kt     ← Interface all channels implement
│   ├── EmailChannel.kt            ← SMTP dispatch; requires UserEmailResolver hook
│   └── WebSocketChannel.kt        ← Push via WebSocket; owns WebSocketSessionManager
├── template/
│   └── TemplateResolver.kt        ← Pure ${var} substitution — no DB, no Ktor dependency
└── dispatch/
    └── NotificationDispatcher.kt  ← Resolves template then routes to the right channel
```

**Plugin installation** (in the notification service's `Application.kt`):
```kotlin
install(NotificationServer) {
    channel(EmailChannel(emailConfig) { userId -> userRepo.findEmail(userId) })
    channel(WebSocketChannel())
    // add more channels: TelegramChannel(...), SmsChannel(...), etc.
}
```

`NotificationServerDefaults.dispatcher` is set by the plugin and can be used anywhere in the service to dispatch programmatically.

### Client (used by any service that needs to trigger notifications)

```kotlin
// configure once at startup
NotificationClient.configure("http://localhost:8088")

// call anywhere
NotificationClient.send(
    userId      = employeeId,
    eventCode   = "salary_paid",
    mergeFields = mapOf("amount" to "1500", "period" to "2026-05"),
)
```

`NotificationClient` is a fire-and-forget HTTP POST to `/api/v1/notification/send`. It uses Ktor CIO engine and does not throw on non-2xx responses (`expectSuccess = false`).

## Adding a New Channel

1. Implement `NotificationChannel` — provide `id`, `name`, and `suspend fun dispatch(...)`.
2. Register it with `channel(YourChannel(...))` in the plugin install block.
3. Seed the matching row in the service's `channels` table (the lib does not seed channels).

## Liquibase Migrations

All CREATE TABLE changesets live in `src/main/resources/db/changelog/ktor-notification/`. They are bundled inside the JAR and available on the classpath of any service that depends on this lib.

**Entry point for services** — add this as the first include in the service's `db.changelog-master.yaml`:
```yaml
- include:
    file: db/changelog/ktor-notification/db.changelog-lib.yaml
```

The lib migrations create all tables (categories, channels, variables, event_notifications, event_variables, notification_templates, users, user_subscriptions, user_web_notifications, audit_log) with no seeds. Seeds (channels, categories, event codes, templates) are the service's responsibility.

Changeset IDs are prefixed with `ktor-noti-` to avoid collisions with service-owned changesets.

## Publishing

Credentials are read from environment variables:
| Variable | Purpose |
|---|---|
| `GIT_PUBLISH_USER` | GitHub username |
| `GIT_PUBLISH_PASSWORD` | GitHub PAT with `write:packages` |

Bump `version` in `build.gradle.kts` before publishing a new release.
