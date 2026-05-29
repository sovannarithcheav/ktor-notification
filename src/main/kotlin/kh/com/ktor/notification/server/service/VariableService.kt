package kh.com.ktor.notification.server.service

import kh.com.ktor.notification.server.entity.Variable
import kh.com.ktor.notification.server.entity.VariableRequest
import kh.com.ktor.notification.server.repository.EventVariableRepository
import kh.com.ktor.notification.server.repository.NotificationTemplateRepository
import kh.com.ktor.notification.server.repository.VariableRepository

class VariableService {
    fun getAll(): List<Variable> = VariableRepository.findAll()
    fun getById(id: Long): Variable? = VariableRepository.findById(id)
    fun getByEventCode(eventCode: String): List<Variable> = EventVariableRepository.findVariablesByEventCode(eventCode)
    fun create(request: VariableRequest): Variable = VariableRepository.create(request)

    fun update(id: Long, request: VariableRequest): Result<Variable?> {
        val existing = VariableRepository.findById(id)
            ?: return Result.success(null)

        if (request.name != existing.name && NotificationTemplateRepository.isVariableNameUsed(existing.name)) {
            return Result.failure(
                IllegalStateException(
                    "Cannot rename variable '${existing.name}' — it is referenced in one or more notification templates"
                )
            )
        }

        return Result.success(VariableRepository.update(id, request))
    }
}
