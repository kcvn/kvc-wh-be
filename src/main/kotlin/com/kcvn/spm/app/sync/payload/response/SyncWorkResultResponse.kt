package com.kcvn.spm.app.sync.payload.response

import java.math.BigDecimal
import java.time.OffsetDateTime

data class SyncWorkResultResponse(
    var OBJECT_ID: Int, // Object ID
    var ANDROID_ID: String?, // ANDROID_ID
    var BIKO: String?, // Ghi chú
    var BUMON_GRP: String, // Phòng ban GRP
    var BUSHO_CD: String?, // Mã phòng ban
    var BUSHO_MEI: String?, // Tên phòng ban
    var SEIZO_ORDER_NO: String?, // Số đơn đặt hàng chế tạo
    var KANRI_NO: String, // Số quản lý
    var KYAKUSAKI_CD: String?, // Mã khách hàng
    var HINMOKU_CD: String?, // Mã sản phẩm
    var KC_HINMEI: String?, // Tên sản phẩm
    var SO_NO: String?, // Số lớp
    var KOTEI_CD: String, // Mã công đoạn
    var KOTEI_GRP: String?, // Mã nhóm công đoạn
    var KOTEI_MEI: String?, // Tên công đoạn
    var KOTEI_SHUBETSU: String?, // Phân loại công đoạn
    var SHIGEN_CD: String?, // Mã máy sử dụng của công đoạn tương ứng
    var SHIGEN_MEI: String?, // Tên máy
    var TAPE_LOT_NO: String?, // Số lô tấm nguyên liệu/Số lô Sứ
    var CHAKKAN_KBN: String?, // Phân loại hoàn thành
    var DAIHYO_SEIDEN_NO: String?, // Mã đại diện seden ???
    var EDABAN: String, // Mã số phiên bản
    var FURIMUKE_KBN: String?, // Phân loại furimukou ???
    var HAITA_FLG: Int?, // Flag ngoại lệ/ Flag độc quyền
    var HASU: Int?, // Phần lẻ thừa ra (ví dụ số 1015 thừa 15)
    var HOKO: String?, // Phương hướng
    var HON_SU: Int?, // Số lượng (thanh/cây)
    var IDO_LOT_JOTAI: String?, // Trạng thái của lô hàng được chuyển đi
    var JISSEKI_CD: String?, // Mã thành quả thực tế
    var JISSEKI_KANRI_BUMON_GRP: String?, // Phòng ban GRP quản lý kết quả thực tế
    var JISSEKI_KEIJO_DATE: OffsetDateTime?, // Ngày thống kê
    var JISSEKI_NYURYOKU_KBN: String?, // Phân loại nhập kết quả thực tế
    var JISSEKI_SHIKIBETSU: String?, // Phân loại thành quả thực tế
    var JOKEN_CHECK_KOMOKU_1: String?, // Mục check dự án 1
    var JOKEN_CHECK_KOMOKU_2: String?, // Mục check dự án 2
    var JOKEN_CHECK_KOMOKU_3: String?, // Mục check dự án 3
    var KAISHA_CD: String, // Mã công ty
    var KAISHI_SAGYOSHA: String?, // Người bắt đầu công việc
    var KANRISHA_CD: String?, // Mã người quản lý
    var KANZAN_JOSU: BigDecimal?, // Hệ số quy đổi
    var KAN_SU: Int?, // Số lượng lon
    var FURIMUKE_SU: Int?, // Số lượng furimukou
    var FURYO_SU: Int?, // Số lượng sản phẩm không đạt
    var HIFURIMUKE_SU: Int?, // Số lượng hifurimukou ???
    var HORYU_SU: Int?, // Số lượng bảo lưu
    var RYOHIN_SU: Int?, // Số sản phẩm chất lượng tốt
    var SAISEI_SU: Int?, // Số tái tạo
    var SHORI_SU: Int?, // Số lượng xử lý
    var TYOSEI_SU: Int?, // Số lượng điều chỉnh
    var KIBAN_FURIMUKE_SU: Int?, // Số lượng furimukou bảng mạch điện tử ???
    var KIBAN_FURYO_SU: Int?, // Số tấm sản phẩm NG
    var KIBAN_HIFURIMUKE_SU: Int?, // Số lượng hifurimukou bảng mạch điện tử ???
    var KIBAN_HORYU_SU: Int?, // Số lượng bảng mạch điện tử bảo lưu
    var KIBAN_RYOHIN_SU: Int?, // Số tấm sản phẩm tốt
    var KIBAN_SAISEI_SU: Int?, // Số lượng bảng mạch tái tạo
    var KIBAN_SHORI_SU: Int?, // Số tấm sản phẩm xử lý
    var KIBAN_TYOSEI_SU: Int?, // Số lượng bảng mạch điều chỉnh
    var SHEET_FURIMUKE_SU: Int?, // Số lượng miếng furimukou ???
    var SHEET_FURYO_SU: Int?, // Số sheet xử lý NG
    var SHEET_HIFURIMUKE_SU: Int?, // Số lượng miếng hifurimukou ???
    var SHEET_HORYU_SU: Int?, // Số lượng miếng bảo lưu
    var SHEET_RYOHIN_SU: Int?, // Số sheet xử lý tốt
    var SHEET_SAISEI_SU: Int?, // Số lượng miếng tái tạo
    var SHEET_SHORI_SU: Int?, // Số sheet xử lý
    var SHEET_TYOSEI_SU: Int?, // Số lượng miếng điều chỉnh
    var KINMUTAI_SHIFT: String?, // Ca làm việc
    var NYURYOKU_TANI: String?, // Đơn vị input
    var SAGYO_CD1: String?, // Mã công việc 1
    var SAGYO_CD2: String?, // Mã công việc 2
    var SAGYO_CD3: String?, // Mã công việc 3
    var SAGYO_DATE: OffsetDateTime?, // Ngày làm việc
    var SAGYO_TIME: OffsetDateTime?, // Giờ làm việc
    var SAGYO_KAISHI_DATE: OffsetDateTime?, // Ngày bắt đầu làm việc
    var SAGYO_KAISHI_TIME: OffsetDateTime?, // Giờ bắt đầu làm việc
    var SAGYO_SHURYO_TIME: OffsetDateTime?, // Giờ kết thúc công việc
    var SAGYO_SYURYO_DATE: OffsetDateTime?, // Ngày kết thúc công việc
    var SAGYOBA_CD: String?, // Mã nơi làm việc
    var SAGYOBA_MEI: String?, // Tên nơi làm việc
    var SAGYO_JISSHI_HAN: String?, // Đội thực hiện công việc
    var SAGYO_MEMO: String?, // Memo công việc
    var SAGYOKBN_CD: String?, // Mã phân loại công việc
    var SAGYOSHA_CD: String?, // Mã người thao tác/ người thực hiện công việc
    var SAISEI_CD: String?, // Mã tái tạo
    var SAISEI_KBN: String?, // Phân loại tái tạo
    var SAISEI_MEI: String?, // Tên tái tạo
    var SAISEISAKI_KOTEI_CD: String?, // Mã quy trình đích đến tái tạo
    var SAISEISAKI_KOTEI_MEI: String?, // Tên quy trình đích đến tái tạo
    var SEIZOSAKI_CD: String?, // Mã nơi sản xuất
    var SHEET_FLAG: String?, // Sheet flag
    var SHIKAKARI_NYURYOKU_SU: Int?, // Số lượng nhập vào dở dang
    var SHOCHISHIJI_NO: String?, // Mã số chỉ thị khắc phục
    var SHUKKA_LOT_NO: String?, // Mã số lô hàng giao đi
    var SHURYO_GAPPI: OffsetDateTime?, // Ngày tháng kết thúc
    var SO_KOSU: Int?, // Tổng số
    var TANKA: BigDecimal?, // Giá
    var TOKKI_JIKOU: String?, // Mục đặc biệt
    var TOROKU_DATE: OffsetDateTime, // Ngày tạo
    var TOROKUSHA: String?, // Người tạo
    var KOSHIN_DATE: OffsetDateTime?, // Ngày cập nhật
    var KOSHINSHA: String? // Người cập nhật
)