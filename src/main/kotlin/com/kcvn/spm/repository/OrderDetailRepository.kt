package com.kcvn.spm.repository

import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class OrderDetailRepository (private val context: DSLContext) {
}