package com.example.dijasaliou;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// Base H2 en memoire (comme les autres tests d'integration) : ce test ne doit pas
// dependre d'un MySQL local/CI qui n'existe pas forcement (ex. runners GitHub Actions).
@SpringBootTest
@TestPropertySource(locations = "classpath:application-integration.properties")
class DijaSaliouApplicationTests {

    @Test
    void contextLoads() {
    }

}
