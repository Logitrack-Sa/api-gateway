package com.logitrack.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.endpoints.web.exposure.include=health,info"
})
@ActiveProfiles("test")
class BusinessListenerHealthWithoutManagementPortTest {

    @Autowired
    WebTestClient client;

    @Test
    void healthStaysAvailableAndTechnicalEndpointsRemainClosed() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
        client.get().uri("/actuator/prometheus").exchange().expectStatus().value(status -> assertThat(status).isIn(401, 403, 404));
    }
}
