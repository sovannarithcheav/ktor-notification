package kh.com.ktor.notification.server

import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.close
import kh.com.ktor.notification.server.channel.NotificationChannel
import kh.com.ktor.notification.server.channel.WebSocketSessionManager
import kh.com.ktor.notification.server.dispatch.NotificationDispatcher
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("NotificationServer")

class NotificationServerConfig {
    internal val channels = mutableListOf<NotificationChannel>()
    var webSocketPath: String = "/ws/{userId}"

    fun channel(channel: NotificationChannel) {
        channels.add(channel)
    }
}

val NotificationServer = createApplicationPlugin("NotificationServer", ::NotificationServerConfig) {
    val channelMap = pluginConfig.channels.associateBy { it.id }
    val dispatcher = NotificationDispatcher(channelMap)

    log.info(
        "NotificationServer started — channels: [{}], wsPath: {}",
        channelMap.values.joinToString { it.name },
        pluginConfig.webSocketPath,
    )

    NotificationServerDefaults.dispatcher = dispatcher

    application.routing {
        webSocket(pluginConfig.webSocketPath) {
            val userId = call.parameters["userId"]?.toLongOrNull()
            if (userId == null) {
                close()
                return@webSocket
            }
            WebSocketSessionManager.add(userId, this)
            log.debug("WS connected userId={}", userId)
            try {
                for (frame in incoming) { /* keep alive — server pushes only */ }
            } finally {
                WebSocketSessionManager.remove(userId, this)
                log.debug("WS disconnected userId={}", userId)
            }
        }
    }
}

object NotificationServerDefaults {
    var dispatcher: NotificationDispatcher? = null
}
