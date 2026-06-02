package kh.com.ktor.notification.server.audit

import kotlinx.serialization.Serializable

@Serializable
data class AuditEventMessage(
    val activity:    String,
    val module:      String,
    val function:    String,
    val userId:      Long?   = null,
    val username:    String? = null,
    val roleType:    String? = null,
    val description: String? = null,
    val referenceId: Long?   = null,
    val requestChangeId: Long? = null,
)
