package com.taekwondo.examenes;

import com.taekwondo.examenes.common.AppTimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Solo se entra con JWT: sin el usuario «user» por defecto, que además escribía su contraseña en el log
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ExamenesBackendApplication {

	public static void main(String[] args) {
		// Hora de España aunque el servidor esté en otra zona (DE-09)
		AppTimeZone.apply(System.getenv("APP_TIME_ZONE"));
		SpringApplication.run(ExamenesBackendApplication.class, args);
	}

}
