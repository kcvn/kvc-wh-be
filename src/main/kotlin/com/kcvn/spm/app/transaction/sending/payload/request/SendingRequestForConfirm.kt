package com.kcvn.spm.app.transaction.sending.payload.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

// formCode là thông tin của phiếu nên đặt 1 lần ở ngoài, dùng chung cho mọi request trong list
class SendingRequestForConfirm (
    @field:NotBlank(message = "formCode must not be blank")
    var formCode: String,
    @field:NotEmpty(message = "requests must not be empty")
    var requests: List<SendingRequestKey>,
)
