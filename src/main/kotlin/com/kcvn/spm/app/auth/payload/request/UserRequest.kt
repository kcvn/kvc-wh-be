package com.kcvn.spm.app.auth.payload.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate
import kotlin.math.min

class UserRequest {
    @field:NotBlank(message = "Tài khoản đăng nhập không được để trống")
    @field:Size(min = 6,message = "Tài khoản không được ít hơn 6 ký tự")
    var username: @NotBlank String? = null
    @field:NotBlank(message = "Mật khẩu không được trống")
    @field:Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    @field:Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=.])(?=\\S+\$).{6,}\$",
        message = "Mật khẩu phải chứa ít nhất một chữ số, một chữ cái viết thường, một chữ cái viết hoa, và một ký tự đặc biệt."
    )
    var password: @NotBlank String? = null
    var employeeCode: String? = null
    @field:Email(message = "Không đúng định dạng gmail")
    var email: String? = null
    var phoneNumber: String? = null
    var fullName: String? = null
    var dateOfBirth: LocalDate? = null
    var avatar: String? = null
    var status: Short? = null
    var roleIds: Set<String>? = null
    var positions: Set<String>? = null
}