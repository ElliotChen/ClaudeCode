package tw.elliot.cctest;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityIT {

    @Test
    void verifyModularStructure() {
        ApplicationModules modules = ApplicationModules.of(CctestApplication.class);
        modules.verify();
    }
}
