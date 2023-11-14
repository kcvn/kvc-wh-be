package com.kcvn.spm.controller

import com.kcvn.spm.model.tables.pojos.Groups
import com.kcvn.spm.payload.request.GroupRequest
import com.kcvn.spm.payload.response.GroupResponse
import com.kcvn.spm.service.GroupService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/group")
class GroupController(
    private val groupService: GroupService
) {
    @GetMapping("/all")
    fun getAllGroups(): ResponseEntity<List<GroupResponse>?> {
        return try {
            val groups: MutableList<GroupResponse> = mutableListOf()
            groupService.findAll().forEach { u ->
                groups.add(
                    GroupResponse(
                        u.groupId!!,
                        u.groupName!!,
                        u.groupDescription
                    )
                )
            }

            if (groups.isEmpty()) ResponseEntity<List<GroupResponse>?>(HttpStatus.NO_CONTENT)
            else ResponseEntity<List<GroupResponse>?>(groups, HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity<List<GroupResponse>?>(null, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/{id}")
    fun getGroupById(@PathVariable("id") id: Int): ResponseEntity<GroupResponse?> {
        val group = groupService.findById(id)
        return if (group != null) {
            ResponseEntity<GroupResponse?>(
                GroupResponse(
                    group.groupId!!,
                    group.groupName!!,
                    group.groupDescription
                ), HttpStatus.OK
            )
        } else {
            ResponseEntity<GroupResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/create")
    fun createGroup(@RequestBody groupRequest: @Valid GroupRequest?): ResponseEntity<*> {
        // Create new group
        val group = Groups(
            null,
            groupRequest?.name,
            groupRequest?.description
        )
        val groupId = groupService.save(group)
        return ResponseEntity<GroupResponse>(
            GroupResponse(groupId!!, group.groupName!!, group.groupDescription),
            HttpStatus.CREATED
        )
    }

    @PutMapping("/update/{id}")
    fun updateGroup(
        @PathVariable("id") id: Int,
        @RequestBody groupRequest: @Valid GroupRequest
    ): ResponseEntity<GroupResponse?> {
        val group = groupService.findById(id)
        return if (group != null) {
            group.groupName = groupRequest.name
            group.groupDescription = groupRequest.description
            val response: GroupResponse
            groupService.update(group).let {
                response = GroupResponse(
                    group.groupId!!,
                    group.groupName!!,
                    group.groupDescription
                )
            }
            ResponseEntity<GroupResponse?>(response, HttpStatus.OK)
        } else {
            ResponseEntity<GroupResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @DeleteMapping("/delete/{id}")
    fun deleteGroup(@PathVariable("id") id: Int): ResponseEntity<HttpStatus> {
        return try {
            groupService.deleteById(id)
            ResponseEntity<HttpStatus>(HttpStatus.NO_CONTENT)
        } catch (e: Exception) {
            ResponseEntity<HttpStatus>(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}