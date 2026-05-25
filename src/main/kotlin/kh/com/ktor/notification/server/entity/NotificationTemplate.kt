package kh.com.ktor.notification.server.entity

import kh.com.ktor.notification.server.enums.StatusRes
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object NotificationTemplates : LongIdTable("notification_templates") {
    val name      = varchar("name", 255)
    val eventId   = long("event_id").references(EventNotifications.id)
    val channelId = long("channel_id")
    val subject   = varchar("subject", 500).nullable()
    val body      = text("body")
    val statusId  = long("status_id")
    val createdAt = datetime("created_at").nullable()
    val updatedAt = datetime("updated_at").nullable()
    val createdBy = long("created_by").nullable()
    val updatedBy = long("updated_by").nullable()

    init { uniqueIndex(eventId, channelId) }
}

@Serializable
data class NotificationTemplate(
    val id: Long = 0,
    val name: String,
    val eventId: Long,
    val channelId: Long,
    val subject: String? = null,
    val body: String,
    val statusId: Long,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
    val variables: List<TemplateVariableRef> = emptyList(),
)

@Serializable
data class NotificationTemplateRes(
    val id: Long,
    val name: String,
    val event: EventNotificationOption,
    val channel: ChannelRes,
    val subject: String? = null,
    val body: String,
    val status: StatusRes,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
    val variables: List<TemplateVariableRef> = emptyList(),
)

@Serializable
data class TemplateVariableInput(val id: Long)

@Serializable
data class TemplateVariableRef(val id: Long, val label: String)

@Serializable
data class NotificationTemplateUpdateRequest(
    val subject: String? = null,
    val body: String,
    val statusId: Long,
    val variables: List<TemplateVariableInput> = emptyList(),
)

fun ResultRow.toNotificationTemplate(variables: List<TemplateVariableRef> = emptyList()) = NotificationTemplate(
    id        = this[NotificationTemplates.id].value,
    name      = this[NotificationTemplates.name],
    eventId   = this[NotificationTemplates.eventId],
    channelId = this[NotificationTemplates.channelId],
    subject   = this[NotificationTemplates.subject],
    body      = this[NotificationTemplates.body],
    statusId  = this[NotificationTemplates.statusId],
    createdAt = this[NotificationTemplates.createdAt],
    updatedAt = this[NotificationTemplates.updatedAt],
    createdBy = this[NotificationTemplates.createdBy],
    updatedBy = this[NotificationTemplates.updatedBy],
    variables = variables,
)
