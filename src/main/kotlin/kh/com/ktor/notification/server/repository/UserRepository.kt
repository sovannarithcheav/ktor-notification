package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.User
import kh.com.ktor.notification.server.entity.Users
import kh.com.ktor.notification.server.entity.toUser
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object UserRepository {
    fun findById(id: Long): User? = transaction {
        Users.selectAll().where { Users.id eq id }.map { it.toUser() }.firstOrNull()
    }
}
