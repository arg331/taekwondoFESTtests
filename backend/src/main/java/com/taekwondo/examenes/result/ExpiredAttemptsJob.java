package com.taekwondo.examenes.result;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Cada minuto corrige los intentos que han caducado sin entregar (RF-24), para que el alumno vea su
 * nota en «Mis resultados». Los resultados de un examen se ponen al día también al consultarlos.
 * Se apaga con app.attempts.auto-submit.enabled=false (los tests la llaman a mano).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "app.attempts.auto-submit.enabled", havingValue = "true", matchIfMissing = true)
public class ExpiredAttemptsJob {

    private final ResultService resultService;

    public ExpiredAttemptsJob(ResultService resultService) {
        this.resultService = resultService;
    }

    @Scheduled(fixedDelayString = "${app.attempts.auto-submit.interval-ms:60000}", initialDelay = 60_000)
    void autoSubmitExpired() {
        resultService.autoSubmitExpired();
    }
}
