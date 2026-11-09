package com.logitrack.gateway;

import com.logitrack.gateway.support.JwtTestHelper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "management.server.port=0",
        "management.server.address=127.0.0.1",
        "management.endpoints.web.exposure.include=health,info,prometheus",
        "management.endpoint.prometheus.enabled=true",
        "management.prometheus.metrics.export.enabled=true",
        "management.metrics.tags.application=api-gateway",
        "management.metrics.distribution.percentiles-histogram.http.server.requests=true"
})
@ActiveProfiles("test")
class ManagementListenerSeparationTest {

    @Autowired
    WebTestClient business;

    @LocalServerPort
    int businessPort;

    @Autowired
    Environment environment;

    @Test
    void managementListenerExposesPrometheusWithApplicationTag() {
        management().get().uri("/actuator/prometheus").exchange().expectStatus().isOk()
                .expectBody(String.class).value(body -> assertThat(body).contains("application=\"api-gateway\""));
    }

    @Test
    void managementListenerExposesHealth() {
        management().get().uri("/actuator/health").exchange().expectStatus().isOk();
    }

    @Test
    void trafficGeneratesHttpPercentilesBuckets() {
        // Tráfico al listener de negocio
        business.get().uri("/test/ping").exchange().expectStatus().value(status -> assertThat(status).isNotNull());
        // Tráfico al listener de gestión
        management().get().uri("/actuator/health").exchange().expectStatus().isOk();

        management().get().uri("/actuator/prometheus").exchange().expectStatus().isOk()
                .expectBody(String.class).value(body -> {
                    assertThat(body).contains("http_server_requests_seconds_bucket{");
                    assertThat(body).contains("le=");
                    assertThat(body).contains("http_server_requests_seconds_count");
                });
    }

    @Test
    void administrativeEndpointsAreNotExposed() {
        for (String path : new String[] { "/actuator/env", "/actuator/beans", "/actuator/threaddump", "/actuator/gateway" }) {
            business.get().uri(path).exchange().expectStatus().value(status -> assertThat(status).isIn(401, 403, 404, 405));
            management().get().uri(path).exchange().expectStatus().value(status -> assertThat(status).isIn(401, 403, 404, 405));
        }
        // POST /actuator/shutdown debe ser rechazado en ambos listeners
        business.post().uri("/actuator/shutdown").exchange().expectStatus().value(status -> assertThat(status).isIn(401, 403, 404, 405));
        management().post().uri("/actuator/shutdown").exchange().expectStatus().value(status -> assertThat(status).isIn(401, 403, 404, 405));
    }

    @Test
    void businessListenerDoesNotExposePrometheusOrInfoAnonymously() {
        for (String path : new String[] { "/actuator/prometheus", "/actuator/info" }) {
            business.get().uri(path).exchange().expectStatus().value(status -> assertThat(status).isIn(401, 403, 404));
        }
    }

    @Test
    void businessListenerRejectsPrometheusAndInfoEvenWithValidBusinessJwt() {
        String validJwt = JwtTestHelper.generateToken(UUID.randomUUID(), List.of("ADMIN"), null, null);
        assertThat(validJwt).isNotBlank();

        for (String path : new String[] { "/actuator/prometheus", "/actuator/info" }) {
            business.get().uri(path)
                    .header("Authorization", "Bearer " + validJwt)
                    .exchange()
                    .expectStatus()
                    .value(status -> assertThat(status).isIn(401, 403, 404));
        }
    }

    private WebTestClient management() {
        return WebTestClient.bindToServer().baseUrl("http://127.0.0.1:" + managementPort()).build();
    }

    private int managementPort() {
        return Integer.parseInt(environment.getRequiredProperty("local.management.port"));
    }
}
