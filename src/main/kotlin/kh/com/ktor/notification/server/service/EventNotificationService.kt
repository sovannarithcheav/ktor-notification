package kh.com.ktor.notification.server.service

import kh.com.ktor.notification.server.entity.CategoryRes
import kh.com.ktor.notification.server.entity.EventNotification
import kh.com.ktor.notification.server.entity.EventNotificationOption
import kh.com.ktor.notification.server.entity.EventNotificationRequest
import kh.com.ktor.notification.server.entity.EventNotificationRes
import kh.com.ktor.notification.server.entity.toCategoryRes
import kh.com.ktor.notification.server.enums.StatusEnum
import kh.com.ktor.notification.server.repository.CategoryRepository
import kh.com.ktor.notification.server.repository.EventNotificationRepository

class EventNotificationService {

    fun getAll(): List<EventNotificationRes> = enrich(EventNotificationRepository.findAll())
    fun getById(id: Long): EventNotificationRes? = EventNotificationRepository.findById(id)?.let { enrich(listOf(it)).first() }
    fun create(request: EventNotificationRequest): EventNotificationRes = enrich(listOf(EventNotificationRepository.create(request))).first()
    fun update(id: Long, request: EventNotificationRequest): EventNotificationRes? = EventNotificationRepository.update(id, request)?.let { enrich(listOf(it)).first() }
    fun getOptions(): List<EventNotificationOption> = EventNotificationRepository.findAllOptions()

    private fun enrich(events: List<EventNotification>): List<EventNotificationRes> {
        if (events.isEmpty()) return emptyList()
        val categories = CategoryRepository.findAllByIdIn(events.map { it.categoryId }.distinct()).associateBy { it.id }
        return events.map { e ->
            EventNotificationRes(
                id          = e.id,
                code        = e.code,
                name        = e.name,
                description = e.description,
                type        = e.type,
                isRequired  = e.isRequired,
                status      = StatusEnum.fromId(e.statusId),
                category    = categories[e.categoryId]?.toCategoryRes() ?: CategoryRes(e.categoryId, "", null),
                createdAt   = e.createdAt,
                updatedAt   = e.updatedAt,
                createdBy   = e.createdBy,
                updatedBy   = e.updatedBy,
            )
        }
    }
}
