package com.taekwondo.examenes.auth;

/** Envía el correo de recuperación de la cuenta (RF-34): el usuario y el enlace para la contraseña nueva. */
public interface RecoveryMailer {

    void sendRecovery(String to, String username, String resetLink);
}
