package kh.com.ktor.notification.server.service

import kh.com.ktor.notification.server.entity.ChannelRes
import kh.com.ktor.notification.server.entity.EventNotificationOption
import kh.com.ktor.notification.server.entity.NotificationTemplate
import kh.com.ktor.notification.server.entity.NotificationTemplateRes
import kh.com.ktor.notification.server.entity.NotificationTemplateUpdateRequest
import kh.com.ktor.notification.server.entity.PageRequest
import kh.com.ktor.notification.server.entity.PageResponse
import kh.com.ktor.notification.server.enums.StatusEnum
import kh.com.ktor.notification.server.repository.ChannelRepository
import kh.com.ktor.notification.server.repository.EventNotificationRepository
import kh.com.ktor.notification.server.repository.EventVariableRepository
import kh.com.ktor.notification.server.repository.NotificationTemplateRepository
import kh.com.ktor.notification.server.repository.VariableRepository
import kh.com.ktor.notification.server.security.NotificationErrorCode
import kh.com.ktor.notification.server.template.TemplateResolver

open class NotificationTemplateService {

    fun getAll(
        eventId: Long? = null,
        channelId: Long? = null,
        statusId: Long? = null,
        pageReq: PageRequest = PageRequest(),
    ): PageResponse<NotificationTemplateRes> {
        val templates = NotificationTemplateRepository.findAll(eventId, channelId, statusId, pageReq)
        val total     = NotificationTemplateRepository.count(eventId, channelId, statusId)
        return PageResponse.of(enrich(templates), pageReq, total)
    }

    fun getById(id: Long): NotificationTemplateRes? {
        val template = NotificationTemplateRepository.findById(id) ?: return null
        return enrich(listOf(template)).first()
    }

    fun resolve(eventId: Long, channelId: Long, mergeFields: Map<String, String>): ResolvedTemplate? {
        val template = NotificationTemplateRepository.findByEventAndChannel(eventId, channelId)
            ?: return null
        val resolved = TemplateResolver.resolve(template.subject, template.body, mergeFields)
        return ResolvedTemplate(subject = resolved.subject, body = resolved.body)
    }

    fun applyUpdate(
        id: Long,
        request: NotificationTemplateUpdateRequest,
    ): Pair<NotificationTemplateRes, NotificationTemplateRes>? {
        val old = NotificationTemplateRepository.findById(id) ?: return null

        if (request.variables.isNotEmpty()) {
            EventVariableRepository.replace(old.eventId, request.variables.map { it.id })
        }

        val new = NotificationTemplateRepository.update(id, request) ?: return null
        return enrich(listOf(old)).first() to enrich(listOf(new)).first()
    }

    protected fun enrich(templates: List<NotificationTemplate>): List<NotificationTemplateRes> {
        if (templates.isEmpty()) return emptyList()

        val events   = EventNotificationRepository.findAllByIdIn(templates.map { it.eventId }.distinct())
            .associateBy { it.id }
        val channels = ChannelRepository.findAllByIdIn(templates.map { it.channelId }.distinct())
            .associateBy { it.id }

        return templates.map { t ->
            val event   = events[t.eventId]?.let { EventNotificationOption(it.id, it.code, it.name) }
                ?: EventNotificationOption(t.eventId, "", "")
            val channel = channels[t.channelId]?.let { ChannelRes(it.id, it.name) }
                ?: ChannelRes(t.channelId, "")
            NotificationTemplateRes(
                id        = t.id,
                name      = t.name,
                event     = event,
                channel   = channel,
                subject   = t.subject,
                body      = t.body,
                status    = StatusEnum.fromId(t.statusId),
                createdAt = t.createdAt,
                updatedAt = t.updatedAt,
                createdBy = t.createdBy,
                updatedBy = t.updatedBy,
                variables = t.variables,
            )
        }
    }

    protected fun validatePlaceholders(request: NotificationTemplateUpdateRequest) {
        val placeholderRegex = Regex("""\$\{([^}]+)\}""")
        val bodyPlaceholders = buildSet {
            request.subject?.let { addAll(placeholderRegex.findAll(it).map { m -> m.groupValues[1] }) }
            addAll(placeholderRegex.findAll(request.body).map { m -> m.groupValues[1] })
        }

        val variableIds = request.variables.map { it.id }
        val varNames = if (variableIds.isEmpty()) emptySet() else {
            VariableRepository.findAllByIds(variableIds)
                .map { it.name.removePrefix("\${").removeSuffix("}") }
                .toSet()
        }

        val notInBody   = varNames - bodyPlaceholders
        val notDeclared = bodyPlaceholders - varNames

        if (notInBody.isNotEmpty() || notDeclared.isNotEmpty()) {
            NotificationErrorCode.NOTI015.issue(arrayOf(
                notInBody.joinToString().ifEmpty { "none" },
                notDeclared.joinToString().ifEmpty { "none" },
            ))
        }
    }
}

data class ResolvedTemplate(
    val subject: String?,
    val body: String,
)
