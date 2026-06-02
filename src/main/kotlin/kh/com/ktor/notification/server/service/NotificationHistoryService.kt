package kh.com.ktor.notification.server.service

import kh.com.ktor.notification.server.entity.PageRequest
import kh.com.ktor.notification.server.entity.PageResponse
import kh.com.ktor.notification.server.entity.UserWebNotification
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
        return UserWebNotificationRepository.markAsRead(id, user.userId)
    }

    fun markAllAsRead(user: UserInfo): Int {
        return UserWebNotificationRepository.markAllAsRead(user.userId)
    }

    fun countUnread(userId: Long): Long = UserWebNotificationRepository.countUnread(userId)
}
