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

    // inventory
    V_INVENTORY("inventory.v"),
    WH("wh"),
}