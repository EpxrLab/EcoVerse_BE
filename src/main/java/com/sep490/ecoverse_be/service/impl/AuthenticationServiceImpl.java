package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.dto.request.LoginRequest;
import com.sep490.ecoverse_be.dto.request.RefreshTokenRequest;
import com.sep490.ecoverse_be.dto.response.AuthResponse;
import com.sep490.ecoverse_be.entity.User;
import com.sep490.ecoverse_be.exception.DisabledException;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.repository.UserRepository;
import com.sep490.ecoverse_be.service.IAuthenticationService;
import com.sep490.ecoverse_be.service.ITokenService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements IAuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ITokenService tokenService;

    @Autowired
    ModelMapper modelMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new UserPrincipal(user);
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

            User user = userPrincipal.getAccount();

            String accessToken = tokenService.generateToken(user);
            String refreshToken = tokenService.generateRefreshToken(user);

            AuthResponse authResponse = modelMapper.map(user, AuthResponse.class);
            if (authentication.isAuthenticated()) {
                authResponse.setToken(accessToken);
                authResponse.setRefreshToken(refreshToken);
            }
            return authResponse;
        } catch (DisabledException e) {
            throw new DisabledException(e.getMessage());
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Email hoặc mật khẩu sai!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Đã xảy ra lỗi trong quá trình đăng nhập, vui lòng thử lại sau.");
        }
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
}
