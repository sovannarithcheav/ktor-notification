package kh.com.ktor.notification.server.entity

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object EventVariables : LongIdTable("event_variables") {
    val eventId    = long("event_id").references(EventNotifications.id)
    val variableId = long("variable_id").references(Variables.id)
    val createdAt  = datetime("created_at").nullable()
    val updatedAt  = datetime("updated_at").nullable()
}

@Serializable
data class EventVariable(
    val id: Long = 0,
    val eventId: Long,
    val variableId: Long,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

fun ResultRow.toEventVariable() = EventVariable(
    id         = this[EventVariables.id].value,
    eventId    = this[EventVariables.eventId],
    variableId = this[EventVariables.variableId],
    createdAt  = this[EventVariables.createdAt],
    updatedAt  = this[EventVariables.updatedAt],
)
