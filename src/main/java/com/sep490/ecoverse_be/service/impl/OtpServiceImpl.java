package com.sep490.ecoverse_be.service.impl;

import com.sep490.ecoverse_be.service.IOtpService;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpServiceImpl implements IOtpService {
    private final SecureRandom random = new SecureRandom();

    private final Map<String, OtpEntry> otpStorage = new ConcurrentHashMap<>();

    private final Map<String, OffsetDateTime> verifiedEmails = new ConcurrentHashMap<>();

    public String generateOtp(String email) {
        String otp = String.format("%06d", random.nextInt(1_000_000));
        otpStorage.put(email, new OtpEntry(otp, OffsetDateTime.now().plusMinutes(5)));
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        OtpEntry entry = otpStorage.get(email);
        if (entry == null) return false;
        if (OffsetDateTime.now().isAfter(entry.expiry)) {
            otpStorage.remove(email);
            return false;
        }
        boolean isValid = entry.otp.equals(otp);
        if (isValid) otpStorage.remove(email);
        return isValid;
    }

    @Override
    public void markAsVerified(String email) {
        verifiedEmails.put(email, OffsetDateTime.now().plusMinutes(15));
    }

    @Override
    public boolean isEmailVerified(String email) {
        OffsetDateTime expiry = verifiedEmails.get(email);
        if (expiry == null) return false;
        if (OffsetDateTime.now().isAfter(expiry)) {
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
        OffsetDateTime expiry;
        OtpEntry(String otp, OffsetDateTime expiry) {
            this.otp = otp;
            this.expiry = expiry;
        }
    }
}
