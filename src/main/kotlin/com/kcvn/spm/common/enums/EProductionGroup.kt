package com.kcvn.spm.common.enums

/**
 * Bo phan san xuat: ten luu trong he thong (name) <-> ma SAP (sapCode), kem ky tu dau lot (lotPrefix).
 * Ma SAP khong co trong danh sach -> ten ADM, ky tu lot Z (khong map nguoc ve ma SAP duoc).
 */
enum class EProductionGroup(val sapCode: String, val lotPrefix: String) {
    CMOS("2317", "C"),
    SMD("2311", "D"),
    META("2314", "E"),
    CPD("2315", "F"),
    RFD("2318", "G"),
    ITD("2316", "H"),
    PMD("2312", "I"),
    JMD("2313", "J");

    companion object {
        const val DEFAULT_NAME = "ADM"
        const val DEFAULT_LOT_PREFIX = "Z"

        fun fromSapCode(sapCode: String?): EProductionGroup? = values().firstOrNull { it.sapCode == sapCode }

        fun fromName(name: String?): EProductionGroup? = values().firstOrNull { it.name == name }

        /** ma SAP -> ten bo phan, khong co thi ADM */
        fun nameOfSapCode(sapCode: String?): String = fromSapCode(sapCode)?.name ?: DEFAULT_NAME

        /** ma SAP -> ky tu dau lot, khong co thi Z */
        fun lotPrefixOfSapCode(sapCode: String?): String = fromSapCode(sapCode)?.lotPrefix ?: DEFAULT_LOT_PREFIX

        /** ten bo phan -> ma SAP, khong co (vd ADM) thi giu nguyen ten */
        fun sapCodeOfName(name: String?): String = fromName(name)?.sapCode ?: name.orEmpty()
    }
}
