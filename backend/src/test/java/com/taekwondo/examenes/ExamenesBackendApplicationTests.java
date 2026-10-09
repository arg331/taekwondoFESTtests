package com.taekwondo.examenes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Arranca la aplicación completa: Flyway aplica las migraciones e Hibernate valida el esquema. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ExamenesBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
