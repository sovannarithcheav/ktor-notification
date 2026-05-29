package kh.com.ktor.notification.server.entity

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.date
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object ReminderEventDates : LongIdTable("reminder_event_dates") {
    val employeeId    = long("employee_id")
    val employeeName  = varchar("employee_name", 255)
    val dateKind      = varchar("date_kind", 64)
    val targetDate    = date("target_date")
    val metadata      = jsonb<JsonObject>("metadata", Json).nullable()
    val createdAt     = datetime("created_at").nullable()
    val updatedAt     = datetime("updated_at").nullable()
}

data class ReminderEventDate(
    val id: Long,
    val employeeId: Long,
    val employeeName: String,
    val dateKind: String,
    val targetDate: LocalDate,
    val metadata: JsonObject? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

@Serializable
data class ReminderEventDateReq(
    val employeeId: Long,
    val employeeName: String,
    val dateKind: String,
    val targetDate: String,           // ISO yyyy-MM-dd; the service parses
    val metadata: JsonObject? = null,
)

@Serializable
data class ReminderEventDateRes(
    val id: Long,
    val employeeId: Long,
    val employeeName: String,
    val dateKind: String,
    val targetDate: String,
    val metadata: JsonObject? = null,
)

fun ResultRow.toReminderEventDate() = ReminderEventDate(
    id           = this[ReminderEventDates.id].value,
    employeeId   = this[ReminderEventDates.employeeId],
    employeeName = this[ReminderEventDates.employeeName],
    dateKind     = this[ReminderEventDates.dateKind],
    targetDate   = this[ReminderEventDates.targetDate],
    metadata     = this[ReminderEventDates.metadata],
    createdAt    = this[ReminderEventDates.createdAt],
    updatedAt    = this[ReminderEventDates.updatedAt],
)

fun ReminderEventDate.toRes() = ReminderEventDateRes(
    id, employeeId, employeeName, dateKind, targetDate.toString(), metadata,
)
