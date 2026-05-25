package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.Variable
import kh.com.ktor.notification.server.entity.VariableRequest
import kh.com.ktor.notification.server.entity.Variables
import kh.com.ktor.notification.server.entity.toVariable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object VariableRepository {

    fun findAll(): List<Variable> = transaction {
        Variables.selectAll().map { it.toVariable() }
    }

    fun findAllByIds(ids: List<Long>): List<Variable> = transaction {
        Variables.selectAll().where { Variables.id inList ids }.map { it.toVariable() }
    }

    fun findById(id: Long): Variable? = transaction {
        Variables.selectAll().where { Variables.id eq id }.map { it.toVariable() }.firstOrNull()
    }

    fun create(request: VariableRequest): Variable = transaction {
        val insertedId = Variables.insertAndGetId {
            it[Variables.name]  = request.name
            it[Variables.label] = request.label
        }
        findById(insertedId.value)!!
    }

    fun update(id: Long, request: VariableRequest): Variable? = transaction {
        val rows = Variables.update({ Variables.id eq id }) {
            it[Variables.name]  = request.name
            it[Variables.label] = request.label
        }
        if (rows > 0) findById(id) else null
    }
}
