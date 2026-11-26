package com.escapehub.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Έλεγχος με ολόκληρη την εφαρμογή, πραγματική PostgreSQL (Testcontainers) και {@code MockMvcTester}.
 *
 * <p>Χρειάζεται Docker. Χωρίς Docker οι έλεγχοι παραλείπονται τοπικά (εμφανίζονται ως skipped). Στο CI τρέχουν
 * πάντα, γιατί οι runners του GitHub Actions έχουν Docker.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
public @interface IntegrationTest {
}
