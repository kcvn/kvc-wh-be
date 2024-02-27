package com.kcvn.spm.common.batch.excel

import org.springframework.batch.item.ParseException

/**
 * Exception thrown when parsing Excel files. The name of the sheet, the row number on
 * that sheet and the name of the Excel file can be passed in so that in exception
 * handling we can reuse it. This class only has simply dependencies to make it is generic
 * as possible.
 */
class ExcelFileParseException
/**
 * Construct an [ExcelFileParseException].
 * @param message the message
 * @param cause the root cause
 * @param filename the name of the Excel file
 * @param sheet the name of the sheet
 * @param rowNumber the row number in the current sheet
 * @param row the row data as text
 */(
    message: String?,
    cause: Throwable?,
    val filename: String,
    val sheet: String,
    val rowNumber: Int,
    val row: Array<String>
) : ParseException(message!!, cause!!)
