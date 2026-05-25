package kh.com.ktor.notification.server.service

import kh.com.ktor.notification.server.NotificationServerDefaults
import kh.com.ktor.notification.server.channel.WebSocketSessionManager
import kh.com.ktor.notification.server.dispatch.DispatchRequest
import kh.com.ktor.notification.server.entity.UserWebNotificationCreate
import kh.com.ktor.notification.server.enums.AuditAction
import kh.com.ktor.notification.server.repository.EventNotificationRepository
import kh.com.ktor.notification.server.repository.UserSubscriptionRepository
import kh.com.ktor.notification.server.repository.UserWebNotificationRepository
import kh.com.ktor.notification.server.security.UserInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private object ChannelId {
    const val EMAIL    = 1L
    const val PUSH     = 2L
    const val TELEGRAM = 3L
    const val NONE     = 4L
}

@Serializable
data class SendNotificationRequest(
    val userId: Long,
    val eventCode: String,
    val mergeFields: Map<String, String> = emptyMap(),
)

@Serializable
data class SendNotificationResponse(
    val success: Boolean,
    val message: String,
)

@Serializable
data class PushMessage(
    val eventCode: String,
    val title: String,
    val body: String,
)

class NotificationSendService(
    private val templateService: NotificationTemplateService = NotificationTemplateService(),
) {

    suspend fun send(request: SendNotificationRequest, caller: UserInfo? = null): List<SendNotificationResponse> {
        val event = EventNotificationRepository.findByCode(request.eventCode)
            ?: return listOf(SendNotificationResponse(false, "Event '${request.eventCode}' not found"))

        val subscriptions = UserSubscriptionRepository.findByUserIdAndEventId(request.userId, event.id)
            .ifEmpty {
                val defaultChannel = if (event.isRequired) ChannelId.PUSH else ChannelId.NONE
                UserSubscriptionRepository.subscribe(
                    userId     = request.userId,
                    eventId    = event.id,
                    channelIds = listOf(defaultChannel),
                )
            }

        return subscriptions.filter { it.channelId != ChannelId.NONE }.map { subscription ->
            val resolved = templateService.resolve(event.id, subscription.channelId, request.mergeFields)
                ?: return@map SendNotificationResponse(
                    false,
                    "No template found for event '${request.eventCode}' on channelId ${subscription.channelId}",
                )

            val title = resolved.subject ?: event.name

            val response = when (subscription.channelId) {
                ChannelId.EMAIL -> {
                    val result = NotificationServerDefaults.dispatcher?.dispatch(
                        DispatchRequest(
                            userId      = request.userId,
                            title       = title,
                            subject     = null,
                            body        = resolved.body,
                            mergeFields = emptyMap(),
                            channelId   = ChannelId.EMAIL,
                        )
                    ) ?: return@map SendNotificationResponse(false, "Notification dispatcher not initialized")
                    SendNotificationResponse(result.success, result.message)
                }
                ChannelId.PUSH -> {
                    val payload = Json.encodeToString(PushMessage(
                        eventCode = event.code,
                        title     = title,
                        body      = resolved.body,
                    ))
                    WebSocketSessionManager.send(request.userId, payload)
                    val connected = WebSocketSessionManager.isConnected(request.userId)
                    SendNotificationResponse(
                        success = true,
                        message = if (connected) "Push sent to user ${request.userId}"
                                  else "User ${request.userId} is not connected via WebSocket",
                    )
                }
                ChannelId.TELEGRAM -> {
                    SendNotificationResponse(true, "Telegram message queued for user ${request.userId}")
                }
                else -> SendNotificationResponse(false, "Channel ${subscription.channelId} is not supported")
            }

            if (response.success) {
                UserWebNotificationRepository.save(
                    UserWebNotificationCreate(
                        userId     = request.userId,
                        title      = title,
                        subject    = event.code,
                        content    = resolved.body,
                        categoryId = event.categoryId,
                    )
                )
                AuditLogService.log(
                    activity    = AuditAction.SEND,
                    function    = "send",
                    module      = "NotificationSend",
                    user        = caller,
                    userId      = request.userId,
                    description = "event=${event.code} channelId=${subscription.channelId}",
                )
            }

            response
        }
    }
}
