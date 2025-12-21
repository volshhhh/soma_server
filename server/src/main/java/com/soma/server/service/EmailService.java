package com.soma.server.service;

import com.soma.server.entity.User;
import com.soma.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for handling email verification and notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    @Value("${spring.mail.username:noreply@soma-app.com}")
    private String fromEmail;

    @Value("${app.name:SOMA}")
    private String appName;

    /**
     * Generates a verification token and sends verification email to user.
     */
    @Transactional
    public String generateVerificationToken(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setVerificationToken(token);
        user.setVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);
        return token;
    }

    /**
     * Sends verification email asynchronously.
     */
    @Async
    public void sendVerificationEmail(User user, String token) {
        try {
            String verificationLink = baseUrl + "/verify-email?token=" + token;
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(appName + " — Подтверждение email");
            
            String htmlContent = buildVerificationEmailHtml(user.getUsername(), verificationLink);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Verification email sent to: {}", user.getEmail());
            
        } catch (MessagingException e) {
            log.error("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
            // Fallback to simple text email
            sendSimpleVerificationEmail(user, token);
        }
    }

    /**
     * Fallback method for sending simple text email.
     */
    private void sendSimpleVerificationEmail(User user, String token) {
        try {
            String verificationLink = baseUrl + "/verify-email?token=" + token;
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(user.getEmail());
            message.setSubject(appName + " — Подтверждение email");
            message.setText(
                "Привет, " + user.getUsername() + "!\n\n" +
                "Спасибо за регистрацию в " + appName + ".\n\n" +
                "Для подтверждения email перейдите по ссылке:\n" +
                verificationLink + "\n\n" +
                "Ссылка действительна 24 часа.\n\n" +
                "Если вы не регистрировались, просто проигнорируйте это письмо.\n\n" +
                "С уважением,\nКоманда " + appName
            );
            
            mailSender.send(message);
            log.info("Simple verification email sent to: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Failed to send simple verification email: {}", e.getMessage());
        }
    }

    /**
     * Verifies email token and marks user as verified.
     */
    @Transactional
    public boolean verifyEmail(String token) {
        Optional<User> userOptional = userRepository.findByVerificationToken(token);
        
        if (userOptional.isEmpty()) {
            log.warn("Invalid verification token: {}", token);
            return false;
        }
        
        User user = userOptional.get();
        
        if (user.getVerificationTokenExpiry() == null || 
            user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("Expired verification token for user: {}", user.getEmail());
            return false;
        }
        
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
        
        log.info("Email verified successfully for user: {}", user.getEmail());
        return true;
    }

    /**
     * Resends verification email to user.
     */
    @Transactional
    public boolean resendVerificationEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isEmpty()) {
            return false;
        }
        
        User user = userOptional.get();
        
        if (user.isEmailVerified()) {
            return false; // Already verified
        }
        
        String token = generateVerificationToken(user);
        sendVerificationEmail(user, token);
        return true;
    }

    /**
     * Builds HTML content for verification email.
     */
    private String buildVerificationEmailHtml(String username, String verificationLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background: #0a0a0a; color: #e0e0e0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: #1a1a2e; border-radius: 12px; padding: 40px; }
                    .logo { text-align: center; font-size: 32px; font-weight: bold; color: #1db954; margin-bottom: 30px; }
                    h1 { color: #ffffff; font-size: 24px; margin-bottom: 20px; }
                    p { line-height: 1.6; color: #b0b0b0; }
                    .button { display: inline-block; background: linear-gradient(135deg, #1db954, #1ed760); color: #000; padding: 14px 28px; text-decoration: none; border-radius: 25px; font-weight: bold; margin: 20px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #333; font-size: 12px; color: #666; }
                    .link { color: #1db954; word-break: break-all; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="logo">🎵 SOMA</div>
                    <h1>Привет, %s!</h1>
                    <p>Спасибо за регистрацию в SOMA — платформе для переноса музыки и аналитики.</p>
                    <p>Для завершения регистрации подтвердите ваш email:</p>
                    <p style="text-align: center;">
                        <a href="%s" class="button">Подтвердить Email</a>
                    </p>
                    <p>Или скопируйте ссылку:</p>
                    <p class="link">%s</p>
                    <p>⏰ Ссылка действительна 24 часа.</p>
                    <div class="footer">
                        <p>Если вы не регистрировались в SOMA, просто проигнорируйте это письмо.</p>
                        <p>© 2025 SOMA Team</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, verificationLink, verificationLink);
    }

    /**
     * Sends password reset email.
     */
    @Async
    public void sendPasswordResetEmail(User user, String resetToken) {
        try {
            String resetLink = baseUrl + "/reset-password?token=" + resetToken;
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(appName + " — Сброс пароля");
            
            String htmlContent = buildPasswordResetEmailHtml(user.getUsername(), resetLink);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("Password reset email sent to: {}", user.getEmail());
            
        } catch (MessagingException e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
        }
    }

    private String buildPasswordResetEmailHtml(String username, String resetLink) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background: #0a0a0a; color: #e0e0e0; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: #1a1a2e; border-radius: 12px; padding: 40px; }
                    .logo { text-align: center; font-size: 32px; font-weight: bold; color: #1db954; margin-bottom: 30px; }
                    h1 { color: #ffffff; font-size: 24px; margin-bottom: 20px; }
                    p { line-height: 1.6; color: #b0b0b0; }
                    .button { display: inline-block; background: linear-gradient(135deg, #ff6b6b, #ff8e8e); color: #000; padding: 14px 28px; text-decoration: none; border-radius: 25px; font-weight: bold; margin: 20px 0; }
                    .footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #333; font-size: 12px; color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="logo">🎵 SOMA</div>
                    <h1>Сброс пароля</h1>
                    <p>Привет, %s!</p>
                    <p>Вы запросили сброс пароля. Нажмите кнопку ниже:</p>
                    <p style="text-align: center;">
                        <a href="%s" class="button">Сбросить пароль</a>
                    </p>
                    <p>⏰ Ссылка действительна 1 час.</p>
                    <div class="footer">
                        <p>Если вы не запрашивали сброс пароля, проигнорируйте это письмо.</p>
                        <p>© 2025 SOMA Team</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, resetLink);
    }
}

