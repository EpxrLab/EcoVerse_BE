package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.entity.Account;
import com.sep490.ecoverse_be.repository.AccountRepository;
import com.sep490.ecoverse_be.service.IAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationServiceImpl implements IAuthenticationService {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản với email: " + email));

        return new User(
                account.getEmail(),
                account.getPassword(),
                account.getAuthorities()
        );
    }
}
