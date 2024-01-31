package com.kcvn.spm.app.product.payload.request

import com.kcvn.spm.common.helper.csvHelper.CsvMappingField

data class ProductImportRequest (
    @CsvMappingField("Tên sản phẩm")
    var productName: String? = null,

    @CsvMappingField("Loại xuất hàng")
    var exportType: String? = null,

    @CsvMappingField("Kích thước")
    var size: String? = null,

    @CsvMappingField("Khung 1")
    var frame_1: String? = null,

    @CsvMappingField("Khung 2")
    var frame_2: String? = null,

    @CsvMappingField("Khuôn đục")
    var mold: String? = null,

    @CsvMappingField("Chủng hàng")
    var productLine: String? = null,

    @CsvMappingField("S.R/No S.R")
    var srNosr: String? = null,

    @CsvMappingField("Pcs/SH")
    var pcs_sh: String? = null,

    @CsvMappingField("SH/BLOCK")
    var sh_block: String? = null,

    @CsvMappingField("Số lớp")
    var layerCount: String? = null,

    @CsvMappingField("RING/JIG")
    var ring_jig: String? = null,

    @CsvMappingField("Process")
    var process: String? = null,

    @CsvMappingField("Khuôn snap")
    var snapMold: String? = null,

    @CsvMappingField("Tape dùng chung")
    var tapeCommon: String? = null,

    @CsvMappingField("Loại TAPE")
    var tapeType: String? = null
)