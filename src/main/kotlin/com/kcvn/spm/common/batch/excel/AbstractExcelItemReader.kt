package com.kcvn.spm.common.batch.excel

import com.kcvn.spm.common.batch.excel.rowset.RowSet
import com.kcvn.spm.common.batch.excel.rowset.RowSetFactory
import com.kcvn.spm.common.batch.excel.rowset.impl.DefaultRowSetFactory
import org.apache.commons.logging.LogFactory
import org.apache.logging.log4j.util.Strings
import org.apache.poi.ss.usermodel.DataFormatter
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.io.Resource
import org.springframework.util.Assert
import org.springframework.util.ClassUtils
import java.util.*

/**
 * [org.springframework.batch.item.ItemReader] implementation to read an Excel file.
 * It will read the file sheet for sheet and row for row. It is loosely based on the
 * [org.springframework.batch.item.file.FlatFileItemReader]
 *
 * @param <T> the type
 */
abstract class AbstractExcelItemReader<T> :
    AbstractItemCountingItemStreamItemReader<T?>(),
    ResourceAwareItemReaderItemStream<T>, InitializingBean {
    protected val logger = LogFactory.getLog(javaClass)
    private var resource: Resource? = null
    private var linesToSkip = 0
    private var currentSheet = 0
    private var endAfterBlankLines = 1
    private var rowMapper: RowMapper<T>? = null
    private var skippedRowsCallback: RowCallbackHandler? = null
    private var noInput = false
    private var strict = true
    private var rowSetFactory: RowSetFactory = DefaultRowSetFactory()
    private var rs: RowSet? = null
    private var password: String? = null
    private var userLocale: Locale? = null
    private var dataFormatter: DataFormatter? = null

    init {
        this.name = ClassUtils.getShortName(this.javaClass)
    }

    @Throws(Exception::class)
    override fun read(): T? {
        var item = super.read()
        var blankLines = 0
        while (item == null) {
            blankLines++
            if (blankLines >= endAfterBlankLines) {
                return null
            }
            item = super.read()
            if (item != null) {
                return item
            }
        }
        return item
    }

    override fun doRead(): T? {
        if (noInput) {
            return null
        }
        if (rs == null || !rs!!.next()) {
            if (!nextSheet()) {
                if (logger.isDebugEnabled) {
                    logger.debug("No more sheets in '" + resource!!.description + "'.")
                }
                return null
            }
        }

        // skip all the blank row from which content has been deleted but still a valid row
        while (null != rs!!.currentRow && isInvalidValidRow(rs)) {
            rs!!.next()
        }
        try {
            return if (rs!!.currentRow != null) rowMapper!!.mapRow(rs!!) else doRead()
        } catch (ex: Exception) {
            throw ExcelFileParseException(
                "Exception parsing Excel file.", ex, resource!!.description,
                rs!!.metaData!!.getSheetName()!!, rs!!.currentRowIndex, rs!!.currentRow!!
            )
        }
    }

    /**
     * On restart this will increment rowSet to where job left off previously.
     * Temporarily switch out the configured `RowMapper` so we can use the
     * `#doRead` method and reuse the logic in there, but without actually map to
     * instances (this to save memory and have better performance).
     */
    override fun jumpToItem(itemIndex: Int) {
        val current = rowMapper
        rowMapper = null //RowMapper { rs: RowSet? -> null }
        try {
            for (i in 0 until itemIndex) {
                doRead()
            }
        } finally {
            rowMapper = current
        }
    }

    private fun isInvalidValidRow(rs: RowSet?): Boolean {
        for (str: String? in rs!!.currentRow!!) {
            if (Strings.isEmpty(str)) {
                return false
            }
        }
        return true
    }

    @Throws(Exception::class)
    override fun doOpen() {
        Assert.notNull(resource, "Input resource must be set")
        noInput = true
        if (!resource!!.exists()) {
            if (strict) {
                throw IllegalStateException(
                    "Input resource must exist (reader is in 'strict' mode): $resource"
                )
            }
            logger.warn("Input resource does not exist '" + resource!!.description + "'.")
            return
        }
        if (!resource!!.isReadable) {
            if (strict) {
                throw IllegalStateException(
                    "Input resource must be readable (reader is in 'strict' mode): $resource"
                )
            }
            logger.warn("Input resource is not readable '" + resource!!.description + "'.")
            return
        }
        openExcelFile(resource, password)
        noInput = false
        if (logger.isDebugEnabled) {
            logger.debug(
                "Opened workbook [" + resource!!.filename + "] with " + getNumberOfSheets()
                        + " sheets."
            )
        }
    }

    private fun nextSheet(): Boolean {
        while (currentSheet < getNumberOfSheets()) {
            val sheet = getSheet(currentSheet)
            rs = rowSetFactory.create(sheet)
            if (logger.isDebugEnabled) {
                logger.debug("Opening sheet " + sheet.getName() + ".")
            }
            for (i in 0 until linesToSkip) {
                if (rs!!.next() && skippedRowsCallback != null) {
                    skippedRowsCallback!!.handleRow(rs)
                }
            }
            if (logger.isDebugEnabled) {
                logger.debug("Opened sheet " + sheet.getName() + ", with " + sheet.getNumberOfRows() + " rows.")
            }
            currentSheet++
            if (rs!!.next()) {
                return true
            }
        }
        return false
    }

    @Throws(Exception::class)
    override fun doClose() {
        currentSheet = 0
        rs = null
    }

    /**
     * Public setter for the input resource.
     * @param resource the `Resource` pointing to the Excel file
     */
    override fun setResource(resource: Resource) {
        this.resource = resource
    }

    override fun afterPropertiesSet() {
        Assert.notNull(rowMapper, "RowMapper must be set")
        dataFormatter = if ((userLocale != null)) DataFormatter(userLocale) else DataFormatter()
    }

    protected fun getDataFormatter(): DataFormatter? {
        return dataFormatter
    }

    /**
     * Set the number of lines to skip. This number is applied to all worksheet in the
     * Excel file! default to 0
     * @param linesToSkip number of lines to skip
     */
    fun setLinesToSkip(linesToSkip: Int) {
        this.linesToSkip = linesToSkip
    }

    /**
     * Get the sheet based on the given sheet index.
     * @param sheet the sheet index
     * @return the sheet or `null` when no sheet available.
     */
    protected abstract fun getSheet(sheet: Int): WorkSheet
    protected abstract fun getNumberOfSheets(): Int

    /**
     * Opens the Excel file and reads the file and sheet metadata. Uses a `Resource` to read the sheets,
     * this file can optionally be password protected.
     * @param resource `Resource` pointing to the Excel file to read
     * @param password optional password
     * @throws Exception when the Excel sheet cannot be accessed
     */
    @Throws(Exception::class)
    protected abstract fun openExcelFile(resource: Resource?, password: String?)

    /**
     * In strict mode the reader will throw an exception on
     * [.open] if the input
     * resource does not exist.
     * @param strict true by default
     */
    fun setStrict(strict: Boolean) {
        this.strict = strict
    }

    /**
     * Public setter for the `rowMapper`. Used to map a row read from the underlying
     * Excel workbook.
     * @param rowMapper the `RowMapper` to use.
     */
    fun setRowMapper(rowMapper: RowMapper<T>?) {
        this.rowMapper = rowMapper
    }

    /**
     * Public setter for the `rowSetFactory`. Used to create a `RowSet`
     * implementation. By default, the `DefaultRowSetFactory` is used.
     * @param rowSetFactory the `RowSetFactory` to use.
     */
    fun setRowSetFactory(rowSetFactory: RowSetFactory) {
        this.rowSetFactory = rowSetFactory
    }

    /**
     * Set the callback handler to call when a row is being skipped.
     * @param skippedRowsCallback will be called for each one of the initial skipped lines
     * before any items are read.
     */
    fun setSkippedRowsCallback(skippedRowsCallback: RowCallbackHandler?) {
        this.skippedRowsCallback = skippedRowsCallback
    }

    fun setEndAfterBlankLines(endAfterBlankLines: Int) {
        this.endAfterBlankLines = endAfterBlankLines
    }

    /**
     * The password used to protect the file to open.
     * @param password the password
     */
    fun setPassword(password: String?) {
        this.password = password
    }

    /**
     * The `Locale` to use when reading sheets. Defaults to the platform default as set by Java.
     * @param userLocale the `Locale` to use, default `null`
     */
    fun setUserLocale(userLocale: Locale?) {
        this.userLocale = userLocale
    }
}
