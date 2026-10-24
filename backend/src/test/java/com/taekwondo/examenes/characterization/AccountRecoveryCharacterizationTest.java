package com.taekwondo.examenes.characterization;

import com.taekwondo.examenes.auth.RecoveryMailer;
import com.taekwondo.examenes.characterization.support.HttpResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.taekwondo.examenes.characterization.support.Json.obj;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RF-34: quien tenga email recupera sin sesión su usuario y una contraseña nueva. La respuesta es la
 * misma exista o no la cuenta; el correo lleva el usuario y un enlace de un solo uso que caduca.
 */
@Import(AccountRecoveryCharacterizationTest.MailConfig.class)
@DisplayName("Caracterización: recuperar la cuenta por email")
class AccountRecoveryCharacterizationTest extends ApiCharacterizationTest {

    private static final String SENT = "Si ese email está registrado, te hemos enviado un correo con tu usuario "
            + "y un enlace para cambiar la contraseña.";
    private static final String INVALID = "El enlace no es válido o ha caducado: pide otro";
    private static final String LINK_PREFIX = "http://localhost:4200/auth/reset-password?token=";

    /** Guarda los correos en vez de enviarlos. */
    @TestConfiguration(proxyBeanMethods = false)
    static class MailConfig {
        @Bean
        @Primary
        CapturingMailer capturingMailer() {
            return new CapturingMailer();
        }
    }

    record Mail(String to, String username, String link) {}

    static class CapturingMailer implements RecoveryMailer {
        final List<Mail> sent = new CopyOnWriteArrayList<>();

        @Override
        public void sendRecovery(String to, String username, String resetLink) {
            sent.add(new Mail(to, username, resetLink));
        }
    }

    @Autowired
    CapturingMailer mailer;

    @Test
    @DisplayName("Se envía el usuario y un enlace a quien tiene ese email; la respuesta es la misma exista o no")
    void request_sendsMailOnlyToExistingAccount() {
        Session student = newStudent();
        String email = student.username() + "@test.com";

        assertThat((String) request(email.toUpperCase()).expectStatus(202).read("$.message")).isEqualTo(SENT);
        assertThat((String) request("nadie_" + email).expectStatus(202).read("$.message")).isEqualTo(SENT);

        List<Mail> mails = mailsTo(email);
        assertThat(mails).hasSize(1);
        assertThat(mails.get(0).username()).isEqualTo(student.username());
        assertThat(mails.get(0).link()).startsWith(LINK_PREFIX);
        assertThat(mailsTo("nadie_" + email)).isEmpty();
    }

    @Test
    @DisplayName("Con el enlace se pone una contraseña nueva: vale para entrar, cierra las sesiones y no sirve dos veces")
    void reset_changesPasswordOnce() {
        Session student = newStudent();
        String token = requestToken(student);

        reset(token, "nueva123").expectStatus(204);

        login(student.username(), "nueva123");
        get("/api/auth/me", student.token()).expectStatus(401);
        assertThat(reset(token, "otra1234").expectStatus(409).message()).isEqualTo(INVALID);
    }

    @Test
    @DisplayName("El enlace caduca a los 30 minutos, y pedir otro anula el anterior")
    void reset_expiresAndIsReplaced() {
        Session student = newStudent();
        String expired = requestToken(student);
        clock.advance(Duration.ofMinutes(31));
        assertThat(reset(expired, "nueva123").expectStatus(409).message()).isEqualTo(INVALID);

        String first = requestToken(student);
        String second = requestToken(student);
        assertThat(reset(first, "nueva123").expectStatus(409).message()).isEqualTo(INVALID);
        reset(second, "nueva123").expectStatus(204);
    }

    @Test
    @DisplayName("Una cuenta desactivada no recibe el correo; un enlace inventado no vale; la contraseña corta, 400")
    void rules() {
        Session student = newStudent();
        String email = student.username() + "@test.com";
        patch("/api/users/" + student.id() + "/active", adminToken(), obj("active", false)).expectStatus(200);

        request(email).expectStatus(202);

        assertThat(mailsTo(email)).isEmpty();
        assertThat(reset("inventado", "nueva123").expectStatus(409).message()).isEqualTo(INVALID);
        assertThat(reset("inventado", "123").expectStatus(400).message()).startsWith("Contraseña nueva: ");
    }

    // ───── Ayudas ─────

    private HttpResult request(String email) {
        return post("/api/auth/recovery", null, obj("email", email));
    }

    private HttpResult reset(String token, String newPassword) {
        return post("/api/auth/recovery/reset", null, obj("token", token, "newPassword", newPassword));
    }

    /** Pide la recuperación y devuelve el token del último enlace enviado. */
    private String requestToken(Session student) {
        String email = student.username() + "@test.com";
        request(email).expectStatus(202);
        List<Mail> mails = mailsTo(email);
        return mails.get(mails.size() - 1).link().substring(LINK_PREFIX.length());
    }

    private List<Mail> mailsTo(String email) {
        return mailer.sent.stream().filter(m -> m.to().equalsIgnoreCase(email)).toList();
    }
}
