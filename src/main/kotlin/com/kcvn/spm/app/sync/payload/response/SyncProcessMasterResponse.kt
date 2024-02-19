package com.kcvn.spm.app.sync.payload.response

import java.math.BigDecimal
import java.time.OffsetDateTime

data class SyncProcessMasterResponse (
        var OBJECT_ID: Int, //Object ID
        var KAISHA_CD: String, //Mã công ty
        var BUMON_GRP: String, //Phòng ban GRP
        var KOTEI_CD: String, //Mã quy trình
        var KOTEI_MEI: String?, //Tên công đoạn
        var KOTEI_MEI_JPN: String?, //Tên công đoạn (tiếng Nhật)
        var KOTEI_GRP: String?, //Quy trình GRP
        var KOTEI_SHUKEI_GRP: String?, //Tổng hợp quy trình GRP
        var DEFAULT_BUDOMARI: BigDecimal?, //Tỷ lệ thành phẩm tiêu chuẩn được tạo ra trên số lượng nguyên vật liệu
        var HYOJUN_KANSEI_WARIAI: BigDecimal?, //Tỷ lệ hoàn thành tiêu chuẩn
        var HYOJUN_NYURYOKU_TAIKEI: String?, //Hệ thống imput tiêu chuẩn
        var HYOJUN_JISSEKI_NYURYOKU_UMU: String?, //Có/ không nhập thành quả tiêu chuẩn
        var HYOJUN_JIGYOSHO_CD: String?, //Mã văn phòng tiêu chuẩn
        var HYOJUN_TONYU_LOT_SIZE: Int?, //Size lô hàng tiêu chuẩn nhập vào
        var HYOJUN_KANZAN_JOSU: BigDecimal?, //Hệ số chuyển đổi tiêu chuẩn
        var HYOJUN_KANZAN_JOSU_TANI: String?, //Đơn vị hệ số chuyển đổi tiêu chuẩn
        var HYOJUN_JOSU_FURYO_SIYO_KBN: String?, //Phân loại sử dụng chưa tốt hệ số nhân tiêu chuẩn
        var HYOJUN_JOSU_CHOSEI_SIYO_KBN: String?, //Phân loại sử dụng điều chỉnh hệ số nhân tiêu chuẩn
        var HYOJUN_TSUJO_LEAD_TIME_DANDORI: BigDecimal?, //Lead time tiêu chuẩn thông thường(lên kế hoạch trình tự công việc)
        var HYOJUN_TSUJO_LEAD_TIME_SAGYO: BigDecimal?, //Lead time tiêu chuẩn thông thường(thao tác, làm việc)
        var HYOJUN_TSUJO_LEAD_TIME_UNPAN: BigDecimal?, //Lead time tiêu chuẩn thông thường(vận chuyển)
        var HYOJUN_TOKYU_LEAD_TIME_DANDORI: BigDecimal?, //Lead time tiêu chuẩn cấp tốc (lên kế hoạch trình tự công việc)
        var HYOJUN_TOKYU_LEAD_TIME_SAGYO: BigDecimal?, //Lead time tiêu chuẩn cấp tốc (thao tác, làm việc)
        var HYOJUN_TOKYU_LEAD_TIME_UNPAN: BigDecimal?, //Lead time tiêu chuẩn cấp tốc (vận chuyển)
        var HYOJUN_SETTEI_BUDOMARI: BigDecimal?, //Cài đặt tiêu chuẩn tỷ lệ thành phẩm tạo ra trên tổng số nguyên liệu
        var HYOJUN_YAMADUMI_KBN: String?, //Phân loại tồn ứ tiêu chuẩn
        var HYOJUN_SHIKAKARI_TORISU: Int?, //Số lượng tấm tiêu chuẩn in ra được từ 1 khuôn đang làm dở
        var HYOJUN_KOTEI_INJI_KBN: String?, //Phân loại quy trình in tiêu chuẩn
        var HYOJUN_SHIKAKARI_KOTEI_IDO_KBN: String?, //Phân loại quy trình di động tiêu chuẩn đang tiến hành  dở  dang
        var HYOJUN_KOTEI_SYUBETSU: String?, //Phân loại quy trình tiêu chuẩn
        var TNORSHKBN_CD: String?, //Mã kiểm kê
        var ZISSEKI_SHUKEI_KBN: String?, //Phân loại tổng hợp thành quả thực tế
        var LEAD_TIME_MIN: BigDecimal?, //Lead time (nhỏ nhất)
        var WS_TYPE: String?, //Loại WS
        var WS_CD: String?, //Mã WS
        var PERSONS: Int?, //Người , person
        var WORK_TIME: String?, //Thời gian làm việc
        var GRP_SU: Int?, //Số lượng GRP
        var UNIT: String?, //Unit
        var NECK_FLG: String?, //Neck flag
        var KEIKAKU_KOTEI_CD: String?, //Mã quy trình đã lên kế hoạch
        var JISSEKI_GRP: String?, //GRP thành quả thực tế
        var GENKA_GRP: String?, //GRP giá vốn
        var TOROKUSHA: String?, //Người đăng ký
        var TOROKU_DATE: OffsetDateTime?, //Ngày đăng ký
        var KOSHINSHA: String?, //Người update
        var KOSHIN_DATE: OffsetDateTime?, //Ngày update
        var HAITA_FLG: Int?, //Flag độc quyền/ Flag ngoại lệ
        var SAGYOBA_CD: String?, //Mã nơi làm việc
)