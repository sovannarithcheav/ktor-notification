# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Kotlin library (`kh.com.ktor:notification`) providing the full notification domain for Ktor services — database schema, repositories, services, channels, dispatch, and an optional all-in-one plugin. All dependencies are `compileOnly`; the consuming service provides them at runtime.

- **Group / artifact**: `kh.com.ktor:notification`
- **Current version**: `0.0.5` (in `build.gradle.kts`)
- **Published to**: GitHub Packages at `github.com/sovannarithcheav/ktor-notification`

## Common Commands

```bash
# Compile
./gradlew compileKotlin

# Publish to local Maven repo (for local testing in consuming services)
./gradlew publishToMavenLocal

# Publish to GitHub Packages (requires GIT_PUBLISH_USER + GIT_PUBLISH_PASSWORD)
./gradlew publish
```

After publishing, update `ktor_notification_version` in the consuming service's `gradle.properties` and run `./gradlew clean compileKotlin` there to pick it up.

## Architecture

```
server/
├── NotificationServer.kt          # Optional Ktor plugin — wires everything in one install() call
├── channel/
│   ├── NotificationChannel.kt     # Interface: dispatch(userId, title, body, subject?, mergeFields)
│   ├── EmailChannel.kt            # SMTP impl — requires EmailConfig + UserEmailResolver
│   └── WebSocketChannel.kt        # WS push — owns WebSocketSessionManager + PushMessage JSON
├── dispatch/
│   └── NotificationDispatcher.kt  # Routes DispatchRequest to the right channel by channelId
├── service/                       # Business logic — open classes / objects, safe to subclass or use directly
│   ├── NotificationTemplateService.kt  # open — extend to add requestUpdate() or custom validation
│   ├── NotificationSendService.kt      # Resolves subscription → template → dispatches per channel
│   └── ...                             # AuditLog, Category, EventNotification, History, Subscription, Variable
├── repository/                    # Exposed DSL — one object per table, no ORM
├── entity/                        # Data classes + Exposed table objects + ResultRow mappers
├── security/
│   ├── UserInfo.kt                # currentUser() / optionalUser() — read X-User-Id / X-Username headers
│   └── NotificationErrorCode.kt   # Typed error codes — call .issue() to throw NotificationException
├── common/
│   └── ApiResponse.kt             # ok/created/accepted/notFound/badRequest/unprocessable/serverError
├── enums/                         # AuditAction, ChannelEnum (EMAIL=1, PUSH=2, TELEGRAM=3, NONE=4), StatusEnum
├── routes/
│   └── NotificationRoutes.kt      # installNotificationRoutes() — all HTTP endpoints as one Routing extension
└── template/
    └── TemplateResolver.kt        # Replaces ${key} placeholders in subject/body from mergeFields map
client/
└── NotificationClient.kt          # HTTP client for other services to POST /send
```

## Key Design Points

**Channel IDs are fixed**: `EMAIL=1`, `PUSH=2`, `TELEGRAM=3`, `NONE=4` — defined in `ChannelEnum` and used by `NotificationSendService`. Any new channel needs a new ID and a row in the `channels` table.

**`NotificationServerDefaults.dispatcher`**: Global singleton set at startup. `NotificationSendService` reads it at dispatch time. Must be initialized before the first send — the `NotificationServer` plugin does this automatically; without the plugin, set it manually:
```kotlin
NotificationServerDefaults.dispatcher = NotificationDispatcher(
    mapOf(EmailChannel.CHANNEL_ID to emailChannel, WebSocketChannel.CHANNEL_ID to WebSocketChannel())
)
```

**`NotificationTemplateService` is `open`**: Subclass to add logic (e.g. change-management integration via `requestUpdate()`). Call `validatePlaceholders()` before submitting an update to ensure body/subject placeholders match declared variables.

**`NotificationServer` plugin** (optional): Convenience wrapper that registers the WebSocket route, calls `installNotificationRoutes()`, and sets the dispatcher. Services that prefer explicit routing skip this and wire things directly.

**Template placeholder format**: `${variableName}` — resolved by `TemplateResolver` at send time, not at save time.

**`NotificationClient`**: Fire-and-forget HTTP POST to `/api/v1/notification/send`. Configure once at startup:
```kotlin
NotificationClient.configure("http://notification-service:8088")
NotificationClient.send(userId = 10L, eventCode = "PWD_RESET_SUCC", mergeFields = mapOf(...))
```

## Liquibase DDL

All CREATE TABLE changesets live in `src/main/resources/db/changelog/ktor-notification/` and are bundled in the JAR. Consuming services include them via:
```yaml
- include:
    file: db/changelog/ktor-notification/db.changelog-lib.yaml
```
This creates all tables (categories, channels, variables, event_notifications, event_variables, notification_templates, users, user_subscriptions, user_web_notifications, audit_log). Seeds are the consuming service's responsibility.

`reset-sequences-after-seeding.yaml` resets all serial sequences to `MAX(id)` — include it **last** in the consuming service's master changelog, after all seeds.

## Publishing

| Variable | Purpose |
|---|---|
| `GIT_PUBLISH_USER` | GitHub username |
| `GIT_PUBLISH_PASSWORD` | GitHub PAT with `write:packages` |

Bump `version` in `build.gradle.kts` before publishing a new release.
