package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.*;
import com.sep490.ecoverse_be.dto.response.UserResponse;
import com.sep490.ecoverse_be.dto.response.AuthResponse;
import com.sep490.ecoverse_be.entity.Partnership;
import com.sep490.ecoverse_be.entity.School;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.enums.Role;
import com.sep490.ecoverse_be.exception.DisabledException;
import com.sep490.ecoverse_be.exception.DuplicateEntity;
import com.sep490.ecoverse_be.exception.NotFoundException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.PartnershipRepository;
import com.sep490.ecoverse_be.repository.SchoolRepository;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthenticationServiceImpl implements IAuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ITokenService tokenService;

    @Autowired
    private IEmailService emailService;

    @Autowired
    private IOtpService otpService;

    @Autowired
    ModelMapper modelMapper;

    private BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private PartnershipRepository partnershipRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrUsername(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

        return new UserPrincipal(user);
    }



    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateEntity("Email này đã được sử dụng!");
        }
        emailService.sendOtpEmail(registerRequest.getEmail(), otpService.generateOtp(registerRequest.getEmail()));
    }

    @Transactional
    @Override
    public UserResponse verifyRegisterSchool(VerifyRegisterSchoolRequest registerRequest) {
        try {
            verifyOtpOrThrow(registerRequest.getContactEmail(), registerRequest.getOtp());

            User newUser = buildUser(registerRequest.getContactEmail(), registerRequest.getPassword(), Role.PARTNERSHIP_SCHOOL);

            School school = new School();
            school.setUser(newUser);
            school.setSchoolName(registerRequest.getSchoolName());
            school.setContactEmail(registerRequest.getContactEmail());
            school.setProvince(registerRequest.getProvince());
            school.setDistrict(registerRequest.getDistrict());
            school.setAddress(registerRequest.getStreetAddress());
            school.setPhoneNumber(registerRequest.getPhoneNumber());
            school.setPrincipalName(registerRequest.getPrincipalName());
            school.setLinkWeb(registerRequest.getLinkWeb());
            school.setTaxCode(registerRequest.getTaxCode());
            school.setDescription(registerRequest.getDescription());
            school.setPosition(registerRequest.getPosition());
            school.setSchoolType(registerRequest.getSchoolType());
            school.setLogoUrl(registerRequest.getLogoUrl());
            school.setLicenseUrl(registerRequest.getLicenseUrl());
            schoolRepository.save(school);

            return modelMapper.map(newUser, UserResponse.class);
        } catch (DataIntegrityViolationException e) {
            e.printStackTrace();
            throw new RuntimeException("Đã xảy ra lỗi trong quá trình đăng ký: " + e.getMessage());
        } catch (DuplicateEntity e) {
            throw new DuplicateEntity(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Đã xảy ra lỗi không xác định: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public UserResponse verifyRegisterPartnership(VerifyRegisterPartnershipRequest registerRequest) {
        try {
            verifyOtpOrThrow(registerRequest.getContactEmail(), registerRequest.getOtp());

            User newUser = buildUser(registerRequest.getContactEmail(), registerRequest.getPassword(), Role.THIRD_PARTY_PARTNERSHIP);

            Partnership partnership = new Partnership();
            partnership.setUser(newUser);
            partnership.setOrganizationName(registerRequest.getOrganizationName());
            partnership.setContactEmail(registerRequest.getContactEmail());
            partnership.setGeographicScopeProvince(registerRequest.getProvince());
            partnership.setGeographicScopeDistrict(registerRequest.getDistrict());
            partnership.setRegisteredAddress(registerRequest.getStreetAddress());
            partnership.setPhoneNumber(registerRequest.getPhoneNumber());
            partnership.setContactPerson(registerRequest.getContactPerson());
            partnership.setLinkWeb(registerRequest.getLinkWeb());
            partnership.setDescription(registerRequest.getDescription());
            partnership.setTaxCode(registerRequest.getTaxCode());
            partnership.setPosition(registerRequest.getPosition());
            partnership.setPartnershipType(registerRequest.getPartnershipType());
            partnership.setLogoUrl(registerRequest.getLogoUrl());
            partnership.setLicenseUrl(registerRequest.getLicenseUrl());
            partnershipRepository.save(partnership);

            return modelMapper.map(newUser, UserResponse.class);
        } catch (DataIntegrityViolationException e) {
            e.printStackTrace();
            throw new RuntimeException("Đã xảy ra lỗi trong quá trình đăng ký: " + e.getMessage());
        } catch (DuplicateEntity e) {
            throw new DuplicateEntity(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Đã xảy ra lỗi không xác định: " + e.getMessage());
        }
    }

    @Override
    public AuthResponse login(LoginRequest request){
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            if (!authentication.isAuthenticated()) {
                throw new BadCredentialsException("Invalid email or password.");
            }
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            User user = userPrincipal.getUser();

            String accessToken = tokenService.generateToken(user);
            String refreshToken = tokenService.generateRefreshToken(user);

            AuthResponse authResponse = modelMapper.map(user, AuthResponse.class);
            if (authentication.isAuthenticated()) {
                authResponse.setAccessToken(accessToken);
                authResponse.setRefreshToken(refreshToken);
            }
            return authResponse;
        } catch (DisabledException e) {
            throw new DisabledException(e.getMessage());
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Email/số điện thoại/username hoặc mật khẩu sai!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Đã xảy ra lỗi trong quá trình đăng nhập, vui lòng thử lại sau.");
        }
    }

    private void verifyOtpOrThrow(String email, String otp) {
        if (!otpService.verifyOtp(email, otp)) {
            throw new NotFoundException("Invalid Otp");
        }
    }

    private User buildUser(String email, String password, Role role) {

        User user = new User();
        user.setEmail(email);
        user.setRole(role);

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        user.setPasswordHash(passwordEncoder.encode(password));

        return userRepository.save(user);
    }

    @Override
    public void logout (String token, RefreshTokenRequest request){
        String accessToken = tokenService.getToken(token);
        if (accessToken != null) {
            tokenService.invalidateToken(accessToken);
        }

        if (request != null && request.getRefreshToken() != null) {
            tokenService.deleteRefreshToken(request.getRefreshToken());
        }
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("Tài khoản không tồn tại: " + request.getEmail()));

        emailService.sendForgotPasswordEmail(request.getEmail(), otpService.generateOtp(request.getEmail()));
    }

    @Override
    public AuthResponse verifyResetPassword(VerifyForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("Tài khoản không tồn tại: " + request.getEmail()));

        boolean flag = otpService.verifyOtp(request.getEmail(), request.getOtp());

        if (!flag)
            throw new NotFoundException("OTP không hợp lệ");

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setUpdatedAt(LocalDateTime.now());

        User newUser = userRepository.save(user);

        return modelMapper.map(newUser, AuthResponse.class);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu cũ không đúng");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }



}
