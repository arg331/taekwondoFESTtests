package com.taekwondo.examenes;

import com.taekwondo.examenes.common.AppTimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExamenesBackendApplication {

	public static void main(String[] args) {
		// Hora de España aunque el servidor esté en otra zona (DE-09)
		AppTimeZone.apply(System.getenv("APP_TIME_ZONE"));
		SpringApplication.run(ExamenesBackendApplication.class, args);
	}

}
