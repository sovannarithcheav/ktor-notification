package kh.com.ktor.notification.server.service

import kh.com.ktor.notification.server.entity.ChannelReq
import kh.com.ktor.notification.server.entity.ChannelRes
import kh.com.ktor.notification.server.entity.NotificationSubscriptionFilterReq
import kh.com.ktor.notification.server.entity.NotificationSubscriptionRes
import kh.com.ktor.notification.server.entity.NotificationSubscriptionView
import kh.com.ktor.notification.server.entity.PageRequest
import kh.com.ktor.notification.server.entity.PageResponse
import kh.com.ktor.notification.server.enums.AuditAction
import kh.com.ktor.notification.server.enums.ChannelEnum
import kh.com.ktor.notification.server.repository.ChannelRepository
import kh.com.ktor.notification.server.repository.EventNotificationRepository
import kh.com.ktor.notification.server.repository.NotificationTemplateRepository
import kh.com.ktor.notification.server.repository.UserSubscriptionRepository
import kh.com.ktor.notification.server.security.NotificationErrorCode
import kh.com.ktor.notification.server.security.UserInfo

class NotificationSubscriptionService {

    fun findAll(
        userId: Long,
        filter: NotificationSubscriptionFilterReq,
        pageReq: PageRequest,
    ): PageResponse<NotificationSubscriptionView> {
        val content = UserSubscriptionRepository.findAll(
            userId     = userId,
            q          = filter.q,
            categoryId = filter.categoryId,
            pageReq    = pageReq,
        )
        val total = UserSubscriptionRepository.count(
            userId     = userId,
            q          = filter.q,
            categoryId = filter.categoryId,
        )
        return PageResponse.of(content, pageReq, total)
    }

    fun subscribe(user: UserInfo, req: ChannelReq): NotificationSubscriptionRes {
        val containedNone = req.channelIds.contains(ChannelEnum.NONE.id)

        val availableChannelIds = ChannelEnum.withTelegram()

        val event = EventNotificationRepository.findByIdActive(req.eventId)
            ?: NotificationErrorCode.NOTI010.issue(arrayOf("id[${req.eventId}]"))

        if (event.isRequired && containedNone) {
            NotificationErrorCode.NOTI007.issue(arrayOf(req.eventId))
        }

        if (containedNone && req.channelIds.size > 1) {
            NotificationErrorCode.NOTI011.issue()
        }

        val channels  = ChannelRepository.findAllByIdIn(availableChannelIds)
        val validIds   = channels.map { it.id }.toSet()
        val invalidIds = req.channelIds.filterNot { it in validIds }
        if (invalidIds.isNotEmpty()) {
            NotificationErrorCode.NOTI011.issue()
        }

        if (!containedNone) {
            val templates        = NotificationTemplateRepository.findByEventId(req.eventId)
            val templateChannels = templates.map { it.channelId }.toSet()
            val channelMap       = channels.associateBy { it.id }
            val missingNames     = req.channelIds
                .filterNot { it in templateChannels }
                .mapNotNull { channelMap[it]?.name }

            if (missingNames.isNotEmpty()) {
                NotificationErrorCode.NOTI013.issue(arrayOf(event.code, missingNames.joinToString()))
            }
        }

        UserSubscriptionRepository.subscribe(user.userId, req.eventId, req.channelIds)
        AuditLogService.log(
            activity    = AuditAction.SUBSCRIBE,
            function    = "subscribe",
            module      = "subscription",
            user        = user,
            description = "eventId=${req.eventId} channels=${req.channelIds}",
        )

        return NotificationSubscriptionRes(
            eventId            = event.id,
            eventCode          = event.code,
            eventName          = event.name,
            categoryId         = event.categoryId,
            channels           = channels.map { ChannelRes(it.id, it.name) },
            selectedChannelIds = req.channelIds,
        )
    }
}
