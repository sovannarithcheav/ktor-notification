package kh.com.ktor.notification.server.notification

import kotlinx.serialization.Serializable

@Serializable
data class NotificationEventMessage(
    val userId: Long,
    val eventCode: String,
    val mergeFields: Map<String, String> = emptyMap(),
    val forceChannels: List<Int> = emptyList(),   // channel ids; EMAIL=1, PUSH=2, TELEGRAM=3, NONE=4
    val recipientEmail: String? = null,            // when set, send uses it directly (no synced-lookup dependency)
)
