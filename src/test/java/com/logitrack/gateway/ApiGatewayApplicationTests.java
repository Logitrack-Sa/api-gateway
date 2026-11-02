package com.logitrack.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias y de integraci&oacute;n b&aacute;sicas para el microservicio API Gateway.
 *
 * <p>Verifica que el contexto reactivo cargue sin conflictos de dependencias (garantizando
 * la ausencia de clases de servlets de Tomcat), que los endpoints de salud de Actuator
 * respondan adecuadamente y que los controladores de contingencia emitan respuestas reactivas v&aacute;lidas.</p>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiGatewayApplicationTests {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Verifica que el contexto de Spring Boot se inicialice de forma exitosa
     * con todos sus beans reactivos y configuraciones asociadas.
     */
    @Test
    @DisplayName("El contexto de la aplicación API Gateway debe cargar correctamente")
    void contextLoads() {
        assertThat(webTestClient).isNotNull();
    }

    /**
     * Verifica que el endpoint de contingencia /fallback devuelva HTTP 503 y la estructura esperada.
     */
    @Test
    @DisplayName("El endpoint de fallback debe responder HTTP 503 con formato JSON estructurado")
    void testFallbackEndpoint() {
        webTestClient.get()
                .uri("/fallback")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.gateway").isEqualTo("LogiTrack API Gateway");
    }

    /**
     * Verifica que el endpoint de salud de Actuator responda adecuadamente.
     */
    @Test
    @DisplayName("El endpoint /actuator/health debe responder exitosamente")
    void testActuatorHealth() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
