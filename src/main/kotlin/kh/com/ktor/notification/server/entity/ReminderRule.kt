package kh.com.ktor.notification.server.entity

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object ReminderRules : LongIdTable("reminder_rules") {
    val name        = varchar("name", 255)
    val dateKind    = varchar("date_kind", 64)
    val offsetDays  = intArray("offset_days")
    val eventId     = long("event_id")
    val channelIds  = longArray("channel_ids")
    val isActive    = bool("is_active")
    val createdAt   = datetime("created_at").nullable()
    val updatedAt   = datetime("updated_at").nullable()
    val createdBy   = long("created_by").nullable()
    val updatedBy   = long("updated_by").nullable()
}

data class ReminderRule(
    val id: Long,
    val name: String,
    val dateKind: String,
    val offsetDays: IntList,
    val eventId: Long,
    val channelIds: LongList,
    val isActive: Boolean,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val createdBy: Long? = null,
    val updatedBy: Long? = null,
)

@Serializable
data class ReminderRuleReq(
    val name: String,
    val dateKind: String,
    val offsetDays: List<Int>,
    val eventId: Long,
    val channelIds: List<Long>,
    val isActive: Boolean = true,
)

@Serializable
data class ReminderRuleRes(
    val id: Long,
    val name: String,
    val dateKind: String,
    val offsetDays: List<Int>,
    val eventId: Long,
    val eventCode: String? = null,
    val channelIds: List<Long>,
    val isActive: Boolean,
)

fun ResultRow.toReminderRule() = ReminderRule(
    id         = this[ReminderRules.id].value,
    name       = this[ReminderRules.name],
    dateKind   = this[ReminderRules.dateKind],
    offsetDays = this[ReminderRules.offsetDays],
    eventId    = this[ReminderRules.eventId],
    channelIds = this[ReminderRules.channelIds],
    isActive   = this[ReminderRules.isActive],
    createdAt  = this[ReminderRules.createdAt],
    updatedAt  = this[ReminderRules.updatedAt],
    createdBy  = this[ReminderRules.createdBy],
    updatedBy  = this[ReminderRules.updatedBy],
)

fun ReminderRule.toRes(eventCode: String? = null) = ReminderRuleRes(
    id, name, dateKind, offsetDays.list, eventId, eventCode, channelIds.list, isActive,
)
