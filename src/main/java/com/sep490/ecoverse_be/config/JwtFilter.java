package com.sep490.ecoverse_be.config;

import com.sep490.ecoverse_be.entity.Account;
import com.sep490.ecoverse_be.enums.AccountStatus;
import com.sep490.ecoverse_be.model.UserPrincipal;
import com.sep490.ecoverse_be.service.ITokenService;
import jakarta.security.auth.message.AuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Arrays;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    @Lazy
    private ITokenService tokenService;

    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver handlerExceptionResolver;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public boolean checkIsPublicAPI(String uri) {
        return Arrays.stream(AppConstants.PUBLIC_URLS)
                .anyMatch(pattern -> pathMatcher.match(pattern, uri));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean isPublicAPI = checkIsPublicAPI(request.getRequestURI());
        if (isPublicAPI) {
            filterChain.doFilter(request, response);
        } else {
            String authHeader = request.getHeader("Authorization");
            String token = tokenService.getToken(authHeader);
            if (token == null) {
                handlerExceptionResolver.resolveException(request, response, null, new AuthException("Empty token!"));
                return;
            }

            try {
                if (tokenService.isTokenBlacklisted(token)) {
                    handlerExceptionResolver.resolveException(request, response, null, new AuthException("Token has been invalidated!"));
                    return;
                }

                Account account = tokenService.getAccountByToken(token);

                // Check account status
                if (!Boolean.TRUE.equals(account.getIsActive())
                        || account.getStatus() != AccountStatus.ACTIVE) {
                    handlerExceptionResolver.resolveException(request, response, null,
                            new AuthException("Account is not active or has been suspended"));
                    return;
                }

                UserPrincipal principal = new UserPrincipal(account);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        principal, token, principal.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                filterChain.doFilter(request, response);
            } catch (Exception e) {
                handlerExceptionResolver.resolveException(request, response, null, new AuthException(e.getMessage()));
            }
        }
    }
}
