package com.kcvn.spm.service

import com.kcvn.spm.model.tables.pojos.Functions
import com.kcvn.spm.payload.request.FunctionRequest
import com.kcvn.spm.payload.response.FunctionResponse
import com.kcvn.spm.repository.FunctionDAO
import org.springframework.stereotype.Service

@Service
class FunctionService(private val functionDAO: FunctionDAO) {
    fun findAll(): List<FunctionResponse> =
        functionDAO.findAll().map { FunctionResponse(it.functionId!!, it.functionName!!) }

    fun findById(id: Int): FunctionResponse? {
        val function = functionDAO.findById(id)
        return if (function == null) {
            null
        } else {
            FunctionResponse(function.functionId!!, function.functionName!!)
        }
    }

    fun save(request: FunctionRequest): FunctionResponse? {
        val function = Functions(
            null,
            functionName = request.name,
        )
        val functionId = functionDAO.save(function)
        return FunctionResponse(functionId!!, function.functionName!!)
    }

    fun update(id: Int, request: FunctionRequest): FunctionResponse? {
        val function = functionDAO.findById(id)
        return if (function == null) {
            null
        } else {
            function.functionName = request.name
            functionDAO.update(function)
            FunctionResponse(function.functionId!!, function.functionName!!)
        }
    }

    fun deleteById(id: Int) = functionDAO.deleteById(id)
}