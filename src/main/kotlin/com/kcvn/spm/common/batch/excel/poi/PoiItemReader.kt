package com.kcvn.spm.common.batch.excel.poi

import com.kcvn.spm.common.batch.excel.AbstractExcelItemReader
import com.kcvn.spm.common.batch.excel.WorkSheet
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.core.io.Resource
import java.io.InputStream

/**
 * [org.springframework.batch.item.ItemReader] implementation which uses apache POI
 * to read an Excel file. It will read the file sheet for sheet and row for row. It is
 * based on the [org.springframework.batch.item.file.FlatFileItemReader]
 *
 * This class is **not** thread-safe.
 *
 * @param <T> the type
 */
open class PoiItemReader<T> : AbstractExcelItemReader<T>() {
    private var workbook: Workbook? = null
    private var inputStream: InputStream? = null

    override fun getSheet(sheet: Int): WorkSheet {
        return PoiWorkSheet(workbook!!.getSheetAt(sheet), getDataFormatter()!!)
    }

    override fun getNumberOfSheets(): Int = workbook!!.numberOfSheets

    @Throws(Exception::class)
    override fun doClose() {
        super.doClose()
        if (inputStream != null) {
            inputStream!!.close()
            inputStream = null
        }
        if (workbook != null) {
            workbook!!.close()
            workbook = null
        }
    }

    /**
     * Open the underlying file using the `WorkbookFactory`. Prefer `File`
     * based access over an `InputStream`. Using a file will use fewer resources
     * compared to an input stream. The latter will need to cache the whole sheet
     * in-memory.
     * @param resource the `Resource` pointing to the Excel file.
     * @param password the password for opening the file
     * @throws Exception is thrown for any errors.
     */
    @Throws(Exception::class)
    override fun openExcelFile(resource: Resource?, password: String?) {
        if (resource!!.isFile) {
            val file = resource.file
            workbook = WorkbookFactory.create(file, password, false)
        } else {
            inputStream = resource.inputStream
            workbook = WorkbookFactory.create(inputStream, password)
        }
        workbook!!.missingCellPolicy = Row.MissingCellPolicy.CREATE_NULL_AS_BLANK
    }
}
