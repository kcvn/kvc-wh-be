package com.kcvn.spm.common.batch.excel

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.batch.item.ExecutionContext
import org.springframework.batch.item.ItemStreamException
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream
import org.springframework.batch.item.support.AbstractItemStreamItemWriter
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.io.WritableResource
import org.springframework.util.Assert
import java.io.BufferedOutputStream
import java.io.IOException


abstract class AbstractExcelItemWriter<T> : AbstractItemStreamItemWriter<T?>(), ResourceAwareItemWriterItemStream<T?>, InitializingBean {
    protected var wb: Workbook? = null
    private var resource: WritableResource? = null
    protected var row = 0
    private var headers: List<String>? = null
    private var isXlsFile = false

    override fun setResource(resource: WritableResource) {
        this.resource = resource
    }

    fun setHeaders(names: List<String>) {
        this.headers = names
    }

    fun getHeaders(): List<String> = this.headers!!

    fun setXlsxFile(value: Boolean) {
        this.isXlsFile = value
    }

    override fun open(executionContext: ExecutionContext) {
        wb = if (isXlsFile) HSSFWorkbook() else XSSFWorkbook();
        row = 0;
        val s = wb!!.createSheet()
        createHeaderRow(s)
    }

    override fun close() {
        if (wb == null) {
            return;
        }
        try {
            BufferedOutputStream(resource!!.outputStream).use { bos ->
                wb!!.write(bos)
                bos.flush()
                wb!!.close()
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
            throw ItemStreamException("Error writing to output file", ex)
        }
        row = 0
        wb = null
    }

    override fun afterPropertiesSet() {
        Assert.notNull(resource, "Resource must be set")
        Assert.notNull(headers, "Headers must be set")
    }

    abstract protected fun createHeaderRow(s: Sheet)
}