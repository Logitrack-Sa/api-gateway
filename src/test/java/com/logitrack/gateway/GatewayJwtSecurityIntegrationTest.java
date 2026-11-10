package com.logitrack.gateway;

import com.logitrack.gateway.security.SecurityErrorResponse;
import com.logitrack.gateway.support.JwtTestHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integraci&oacute;n reactivas para la gesti&oacute;n de seguridad, validaci&oacute;n de JWT y control de acceso RBAC.
 *
 * <p>Verifica exhaustivamente el cumplimiento de la Issue #82 ([SPRINT 3][SEGURIDAD] Gestionar JWT y roles desde el API Gateway):</p>
 * <ul>
 *   <li>Acceso libre a rutas p&uacute;blicas de autenticaci&oacute;n, Actuator, Fallback y OpenAPI.</li>
 *   <li>Rechazo con HTTP 401 Unauthorized ante solicitudes sin token, tokens malformados, firmas inv&aacute;lidas o expirados.</li>
 *   <li>Rechazo con HTTP 403 Forbidden cuando el rol del usuario autenticado no cuenta con permisos suficientes.</li>
 *   <li>Acceso autorizado para roles v&aacute;lidos de acuerdo a la matriz del Sprint 2 (ADMIN, CLIENTE, REPARTIDOR).</li>
 * </ul>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayJwtSecurityIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    private static final UUID USER_ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_CLIENTE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_REPARTIDOR_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Nested
    @DisplayName("1. Rutas Públicas (Sin Requerimiento de Autenticación)")
    class PublicRoutesTests {

        @Test
        @DisplayName("POST /api/v1/auth/login debe ser accesible públicamente sin token (no 401/403)")
        void loginEndpointMustBePublic() {
            webTestClient.post()
                    .uri("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"username\":\"admin\",\"password\":\"admin123\"}")
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));
        }

        @Test
        @DisplayName("POST /api/v1/auth/register debe ser accesible públicamente sin token (no 401/403)")
        void registerEndpointMustBePublic() {
            webTestClient.post()
                    .uri("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"username\":\"cliente\",\"password\":\"Password123!\"}")
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));
        }

        @Test
        @DisplayName("GET /api/v1/auth/jwks y /public-key deben ser accesibles públicamente")
        void jwksAndPublicKeyMustBePublic() {
            webTestClient.get()
                    .uri("/api/v1/auth/jwks")
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));

            webTestClient.get()
                    .uri("/api/v1/auth/public-key")
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));
        }

        @Test
        @DisplayName("GET /actuator/health debe responder HTTP 200 sin autenticación")
        void actuatorHealthMustBePublic() {
            webTestClient.get()
                    .uri("/actuator/health")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("GET /fallback debe responder HTTP 503 sin autenticación")
        void fallbackEndpointMustBePublic() {
            webTestClient.get()
                    .uri("/fallback")
                    .exchange()
                    .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @Nested
    @DisplayName("2. Autenticación Fallida: Token Ausente o Inválido (HTTP 401)")
    class AuthenticationRejectionTests {

        @Test
        @DisplayName("Solicitud a ruta protegida /api/v1/auth/me sin encabezado Authorization debe responder 401")
        void missingAuthHeaderMustReturn401() {
            webTestClient.get()
                    .uri("/api/v1/auth/me")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectHeader().contentType(MediaType.APPLICATION_JSON)
                    .expectBody(SecurityErrorResponse.class)
                    .value(res -> {
                        assertThat(res.getStatus()).isEqualTo(401);
                        assertThat(res.getCode()).isEqualTo("UNAUTHORIZED");
                        assertThat(res.getMessage()).contains("Encabezado de autorización ausente o inválido");
                        assertThat(res.getPath()).isEqualTo("/api/v1/auth/me");
                    });
        }

        @Test
        @DisplayName("Solicitud a ruta protegida con esquema que no sea Bearer debe responder 401")
        void nonBearerSchemeMustReturn401() {
            webTestClient.get()
                    .uri("/api/v1/users")
                    .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
        }

        @Test
        @DisplayName("Solicitud con token expirado debe responder 401 con mensaje específico")
        void expiredTokenMustReturn401() {
            String expiredToken = JwtTestHelper.generateExpiredToken(USER_CLIENTE_ID, List.of("CLIENTE"));

            webTestClient.get()
                    .uri("/api/v1/shipments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                    .jsonPath("$.message").isEqualTo("El token JWT ha expirado.");
        }

        @Test
        @DisplayName("Solicitud con token firmado con clave ajena/falsa debe responder 401 por firma inválida")
        void untrustedSignatureMustReturn401() {
            String forgedToken = JwtTestHelper.generateUntrustedToken(USER_ADMIN_ID, List.of("ADMIN"));

            webTestClient.get()
                    .uri("/api/v1/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken)
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
                    .jsonPath("$.message").value(msg -> assertThat(msg.toString()).contains("Firma criptográfica"));
        }

        @Test
        @DisplayName("Solicitud con token malformado o corrupto debe responder 401")
        void malformedTokenMustReturn401() {
            webTestClient.get()
                    .uri("/api/v1/couriers")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer token_completamente_invalido.123")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
        }
    }

    @Nested
    @DisplayName("3. Control de Acceso por Roles: Denegación de Permisos (HTTP 403)")
    class AuthorizationForbiddenTests {

        @Test
        @DisplayName("Rol CLIENTE intentando acceder a /api/v1/users debe ser rechazado con HTTP 403")
        void clienteCannotAccessUsers() {
            String clientToken = JwtTestHelper.generateToken(USER_CLIENTE_ID, List.of("CLIENTE"), UUID.randomUUID(), null);

            webTestClient.get()
                    .uri("/api/v1/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody(SecurityErrorResponse.class)
                    .value(res -> {
                        assertThat(res.getStatus()).isEqualTo(403);
                        assertThat(res.getCode()).isEqualTo("ACCESS_DENIED");
                        assertThat(res.getMessage()).contains("permisos insuficientes");
                    });
        }

        @Test
        @DisplayName("Rol CLIENTE intentando acceder al CRUD de repartidores debe ser rechazado con HTTP 403")
        void clienteCannotAccessCouriersCrud() {
            String clientToken = JwtTestHelper.generateToken(USER_CLIENTE_ID, List.of("CLIENTE"), UUID.randomUUID(), null);

            webTestClient.get()
                    .uri("/api/v1/couriers")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                    .exchange()
                    .expectStatus().isForbidden();

            webTestClient.post()
                    .uri("/api/v1/couriers")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"name\":\"Juan\"}")
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("El Gateway permite cancelar al CLIENTE; Shipment comprueba la pertenencia")
        void clienteCancellationIsForwardedToShipment() {
            String clientToken = JwtTestHelper.generateToken(USER_CLIENTE_ID, List.of("CLIENTE"), UUID.randomUUID(), null);

            webTestClient.post()
                    .uri("/api/v1/shipments/550e8400-e29b-41d4-a716-446655440000/cancellation")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                    .exchange()
                    .expectStatus().isEqualTo(503);
        }

        @Test
        @DisplayName("Rol CLIENTE intentando asignar repartidor debe ser rechazado con HTTP 403 (solo ADMIN)")
        void clienteCannotAssignCourier() {
            String clientToken = JwtTestHelper.generateToken(USER_CLIENTE_ID, List.of("CLIENTE"), UUID.randomUUID(), null);

            webTestClient.post()
                    .uri("/api/v1/shipments/550e8400-e29b-41d4-a716-446655440000/courier")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"courierId\":\"474a3531-2c9d-486f-8a65-23fc1ff82319\"}")
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("Rol REPARTIDOR intentando crear un envío debe ser rechazado con HTTP 403")
        void repartidorCannotCreateShipment() {
            String courierToken = JwtTestHelper.generateToken(USER_REPARTIDOR_ID, List.of("REPARTIDOR"), null, UUID.randomUUID());

            webTestClient.post()
                    .uri("/api/v1/shipments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + courierToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"recipientName\":\"Destinatario\"}")
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("Rol REPARTIDOR intentando acceder a usuarios o gestión de clientes debe ser rechazado con HTTP 403")
        void repartidorCannotAccessUsersOrCustomers() {
            String courierToken = JwtTestHelper.generateToken(USER_REPARTIDOR_ID, List.of("REPARTIDOR"), null, UUID.randomUUID());

            webTestClient.get()
                    .uri("/api/v1/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + courierToken)
                    .exchange()
                    .expectStatus().isForbidden();

            webTestClient.get()
                    .uri("/api/v1/customers")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + courierToken)
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    @Nested
    @DisplayName("4. Control de Acceso por Roles: Autorización Exitosa")
    class AuthorizationSuccessTests {

        @Test
        @DisplayName("Rol CLIENTE puede consultar su perfil en /api/v1/auth/me y crear envíos")
        void clienteCanAccessProfileAndCreateShipments() {
            String clientToken = JwtTestHelper.generateToken(USER_CLIENTE_ID, List.of("CLIENTE"), UUID.randomUUID(), null);

            // Debe superar el filtro de seguridad (no 401 ni 403)
            webTestClient.get()
                    .uri("/api/v1/auth/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));

            webTestClient.post()
                    .uri("/api/v1/shipments")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + clientToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"recipientName\":\"Prueba\"}")
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));
        }

        @Test
        @DisplayName("Rol REPARTIDOR puede actualizar estado de envío y consultar estado mínimo")
        void repartidorCanChangeStatusAndQueryInternalStatus() {
            String courierToken = JwtTestHelper.generateToken(USER_REPARTIDOR_ID, List.of("REPARTIDOR"), null, UUID.randomUUID());

            webTestClient.patch()
                    .uri("/api/v1/shipments/550e8400-e29b-41d4-a716-446655440000/status")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + courierToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"status\":\"EN_RUTA\"}")
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));

            webTestClient.get()
                    .uri("/api/v1/internal/couriers/474a3531-2c9d-486f-8a65-23fc1ff82319/status")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + courierToken)
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));
        }

        @Test
        @DisplayName("Rol ADMIN cuenta con acceso autorizado a todas las operaciones protegidas")
        void adminHasUniversalAccess() {
            String adminToken = JwtTestHelper.generateToken(USER_ADMIN_ID, List.of("ADMIN"), null, null);

            // Gestión de usuarios
            webTestClient.get()
                    .uri("/api/v1/users")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));

            // Gestión de repartidores
            webTestClient.get()
                    .uri("/api/v1/couriers")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));

            // Gestión de clientes
            webTestClient.get()
                    .uri("/api/v1/customers")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));

            // Asignación de repartidores
            webTestClient.post()
                    .uri("/api/v1/shipments/550e8400-e29b-41d4-a716-446655440000/courier")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue("{\"courierId\":\"474a3531-2c9d-486f-8a65-23fc1ff82319\"}")
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));

            // Cancelación de envíos
            webTestClient.post()
                    .uri("/api/v1/shipments/550e8400-e29b-41d4-a716-446655440000/cancellation")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                    .exchange()
                    .expectStatus().value(status -> assertThat(status).isNotIn(401, 403));
        }
    }
}
