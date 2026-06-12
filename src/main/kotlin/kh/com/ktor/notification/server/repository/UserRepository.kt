package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.User
import kh.com.ktor.notification.server.entity.Users
import kh.com.ktor.notification.server.entity.toUser
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

object UserRepository {
    fun findById(id: Long): User? = transaction {
        Users.selectAll().where { Users.id eq id }.map { it.toUser() }.firstOrNull()
    }

    fun upsert(id: Long, email: String, fullName: String?) = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val updated = Users.update({ Users.id eq id }) {
            it[Users.email]     = email
            it[Users.fullName]  = fullName
            it[Users.updatedAt] = now
        }
        if (updated == 0) Users.insert {
            it[Users.id]        = id            // externally-supplied PK = US user id
            it[Users.email]     = email
            it[Users.fullName]  = fullName
            it[Users.createdAt] = now
        }
    }
}
