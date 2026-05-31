package kh.com.ktor.notification.server.entity

import kotlinx.serialization.Serializable

@Serializable
data class Pagination(
    val page: Int,
    val size: Int,
    val total: Long,
    val totalPages: Int,
)

@Serializable
data class PageResponse<T>(
    val content: List<T>,
    val pagination: Pagination,
) {
    companion object {
        fun <T> of(content: List<T>, pageReq: PageRequest, total: Long) = PageResponse(
            content    = content,
            pagination = Pagination(
                page       = pageReq.page,
                size       = pageReq.size,
                total      = total,
                totalPages = if (pageReq.size == 0) 0 else ((total + pageReq.size - 1) / pageReq.size).toInt(),
            ),
        )
    }
}
