package com.testpulse.service;

import com.testpulse.dto.ForgotPasswordRequest;
import com.testpulse.dto.ResetPasswordRequest;
import com.testpulse.model.PasswordResetOtp;
import com.testpulse.model.User;
import com.testpulse.repository.PasswordResetOtpRepository;
import com.testpulse.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class PasswordResetService {
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int OTP_COOLDOWN_SECONDS = 60;

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final PasswordResetEmailService passwordResetEmailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetOtpRepository otpRepository,
                                PasswordResetEmailService passwordResetEmailService,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.otpRepository = otpRepository;
        this.passwordResetEmailService = passwordResetEmailService;
        this.passwordEncoder = passwordEncoder;
    }

    public void sendOtp(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No account is registered with this email."));

        otpRepository.findTopByEmailAndUsedAtIsNullOrderByCreatedAtDesc(email).ifPresent(previous -> {
            if (previous.getCreatedAt().plusSeconds(OTP_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("Please wait before requesting another OTP.");
            }
        });

        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        PasswordResetOtp resetOtp = new PasswordResetOtp();
        resetOtp.setEmail(email);
        resetOtp.setOtpHash(passwordEncoder.encode(otp));
        resetOtp.setCreatedAt(LocalDateTime.now());
        resetOtp.setExpiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpRepository.save(resetOtp);

        passwordResetEmailService.sendOtpEmailAsync(user.getEmail(), user.getFullName(), otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or OTP."));
        PasswordResetOtp resetOtp = otpRepository.findTopByEmailAndUsedAtIsNullOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or OTP."));

        if (resetOtp.getExpiresAt().isBefore(LocalDateTime.now())
                || !passwordEncoder.matches(request.getOtp(), resetOtp.getOtpHash())) {
            throw new IllegalArgumentException("Invalid or expired OTP.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        resetOtp.setUsedAt(LocalDateTime.now());
        otpRepository.save(resetOtp);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}