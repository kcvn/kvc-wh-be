package com.kcvn.spm.common.constants

import com.kcvn.spm.common.payload.KeyValueResponse

class Constants {
    companion object {
        //Sync type
        const val PROCESS_PROCEDURE_STRUCTURE = "PROCESS_PROCEDURE_STRUCTURE"
        const val PROCESS_MASTER = "PROCESS_MASTER"
        const val WORK_RESULT = "WORK_RESULT"

        //jwt
        const val CLAIM_TYPE_USER_ID = "UserId"
        const val CLAIM_TYPE_POSITION = "Position"

        //Master Data Type
        const val KHUNG_1 = "KHUNG_1"
        const val KHUNG_2 = "KHUNG_2"
        const val KHUON_DUC = "KHUON_DUC"
        const val LOAI_TAPE = "LOAI_TAPE"
        const val LOAI_XUAT_HANG = "LOAI_XUAT_HANG"
        const val SR_OR_NSR = "SR_OR_NSR"
        const val RING_JIG = "RING_JIG"
        const val TAPE_DUNG_CHUNG = "TAPE_DUNG_CHUNG"
        const val MACHUYENDOI = "MA_CHUYEN_DOI"
        const val MATHONGKE = "MA_THONG_KE"

        //KHUON_DUC
        const val KHUONDUC_KVC = "KVC"
        const val KHUONDUC_ML = "ML"
        const val KHUONDUC_SKE = "SKE"
        const val KHUONDUC_SWR = "SWR"
        const val KHUONDUC_SUR = "SUR"

        //KHUNG 1
        const val KHUNG1_MU = "MU"
        const val KHUNG1_ML = "ML"
        const val KHUNG1_SWR = "SWR"

	// SYSTEM
        const val SYSTEM = "SYSTEM"
    }
}

class PagingDefault {
    companion object {
        const val PAGE = 0
        const val SIZE = 10
        const val EXPORT_SIZE = 1000000
    }
}

class DateTimeFormat {
    companion object {
        const val dd_MM_yyyy = "dd/MM/yyyy"
        const val yyyyMMdd = "yyyyMMdd"
        const val MM_dd = "MM/dd"
        const val MM_dd_yyyy = "MM/dd/yyyy"
        const val yyyy_MM_dd_HH_mm_ss = "yyyy_MM_dd_HH_mm_ss"
        const val MM_yyyy = "MM/yyyy"
    }
}

class OrderFilterType {
    companion object {
        const val DATE = 0
        const val ORDER = 1
    }
}

class ProcessStatisticCode {
    companion object {
        const val KO = "KO"
        const val IN_MACH = "INMACH"
        const val IN_LO = "INLO"
        const val GHEP_LOP = "GHEPLOP"
        const val TAN = "TAN"
        const val ZEN = "ZEN"
        const val HP_TAN = "HP TAN"
        const val HP_ALL = "HP ALL"
        const val M = "M"
        const val M_ALL = "M ALL"
        const val M_TAN = "M TAN"
        const val GHEPLOP_GIAAPNHIET = "M 熱圧着"
    }
}

class ExcelConstant {
    companion object {
        const val EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        const val FONT_TIMES_NEW_ROMAN = "Times New Roman"
        const val PAGING = 1000000
    }
}

class PlanTitle {
    companion object {
        val DATA = listOf<KeyValueResponse>(
            KeyValueResponse("PLAN", "予定"),
            KeyValueResponse("PLAN_ACCUMULATION", "累計"),
            KeyValueResponse("ACTUAL", "実績"),
            KeyValueResponse("ACTUAL_ACCUMULATION", "累計"),
            KeyValueResponse("DIFFERENCE", "差")
        )

        const val PLAN = "予定"
        const val PLAN_ACCUMULATION = "累計"
        const val ACTUAL = "実績"
        const val ACTUAL_ACCUMULATION = "累計"
        const val DIFFERENCE = "差"

        const val PLAN_KEY = "PLAN"
        const val PLAN_ACCUMULATION_KEY = "PLAN_ACCUMULATION"
        const val ACTUAL_KEY = "ACTUAL"
        const val ACTUAL_ACCUMULATION_KEY = "ACTUAL_ACCUMULATION"
        const val DIFFERENCE_KEY = "DIFFERENCE"
    }
}

class ProcessUnit {
    companion object {
        const val SHEET = "Sheet"
        const val BLOCK = "Block"
    }
}