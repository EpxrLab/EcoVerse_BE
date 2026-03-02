package com.sep490.ecoverse_be.service;

public interface IOtpService {
    String generateOtp(String email);
    boolean verifyOtp(String email, String otp);
}
