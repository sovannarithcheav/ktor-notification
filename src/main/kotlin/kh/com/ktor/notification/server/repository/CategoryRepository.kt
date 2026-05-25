package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.Categories
import kh.com.ktor.notification.server.entity.Category
import kh.com.ktor.notification.server.entity.toCategory
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object CategoryRepository {
    fun findAllByIdIn(ids: List<Long>): List<Category> = transaction {
        if (ids.isEmpty()) return@transaction emptyList()
        Categories.selectAll().where { Categories.id inList ids }.map { it.toCategory() }
    }
}
