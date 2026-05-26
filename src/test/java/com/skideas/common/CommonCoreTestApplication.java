package com.skideas.common;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class CommonCoreTestApplication {
    // Test-only entry point — not shipped in the JAR
}
