package com.sep490.ecoverse_be.service;

public interface IOtpService {
    String generateOtp(String email);
    boolean verifyOtp(String email, String otp);

    // Đánh dấu email đã xác thực OTP thành công (TTL 15 phút)
    void markAsVerified(String email);

    // Kiểm tra email đã xác thực OTP chưa (dùng trước khi tạo tài khoản)
    boolean isEmailVerified(String email);

    // Xóa trạng thái verified sau khi đã tạo tài khoản xong
    void clearVerified(String email);
}
