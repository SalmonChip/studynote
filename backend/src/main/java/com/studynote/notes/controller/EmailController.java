package com.studynote.notes.controller;

import com.studynote.notes.model.base.ApiResponse;
import com.studynote.notes.service.EmailService;
import com.studynote.notes.utils.ApiResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@RestController
@RequestMapping("/api/email")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/verify-code")
    public ApiResponse<Void> sendVerifyCode(@RequestParam @NotBlank @Email String email) {
        try {
            emailService.sendVerificationCode(email);
            return ApiResponseUtil.success(null);
        } catch (Exception e) {
            return ApiResponseUtil.error(e.getMessage());
        }
    }
}