package com.infotact.warehouse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test — verifies the full Spring application context loads successfully.
 *
 * How the schema is handled in CI:
 *   src/test/resources/application.yml sets ddl-auto=create-drop, which overrides
 *   the production application.yml (ddl-auto=validate) for all test runs.
 *   Hibernate generates the full schema from JPA entities on the blank CI
 *   PostgreSQL container — no manual SQL scripts or Flyway migrations needed for tests.
 *
 * @ActiveProfiles("test") additionally loads application-test.properties for
 *   any profile-specific overrides (JWT secret fallback, logging levels, etc.)
 *
 * JavaMailSender is mocked so the context doesn't attempt a live SMTP connection
 *   on startup. All other external services (PostgreSQL, Redis) are provided as
 *   real containers by the CI workflow.
 */
@SpringBootTest
@ActiveProfiles("test")
class WarehouseApplicationTests {

    /** Prevents a live SMTP connection attempt during context startup. */
    @MockBean
    private JavaMailSender javaMailSender;

    @Test
    void contextLoads() {
        // Passes if the Spring context assembles without errors.
        // Catches: misconfigured beans, missing env vars, schema mismatches.
    }
}
