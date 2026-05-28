package kh.com.ktor.notification.server.entity

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object UserWebNotifications : LongIdTable("user_web_notifications") {
    val userId       = long("user_id").references(Users.id)
    val subject      = varchar("subject", 255)
    val eventCode    = varchar("event_code", 255)
    val content      = text("content")
    val eventDate    = datetime("event_date")
    val read         = bool("read").default(false)
    val categoryId   = long("category_id").nullable()
    val redirectable = bool("redirectable").default(false)
    val metadata     = text("metadata").nullable()
    val updatedAt    = datetime("updated_at").nullable()
}

@Serializable
data class UserWebNotification(
    val id: Long = 0,
    val userId: Long,
    val eventCode: String,
    val subject: String,
    val content: String,
    val eventDate: LocalDateTime,
    val read: Boolean = false,
    val categoryId: Long? = null,
    val redirectable: Boolean = false,
    val metadata: String? = null,
    val updatedAt: LocalDateTime? = null,
)

data class UserWebNotificationCreate(
    val userId: Long,
    val eventCode: String,
    val subject: String,
    val content: String,
    val categoryId: Long? = null,
)

fun ResultRow.toUserWebNotification() = UserWebNotification(
    id           = this[UserWebNotifications.id].value,
    userId       = this[UserWebNotifications.userId],
    eventCode    = this[UserWebNotifications.eventCode],
    subject      = this[UserWebNotifications.subject],
    content      = this[UserWebNotifications.content],
    eventDate    = this[UserWebNotifications.eventDate],
    read         = this[UserWebNotifications.read],
    categoryId   = this[UserWebNotifications.categoryId],
    redirectable = this[UserWebNotifications.redirectable],
    metadata     = this[UserWebNotifications.metadata],
    updatedAt    = this[UserWebNotifications.updatedAt],
)
