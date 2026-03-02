package com.sep490.ecoverse_be.service;

public interface IEmailService {
    void sendOtpEmail(String to, String otp);
    void sendForgotPasswordEmail(String to, String otp);
    void sendApprovalEmail(String to, String organizationName);
    void sendRejectionEmail(String to, String organizationName, String reason);
}
