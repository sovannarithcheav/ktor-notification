package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.EventNotifications
import kh.com.ktor.notification.server.entity.EventVariables
import kh.com.ktor.notification.server.entity.Variable
import kh.com.ktor.notification.server.entity.Variables
import kh.com.ktor.notification.server.entity.toVariable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object EventVariableRepository {

    fun findVariablesByEventId(eventId: Long): List<Variable> = transaction {
        (EventVariables innerJoin Variables).selectAll()
            .where { EventVariables.eventId eq eventId }.map { it.toVariable() }
    }

    fun findVariablesByEventCode(eventCode: String): List<Variable> = transaction {
        val eventRow = EventNotifications.selectAll()
            .where { EventNotifications.code eq eventCode }.firstOrNull()
            ?: return@transaction emptyList()
        val eid = eventRow[EventNotifications.id].value
        (EventVariables innerJoin Variables).selectAll()
            .where { EventVariables.eventId eq eid }.map { it.toVariable() }
    }

    fun replace(eventId: Long, variableIds: List<Long>) = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        EventVariables.deleteWhere { EventVariables.eventId eq eventId }
        variableIds.forEach { variableId ->
            EventVariables.insert {
                it[EventVariables.eventId]    = eventId
                it[EventVariables.variableId] = variableId
                it[EventVariables.createdAt]  = now
                it[EventVariables.updatedAt]  = now
            }
        }
    }
}
