package com.kcvn.spm.controller

import com.kcvn.spm.model.tables.pojos.Functions
import com.kcvn.spm.model.tables.pojos.Permissions
import com.kcvn.spm.payload.request.FunctionRequest
import com.kcvn.spm.payload.request.PermissionRequest
import com.kcvn.spm.payload.response.FunctionResponse
import com.kcvn.spm.payload.response.PermissionResponse
import com.kcvn.spm.service.FunctionService
import com.kcvn.spm.service.PermissionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/function")

class FunctionController(
    private val functionService: FunctionService
)
{
    @GetMapping("/all")
    fun getAllFunction(): ResponseEntity<List<FunctionResponse>?> {
        return try {
            val groups: MutableList<FunctionResponse> = mutableListOf()
            functionService.findAll().forEach { u ->
                groups.add(
                    FunctionResponse(
                        id = u.functionId!!,
                        name = u.functionName!!
                    )
                )
            }
            if (groups.isEmpty()) ResponseEntity<List<FunctionResponse>?>(HttpStatus.NO_CONTENT)
            else ResponseEntity<List<FunctionResponse>?>(groups, HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity<List<FunctionResponse>?>(null, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
    @GetMapping("/{id}")
    fun getGroupById(@PathVariable("id") id: Int): ResponseEntity<FunctionResponse?> {
        val function = functionService.findById(id)
        return if (function != null) {
            ResponseEntity<FunctionResponse?>(
                FunctionResponse(
                    id = function.functionId!!,
                    name = function.functionName!!
                ), HttpStatus.OK
            )
        } else {
            ResponseEntity<FunctionResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/create")
    fun createFunction(@RequestBody request: @Valid FunctionRequest?): ResponseEntity<*> {
        // Create new function
        val function = Functions(
            null,
            functionName = request?.name,
        )
        val functionId = functionService.save(function)
        return ResponseEntity<FunctionResponse>(
            FunctionResponse(
                id = functionId!!,
                name = function.functionName!!
            ), HttpStatus.CREATED
        )
    }

    @PutMapping("/update")
    fun updateFunction(
        @RequestBody request: @Valid FunctionRequest
    ): ResponseEntity<FunctionResponse?> {
        val function = functionService.findById(request.id)
        return if (function != null) {
            function.functionName = request.name
            val response: FunctionResponse
            functionService.update(function).let {
                response = FunctionResponse(
                    id = function.functionId!!,
                    name = function.functionName!!
                )
            }
            ResponseEntity<FunctionResponse?>(response, HttpStatus.OK)
        } else {
            ResponseEntity<FunctionResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @DeleteMapping("/delete/{id}")
    fun deleteFunction(@PathVariable("id") id: Int): ResponseEntity<HttpStatus> {
        return try {
            functionService.deleteById(id)
            ResponseEntity<HttpStatus>(HttpStatus.NO_CONTENT)
        } catch (e: Exception) {
            ResponseEntity<HttpStatus>(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

}