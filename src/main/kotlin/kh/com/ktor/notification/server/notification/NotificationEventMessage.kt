package kh.com.ktor.notification.server.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationEventMessage(
    val userId: Long,
    val eventCode: String,
    val mergeFields: Map<String, String> = emptyMap(),
)
