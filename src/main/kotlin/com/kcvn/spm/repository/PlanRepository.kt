package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.PlanDetailTemp
import com.kcvn.spm.model.tables.pojos.PlanProcessTemp
import com.kcvn.spm.model.tables.pojos.PlanProductTemp
import com.kcvn.spm.model.tables.pojos.PlanTemp
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PlanRepository(private val context: DSLContext) {

    fun createPlanTemp(
        plan: PlanTemp,
        planProducts: List<PlanProductTemp>,
        planProcesses: List<Pair<String, List<PlanProcessTemp>>>,
        planChildrenProcesses: List<Pair<String, List<PlanProcessTemp>>>,
        planDetails: List<Pair<String, List<PlanDetailTemp>>>
    ) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)


        }
    }
}