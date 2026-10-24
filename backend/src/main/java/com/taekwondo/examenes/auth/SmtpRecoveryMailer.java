package com.taekwondo.examenes.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envía el correo de recuperación por SMTP (RF-34), configurado con spring.mail.* y app.mail.from.
 * Se envía aparte de la petición: así responde igual de rápido exista o no la cuenta, y un servidor de
 * correo lento o caído no la bloquea. Si falla, solo queda en el registro.
 */
@Component
public class SmtpRecoveryMailer implements RecoveryMailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpRecoveryMailer.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final TaskExecutor executor = new SimpleAsyncTaskExecutor("correo-");

    public SmtpRecoveryMailer(JavaMailSender mailSender, @Value("${app.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendRecovery(String to, String username, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Taekwondo ITF España: tu usuario y cómo cambiar la contraseña");
        message.setText("""
                Hola:

                Tu usuario es: %s

                Para poner una contraseña nueva, abre este enlace en los próximos 30 minutos:
                %s

                Si no lo has pedido tú, ignora este correo: tu contraseña no cambia.
                """.formatted(username, resetLink));
        executor.execute(() -> {
            try {
                mailSender.send(message);
            } catch (MailException ex) {
                // Sin la dirección: no se guardan datos personales en el registro
                log.warn("No se pudo enviar un correo de recuperación: {}", ex.getMessage());
            }
        });
    }
}
