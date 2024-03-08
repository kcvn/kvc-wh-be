package com.kcvn.spm.repository

import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PlanRepository (private val context: DSLContext) {
    fun getListPlan() {

    }
}