package com.kcvn.spm.controller

import com.kcvn.spm.payload.request.FunctionRequest
import com.kcvn.spm.payload.response.FunctionResponse
import com.kcvn.spm.service.FunctionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/function")

class FunctionController(
    private val functionService: FunctionService
) {
    @GetMapping("/all")
    fun getAllFunction(): ResponseEntity<List<FunctionResponse>?> {
        return try {
            val functions = functionService.findAll()
            if (functions.isEmpty()) ResponseEntity<List<FunctionResponse>?>(HttpStatus.NO_CONTENT)
            else ResponseEntity<List<FunctionResponse>?>(functions, HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity<List<FunctionResponse>?>(null, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/{id}")
    fun getGroupById(@PathVariable("id") id: Int): ResponseEntity<FunctionResponse?> {
        val function = functionService.findById(id)
        return if (function != null) {
            ResponseEntity<FunctionResponse?>(function, HttpStatus.OK)
        } else {
            ResponseEntity<FunctionResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/create")
    fun createFunction(@RequestBody request: @Valid FunctionRequest?): ResponseEntity<*> {
        // Create new function
        val function = functionService.save(request!!)
        return ResponseEntity<FunctionResponse>(function, HttpStatus.CREATED)
    }

    @PutMapping("/update/{id}")
    fun updateFunction(
        @PathVariable("id") id: Int,
        @RequestBody request: @Valid FunctionRequest
    ): ResponseEntity<FunctionResponse?> {
        val function = functionService.update(id, request)
        return if (function != null) {
            ResponseEntity<FunctionResponse?>(function, HttpStatus.OK)
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