package com.testpulse.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
@Slf4j
public class PasswordResetEmailService {

    private static final int OTP_EXPIRY_MINUTES = 10;

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public PasswordResetEmailService(JavaMailSender mailSender,
                                     @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Async
    public void sendOtpEmailAsync(String email, String fullName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(email);
            helper.setSubject("TestPlus password reset OTP");

            String safeName = HtmlUtils.htmlEscape(fullName == null ? "there" : fullName);
            String plainText = "Hello " + (fullName == null ? "there" : fullName) + ",\n\n"
                    + "Your TestPlus password reset OTP is: " + otp + "\n"
                    + "It expires in " + OTP_EXPIRY_MINUTES + " minutes.\n\n"
                    + "If you did not request this, ignore this email.";
            String htmlText = "<p>Hello " + safeName + ",</p>"
                    + "<p>Your TestPlus password reset OTP is: <strong>" + otp + "</strong></p>"
                    + "<p>It expires in " + OTP_EXPIRY_MINUTES + " minutes.</p>"
                    + "<p>If you did not request this, ignore this email.</p>";
            helper.setText(plainText, htmlText);
            mailSender.send(message);
            log.info("Password reset email sent successfully to {}", email);
        } catch (MessagingException | RuntimeException ex) {
            log.error("Failed to send password reset email to {}", email, ex);
        }
    }
}
