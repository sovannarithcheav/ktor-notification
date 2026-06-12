package kh.com.ktor.notification.server.user

import kotlinx.serialization.Serializable

@Serializable
data class UserSyncMessage(
    val id: Long,
    val email: String?,        // US users.email is nullable
    val fullName: String?,
    val status: String,        // ACTIVE | DEACTIVATED | LOCKED — carried for forward-compat
)
