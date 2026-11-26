package com.escapehub.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * PostgreSQL σε container για τους ελέγχους, με την ίδια έκδοση όπως στα compose files.
 * Χρησιμοποιείται μέσω του {@link IntegrationTest}, ή με {@code @Import} σε ελέγχους που χρειάζονται κάτι άλλο
 * (π.χ. {@code @DataJpaTest} ή πραγματικό server με {@code RANDOM_PORT}).
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
	}

}
