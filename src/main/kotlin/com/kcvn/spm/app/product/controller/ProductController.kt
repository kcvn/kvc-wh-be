package com.kcvn.spm.app.product.controller

import com.kcvn.spm.app.auth.payload.response.UserResponse
import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.app.product.payload.response.ProductAndProcessResponse
import com.kcvn.spm.app.product.payload.response.ProductResponse
import com.kcvn.spm.app.product.service.ProductService
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.FileResponse
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import com.kcvn.spm.sample.service.ProductProcessService
import com.opencsv.CSVReaderBuilder
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.http.MediaType
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.file.Files
import java.util.Base64
import kotlin.io.path.fileSize

@RestController
@RequestMapping("/api/product")
class ProductController(
    private val productService: ProductService,
    private val productProcessService: ProductProcessService
) {
    @GetMapping("/get-list")
    fun getList(
        request: ProductSearchRequest?,
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<PagingProductResponse> {
        return try {
            val data = productService.getListProduct(request, pageable)
            ResponseEntity<PagingProductResponse>(data, HttpStatus.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity<PagingProductResponse>(null, HttpStatus.OK)
        }
    }

    @PostMapping(value = ["/import-csv"], consumes = ["multipart/form-data"])
    fun importCsv(@RequestPart("file") file: MultipartFile): ResponseEntity<*> {
        try {
            val data = productService.importCsvProduct(file)
            return ResponseEntity<MessageResponse>(
                MessageResponse(data),
                HttpStatus.OK
            )
        }
        catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("import.failed")),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

//    @GetMapping("/download-template-csv")
//    fun downloadTemplateCsv(response: HttpServletResponse): StreamingResponseBody {
//        try {
//            var resource = ClassPathResource("media/template/ImportProductTemplate.csv")
//            if (resource.exists()) throw BusinessException("Đường dẫn file không tồn tại")
//            println(resource.file.absolutePath)
//            val file = FileSystemResource(resource.file.absolutePath)
//
//            val streamingResponseBody = StreamingResponseBody { outputStream ->
//                file.inputStream.use { input ->
//                    input.copyTo(outputStream)
//                }
//            }
//
//            response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
//            response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data.csv")
//            response.contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE
//            response.setContentLengthLong(1048576)
//            return streamingResponseBody
//        }
//        catch (e: Exception) {
//            e.printStackTrace()
//            throw BusinessException(e.localizedMessage)
//        }
//    }

    @GetMapping("/download-template-csv")
    fun downloadTemplateCsv(response: HttpServletResponse) : ResponseEntity<FileResponse> {

        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportProductTemplate.csv"
        val file = File(filePath)

        val fileContent = Files.readAllBytes(file.toPath())

        val response = FileResponse(
            fileName = "ImportProductTemplate.csv",
            contentType = "text/csv",
            content = fileContent
        )
        return ResponseEntity(response, HttpStatus.OK)
    }

    fun convertFileInputStreamToByteArray(fileInputStream: FileInputStream): ByteArrayOutputStream {
        val byteStream = ByteArrayOutputStream()

        try {
            // Đọc dữ liệu từ FileInputStream và ghi vào ByteArrayOutputStream
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                byteStream.write(buffer, 0, bytesRead)
            }
        } finally {
            // Đóng FileInputStream
            fileInputStream.close()
        }

        // Chuyển đổi ByteArrayOutputStream thành ByteArray
        return byteStream
    }

    @GetMapping("/export-excel")
    fun exportExcel(request: ProductSearchRequest?): StreamingResponseBody? {
        return null
    }

    @PostMapping("/sync")
    fun sync(): ResponseEntity<*> {
        return ResponseEntity<Any?>(null, HttpStatus.OK)
    }

    @GetMapping("/get-product-detail/{id}")
    fun getProductDetail(@PathVariable("id") id: String): ResponseEntity<ProductAndProcessResponse> {
        val dataProduct = productService.getProductDetail(id)
        val nameProduct = dataProduct?.name
        val dataProcess = productProcessService.getProductProcessDetail(nameProduct)
        val resultData = ProductAndProcessResponse(
            detail = dataProduct,
            listProcess = dataProcess
        )
        return if (resultData.detail != null ) {
            ResponseEntity<ProductAndProcessResponse>(resultData, HttpStatus.OK)
        } else {
            ResponseEntity<ProductAndProcessResponse>(HttpStatus.NOT_FOUND)
        }
    }
}