package com.kcvn.spm.sample.config.support

import com.kcvn.spm.common.batch.excel.AbstractExcelItemWriter
import com.kcvn.spm.model.tables.pojos.AuthUser
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.springframework.batch.item.Chunk

open class UserExcelItemWriter : AbstractExcelItemWriter<AuthUser>() {
    override fun write(chunk: Chunk<out AuthUser?>) {
        val s = wb!!.getSheetAt(0)
        for (o in chunk) {
            val r: Row = s.createRow(row++)

            for (h in getHeaders()) {
                val c: Cell = r.createCell(getHeaders().indexOf(h))
                if (o != null) {
                    when(h) {
                        "id" -> c.setCellValue(o.id)
                        "username" -> c.setCellValue(o.username)
                        "password" -> c.setCellValue(o.password)
                        "employeeCode" -> c.setCellValue(o.employeeCode)
                        "email" -> c.setCellValue(o.email)
                        "phoneNumber" -> c.setCellValue(o.phoneNumber)
                        "fullName" -> c.setCellValue(o.fullName)
                        "fullNameUnsigned" -> c.setCellValue(o.fullNameUnsigned)
                        "dateOfBirth" -> c.setCellValue(o.dateOfBirth?.toString())
                        else -> o.status?.let { c.setCellValue(it.toDouble()) }
                    }
                }
            }
        }
    }

    override fun createHeaderRow(s: Sheet) {
        val cs: CellStyle = wb!!.createCellStyle()
        cs.wrapText = true
        cs.alignment = HorizontalAlignment.LEFT
        val r = s.createRow(row)
        r.setRowStyle(cs)

        for (h in getHeaders()) {
            val c: Cell = r.createCell(getHeaders().indexOf(h))
            c.setCellValue(h)
        }
        row++
    }
}