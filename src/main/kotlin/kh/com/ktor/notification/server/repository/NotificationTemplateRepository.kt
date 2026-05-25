package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.EventVariables
import kh.com.ktor.notification.server.entity.NotificationTemplate
import kh.com.ktor.notification.server.entity.NotificationTemplateUpdateRequest
import kh.com.ktor.notification.server.entity.NotificationTemplates
import kh.com.ktor.notification.server.entity.PageRequest
import kh.com.ktor.notification.server.entity.order
import kh.com.ktor.notification.server.entity.TemplateVariableRef
import kh.com.ktor.notification.server.entity.Variables
import kh.com.ktor.notification.server.entity.toNotificationTemplate
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object NotificationTemplateRepository {

    fun findAll(
        eventId: Long? = null,
        channelId: Long? = null,
        statusId: Long? = null,
        pageReq: PageRequest = PageRequest(),
    ): List<NotificationTemplate> = transaction {
        NotificationTemplates.selectAll()
            .where { buildFilter(eventId, channelId, statusId) }
            .orderBy(NotificationTemplates.id to pageReq.order())
            .limit(pageReq.size, offset = pageReq.offset)
            .map { row -> row.toNotificationTemplate(variablesForEvent(row[NotificationTemplates.eventId])) }
    }

    fun count(eventId: Long? = null, channelId: Long? = null, statusId: Long? = null): Long = transaction {
        NotificationTemplates.selectAll().where { buildFilter(eventId, channelId, statusId) }.count()
    }

    fun findById(id: Long): NotificationTemplate? = transaction {
        NotificationTemplates.selectAll().where { NotificationTemplates.id eq id }
            .map { it.toNotificationTemplate(variablesForEvent(it[NotificationTemplates.eventId])) }
            .firstOrNull()
    }

    fun findByEventAndChannel(eventId: Long, channelId: Long): NotificationTemplate? = transaction {
        NotificationTemplates.selectAll()
            .where { (NotificationTemplates.eventId eq eventId) and (NotificationTemplates.channelId eq channelId) }
            .map { it.toNotificationTemplate() }.firstOrNull()
    }

    fun findByEventId(eventId: Long): List<NotificationTemplate> = transaction {
        NotificationTemplates.selectAll().where { NotificationTemplates.eventId eq eventId }
            .map { it.toNotificationTemplate() }
    }

    fun update(id: Long, request: NotificationTemplateUpdateRequest): NotificationTemplate? = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val rows = NotificationTemplates.update({ NotificationTemplates.id eq id }) {
            it[NotificationTemplates.subject]   = request.subject
            it[NotificationTemplates.body]      = request.body
            it[NotificationTemplates.statusId]  = request.statusId
            it[NotificationTemplates.updatedAt] = now
        }
        if (rows > 0) findById(id) else null
    }

    private fun buildFilter(eventId: Long?, channelId: Long?, statusId: Long?): Op<Boolean> {
        val conditions = buildList {
            eventId?.let   { add(NotificationTemplates.eventId   eq it) }
            channelId?.let { add(NotificationTemplates.channelId eq it) }
            statusId?.let  { add(NotificationTemplates.statusId  eq it) }
        }
        return if (conditions.isEmpty()) Op.TRUE else conditions.reduce { a, b -> a and b }
    }

    private fun variablesForEvent(eventId: Long): List<TemplateVariableRef> =
        (EventVariables innerJoin Variables).selectAll()
            .where { EventVariables.eventId eq eventId }
            .map { TemplateVariableRef(id = it[Variables.id].value, label = it[Variables.name]) }
}
