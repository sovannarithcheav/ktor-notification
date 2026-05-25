package kh.com.ktor.notification.server

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.close
import kh.com.ktor.notification.server.channel.NotificationChannel
import kh.com.ktor.notification.server.channel.WebSocketSessionManager
import kh.com.ktor.notification.server.dispatch.NotificationDispatcher
import kh.com.ktor.notification.server.routes.installNotificationRoutes
import kh.com.ktor.notification.server.service.NotificationTemplateService
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("NotificationServer")

class NotificationServerConfig {
    internal val channels = mutableListOf<NotificationChannel>()
    var webSocketPath: String = "/ws/{userId}"
    var routeBasePath: String = "/api/v1/notification"
    var templateUpdateHandler: (suspend ApplicationCall.(id: Long) -> Unit)? = null
    internal var templateService: NotificationTemplateService = NotificationTemplateService()

    fun channel(channel: NotificationChannel) {
        channels.add(channel)
    }

    fun templateService(service: NotificationTemplateService) {
        templateService = service
    }
}

val NotificationServer = createApplicationPlugin("NotificationServer", ::NotificationServerConfig) {
    val channelMap = pluginConfig.channels.associateBy { it.id }
    val dispatcher = NotificationDispatcher(channelMap)

    log.info(
        "NotificationServer started — channels: [{}], wsPath: {}, basePath: {}",
        channelMap.values.joinToString { it.name },
        pluginConfig.webSocketPath,
        pluginConfig.routeBasePath,
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

        installNotificationRoutes(
            basePath              = pluginConfig.routeBasePath,
            templateService       = pluginConfig.templateService,
            templateUpdateHandler = pluginConfig.templateUpdateHandler,
        )
    }
}

object NotificationServerDefaults {
    var dispatcher: NotificationDispatcher? = null
}
