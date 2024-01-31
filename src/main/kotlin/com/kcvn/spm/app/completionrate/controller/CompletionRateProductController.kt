package com.kcvn.spm.app.completionrate.controller

import com.kcvn.spm.app.completionrate.service.CompletionRateProductService
import com.kcvn.spm.common.payload.PaginatedResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/api/completion-rate/product")
class CompletionRateProductController(private val completionRateProductService: CompletionRateProductService) {

    @GetMapping("/all")
    fun getAllProducts(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 10, page = 0) pageable: Pageable?
    ): ResponseEntity<*> {
        return try {
            val result =
                completionRateProductService.getPaginatedCompletionRateProduct(search, pageable!!)
            if (result.data.isEmpty())
                ResponseEntity<Any>(HttpStatus.NO_CONTENT)
            else
                ResponseEntity<PaginatedResponse>(result, HttpStatus.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity<Any?>(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }


    @PostMapping(value = ["/import-csv"], consumes = ["multipart/form-data"])
    fun importCsv(@RequestPart("file") multipartFile: MultipartFile): ResponseEntity<List<CsvRecord>> {
        val csvRecords = mutableListOf<CsvRecord>()

        try {
            if (!multipartFile.isEmpty) {
                val bytes: ByteArray = multipartFile.bytes
                val completeData = String(bytes)
                val rows = completeData.split("#")
                val columns = rows[0].split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()


                val check = 1;
            }

            return ResponseEntity<List<CsvRecord>>(csvRecords, HttpStatus.OK)

        } catch (e: Exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null)
        }
    }




    data class CsvRecord(val key: String, val tld: String)


}

