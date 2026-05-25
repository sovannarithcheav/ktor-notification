package kh.com.ktor.notification.server.entity

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object UserSubscriptions : LongIdTable("user_subscriptions") {
    val userId    = long("user_id").references(Users.id)
    val eventId   = long("event_id").references(EventNotifications.id)
    val channelId = long("channel_id")
    val createdAt = datetime("created_at").nullable()
    val updatedAt = datetime("updated_at").nullable()
}

@Serializable
data class UserSubscription(
    val id: Long = 0,
    val userId: Long,
    val eventId: Long,
    val channelId: Long,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

@Serializable
data class NotificationSubscriptionView(
    val id: Long,
    val userId: Long,
    val eventId: Long,
    val eventCode: String,
    val eventName: String,
    val channelId: Long,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

@Serializable
data class NotificationSubscriptionRes(
    val eventId: Long,
    val eventCode: String,
    val eventName: String,
    val categoryId: Long,
    val channels: List<ChannelRes>,
    val selectedChannelIds: List<Long>,
)

@Serializable
data class ChannelReq(val eventId: Long, val channelIds: List<Long>)

@Serializable
data class NotificationSubscriptionFilterReq(val q: String? = null, val categoryId: Long? = null)

fun ResultRow.toUserSubscription() = UserSubscription(
    id        = this[UserSubscriptions.id].value,
    userId    = this[UserSubscriptions.userId],
    eventId   = this[UserSubscriptions.eventId],
    channelId = this[UserSubscriptions.channelId],
    createdAt = this[UserSubscriptions.createdAt],
    updatedAt = this[UserSubscriptions.updatedAt],
)

fun ResultRow.toNotificationSubscriptionView() = NotificationSubscriptionView(
    id        = this[UserSubscriptions.id].value,
    userId    = this[UserSubscriptions.userId],
    eventId   = this[UserSubscriptions.eventId],
    eventCode = this[EventNotifications.code],
    eventName = this[EventNotifications.name],
    channelId = this[UserSubscriptions.channelId],
    createdAt = this[UserSubscriptions.createdAt],
    updatedAt = this[UserSubscriptions.updatedAt],
)
