package kh.com.ktor.notification.server.service

import kh.com.ktor.notification.server.entity.Variable
import kh.com.ktor.notification.server.entity.VariableRequest
import kh.com.ktor.notification.server.repository.EventVariableRepository
import kh.com.ktor.notification.server.repository.VariableRepository

class VariableService {
    fun getAll(): List<Variable> = VariableRepository.findAll()
    fun getById(id: Long): Variable? = VariableRepository.findById(id)
    fun getByEventCode(eventCode: String): List<Variable> = EventVariableRepository.findVariablesByEventCode(eventCode)
    fun create(request: VariableRequest): Variable = VariableRepository.create(request)
    fun update(id: Long, request: VariableRequest): Variable? = VariableRepository.update(id, request)
}
