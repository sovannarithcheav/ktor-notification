package kh.com.ktor.notification.server.service

import kh.com.ktor.notification.server.NotificationServerDefaults
import kh.com.ktor.notification.server.dispatch.DispatchRequest
import kh.com.ktor.notification.server.entity.UserWebNotificationCreate
import kh.com.ktor.notification.server.repository.EventNotificationRepository
import kh.com.ktor.notification.server.repository.UserSubscriptionRepository
import kh.com.ktor.notification.server.repository.UserWebNotificationRepository
import kh.com.ktor.notification.server.security.UserInfo
import kotlinx.serialization.Serializable

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
    val forceChannels: List<Int> = emptyList(),
)

@Serializable
data class SendNotificationResponse(
    val success: Boolean,
    val message: String,
)

class NotificationSendService(
    private val templateService: NotificationTemplateService = NotificationTemplateService(),
) {

    suspend fun send(request: SendNotificationRequest, caller: UserInfo? = null): List<SendNotificationResponse> {
        val event = EventNotificationRepository.findByCode(request.eventCode)
            ?: return listOf(SendNotificationResponse(false, "Event '${request.eventCode}' not found"))

        val channelIds: List<Long> =
            if (request.forceChannels.isNotEmpty()) {
                request.forceChannels.map { it.toLong() }
            } else {
                UserSubscriptionRepository.findByUserIdAndEventId(request.userId, event.id)
                    .ifEmpty {
                        val defaultChannel = if (event.isRequired) ChannelId.PUSH else ChannelId.NONE
                        UserSubscriptionRepository.subscribe(
                            userId     = request.userId,
                            eventId    = event.id,
                            channelIds = listOf(defaultChannel),
                        )
                    }
                    .map { it.channelId }
            }

        return channelIds.filter { it != ChannelId.NONE }.map { channelId ->
            val resolved = templateService.resolve(event.id, channelId, request.mergeFields)
                ?: return@map SendNotificationResponse(
                    false,
                    "No template found for event '${request.eventCode}' on channelId $channelId",
                )

            val title = resolved.subject ?: event.name

            val response = when (channelId) {
                ChannelId.EMAIL, ChannelId.PUSH -> {
                    val result = NotificationServerDefaults.dispatcher?.dispatch(
                        DispatchRequest(
                            userId      = request.userId,
                            title       = title,
                            subject     = event.code,
                            body        = resolved.body,
                            mergeFields = request.mergeFields,
                            channelId   = channelId,
                            contentType = resolved.contentType,
                        )
                    ) ?: return@map SendNotificationResponse(false, "Notification dispatcher not initialized")
                    SendNotificationResponse(result.success, result.message)
                }
                ChannelId.TELEGRAM -> {
                    SendNotificationResponse(true, "Telegram message queued for user ${request.userId}")
                }
                else -> SendNotificationResponse(false, "Channel $channelId is not supported")
            }

            if (response.success) {
                // user_web_notifications is the in-app push inbox. Only PUSH-channel
                // dispatches land here; email/telegram stay in audit_log only.
                if (channelId == ChannelId.PUSH) {
                    UserWebNotificationRepository.save(
                        UserWebNotificationCreate(
                            userId     = request.userId,
                            eventCode  = event.code,
                            subject    = title,
                            content    = resolved.body,
                            categoryId = event.categoryId,
                        )
                    )
                }
                if (caller != null) AuditLogService.log(
                    activity    = "send",
                    function    = "notification",
                    module      = "notification",
                    user        = caller,
                    description = "Sent ${event.code} notification to user #${request.userId} via channel $channelId",
                )
            }

            response
        }
    }
}
