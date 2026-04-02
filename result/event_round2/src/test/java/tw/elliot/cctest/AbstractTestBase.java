package tw.elliot.cctest;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base test class for integration tests using H2 in-memory database.
 * Requires Docker to run Testcontainers-based tests.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractTestBase {
}
