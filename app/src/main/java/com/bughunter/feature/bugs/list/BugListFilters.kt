package com.bughunter.feature.bugs.list

import com.bughunter.core.data.repository.BugListFilters as RepoFilters

internal data class BugListFilters(
    val projectIds: Set<Int> = emptySet(),
    val statuses: Set<String> = emptySet(),
    val priorities: Set<String> = emptySet(),
    val environments: Set<String> = emptySet(),
    val itemTypes: Set<String> = emptySet(),
    val assigneeIds: Set<Int> = emptySet(),
    val query: String? = null,
    val page: Int = 1,
    val pageSize: Int = PAGE_SIZE,
    val eventId: Int? = null,
    val reporterId: Int? = null,
) {

    val hasActiveFilters: Boolean
        get() = projectIds.isNotEmpty() ||
            statuses.isNotEmpty() ||
            priorities.isNotEmpty() ||
            environments.isNotEmpty() ||
            itemTypes.isNotEmpty() ||
            assigneeIds.isNotEmpty() ||
            !query.isNullOrBlank()

    fun toggleProject(id: Int): BugListFilters =
        copy(projectIds = projectIds.toggle(id), page = 1)

    fun toggleStatus(value: String): BugListFilters =
        copy(statuses = statuses.toggle(value), page = 1)

    fun togglePriority(value: String): BugListFilters =
        copy(priorities = priorities.toggle(value), page = 1)

    fun toggleEnvironment(value: String): BugListFilters =
        copy(environments = environments.toggle(value), page = 1)

    fun toggleItemType(value: String): BugListFilters =
        copy(itemTypes = itemTypes.toggle(value), page = 1)

    fun setOnlyItemType(value: String?): BugListFilters =
        copy(itemTypes = if (value == null) emptySet() else setOf(value), page = 1)

    fun toggleAssignee(id: Int): BugListFilters =
        copy(assigneeIds = assigneeIds.toggle(id), page = 1)

    fun withQuery(value: String?): BugListFilters =
        copy(query = value?.takeIf { it.isNotBlank() }, page = 1)

    fun cleared(): BugListFilters = BugListFilters(pageSize = pageSize)

    fun toRepo(): RepoFilters = RepoFilters(
        projectIds = projectIds.toList(),
        statuses = statuses.toList(),
        priorities = priorities.toList(),
        environments = environments.toList(),
        reporterId = reporterId,
        assigneeIds = assigneeIds.toList(),
        itemTypes = itemTypes.toList(),
        eventId = eventId,
        query = query,
        page = page,
        pageSize = pageSize,
    )

    companion object {
        const val PAGE_SIZE: Int = 50
    }
}

private fun <T> Set<T>.toggle(item: T): Set<T> =
    if (item in this) this - item else this + item
