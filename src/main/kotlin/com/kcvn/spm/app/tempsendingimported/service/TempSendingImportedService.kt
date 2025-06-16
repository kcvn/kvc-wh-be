package com.kcvn.spm.app.tempsendingimported.service

import com.kcvn.spm.app.tempsendingimported.payload.response.TempSendingImportedResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TempSendingImported
import com.kcvn.spm.repository.TempSendingImportedRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal

@Service
@Transactional
class TempSendingImportedService(private val tempSendingImportedRepo: TempSendingImportedRepository) {
    fun getList(): BasePagingResponse<TempSendingImportedResponse> {
        val data = tempSendingImportedRepo.getList()
        val response = data.first.map {
            TempSendingImportedResponse(
                inspectionDate = it.inspectionDate,
                locationCode = it.locationCode,
                poNumber = it.poNumber,
                qty = it.qty?.stripTrailingZeros()?.toPlainString()
            )
        }
        return BasePagingResponse(
            response,
            data.second
        )
    }

    fun importTxtSending(file: MultipartFile): BaseResponse<Int> {
        val dataList = mutableListOf<TempSendingImported>()
        try {
            if (!file.originalFilename.orEmpty().lowercase().endsWith(".txt")) {
                throw BusinessException(CommonUtils.getMessage("validate.invalidFormatTXT"))
            }
            val lines = file.inputStream.bufferedReader().readLines()
            if (lines.isEmpty() || lines.size <= 1) {
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))
            }
            val header = lines[0].split("\t")
            if (header.size != 50) {
                throw BusinessException(CommonUtils.getMessage("validate.invalidFormat"))
            }
            for (i in 1 until lines.size) {
                val columns = lines[i].split("\t")

                if (columns.size < 50) continue

                val data = TempSendingImported(
                    inspectionDate = CommonUtils.parseDateSending(columns[29]),
                    locationCode = columns[20].trim(),
                    poNumber = columns[33].trim(),
                    qty = columns[21].trim().toBigDecimalOrNull() ?: BigDecimal.ZERO
                )
                dataList.add(data)
            }
            // delete record of user import before
            tempSendingImportedRepo.delete(CommonUtils.loggedInUser() ?: "")
            // save temp sending imported date
            val totalRecord = tempSendingImportedRepo.saveAll(dataList)

            return BaseResponse(totalRecord, CommonUtils.getMessage("action.succeeded"))
        } catch (e: Exception) {
            throw e
        }
    }
}