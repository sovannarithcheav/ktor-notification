package kh.com.ktor.notification.server.entity

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Users : LongIdTable("users") {
    val email     = varchar("email", 255)
    val fullName  = varchar("full_name", 255).nullable()
    val createdAt = datetime("created_at").nullable()
    val updatedAt = datetime("updated_at").nullable()
}

@Serializable
data class User(
    val id: Long = 0,
    val email: String,
    val fullName: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

fun ResultRow.toUser() = User(
    id        = this[Users.id].value,
    email     = this[Users.email],
    fullName  = this[Users.fullName],
    createdAt = this[Users.createdAt],
    updatedAt = this[Users.updatedAt],
)
