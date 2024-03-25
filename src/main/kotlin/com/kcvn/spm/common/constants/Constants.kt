package com.kcvn.spm.common.constants

import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.KeyValueResponse

class Constants {
    companion object {
        const val SYSTEM = "SYSTEM"
    }
}

class SyncType {
    companion object {
        const val PROCESS_PROCEDURE_STRUCTURE = "PROCESS_PROCEDURE_STRUCTURE"
        const val PROCESS_MASTER = "PROCESS_MASTER"
        const val WORK_RESULT = "WORK_RESULT"
    }
}

class ClaimType {
    companion object {
        const val USER_ID = "UserId"
        const val POSITION = "Position"
    }
}

class MasterDataType {
    companion object {
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
    }
}

class Frame1 {
    companion object {
        const val MU = "MU"
        const val ML = "ML"
        const val SWR = "SWR"
    }
}


class ProcessCode{
    companion object {
        const val KTTN = "217020"
    }
}

class Mold {
    companion object {
        const val KVC = "KVC"
        const val ML = "ML"
        const val SKE = "SKE"
        const val SWR = "SWR"
        const val SUR = "SUR"

        fun DATA_BY_FRAME1(frame1: String?) : List<String> {
            return when (frame1) {
                Frame1.ML -> listOf(this.ML)
                Frame1.MU -> listOf(this.KVC, this.SKE)
                Frame1.SWR -> listOf(this.SUR, this.SWR)
                else -> listOf(this.ML, this.KVC, this.SKE, this.SUR, this.SWR)
            }
        }

    }
}

class PagingDefault {
    companion object {
        const val PAGE = 0
        const val SIZE = 10
        const val EXPORT_SIZE = 1000000
    }
}

class ExcelConstant {
    companion object {
        const val EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        const val FONT_TIMES_NEW_ROMAN = "Times New Roman"
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

class OrderVersion {
    companion object {
        const val LATEST = "LATEST"

        val DATA = listOf<DropdownResponse>(
            DropdownResponse(LATEST, "Mới nhất"),
            DropdownResponse("0", "V00"),
            DropdownResponse("1", "V01"),
            DropdownResponse("2", "V02")
        )
    }
}

class ExternalReportDetailType {
    companion object {
        const val ORDER_QUANTITY = "納予定(BLOCK)"
        const val ACCUMULATED_ORDER_QUANTITY = "納累計 ①(BLOCK)"
        const val PRODUCTION_RESULT = "納実績(BLOCK)"
        const val ACCUMULATED_PRODUCTION_RESULT = "納累計(BLOCK)"
        const val DIFFERENCE_1 = "納差(BLOCK)"
        const val DIFFERENCE_2 = "予定と月初在庫の差(BLOCK)"
        const val PLANNED_TAPE_SET = "TAPE 予定(SET)"
        const val ACCUMULATED_PLANNED_TAPE_SET = "TAPE 累計 ①(SET)"
        const val PLANNED_TAPE_BLOCK = "TAPE 予定(BLOCK)"
        const val ACCUMULATED_PLANNED_TAPE_BLOCK = "TAPE 累計 ①(BLOCK)"
        const val TAPE_REQUIRED_FOR_PRODUCTION_BLOCK = "投入必要テープ数(BLOCK)"
        const val TAPE_DIFFERENCE_BLOCK = "TAPE 差(BLOCK)"

    }
}

class ExternalReportShippingType {
    companion object {
        const val PRODUCTION_PLAN_TITLE = "生産計画　Ke hoach san xuat"
        const val QUANTITY_REMAINING_TITLE = "出荷残数 Sluong xuat hang còn"
        const val TAPE_INVENTORY_TITLE = "5月27日テープ在庫Tồn kho tape ( set )"
        const val EXPIRED_TAPE  = "10月の期限切れテープTape hết hạn (set )"



    }
}



class ProcessStatisticCode {
    companion object {
        const val KO = "KO"
        const val T = "T"
        const val IN_MACH = "INMACH"
        const val IN_LO = "INLO"
        const val GHEP_LOP = "GHEPLOP"
        const val GHEP_LOP_SUM = "GHEPLOPSUM"
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

class ProcessConvertCode {
    companion object {
        const val W = "W"
        const val U = "U"
        const val HP_ALL = "HP ALL"
        const val HP = "HP"
        const val T = "T"
        const val TH = "TH"
        const val M_ALL = "M ALL"
        const val M_ANY = "M*"
        const val M = "M"
        const val TAN = "TAN"
        const val ZEN = "ZEN"
        const val K = "K"
        const val SHN = "SHN"
        const val SNAP = "SNAP"
        const val TK = "TK"
        const val INS = "INS"
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

class ProcessPlan {
    companion object {
        const val PROCESS = "Process"
        const val MACHINE = "Cap(1machine)"
        const val MACHINENUMBER = "Số máy sử dụng"
        const val PROCESS_DUC_LO ="Đục lỗ"
        const val PROCESS_DUC_LO_M = "T/H"
    }
}

class ProcessUnit {
    companion object {
        const val SHEET = "Sheet"
        const val BLOCK = "Block"
        const val SET = "Set"
    }
}

class PlanProcessSummary {
    companion object {
        val DATA = listOf<String>(
            ProcessConvertCode.W,
            ProcessConvertCode.U,
            ProcessConvertCode.T,
            ProcessConvertCode.TH,
            ProcessConvertCode.TAN,
            ProcessConvertCode.ZEN,
            ProcessConvertCode.HP,
            ProcessConvertCode.HP_ALL,
            ProcessConvertCode.M_ALL,
            ProcessConvertCode.M,
            ProcessConvertCode.K,
            ProcessConvertCode.SHN,
            ProcessConvertCode.SNAP,
            ProcessConvertCode.TK,
            ProcessConvertCode.INS
        )
    }
}

class PlanStyleKey {
    companion object {
        const val PLAN_PRODUCT_TITLE = "PLAN_PRODUCT_TITLE"
        const val PLAN_PRODUCT_VALUE = "PLAN_PRODUCT_VALUE"
        const val PROCESS_PRIMARY = "PROCESS_PRIMARY"
        const val PROCESS_CHILDREN = "PROCESS_CHILDREN"
        const val PROCESS_CHILDREN_END_ROW = "PROCESS_CHILDREN_END_ROW"
        const val PLAN_DETAIL = "PLAN_DETAIL"
        const val PLAN_SUMMARY_FIRST_ROW = "PLAN_SUMMARY_FIRST_ROW"
        const val PLAN_SUMMARY_END_ROW = "PLAN_SUMMARY_END_ROW"
        const val PLAN_SUMMARY_MIDDLE_ROW = "PLAN_SUMMARY_MIDDLE_ROW"
        const val PLAN_SUMMARY_DETAIL = "PLAN_SUMMARY_DETAIL"
    }
}