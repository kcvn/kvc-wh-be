package com.kcvn.spm.app.sync.payload.response

import java.math.BigDecimal
import java.time.OffsetDateTime

data class SyncProcessProcedureStructureResponse(
    var OBJECT_ID: Int, //Object ID
    var KAISHA_CD: String, //Mã công ty
    var BUMON_GRP: String, //Phòng ban GRP
    var SHOCHISHIJI_NO: String, //Mã số chỉ thị khắc phục
    var KOTEI_TEJUN_CD: String, //Mã sản phẩm
    var SO_NO: String, //Mã lớp
    var KOTEI_CD: String, //Mã công đoạn
    var KOTEI_NO: Int?, //Thứ tự thực hiện của công đoạn trong từng lớp
    var KOTEI_TEJUN_REV: Int, //Rev trình tự công đoạn
    var KAKOU_TEJUN_REV: String?, //Rev trình tự gia công
    var KANSEI_WARIAI: BigDecimal?, //Tỷ lệ hoàn thành
    var SETTEI_BUDOMARI: BigDecimal?, //Tỷ lệ thành phẩm tạo ra được cài đặt (so với tổng nguyên liệu)
    var KOTEI_INJI_KBN: String?, //Phân loại quy trình in; "= 0 : SO_NO đều có giá trị = 00; = 1: SO_NO có đủ giá trị 00/01/02...."
    var KOTEI_SHUBETSU: String?, //Phân loại công đoạn; "0: Các công đoạn còn lại; 1:Ghép lớp 01 và 02/ Ghép lớp 02 và 03/ Ghép lớp 03 và lớp 04; 2: Ghép lớp M (Tất cả lớp)"
    var KOTEI_HYOJI_JUN: Int?, //Trình tự hiển thị các công đoạn
    var NYURYOKU_TAIKEI: String?, //Hệ thống đầu vào
    var JISSEKI_NYURYOKU_UMU: String?, //Có /Không nhập thành quả thực tế
    var TONYU_LOT_SIZE: Int?, //Size lô hàng nhập vào
    var KANZAN_JOSU: BigDecimal?, //Hệ số chuyển đổi
    var KANZAN_JOSU_TANI: String?, //Đơn vị hệ số chuyển đổi
    var KANZAN_JOSU_FURYO_SHIYO_KBN: String?, //Phân loại cách sử dụng hệ số chuyển đổi chưa tốt
    var KANZAN_JOSU_CHOSEI_SHIYO_KBN: String?, //Phân loại sử dụng điều chỉnh hệ số chuyển đổi
    var TSUJO_LEAD_TIME_DANDORI: BigDecimal?, //Lead time thông thường (lên kế hoạch trình tự công việc)
    var TSUJO_LEAD_TIME_SAGYO: BigDecimal?, //Lead time thông thường (thao tác, làm việc)
    var TSUJO_LEAD_TIME_UNPAN: BigDecimal?, //Lead time thông thường (vận chuyển)
    var TOKKYU_LEAD_TIME_DANDORI: BigDecimal?, //Lead time cấp tốc (lên kế hoạch trình tự công việc)
    var TOKKYU_LEAD_TIME_SAGYO: BigDecimal?, //Lead time cấp tốc (thao tác, làm việc)
    var TOKKYU_LEAD_TIME_UNPAN: BigDecimal?, //Lead time cấp tốc (vận chuyển)
    var YAMADUMI_KBN: String?, //Phân loại tồn đọng, chất đống
    var SHIKAKARI_TORISU: Int?, //Số lượng thành phẩm dở dang được in từ 1 khuôn
    var SHIKAKARI_KOTEI_IDO_KBN: String?, //Phân loại quy trình di động tiêu chuẩn đang tiến hành
    var TANAOROSI_KENSA_KOTEI_GRP: String?, //Quy trình kiểm tra, kiểm kê GRP
    var TOROKUSHA: String?, //Người đăng ký
    var TOROKU_DATE: OffsetDateTime?, //Ngày đăng ký
    var KOSHINSHA: String?, //Người update
    var KOSHIN_DATE: OffsetDateTime?, //Ngày update
    var HAITA_FLG: Int?, //Flag  độc quyền/ Flag ngoại lệ
    var SAGYOBA_CD: String? //Mã nơi làm việc
)
