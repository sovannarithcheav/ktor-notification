package kh.com.ktor.notification.server.service

import kh.com.ktor.notification.server.entity.CategoryRes
import kh.com.ktor.notification.server.entity.toCategoryRes
import kh.com.ktor.notification.server.repository.CategoryRepository
import kh.com.ktor.notification.server.repository.EventNotificationRepository

class CategoryService {

    fun findAll(roleType: String): List<CategoryRes> {
        val types       = listOf(roleType, "All").distinct()
        val categoryIds = EventNotificationRepository.findAllByTypeIn(types).map { it.categoryId }.distinct()
        if (categoryIds.isEmpty()) return emptyList()
        return CategoryRepository.findAllByIdIn(categoryIds).map { it.toCategoryRes() }
    }
}
