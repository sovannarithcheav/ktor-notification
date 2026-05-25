package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.EventNotifications
import kh.com.ktor.notification.server.entity.NotificationSubscriptionView
import kh.com.ktor.notification.server.entity.PageRequest
import kh.com.ktor.notification.server.entity.order
import kh.com.ktor.notification.server.entity.UserSubscription
import kh.com.ktor.notification.server.entity.UserSubscriptions
import kh.com.ktor.notification.server.entity.toNotificationSubscriptionView
import kh.com.ktor.notification.server.entity.toUserSubscription
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object UserSubscriptionRepository {

    fun findByUserIdAndEventId(userId: Long, eventId: Long): List<UserSubscription> = transaction {
        UserSubscriptions.selectAll()
            .where { (UserSubscriptions.userId eq userId) and (UserSubscriptions.eventId eq eventId) }
            .map { it.toUserSubscription() }
    }

    fun findAll(
        userId: Long,
        q: String? = null,
        categoryId: Long? = null,
        pageReq: PageRequest = PageRequest.of(),
    ): List<NotificationSubscriptionView> = transaction {
        val orderCol = when (pageReq.sortBy) {
            "eventCode"  -> EventNotifications.code
            "categoryId" -> EventNotifications.categoryId
            "createdAt"  -> UserSubscriptions.createdAt
            "channelId"  -> UserSubscriptions.channelId
            else         -> EventNotifications.name
        }
        UserSubscriptions
            .join(EventNotifications, JoinType.INNER) { UserSubscriptions.eventId eq EventNotifications.id }
            .selectAll()
            .where { buildFilter(userId, q, categoryId) }
            .orderBy(orderCol to pageReq.order())
            .limit(pageReq.size, offset = pageReq.offset)
            .map { it.toNotificationSubscriptionView() }
    }

    fun count(userId: Long, q: String? = null, categoryId: Long? = null): Long = transaction {
        UserSubscriptions
            .join(EventNotifications, JoinType.INNER) { UserSubscriptions.eventId eq EventNotifications.id }
            .selectAll().where { buildFilter(userId, q, categoryId) }.count()
    }

    fun subscribe(userId: Long, eventId: Long, channelIds: List<Long>): List<UserSubscription> = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        UserSubscriptions.deleteWhere {
            (UserSubscriptions.userId eq userId) and (UserSubscriptions.eventId eq eventId)
        }
        channelIds.forEach { channelId ->
            UserSubscriptions.insert {
                it[UserSubscriptions.userId]    = userId
                it[UserSubscriptions.eventId]   = eventId
                it[UserSubscriptions.channelId] = channelId
                it[UserSubscriptions.createdAt] = now
                it[UserSubscriptions.updatedAt] = now
            }
        }
        findByUserIdAndEventId(userId, eventId)
    }

    private fun buildFilter(userId: Long, q: String?, categoryId: Long?): Op<Boolean> {
        val conditions = buildList {
            add(UserSubscriptions.userId eq userId)
            q?.takeIf { it.isNotBlank() }?.lowercase()?.let { term ->
                add(
                    (EventNotifications.name.lowerCase() like "%$term%") or
                    (EventNotifications.code.lowerCase() like "%$term%")
                )
            }
            categoryId?.let { add(EventNotifications.categoryId eq it) }
        }
        return conditions.reduce { a, b -> a and b }
    }
}
