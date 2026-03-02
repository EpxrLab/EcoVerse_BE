package com.sep490.ecoverse_be.service;

import com.sep490.ecoverse_be.dto.response.AuthResponse;
import com.sep490.ecoverse_be.entity.User;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;


/**
 * Interface định nghĩa các phương thức xử lý JWT token
 * Bao gồm tạo token, validate, blacklist và extract thông tin
 */
public interface ITokenService {

    /**
     * Tạo JWT token mới cho user
     * @param user Tài khoản cần tạo token
     * @return String JWT token
     */
    String generateToken(User user);

    /**
     * Đưa token vào danh sách đen (vô hiệu hóa token)
     * @param token JWT token cần vô hiệu hóa
     */
    void invalidateToken(String token);

    /**
     * Lấy thông tin User từ token
     * @param token JWT token
     * @return User Thông tin tài khoản
     */
    User getUserByToken(String token);

    /**
     * Kiểm tra token có bị blacklist không
     * @param token JWT token cần kiểm tra
     * @return boolean True nếu token bị blacklist
     */
    boolean isTokenBlacklisted(String token);

    /**
     * Lấy token từ Authorization header
     * @param authHeader Authorization header
     * @return String JWT token (không có Bearer prefix)
     */
    String getToken(String authHeader);

    /**
     * Extract username từ token
     * @param token JWT token
     * @return String Username
     */
    String extractUsername(String token);

    /**
     * Extract claims từ token
     * @param token JWT token
     * @param claimsResolver Function để resolve claims
     * @return T Kết quả sau khi resolve
     */
    <T> T extractClaims(String token, java.util.function.Function<Claims, T> claimsResolver);

    /**
     * Extract tất cả claims từ token
     * @param token JWT token
     * @return Claims Tất cả claims trong token
     */
    Claims extractAllClaims(String token);

    /**
     * Validate token với user details
     * @param token JWT token
     * @param userDetails User details để validate
     * @return boolean True nếu token hợp lệ
     */
    boolean validateToken(String token, UserDetails userDetails);

    /**
     * Tạo refresh token cho user, lưu vào Redis
     * @param user Tài khoản cần tạo refresh token
     * @return String Refresh token
     */
    String generateRefreshToken(User user);

    /**
     * Dùng refresh token để lấy access token mới + refresh token mới (rotation)
     * @param refreshToken Refresh token hiện tại
     * @return AuthResponse chứa accessToken và refreshToken mới
     */
    AuthResponse refreshAccessToken(String refreshToken);

    /**
     * Xóa refresh token khỏi Redis (dùng khi logout)
     * @param refreshToken Refresh token cần xóa
     */
    void deleteRefreshToken(String refreshToken);
}
