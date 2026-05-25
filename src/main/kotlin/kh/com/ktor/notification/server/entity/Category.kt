package kh.com.ktor.notification.server.entity

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object Categories : Table("categories") {
    val id          = long("id")
    val name        = varchar("name", 255)
    val description = text("description").nullable()
    val createdAt   = datetime("created_at").nullable()
    val updatedAt   = datetime("updated_at").nullable()
    override val primaryKey = PrimaryKey(id)
}

data class Category(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

@Serializable
data class CategoryRes(val id: Long, val name: String, val description: String?)

fun ResultRow.toCategory() = Category(
    id          = this[Categories.id],
    name        = this[Categories.name],
    description = this[Categories.description],
    createdAt   = this[Categories.createdAt],
    updatedAt   = this[Categories.updatedAt],
)

fun Category.toCategoryRes() = CategoryRes(id, name, description)
