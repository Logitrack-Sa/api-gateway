package com.logitrack.gateway;

import com.logitrack.gateway.config.CorsProperties;
import com.logitrack.gateway.support.JwtTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integraci&oacute;n reactivas para la gesti&oacute;n perimetral de CORS
 * y centralizaci&oacute;n de propiedades en el API Gateway.
 *
 * <p>Verifica:</p>
 * <ul>
 *   <li>Resoluci&oacute;n correcta de solicitudes preflight {@code OPTIONS} sin requerir token JWT.</li>
 *   <li>Emisi&oacute;n de cabeceras {@code Access-Control-Allow-*} para or&iacute;genes web autorizados.</li>
 *   <li>Bloqueo o descarte de cabeceras CORS para or&iacute;genes no autorizados.</li>
 *   <li>Enlace correcto de {@link CorsProperties} externalizadas.</li>
 *   <li>Configuraci&oacute;n de filtros de deduplicaci&oacute;n {@code DedupeResponseHeader}.</li>
 * </ul>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Pruebas de Integración de CORS y Centralización de Propiedades")
public class GatewayCorsIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CorsProperties corsProperties;

    @Autowired
    private GatewayProperties gatewayProperties;

    /**
     * Constructor predeterminado de la suite de pruebas de CORS.
     */
    public GatewayCorsIntegrationTest() {
    }

    /**
     * Grupo de pruebas enfocadas en el procesamiento de solicitudes preflight (OPTIONS).
     */
    @Nested
    @DisplayName("1. Solicitudes Preflight OPTIONS")
    public class PreflightRequestTests {

        /**
         * Constructor predeterminado del grupo de pruebas preflight.
         */
        public PreflightRequestTests() {
        }

        /**
         * Verifica que una solicitud preflight OPTIONS desde un origen permitido (ej. React en puerto 3000)
         * sea respondida con HTTP 200 y las cabeceras CORS requeridas, sin ser bloqueada por seguridad.
         */
        @Test
        @DisplayName("OPTIONS a ruta protegida desde localhost:3000 retorna 200 OK con cabeceras CORS")
        public void testPreflightFromAuthorizedOriginReact() {
            webTestClient.options()
                    .uri("/api/v1/shipments")
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                    .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, methods -> {
                        assertThat(methods).contains("POST");
                        assertThat(methods).contains("OPTIONS");
                    })
                    .expectHeader().value(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, headers -> {
                        assertThat(headers).contains("Authorization");
                        assertThat(headers).contains("Content-Type");
                    })
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
        }

        /**
         * Verifica que una solicitud preflight OPTIONS desde otro origen permitido (ej. Vite en puerto 5173)
         * funcione correctamente.
         */
        @Test
        @DisplayName("OPTIONS a ruta de repartidores desde localhost:5173 retorna 200 OK")
        public void testPreflightFromAuthorizedOriginVite() {
            webTestClient.options()
                    .uri("/api/v1/couriers")
                    .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173")
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        /**
         * Verifica que una solicitud preflight OPTIONS desde un origen no autorizado no reciba
         * la cabecera Access-Control-Allow-Origin o sea rechazada con 403 Forbidden.
         */
        @Test
        @DisplayName("OPTIONS desde origen no autorizado es rechazado sin cabecera Allow-Origin")
        public void testPreflightFromUnauthorizedOrigin() {
            webTestClient.options()
                    .uri("/api/v1/shipments")
                    .header(HttpHeaders.ORIGIN, "http://sitio-malicioso.com")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                    .exchange()
                    .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        }
    }

    /**
     * Grupo de pruebas enfocadas en solicitudes reales de origen cruzado (solicitudes GET/POST con cabecera Origin).
     */
    @Nested
    @DisplayName("2. Solicitudes Reales de Origen Cruzado (CORS Actual Requests)")
    public class ActualRequestTests {

        /**
         * Constructor predeterminado del grupo de pruebas de solicitudes reales.
         */
        public ActualRequestTests() {
        }

        /**
         * Verifica que una solicitud simple GET al endpoint público Actuator desde un origen autorizado
         * incluya la cabecera Access-Control-Allow-Origin y admita credenciales.
         */
        @Test
        @DisplayName("GET /actuator/health con Origin autorizado retorna cabeceras CORS")
        public void testActualGetRequestWithAuthorizedOrigin() {
            webTestClient.get()
                    .uri("/actuator/health")
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                    .exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        /**
         * Verifica que una solicitud GET desde un origen no autorizado sea rechazada con 403 Forbidden.
         */
        @Test
        @DisplayName("GET /actuator/health con Origin no autorizado es rechazada con 403 Forbidden")
        public void testActualGetRequestWithUnauthorizedOrigin() {
            webTestClient.get()
                    .uri("/actuator/health")
                    .header(HttpHeaders.ORIGIN, "http://dominio-desconocido.org")
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        }

        /**
         * Verifica que una solicitud de origen cruzado a una ruta protegida sin token reciba 401 Unauthorized
         * junto con las cabeceras CORS de origen permitido, posibilitando que el frontend procese el error.
         */
        @Test
        @DisplayName("POST a ruta protegida sin token desde localhost:3000 retorna 401 y cabeceras CORS")
        public void testActualRequestToProtectedRouteWithoutTokenPreservesCors() {
            webTestClient.post()
                    .uri("/api/v1/shipments")
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

        /**
         * Verifica que una solicitud de origen cruzado a una ruta protegida con token JWT válido
         * incluya las cabeceras CORS de respuesta apropiadas.
         */
        @Test
        @DisplayName("GET a ruta protegida con token válido desde localhost:3000 contiene cabeceras CORS")
        public void testActualRequestToProtectedRouteWithValidTokenPreservesCors() {
            String token = JwtTestHelper.generateToken(
                    UUID.randomUUID(),
                    Collections.singletonList("CLIENTE"),
                    null,
                    null
            );

            webTestClient.get()
                    .uri("/api/v1/shipments")
                    .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .exchange()
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000")
                    .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
    }

    /**
     * Grupo de pruebas para validar la carga y enlace de propiedades tipadas de CORS.
     */
    @Nested
    @DisplayName("3. Enlace y Contrato de Propiedades de CORS")
    public class CorsPropertiesBindingTests {

        /**
         * Constructor predeterminado del grupo de pruebas de propiedades.
         */
        public CorsPropertiesBindingTests() {
        }

        /**
         * Verifica que {@link CorsProperties} contenga los valores configurados en el perfil de entorno.
         */
        @Test
        @DisplayName("CorsProperties se enlaza correctamente con los orígenes y métodos configurados")
        public void testCorsPropertiesBinding() {
            assertThat(corsProperties).isNotNull();
            assertThat(corsProperties.isEnabled()).isTrue();
            assertThat(corsProperties.getAllowedOrigins())
                    .contains("http://localhost:3000", "http://localhost:5173", "http://localhost:4200");
            assertThat(corsProperties.getAllowedMethods())
                    .contains("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
            assertThat(corsProperties.getAllowedHeaders())
                    .contains("Authorization", "Content-Type", "Accept", "Origin");
            assertThat(corsProperties.getExposedHeaders())
                    .contains("Authorization", "Content-Disposition", "X-User-Id", "X-User-Roles");
            assertThat(corsProperties.isAllowCredentials()).isTrue();
            assertThat(corsProperties.getMaxAge()).isEqualTo(3600L);
        }
    }

    /**
     * Grupo de pruebas para la prevención de duplicación de cabeceras CORS aguas abajo.
     */
    @Nested
    @DisplayName("4. Prevención de Respuestas CORS Duplicadas")
    public class DuplicateCorsPreventionTests {

        /**
         * Constructor predeterminado del grupo de pruebas de deduplicación.
         */
        public DuplicateCorsPreventionTests() {
        }

        /**
         * Verifica que el Gateway tenga configurado el filtro predeterminado DedupeResponseHeader
         * para eliminar encabezados Access-Control-Allow-* duplicados provenientes de microservicios downstream.
         */
        @Test
        @DisplayName("El Gateway registra el filtro DedupeResponseHeader en default-filters")
        public void testDedupeResponseHeaderFilterConfigured() {
            assertThat(gatewayProperties.getDefaultFilters()).isNotEmpty();

            boolean hasDedupeFilter = gatewayProperties.getDefaultFilters().stream()
                    .anyMatch(filter -> filter.getName().equals("DedupeResponseHeader"));

            assertThat(hasDedupeFilter)
                    .as("Debe existir el filtro DedupeResponseHeader configurado en default-filters")
                    .isTrue();

            FilterDefinition dedupeFilter = gatewayProperties.getDefaultFilters().stream()
                    .filter(filter -> filter.getName().equals("DedupeResponseHeader"))
                    .findFirst()
                    .orElseThrow();

            assertThat(dedupeFilter.getArgs().values().toString())
                    .contains("Access-Control-Allow-Origin");
        }
    }
}
