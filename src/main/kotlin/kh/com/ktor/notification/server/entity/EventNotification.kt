package kh.com.ktor.notification.server.entity

import kh.com.ktor.notification.server.enums.StatusRes
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object EventNotifications : LongIdTable("event_notifications") {
    val code        = varchar("code", 255)
    val name        = varchar("name", 255)
    val description = text("description").nullable()
    val type        = varchar("type", 100)
    val isRequired  = bool("is_required").default(false)
    val statusId    = long("status_id")
    val categoryId  = long("category_id")
    val createdAt   = datetime("created_at").nullable()
    val updatedAt   = datetime("updated_at").nullable()
    val createdBy   = long("created_by").nullable()
    val updatedBy   = long("updated_by").nullable()
}

@Serializable
data class EventNotification(
    val id: Long = 0,
    val code: String,
    val name: String,
    val description: String? = null,
    val type: String,
    val isRequired: Boolean = false,
    val statusId: Long,
    val categoryId: Long,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
)

@Serializable
data class EventNotificationRes(
    val id: Long,
    val code: String,
    val name: String,
    val description: String? = null,
    val type: String,
    val isRequired: Boolean,
    val status: StatusRes,
    val category: CategoryRes,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
)

@Serializable
data class EventNotificationOption(val id: Long, val code: String, val name: String)

@Serializable
data class EventNotificationRequest(
    val code: String,
    val name: String,
    val description: String? = null,
    val type: String,
    val isRequired: Boolean = false,
    val statusId: Long,
    val categoryId: Long,
)

fun ResultRow.toEventNotification() = EventNotification(
    id          = this[EventNotifications.id].value,
    code        = this[EventNotifications.code],
    name        = this[EventNotifications.name],
    description = this[EventNotifications.description],
    type        = this[EventNotifications.type],
    isRequired  = this[EventNotifications.isRequired],
    statusId    = this[EventNotifications.statusId],
    categoryId  = this[EventNotifications.categoryId],
    createdAt   = this[EventNotifications.createdAt],
    updatedAt   = this[EventNotifications.updatedAt],
    createdBy   = this[EventNotifications.createdBy],
    updatedBy   = this[EventNotifications.updatedBy],
)
