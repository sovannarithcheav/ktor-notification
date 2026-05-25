package kh.com.ktor.notification.server.dispatch

import kh.com.ktor.notification.server.channel.NotificationChannel
import kh.com.ktor.notification.server.channel.DispatchResult
import kh.com.ktor.notification.server.template.TemplateResolver

data class DispatchRequest(
    val userId: Long,
    val title: String,
    val subject: String?,
    val body: String,
    val mergeFields: Map<String, String> = emptyMap(),
    val channelId: Long,
)

class NotificationDispatcher(private val channels: Map<Long, NotificationChannel>) {

    suspend fun dispatch(request: DispatchRequest): DispatchResult {
        val channel = channels[request.channelId]
            ?: return DispatchResult(false, "Channel ${request.channelId} is not registered")

        val resolved = TemplateResolver.resolve(request.subject, request.body, request.mergeFields)

        return channel.dispatch(
            userId      = request.userId,
            title       = request.title,
            body        = resolved.body,
            mergeFields = request.mergeFields,
        )
    }
}
