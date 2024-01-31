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
    SY_PRODUCT("product.sy"),
    E_PRODUCT("product.e"),

    // process
    V_PROCESS("process.v"),
    I_PROCESS("process.i"),
    E_PROCESS("process.e"),

    // completion - rate
    V_COMPLETION_RATE("com_rate.v"),
    I_COMPLETION_RATE("com_rate.i"),
    E_COMPLETION_RATE("com_rate.e"),

    // the - order
    V_ORDER("order.v"),

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