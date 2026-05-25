package kh.com.ktor.notification.server.entity

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

object Channels : Table("channels") {
    val id   = long("id")
    val name = varchar("name", 100)
    override val primaryKey = PrimaryKey(id)
}

data class Channel(val id: Long, val name: String)

@Serializable
data class ChannelRes(val id: Long, val name: String)

fun ResultRow.toChannel() = Channel(id = this[Channels.id], name = this[Channels.name])
