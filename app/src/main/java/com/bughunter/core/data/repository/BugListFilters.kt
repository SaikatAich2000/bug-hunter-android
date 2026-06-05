package com.bughunter.core.data.repository

internal data class BugListFilters(
    val projectIds: List<Int> = emptyList(),
    val statuses: List<String> = emptyList(),
    val priorities: List<String> = emptyList(),
    val environments: List<String> = emptyList(),
    val reporterId: Int? = null,
    val assigneeIds: List<Int> = emptyList(),
    val itemTypes: List<String> = emptyList(),
    val eventId: Int? = null,
    val query: String? = null,
    val page: Int = 1,
    val pageSize: Int = 50,
) {
    fun projectIdsOrNull(): List<Int>? = projectIds.ifEmpty { null }
    fun statusesOrNull(): List<String>? = statuses.ifEmpty { null }
    fun prioritiesOrNull(): List<String>? = priorities.ifEmpty { null }
    fun environmentsOrNull(): List<String>? = environments.ifEmpty { null }
    fun assigneeIdsOrNull(): List<Int>? = assigneeIds.ifEmpty { null }
    fun itemTypesOrNull(): List<String>? = itemTypes.ifEmpty { null }
}
