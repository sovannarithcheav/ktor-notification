package kh.com.ktor.notification.server.enums

import kotlinx.serialization.Serializable

@Serializable
data class StatusRes(val id: Long, val name: String)

enum class StatusEnum(val id: Long, val label: String) {
    ACTIVE(1, "Active"),
    INACTIVE(2, "Inactive");

    fun toRes() = StatusRes(id, label)

    companion object {
        fun fromId(id: Long): StatusRes =
            entries.find { it.id == id }?.toRes() ?: StatusRes(id, "Unknown")
    }
}
