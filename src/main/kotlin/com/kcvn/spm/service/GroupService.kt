package com.kcvn.spm.service

import com.kcvn.spm.model.tables.pojos.Groups
import com.kcvn.spm.payload.request.GroupRequest
import com.kcvn.spm.payload.response.GroupResponse
import com.kcvn.spm.repository.GroupDAO
import org.springframework.stereotype.Service

@Service
class GroupService(private val groupDAO: GroupDAO) {
    fun findAll(): List<GroupResponse> = groupDAO.findAll().map { g ->
        GroupResponse(
            g.groupId!!,
            g.groupName!!,
            g.groupDescription
        )
    }

    fun findById(id: Int): GroupResponse? {
        val group = groupDAO.findById(id)
        return if (group == null) {
            null
        } else {
            GroupResponse(
                group.groupId!!,
                group.groupName!!,
                group.groupDescription
            )
        }
    }

    fun save(group: Groups) = groupDAO.save(group)

    fun update(id: Int, request: GroupRequest): GroupResponse? {
        var group = groupDAO.findById(id)
        return if (group == null) {
            null
        } else {
            group.groupName = request.name
            group.groupDescription = request.description
            group = groupDAO.update(group)
            GroupResponse(
                group?.groupId!!,
                group.groupName!!,
                group.groupDescription
            )
        }
    }

    fun deleteById(id: Int) = groupDAO.deleteById(id)
}