package tw.elliot.cctest;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfig.class)
class CctestApplicationTests {

    @Test
    void contextLoads() {
    }
}
