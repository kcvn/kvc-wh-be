package com.kcvn.spm.service

import com.kcvn.spm.model.tables.pojos.Groups
import com.kcvn.spm.model.tables.references.GROUPS
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class GroupService(private val context: DSLContext) {
    fun findAll() = context.selectFrom(GROUPS).fetchInto(Groups::class.java)

    fun findById(id: Int): Groups? =
        context.selectFrom(GROUPS).where(GROUPS.GROUP_ID.eq(id)).fetchInto(Groups::class.java).firstOrNull()

    fun save(group: Groups) = context.insertInto(GROUPS, GROUPS.GROUP_NAME, GROUPS.GROUP_DESCRIPTION)
        .values(group.groupName, group.groupDescription)
        .returningResult(GROUPS.GROUP_ID)
        .fetchOne()?.value1()

    fun update(group: Groups) = context.update(GROUPS)
        .set(GROUPS.GROUP_NAME, group.groupName)
        .set(GROUPS.GROUP_DESCRIPTION, group.groupDescription)
        .where(GROUPS.GROUP_ID.eq(group.groupId))
        .returningResult(GROUPS)
        .fetchInto(Groups::class.java).firstOrNull()

    fun deleteById(id: Int) = context.deleteFrom(GROUPS).where(GROUPS.GROUP_ID.eq(id)).execute()
}