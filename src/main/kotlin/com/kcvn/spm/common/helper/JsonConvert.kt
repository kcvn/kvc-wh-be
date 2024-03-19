package com.kcvn.spm.common.helper

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

class JsonConvert {
    companion object {
        val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())

        fun serialize(obj: Any): String {
            return objectMapper.writeValueAsString(obj)
        }

        inline fun <reified T> deserialize(json: String, type: TypeReference<T>): T {
            return objectMapper.readValue(json, type)
        }

        inline fun <reified T> cloneJson(obj: T): T {
            val json = objectMapper.writeValueAsString(obj)
            val type = object : TypeReference<T>() {}
            return objectMapper.readValue(json, type)
        }
    }
}