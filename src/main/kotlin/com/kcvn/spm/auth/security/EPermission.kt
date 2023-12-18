package com.kcvn.spm.auth.security

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

    // report
    VIEW_REPORT_AVERAGE_OUTPUT_OF_TWO_MONTHS("rp.aootm.v"),
    VIEW_REPORT_KTTN_PRODUCT_DELIVERY("rp.kpd.v"),

    //import
    VIEW_IMPORT_INVENTORY("ip.i.v"),
    VIEW_ODER_QUANTITY("ip.o.v"),
    VIEW_PASS_RATE("ip.p.v"),
    VIEW_WORK_RESULT("ip.r.v"),

    // manage-product-information
    VIEW_MANAGEMENT_PRODUCT_INFO("m.p.i.v"),

    //manage-product-creation-flow
    VIEW_MANAGEMENT_PRODUCT_CREATION_FLOW("m.p.c.f.v"),

    //plan Process
    VIEW_PLAN_PROCESS("pl.p.v")
}