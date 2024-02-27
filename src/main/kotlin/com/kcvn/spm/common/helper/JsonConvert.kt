package com.kcvn.spm.common.helper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

class JsonConvert {
    companion object {
        val objectMapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())

        fun serialize(obj: Any): String {
            return objectMapper.writeValueAsString(obj)
        }

        inline fun <reified T> deserialize(json: String): T {
            return objectMapper.readValue(json, T::class.java)
        }
    }
}