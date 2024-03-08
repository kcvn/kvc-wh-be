package com.kcvn.spm.app.equipmentproductivity.controller

import com.kcvn.spm.app.equipmentproductivity.payload.Model.EquipmentProductivityModel
import com.kcvn.spm.app.equipmentproductivity.payload.Model.ProcessDetailListModel
import com.kcvn.spm.app.equipmentproductivity.payload.Model.ProcessDetailModel
import com.kcvn.spm.app.equipmentproductivity.payload.Response.PagingEquipmentProdResponse
import com.kcvn.spm.app.equipmentproductivity.service.EquipmentProductivityService
import com.kcvn.spm.app.order.payload.response.CalendarValueResponse
import com.kcvn.spm.app.plan.payload.model.ProductPlanModel
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/plan")
class EquipmentProductivityController(
    private val equipmentProductivityService: EquipmentProductivityService)
{

    @GetMapping("/equipment-productivity/get-list")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_ORDER.value) || hasRole('ADMIN')")
    fun getList(
        request: PlanSearchRequest?,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<EquipmentProductivityModel>> {
        val fakeCalendars: MutableList<CalendarValueResponse> = mutableListOf()
        for (i in 1..5) {
            val calendarValue = CalendarValueResponse(
                key = "Key$i",
                value = "Value$i",
                isHoliday = false
            )
            fakeCalendars.add(calendarValue)
        }
        val fakeProcessDetailList: MutableList<ProcessDetailListModel> = mutableListOf()
        for (i in 1..3) {
            val processDetail = ProcessDetailListModel(
                type = "Type$i",
                quantityByCalendars = fakeCalendars
            )
            fakeProcessDetailList.add(processDetail)
        }

        val fakeProcessDetail: MutableList<ProcessDetailModel> = mutableListOf()
        for (i in 1..2) {
            val processDetailModel = ProcessDetailModel(
                name = "Process $i",
                totalProcess = 100 + i,
                processDetailList = fakeProcessDetailList
            )
            fakeProcessDetail.add(processDetailModel)
        }


        // Tạo danh sách giả cho EquipmentProductivityModel
        val fakeEquipmentProductivityModelList: MutableList<EquipmentProductivityModel> = mutableListOf()
        for (i in 1..5) {
            val fakeEquipmentProductivityModel = EquipmentProductivityModel(
                frame1 = "Frame1 $i",
                processName = "ProcessName $i",
                processNameJp = "ProcessNameJP $i",
                processConvertCode = "ConvertCode $i",
                processDetail = fakeProcessDetail
            )
            fakeEquipmentProductivityModelList.add(fakeEquipmentProductivityModel)
        }

        // Tạo dữ liệu giả cho PagingEquipmentProdResponse
        val pagingEquipmentProdResponse = PagingEquipmentProdResponse(columns = fakeCalendars)
        pagingEquipmentProdResponse.data = fakeEquipmentProductivityModelList
        return ResponseEntity<BasePagingResponse<EquipmentProductivityModel>>(pagingEquipmentProdResponse, HttpStatus.OK)
    }



    @PostMapping(value = ["/equipment-productivity/import-excel"], consumes = ["multipart/form-data"])
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun importExcel(
        @RequestPart("file") file: MultipartFile,
        @RequestParam("startDate") startDate: OffsetDateTime,
        @RequestParam("endDate") endDate: OffsetDateTime
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = equipmentProductivityService.importExcel(file)
        return ResponseEntity(data, HttpStatus.OK)
    }
}