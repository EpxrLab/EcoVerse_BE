package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.service.IOtpService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpServiceImpl implements IOtpService {
    private final SecureRandom random = new SecureRandom();

    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    private final Map<String, LocalDateTime> verifiedEmails = new ConcurrentHashMap<>();

    public String generateOtp(String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        otpStorage.put(email, new OtpEntry(otp, LocalDateTime.now().plusMinutes(5)));
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        OtpEntry entry = otpStorage.get(email);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiry)) {
            otpStorage.remove(email);
            return false;
        }
        boolean isValid = entry.otp.equals(otp);
        if (isValid) otpStorage.remove(email);
        return isValid;
    }

    @Override
    public void markAsVerified(String email) {
        verifiedEmails.put(email, LocalDateTime.now().plusMinutes(15));
    }

    @Override
    public boolean isEmailVerified(String email) {
        LocalDateTime expiry = verifiedEmails.get(email);
        if (expiry == null) return false;
        if (LocalDateTime.now().isAfter(expiry)) {
            verifiedEmails.remove(email);
            return false;
        }
        return true;
    }

    @Override
    public void clearVerified(String email) {
        verifiedEmails.remove(email);
    }

    private static class OtpEntry {
        String otp;
        LocalDateTime expiry;
        OtpEntry(String otp, LocalDateTime expiry) {
            this.otp = otp;
            this.expiry = expiry;
        }
    }
}
