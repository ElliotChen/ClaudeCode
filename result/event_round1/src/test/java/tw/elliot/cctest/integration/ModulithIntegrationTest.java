package tw.elliot.cctest.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tw.elliot.cctest.CctestApplication;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class ModulithIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
        .withDatabaseName("cctest")
        .withUsername("cctest")
        .withPassword("cctest");

    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void verifyModulithStructure() {
        ApplicationModules modules = ApplicationModules.of(CctestApplication.class);
        modules.verify();
    }

    @Test
    void contextLoads() {
        // 驗證應用程式可以正常啟動
    }
}
