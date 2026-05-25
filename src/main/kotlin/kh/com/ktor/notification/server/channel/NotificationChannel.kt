package kh.com.ktor.notification.server.channel

interface NotificationChannel {
    val id: Long
    val name: String

    suspend fun dispatch(
        userId: Long,
        title: String,
        body: String,
        subject: String? = null,
        mergeFields: Map<String, String> = emptyMap(),
    ): DispatchResult
}

data class DispatchResult(
    val success: Boolean,
    val message: String,
)
