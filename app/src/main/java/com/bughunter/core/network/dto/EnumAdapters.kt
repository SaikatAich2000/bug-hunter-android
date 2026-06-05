package com.bughunter.core.network.dto

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.EnumJsonAdapter

internal enum class Role {
    @Json(name = "admin") ADMIN,
    @Json(name = "manager") MANAGER,
    @Json(name = "member") MEMBER,
}

internal enum class ProjectRole {
    @Json(name = "lead") LEAD,
    @Json(name = "member") MEMBER,
}

internal enum class ItemType {
    @Json(name = "Bug") BUG,
    @Json(name = "Requirement") REQUIREMENT,
    @Json(name = "Task") TASK,
}

internal enum class Priority {
    @Json(name = "Low") LOW,
    @Json(name = "Medium") MEDIUM,
    @Json(name = "High") HIGH,
    @Json(name = "Critical") CRITICAL,
}

internal enum class Environment {
    @Json(name = "DEV") DEV,
    @Json(name = "UAT") UAT,
    @Json(name = "PROD") PROD,
}

// Union of every status across item types. Server normalizes to canonical case.
internal enum class Status {
    @Json(name = "New") NEW,
    @Json(name = "In Progress") IN_PROGRESS,
    @Json(name = "Resolved") RESOLVED,
    @Json(name = "Closed") CLOSED,
    @Json(name = "Reopened") REOPENED,
    @Json(name = "Not a Bug") NOT_A_BUG,
    @Json(name = "Resolve Later") RESOLVE_LATER,
    @Json(name = "In Review") IN_REVIEW,
    @Json(name = "Approved") APPROVED,
    @Json(name = "Implemented") IMPLEMENTED,
    @Json(name = "Rejected") REJECTED,
    @Json(name = "Deferred") DEFERRED,
    @Json(name = "Done") DONE,
    @Json(name = "Blocked") BLOCKED,
    @Json(name = "Cancelled") CANCELLED,
    @Json(name = "UNKNOWN") UNKNOWN,
}

internal object StatusesByItemType {
    val BUG: Set<Status> = setOf(
        Status.NEW, Status.IN_PROGRESS, Status.RESOLVED, Status.CLOSED,
        Status.REOPENED, Status.NOT_A_BUG, Status.RESOLVE_LATER,
    )
    val REQUIREMENT: Set<Status> = setOf(
        Status.NEW, Status.IN_REVIEW, Status.APPROVED,
        Status.IMPLEMENTED, Status.REJECTED, Status.DEFERRED,
    )
    val TASK: Set<Status> = setOf(
        Status.NEW, Status.IN_PROGRESS, Status.DONE,
        Status.BLOCKED, Status.CANCELLED,
    )

    fun forType(type: ItemType): Set<Status> = when (type) {
        ItemType.BUG -> BUG
        ItemType.REQUIREMENT -> REQUIREMENT
        ItemType.TASK -> TASK
    }
}

internal fun registerEnumAdapters(builder: Moshi.Builder): Moshi.Builder = builder
    .add(Role::class.java, EnumJsonAdapter.create(Role::class.java).withUnknownFallback(Role.MEMBER))
    .add(ProjectRole::class.java, EnumJsonAdapter.create(ProjectRole::class.java).withUnknownFallback(ProjectRole.MEMBER))
    .add(ItemType::class.java, EnumJsonAdapter.create(ItemType::class.java).withUnknownFallback(ItemType.BUG))
    .add(Priority::class.java, EnumJsonAdapter.create(Priority::class.java).withUnknownFallback(Priority.MEDIUM))
    .add(Environment::class.java, EnumJsonAdapter.create(Environment::class.java).withUnknownFallback(Environment.DEV))
    .add(Status::class.java, EnumJsonAdapter.create(Status::class.java).withUnknownFallback(Status.UNKNOWN))
