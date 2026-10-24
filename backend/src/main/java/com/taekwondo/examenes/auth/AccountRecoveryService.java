package com.taekwondo.examenes.auth;

import com.taekwondo.examenes.common.BusinessRuleException;
import com.taekwondo.examenes.user.User;
import com.taekwondo.examenes.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Recuperar la cuenta sin sesión (RF-34). Quien tiene email recibe su usuario y un enlace para poner
 * una contraseña nueva; la respuesta es la misma exista o no la cuenta, para no revelar quién está
 * registrado. Sin email, se le pide al administrador fuera de la aplicación (RF-35).
 */
@Service
@Transactional
public class AccountRecoveryService {

    static final Duration LINK_VALIDITY = Duration.ofMinutes(30);
    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RecoveryMailer mailer;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final String publicUrl;
    private final SecureRandom random = new SecureRandom();

    public AccountRecoveryService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository,
                                  RecoveryMailer mailer, PasswordEncoder passwordEncoder, Clock clock,
                                  @Value("${app.public-url}") String publicUrl) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailer = mailer;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
        this.publicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
    }

    /** Si hay una cuenta activa con ese email, le envía su usuario y un enlace nuevo; los anteriores dejan de valer. */
    public void requestRecovery(String email) {
        userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(User::isActive)
                .ifPresent(this::sendLink);
    }

    /** Pone la contraseña nueva con el código del enlace y cierra las sesiones abiertas (como RF-06). */
    public void resetPassword(String token, String newPassword) {
        LocalDateTime now = LocalDateTime.now(clock);
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hash(token))
                .filter(t -> t.isValidAt(now))
                .orElseThrow(() -> new BusinessRuleException("El enlace no es válido o ha caducado: pide otro"));
        User user = userRepository.findById(resetToken.getUserId())
                .filter(User::isActive)
                .orElseThrow(() -> new BusinessRuleException("El enlace no es válido o ha caducado: pide otro"));
        user.changePassword(passwordEncoder.encode(newPassword));
        tokenRepository.deleteAllByUserId(user.getId());
    }

    private void sendLink(User user) {
        LocalDateTime now = LocalDateTime.now(clock);
        String token = newToken();
        tokenRepository.deleteAllByUserId(user.getId());
        tokenRepository.save(new PasswordResetToken(user.getId(), hash(token), now, now.plus(LINK_VALIDITY)));
        mailer.sendRecovery(user.getEmail(), user.getUsername(), publicUrl + "/auth/reset-password?token=" + token);
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
