package com.kcvn.spm.sample.service

import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.productprocess.payload.request.ProductProcessSearchRequest
import com.kcvn.spm.app.productprocess.payload.request.UpdateProductProcessDetailRequest
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.repository.ProductProcessRepository
import com.kcvn.spm.repository.ProductRepository
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream

@Service
@Transactional
class ProductProcessService(
    private val productProcessRep : ProductProcessRepository
) {
    fun  getPaginatedProductProcess(search: String?, hasProcessConvertCode: Boolean, pageable: Pageable): BasePagingResponse<ProductProcessResponse>
    {
        val result = productProcessRep.findByKeywordPaginated(search,hasProcessConvertCode,pageable);
        val response = BasePagingResponse<ProductProcessResponse>();
            response.data = result.first.map { productProcess ->
                ProductProcessResponse(
                    processId = productProcess.processId,
                    processName = productProcess.processName,
                    processNameJp = productProcess.processNameJp,
                    processConvertCode = productProcess.processConvertCode,
                    processStatisticCode = productProcess.processStatisticCode,
                    processInventoryCode = productProcess.processInventoryCode,
                    productName = productProcess.productName,
                    layerCode = productProcess.layerCode,
                    processCode = productProcess.processCode,
                    productId = productProcess.productId,
                );
            }
            response.total = result.second;

        return response;
    }

    fun getProductProcessDetail(nameProduct: String?) : List<ProductProcessResponse?>?{
        return productProcessRep.getByProductProcessDetail(nameProduct)
    }

    fun updateProductProcessDetail(request: UpdateProductProcessDetailRequest) : List<ProductProcess?> {
        val dataResult: MutableList<ProductProcess?> = mutableListOf()
        for (item in request.listProcess!!){
            val productProcess = productProcessRep.getByProductProcessDetailById(item.id)
                ?: throw BusinessException(CommonUtils.getMessage("productProcess.notFound"))
            if (item.processInventoryCode != null){
               val productProcessAfter =  request.listProcess!!.find {  it.idx == item.idx + 1 }
                if(productProcessAfter == null || (productProcessAfter.processCode != null && productProcessAfter.processCode != item.processInventoryCode) )
                {
                    throw BusinessException(CommonUtils.getMessage("processCode.notMap.processInventoryCode"))
                }
            }
            productProcess.processConvertCode = item.processConvertCode;
            productProcess.processStatisticCode = item.processStatisticCode;
            productProcess.processInventoryCode = item.processInventoryCode;

            val data = productProcessRep.updateProductDetail(productProcess);
            dataResult.add(data)
        }
        return  dataResult
    }

    fun exportExcel(search: String?, hasProcessConvertCode: Boolean, pageable: Pageable) : BaseResponse<FileContentModel> {
        val products = productProcessRep.findByKeywordPaginated(search,hasProcessConvertCode, pageable)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportProductProcessTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (products.first.isNotEmpty()) {
            val style: CellStyle = workbook.createCellStyle()
            style.borderBottom = BorderStyle.THIN
            style.borderTop = BorderStyle.THIN
            style.borderRight = BorderStyle.THIN
            style.borderLeft = BorderStyle.THIN
            style.wrapText = true

            val font: Font = workbook.createFont()
            font.fontName = "Times New Roman"
            font.fontHeightInPoints = 12.toShort()
            style.setFont(font)


            var rowNumber = 2
            for (item in products.first) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                dataRow.createCell(0).setCellValue(item.productName)
                dataRow.getCell(0).cellStyle = style

                dataRow.createCell(1).setCellValue(item.layerCode)
                dataRow.getCell(1).cellStyle = style

                dataRow.createCell(2).setCellValue(item.processCode)
                dataRow.getCell(2).cellStyle = style

                dataRow.createCell(3).setCellValue(item.processName)
                dataRow.getCell(3).cellStyle = style

                dataRow.createCell(4).setCellValue(item.processNameJp)
                dataRow.getCell(4).cellStyle = style

                dataRow.createCell(5).setCellValue(item.processConvertCode)
                dataRow.getCell(5).cellStyle = style

                dataRow.createCell(6).setCellValue(item.processInventoryCode)
                dataRow.getCell(6).cellStyle = style

                dataRow.createCell(7).setCellValue(item.processStatisticCode)
                dataRow.getCell(7).cellStyle = style
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = "Danh_sach_cong_doan.xlsx",
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }
}