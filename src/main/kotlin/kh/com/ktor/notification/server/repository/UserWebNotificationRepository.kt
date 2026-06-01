package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.PageRequest
import kh.com.ktor.notification.server.entity.order
import kh.com.ktor.notification.server.entity.UserWebNotification
import kh.com.ktor.notification.server.entity.UserWebNotificationCreate
import kh.com.ktor.notification.server.entity.UserWebNotifications
import kh.com.ktor.notification.server.entity.toUserWebNotification
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * In-app push inbox. By contract only PUSH-channel dispatches are persisted here;
 * email and telegram are recorded in `audit_log` only. The save gate lives in
 * NotificationSendService — do not call [save] from non-push code paths.
 */
object UserWebNotificationRepository {

    fun save(create: UserWebNotificationCreate): UserWebNotification = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val id = UserWebNotifications.insertAndGetId {
            it[UserWebNotifications.userId]     = create.userId
            it[UserWebNotifications.eventCode]  = create.eventCode
            it[UserWebNotifications.subject]    = create.subject
            it[UserWebNotifications.content]    = create.content
            it[UserWebNotifications.eventDate]  = now
            it[UserWebNotifications.read]       = false
            it[UserWebNotifications.categoryId] = create.categoryId
            it[UserWebNotifications.updatedAt]  = now
        }
        findById(id.value)!!
    }

    fun findById(id: Long): UserWebNotification? = transaction {
        UserWebNotifications.selectAll().where { UserWebNotifications.id eq id }
            .map { it.toUserWebNotification() }.firstOrNull()
    }

    fun findAll(userId: Long, read: Boolean? = null, pageReq: PageRequest = PageRequest.of(sort = "eventDate,desc")): List<UserWebNotification> = transaction {
        val orderCol = when (pageReq.sortBy) {
            "subject"   -> UserWebNotifications.subject
            "updatedAt" -> UserWebNotifications.updatedAt
            else        -> UserWebNotifications.eventDate
        }
        UserWebNotifications.selectAll()
            .where { buildFilter(userId, read) }
            .orderBy(orderCol to pageReq.order())
            .limit(pageReq.size, offset = pageReq.offset)
            .map { it.toUserWebNotification() }
    }

    fun count(userId: Long, read: Boolean? = null): Long = transaction {
        UserWebNotifications.selectAll().where { buildFilter(userId, read) }.count()
    }

    fun countUnread(userId: Long): Long = count(userId, read = false)

    fun markAsRead(id: Long, userId: Long): Boolean = transaction {
        UserWebNotifications.update({
            (UserWebNotifications.id eq id) and (UserWebNotifications.userId eq userId)
        }) { it[UserWebNotifications.read] = true } > 0
    }

    fun markAllAsRead(userId: Long): Int = transaction {
        UserWebNotifications.update({
            (UserWebNotifications.userId eq userId) and (UserWebNotifications.read eq false)
        }) { it[UserWebNotifications.read] = true }
    }

    private fun buildFilter(userId: Long, read: Boolean?) =
        if (read != null) (UserWebNotifications.userId eq userId) and (UserWebNotifications.read eq read)
        else UserWebNotifications.userId eq userId
}
