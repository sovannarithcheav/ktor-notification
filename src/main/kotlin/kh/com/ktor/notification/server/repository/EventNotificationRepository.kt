package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.EventNotification
import kh.com.ktor.notification.server.entity.EventNotificationOption
import kh.com.ktor.notification.server.entity.EventNotificationRequest
import kh.com.ktor.notification.server.entity.EventNotifications
import kh.com.ktor.notification.server.entity.toEventNotification
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object EventNotificationRepository {

    fun findAll(): List<EventNotification> = transaction {
        EventNotifications.selectAll().map { it.toEventNotification() }
    }

    fun findAllByIdIn(ids: List<Long>): List<EventNotification> = transaction {
        if (ids.isEmpty()) return@transaction emptyList()
        EventNotifications.selectAll().where { EventNotifications.id inList ids }.map { it.toEventNotification() }
    }

    fun findAllOptions(): List<EventNotificationOption> = transaction {
        EventNotifications.select(EventNotifications.id, EventNotifications.code, EventNotifications.name)
            .orderBy(EventNotifications.name)
            .map { EventNotificationOption(it[EventNotifications.id].value, it[EventNotifications.code], it[EventNotifications.name]) }
    }

    fun findById(id: Long): EventNotification? = transaction {
        EventNotifications.selectAll().where { EventNotifications.id eq id }.map { it.toEventNotification() }.firstOrNull()
    }

    fun findByIdActive(id: Long, activeStatusId: Long = 1L): EventNotification? = transaction {
        EventNotifications.selectAll()
            .where { (EventNotifications.id eq id) and (EventNotifications.statusId eq activeStatusId) }
            .map { it.toEventNotification() }.firstOrNull()
    }

    fun findAllByTypeIn(types: List<String>): List<EventNotification> = transaction {
        EventNotifications.selectAll().where { EventNotifications.type inList types }.map { it.toEventNotification() }
    }

    fun findByCode(code: String): EventNotification? = transaction {
        EventNotifications.selectAll().where { EventNotifications.code eq code }.map { it.toEventNotification() }.firstOrNull()
    }

    fun create(request: EventNotificationRequest): EventNotification = transaction {
        val id = EventNotifications.insertAndGetId {
            it[EventNotifications.code]        = request.code
            it[EventNotifications.name]        = request.name
            it[EventNotifications.description] = request.description
            it[EventNotifications.type]        = request.type
            it[EventNotifications.isRequired]  = request.isRequired
            it[EventNotifications.statusId]    = request.statusId
            it[EventNotifications.categoryId]  = request.categoryId
        }
        findById(id.value)!!
    }

    fun update(id: Long, request: EventNotificationRequest): EventNotification? = transaction {
        val rows = EventNotifications.update({ EventNotifications.id eq id }) {
            it[EventNotifications.code]        = request.code
            it[EventNotifications.name]        = request.name
            it[EventNotifications.description] = request.description
            it[EventNotifications.type]        = request.type
            it[EventNotifications.isRequired]  = request.isRequired
            it[EventNotifications.statusId]    = request.statusId
            it[EventNotifications.categoryId]  = request.categoryId
        }
        if (rows > 0) findById(id) else null
    }
}
