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
    U_PRODUCT("product.u"),

    // process
    V_PROCESS("process.v"),
    I_PROCESS("process.i"),
    E_PROCESS("process.e"),
    SY_PROCESS_CATALOG("process.sy_catalog"),
    SY_PROCESS_PRODUCT("process.sy_product"),

    // completion - rate
    V_COMPLETION_RATE("com_rate.v"),
    I_COMPLETION_RATE("com_rate.i"),
    E_COMPLETION_RATE("com_rate.e"),

    // work results
    V_WORK_RESULT("work_result.v"),
    E_WORK_RESULT("work_result.e"),
    SY_WORK_RESULT("work_result.sy"),

    // order
    V_ORDER("order.v"),
    I_ORDER("order.i"),
    E_ORDER("order.e"),
    C_WORK_PLAN_ORDER("order.c"),

    //work plan
    V_WORK_PLAN("work_plan.v"),
    E_WORK_PLAN("work_plan.e"),
    AP_WORK_PLAN("work_plan.ap"),


    //production result
    V_PRODUCTION_RESULT("product_result.v"),

    // inventory
    V_INVENTORY("inventory.v"),
    E_INVENTORY("inventory.e"),
    I_INVENTORY("inventory.i"),

    // sync data
    V_SYNC_PROCESS_PROCEDURE_STRUCTURE("sync.ppst.v"),

    //report
    V_REPORT_QUANTITY("rp_quantity.v"),
    E_REPORT_QUANTITY ("rp_quantity.e"),
    CA_REPORT_QUANTITY("rp_quantity.ca"),
    LOCK_REPORT_QUANTITY("rp_quantity.lock"),

    V_REPORT_EXPORT_ITEM("rp_export_item.v"),
    E_REPORT_EXPORT_ITEM("rp_export_item.e"),
    I_REPORT_EXPORT_ITEM("rp_export_item.i"),

    V_REPORT_EXTERNAL_QUALITY("rp_external_quality.v"),
    E_REPORT_EXTERNAL_QUALITY("rp_external_quality.e"),
    I_REPORT_EXTERNAL_QUALITY("rp_external_quality.i"),



}