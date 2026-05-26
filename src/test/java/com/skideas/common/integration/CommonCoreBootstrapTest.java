package com.skideas.common.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CommonCoreBootstrapTest {

    @Test
    void contextLoads() {
        // Verifies Spring context starts successfully with common-core on the classpath
    }
}
