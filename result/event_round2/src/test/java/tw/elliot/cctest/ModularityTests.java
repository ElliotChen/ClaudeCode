package tw.elliot.cctest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

@DisplayName("Modularity Tests")
class ModularityTests {

    @Test
    @DisplayName("Verify modular structure")
    void verifyModularStructure() {
        ApplicationModules modules = ApplicationModules.of(CctestApplication.class);
        modules.verify();
    }
}
