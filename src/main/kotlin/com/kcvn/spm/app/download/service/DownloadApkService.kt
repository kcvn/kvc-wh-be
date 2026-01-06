package com.kcvn.spm.app.download.service

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryRequest
import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryResponse
import com.kcvn.spm.app.download.payload.ApkVersionCheckingRequest
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CheckingHistory
import com.kcvn.spm.repository.CheckingHistoryRepository
import com.kcvn.spm.repository.DownloadApkRepository
import org.springframework.core.io.FileSystemResource
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.LocalDate

@Service
@Transactional
class DownloadApkService(
    private val downloadApkRepository: DownloadApkRepository
) {
    fun downloadApk(version: String, deviceName: String): ResponseEntity<FileSystemResource> {
        val data = ApkVersionCheckingRequest(currentVersion = version, deviceName = deviceName)
        val apk = downloadApkRepository.findOneRecordByDeviceName(data)

        if (apk?.newVersion != data.currentVersion) {
            //val fileTemplate = File("${System.getProperty("user.dir")}/log/${apk?.newVersion}.apk")
            val apkFile =
                File("${System.getProperty("user.dir")}/log/${apk?.newVersion}.apk")

            if (!apkFile.exists()) {
                println("File có đọc được không? ${apkFile.canRead()}")
                return ResponseEntity.notFound().build()
            }

            val resource = FileSystemResource(apkFile)
            val headers = HttpHeaders()
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"app-release.apk\"")

            return ResponseEntity.ok()
                .headers(headers)
                .contentLength(apkFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource)
        } else if (apk.newVersion != apk.currentVersion) {
            downloadApkRepository.updateVersionData(data)
            return ResponseEntity.notFound().build()
        } else return ResponseEntity.notFound().build()
    }
}