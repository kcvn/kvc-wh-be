package com.kcvn.spm.common.helper

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class JsonConvert {
    companion object {
        val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

        fun serialize(obj: Any): String {
            return objectMapper.writeValueAsString(obj)
        }

        inline fun <reified T> deserialize(json: String, type: TypeReference<T>): T {
            return objectMapper.readValue(json, type)
        }
    }
}