package com.kcvn.spm.app.inventoryproduct.service

import com.kcvn.spm.app.inventoryproduct.payload.request.InventorySemiProductSearchRequest
import com.kcvn.spm.app.inventoryproduct.payload.response.InventorySemiProductResponse
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.NumberHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.InventorySemiProduct
import com.kcvn.spm.repository.CompletionRateProductRepository
import com.kcvn.spm.repository.InventorySemiProductRepository
import com.kcvn.spm.repository.ProductRepository
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class InventorySemiProductService(
    private val inventorySemiProductRep: InventorySemiProductRepository,
    private val productRep: ProductRepository,
    private val completionRateProductRep: CompletionRateProductRepository
) {

    fun getList(request: InventorySemiProductSearchRequest, pageable: Pageable): BasePagingResponse<InventorySemiProductResponse> {
        val inventories = inventorySemiProductRep.getList(request, pageable)
        val data = mappingInventorySemiProduct(inventories.first)
        return BasePagingResponse(
            data,
            inventories.second
        )
    }

    fun exportExcel(request: InventorySemiProductSearchRequest, pageable: Pageable): BaseResponse<FileContentModel> {
        val inventories = inventorySemiProductRep.getList(request, pageable, true)
        if (inventories.first.isEmpty()) throw BusinessException(CommonUtils.getMessage("excel.export.noData"))
        val data = mappingInventorySemiProduct(inventories.first)

        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ExportInventorySemiProductTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        val style = ExcelHelper.getCellStyleCommon(workbook)
        style.alignment = HorizontalAlignment.CENTER

        val numberStyle = workbook.createCellStyle()
        numberStyle.cloneStyleFrom(style)
        numberStyle.alignment = HorizontalAlignment.RIGHT

        var rowNumber = 1

        for (item in data) {
            val dataRow: Row = ExcelHelper.createRow(sheet, rowNumber)
            val invDate = DateTimeHelper.toString(
                DateTimeHelper.toTimeZone7(item.inventoryDate)!!,
                DateTimeFormat.dd_MM_yyyy
            )
            ExcelHelper.setCellValue(dataRow, 0, style, invDate)
            ExcelHelper.setCellValue(dataRow, 1, style, item.productName)
            ExcelHelper.setCellValue(dataRow, 2, style, item.tapeLotNo)
            ExcelHelper.setCellValue(dataRow, 3, style, if (item.completionRate != null) "${item.completionRate}%" else "")
            ExcelHelper.setCellValue(dataRow, 4, style, NumberHelper.formatNumber(item.blockSh))
            ExcelHelper.setCellValue(dataRow, 5, numberStyle, NumberHelper.formatNumber(item.setQuantity))
            ExcelHelper.setCellValue(dataRow, 6, numberStyle, NumberHelper.formatNumber(item.blockQuantity))
            ExcelHelper.setCellValue(dataRow, 7, numberStyle, NumberHelper.formatNumber(item.sumBlockQuantity))
            ExcelHelper.setCellValue(dataRow, 8, numberStyle, NumberHelper.formatNumber(item.ngBlockQuantity))
            ExcelHelper.setCellValue(dataRow, 9, numberStyle, NumberHelper.formatNumber(item.successBlockQuantity))
            rowNumber++
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportListInventorySemiProduct", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    private fun mappingInventorySemiProduct(inventories: List<InventorySemiProduct>): List<InventorySemiProductResponse> {
        if (inventories.isEmpty()) return listOf()

        val productNames = inventories.mapNotNull { it.productName }.distinct()
        val products = productRep.getByName(productNames)
        val completionRates = completionRateProductRep.getEffectiveByProduct(productNames)

        val response = inventories.map { item ->
            val product = products.find { it.name == item.productName }
            val completionRate = completionRates?.find { it.productName == item.productName }
            InventorySemiProductResponse(
                inventoryDate = item.inventoryDate,
                productName = item.productName,
                tapeLotNo = item.tapeLotNo,
                completionRate = completionRate?.rate,
                blockSh = product?.shBlock,
                setQuantity = item.setQuantity,
                blockQuantity = item.blockQuantity,
                sumBlockQuantity = item.sumBlockQuantity,
                ngBlockQuantity = item.ngBlockQuantity,
                successBlockQuantity = item.successBlockQuantity
            )
        }

        return response
    }

    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportInventorySemiProductTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.importInventoryTemplate"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return BaseResponse(response)
    }

    fun checkInventory(date: OffsetDateTime) : BaseResponse<Boolean> {
        val isNoInventory = inventorySemiProductRep.isNoInventoryByDate(date)
        val formattedDate = DateTimeHelper.convertOffSetDateTimeUtc7ToString(date)

        return BaseResponse(
            isNoInventory,
            if (isNoInventory) null else CommonUtils.getMessage("check.inventoryDateProduct", arrayOf(formattedDate.toString()))
        )
    }
}

