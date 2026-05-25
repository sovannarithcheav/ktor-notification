package kh.com.ktor.notification.server.repository

import kh.com.ktor.notification.server.entity.Channel
import kh.com.ktor.notification.server.entity.Channels
import kh.com.ktor.notification.server.entity.toChannel
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object ChannelRepository {
    fun findAllByIdIn(ids: List<Long>): List<Channel> = transaction {
        if (ids.isEmpty()) return@transaction emptyList()
        Channels.selectAll().where { Channels.id inList ids }.map { it.toChannel() }
    }
}
