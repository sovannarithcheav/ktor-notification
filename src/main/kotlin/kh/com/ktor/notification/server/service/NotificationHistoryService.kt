package kh.com.ktor.notification.server.service

import kh.com.ktor.notification.server.entity.PageRequest
import kh.com.ktor.notification.server.entity.PageResponse
import kh.com.ktor.notification.server.entity.UserWebNotification
import kh.com.ktor.notification.server.enums.AuditAction
import kh.com.ktor.notification.server.repository.UserWebNotificationRepository
import kh.com.ktor.notification.server.security.UserInfo

class NotificationHistoryService {

    fun findAll(
        userId: Long,
        read: Boolean?,
        pageReq: PageRequest,
    ): PageResponse<UserWebNotification> {
        val content = UserWebNotificationRepository.findAll(userId, read, pageReq)
        val total   = UserWebNotificationRepository.count(userId, read)
        return PageResponse.of(content, pageReq, total)
    }

    fun markAsRead(id: Long, user: UserInfo): Boolean {
        val updated = UserWebNotificationRepository.markAsRead(id, user.userId)
        if (updated) AuditLogService.log(
            activity    = AuditAction.MARK_READ,
            function    = "markAsRead",
            module      = "NotificationHistory",
            user        = user,
            description = "notificationId=$id",
        )
        return updated
    }

    fun markAllAsRead(user: UserInfo): Int {
        val updated = UserWebNotificationRepository.markAllAsRead(user.userId)
        if (updated > 0) AuditLogService.log(
            activity    = AuditAction.MARK_ALL_READ,
            function    = "markAllAsRead",
            module      = "NotificationHistory",
            user        = user,
            description = "$updated notifications marked as read",
        )
        return updated
    }

    fun countUnread(userId: Long): Long = UserWebNotificationRepository.countUnread(userId)
}
