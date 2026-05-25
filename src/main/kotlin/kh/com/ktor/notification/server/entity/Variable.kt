package kh.com.ktor.notification.server.entity

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Variables : LongIdTable("variables") {
    val name      = varchar("name", 255)
    val label     = varchar("label", 255)
    val createdAt = datetime("created_at").nullable()
    val updatedAt = datetime("updated_at").nullable()
}

@Serializable
data class Variable(
    val id: Long = 0,
    val name: String,
    val label: String,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

@Serializable
data class VariableRequest(val name: String, val label: String)

fun ResultRow.toVariable() = Variable(
    id        = this[Variables.id].value,
    name      = this[Variables.name],
    label     = this[Variables.label],
    createdAt = this[Variables.createdAt],
    updatedAt = this[Variables.updatedAt],
)
