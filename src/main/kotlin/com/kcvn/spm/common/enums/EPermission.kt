package com.kcvn.spm.common.enums

enum class EPermission(val value: String) {
    // user
    VIEW_USER("u.v"),
    CREATE_USER("u.c"),
    UPDATE_USER("u.u"),
    DELETE_USER("u.d"),

    // Phân quyền
    VIEW_ROLE("r.v"),
    CREATE_ROLE("r.c"),
    UPDATE_ROLE("r.u"),
    DELETE_ROLE("r.d"),

    // log
    VIEW_LOG("l.v"),

    // products
    V_PRODUCT("product.v"),
    I_PRODUCT("product.i"),
    E_PRODUCT("product.e"),

    // process
    V_PROCESS("process.v"),
    I_PROCESS("process.i"),
    E_PROCESS("process.e"),
    SY_PROCESS_CATALOG("process_catalog.sy"),
    SY_PROCESS_PRODUCT("process_product.sy"),

    // completion - rate
    V_COMPLETION_RATE("com_rate.v"),
    I_COMPLETION_RATE("com_rate.i"),
    E_COMPLETION_RATE("com_rate.e"),

    // work results
    V_WORK_RESULT("work_result.v"),
    E_WORK_RESULT("work_result.e"),
    SY_WORK_RESULT("work_result.sy"),

    // the - order
    V_ORDER("order.v"),
    I_ORDER("order.i"),
    E_ORDER("order.e"),

    //production plan
    V_PRODUCTTION_PLAN("product_plan.v"),

    //production result
    V_PRODUCTION_RESULT("product_result.v"),

    // inventory
    V_INVENTORY("inventory.v"),

    // report
    V_REPORT_ASVERAGE_OUTPUT_OF_TWO_MONTHS("rp.aootm.v"),
    V_REPORT_KTTN_PRODUCT_DELIVERY("rp.kpd.v"),

    // sync data
    V_SYNC_PROCESS_PROCEDURE_STRUCTURE("sync.ppst.v"),
}