package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.StudentAccountInfo;

import java.util.List;

public interface IEmailService {
    void sendOtpEmail(String to, String otp);
    void sendForgotPasswordEmail(String to, String otp);
    void sendApprovalEmail(String to, String organizationName);
    void sendRejectionEmail(String to, String organizationName, String reason);
    void sendCredentialEmail(String toEmail, String parentName, String parentPhone,
                             String parentPassword, List<StudentAccountInfo> children);
}
