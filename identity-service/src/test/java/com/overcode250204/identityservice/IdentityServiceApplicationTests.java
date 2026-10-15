package com.overcode250204.identityservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class IdentityServiceApplicationTests {

    @Test
    void contextLoads() {
        try {
            System.out.println("DIAGNOSTIC: org.flywaydb.core.Flyway = " + Class.forName("org.flywaydb.core.Flyway"));
            System.out.println("DIAGNOSTIC: FlywayAutoConfiguration = "
                    + Class.forName("org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
