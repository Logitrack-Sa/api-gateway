package com.logitrack.gateway;

import com.logitrack.gateway.config.GatewayRoutesConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integraci&oacute;n para el enrutamiento din&aacute;mico de Spring Cloud Gateway hacia los microservicios.
 *
 * <p>Esta clase valida exhaustivamente el cumplimiento de la Issue #81 ([SPRINT 3][GATEWAY] Implementar rutas dinámicas
 * hacia los microservicios):</p>
 * <ul>
 *   <li>Uso de nombres l&oacute;gicos registrados en Eureka mediante el balanceador de Spring Cloud ({@code lb://...}).</li>
 *   <li>Configuraci&oacute;n y cobertura de predicados de ruta para los endpoints existentes de cada microservicio.</li>
 *   <li>Orden y priorizaci&oacute;n expl&iacute;cita para evitar colisiones y conflictos de enrutamiento.</li>
 *   <li>Exposici&oacute;n y auditor&iacute;a de rutas a trav&eacute;s de los endpoints de gesti&oacute;n de Spring Boot Actuator.</li>
 * </ul>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DynamicRoutingIntegrationTest {

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Obtiene de forma reactiva la lista completa de rutas registradas en el RouteLocator.
     *
     * @return Lista de instancias {@link Route}.
     */
    private List<Route> getActiveRoutes() {
        return routeLocator.getRoutes().collectList().block();
    }

    @Nested
    @DisplayName("1. Verificación de Identificadores y Unicidad de Rutas")
    class RouteIdentityAndUniquenessTests {

        @Test
        @DisplayName("Todas las rutas configuradas deben tener identificadores únicos y no nulos")
        void allRoutesMustHaveUniqueIds() {
            List<Route> routes = getActiveRoutes();
            assertThat(routes).isNotNull().isNotEmpty();

            List<String> routeIds = routes.stream()
                    .map(Route::getId)
                    .collect(Collectors.toList());

            Set<String> uniqueIds = Set.copyOf(routeIds);
            assertThat(routeIds)
                    .as("No debe haber identificadores de ruta duplicados para evitar conflictos")
                    .hasSameSizeAs(uniqueIds);
        }

        @Test
        @DisplayName("Deben estar presentes las 6 rutas principales de microservicios y las 3 de OpenAPI")
        void allRequiredMicroserviceRoutesMustBePresent() {
            List<Route> routes = getActiveRoutes();
            Map<String, Route> routeMap = routes.stream()
                    .collect(Collectors.toMap(Route::getId, r -> r));

            assertThat(routeMap)
                    .containsKey(GatewayRoutesConfig.ROUTE_SECURITY_AUTH)
                    .containsKey(GatewayRoutesConfig.ROUTE_SECURITY_USERS)
                    .containsKey(GatewayRoutesConfig.ROUTE_COURIER_COURIERS)
                    .containsKey(GatewayRoutesConfig.ROUTE_COURIER_INTERNAL)
                    .containsKey(GatewayRoutesConfig.ROUTE_SHIPMENT_SHIPMENTS)
                    .containsKey(GatewayRoutesConfig.ROUTE_SHIPMENT_CUSTOMERS)
                    .containsKey("security-service-openapi")
                    .containsKey("courier-service-openapi")
                    .containsKey("shipment-service-openapi");
        }
    }

    @Nested
    @DisplayName("2. Resolución Dinámica hacia Nombres Lógicos de Eureka (lb://...)")
    class DynamicLoadBalancingTests {

        @Test
        @DisplayName("Las rutas de security-service deben apuntar a lb://SECURITY-SERVICE")
        void securityRoutesMustTargetEurekaLogicalName() {
            List<Route> routes = getActiveRoutes();
            Map<String, Route> routeMap = routes.stream()
                    .collect(Collectors.toMap(Route::getId, r -> r));

            URI expectedUri = URI.create(GatewayRoutesConfig.URI_SECURITY);

            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_SECURITY_AUTH).getUri())
                    .as("La ruta de autenticación debe resolverse vía balanceador hacia SECURITY-SERVICE")
                    .isEqualTo(expectedUri);

            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_SECURITY_USERS).getUri())
                    .as("La ruta de usuarios debe resolverse vía balanceador hacia SECURITY-SERVICE")
                    .isEqualTo(expectedUri);
        }

        @Test
        @DisplayName("Las rutas de courier-service deben apuntar a lb://COURIER-SERVICE")
        void courierRoutesMustTargetEurekaLogicalName() {
            List<Route> routes = getActiveRoutes();
            Map<String, Route> routeMap = routes.stream()
                    .collect(Collectors.toMap(Route::getId, r -> r));

            URI expectedUri = URI.create(GatewayRoutesConfig.URI_COURIER);

            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_COURIER_COURIERS).getUri())
                    .as("La ruta de repartidores debe resolverse vía balanceador hacia COURIER-SERVICE")
                    .isEqualTo(expectedUri);

            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_COURIER_INTERNAL).getUri())
                    .as("La ruta de estado interno debe resolverse vía balanceador hacia COURIER-SERVICE")
                    .isEqualTo(expectedUri);
        }

        @Test
        @DisplayName("Las rutas de shipment-service deben apuntar a lb://SHIPMENT-SERVICE")
        void shipmentRoutesMustTargetEurekaLogicalName() {
            List<Route> routes = getActiveRoutes();
            Map<String, Route> routeMap = routes.stream()
                    .collect(Collectors.toMap(Route::getId, r -> r));

            URI expectedUri = URI.create(GatewayRoutesConfig.URI_SHIPMENT);

            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_SHIPMENT_SHIPMENTS).getUri())
                    .as("La ruta de envíos debe resolverse vía balanceador hacia SHIPMENT-SERVICE")
                    .isEqualTo(expectedUri);

            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_SHIPMENT_CUSTOMERS).getUri())
                    .as("La ruta de clientes debe resolverse vía balanceador hacia SHIPMENT-SERVICE")
                    .isEqualTo(expectedUri);
        }

        @Test
        @DisplayName("Ninguna ruta hacia microservicios debe depender de direcciones IP o puertos fijos")
        void noMicroserviceRouteShouldUseFixedIpOrHost() {
            List<Route> routes = getActiveRoutes();

            List<Route> microserviceRoutes = routes.stream()
                    .filter(r -> !r.getId().equals("mock-test-route"))
                    .collect(Collectors.toList());

            for (Route route : microserviceRoutes) {
                assertThat(route.getUri().getScheme())
                        .as("La ruta '%s' debe utilizar el esquema 'lb' para resolución dinámica por Eureka", route.getId())
                        .isEqualTo("lb");

                assertThat(route.getUri().getHost())
                        .as("El host de la ruta '%s' no debe ser una dirección IP numérica", route.getId())
                        .doesNotMatch("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
            }
        }
    }

    @Nested
    @DisplayName("3. Prevención de Conflictos y Priorización de Rutas")
    class RouteConflictPreventionTests {

        @Test
        @DisplayName("Las rutas de repartidores deben tener precedencia explícita sobre envíos")
        void courierRoutesMustPrecedeShipmentRoutes() {
            List<Route> routes = getActiveRoutes();
            Map<String, Route> routeMap = routes.stream()
                    .collect(Collectors.toMap(Route::getId, r -> r));

            Route courierRoute = routeMap.get(GatewayRoutesConfig.ROUTE_COURIER_COURIERS);
            Route shipmentRoute = routeMap.get(GatewayRoutesConfig.ROUTE_SHIPMENT_SHIPMENTS);

            assertThat(courierRoute.getOrder())
                    .as("La ruta de couriers (orden %d) debe tener mayor prioridad (menor valor numérico) que shipments (orden %d)",
                            courierRoute.getOrder(), shipmentRoute.getOrder())
                    .isLessThan(shipmentRoute.getOrder());
        }

        @Test
        @DisplayName("Las rutas deben mantener un orden estrictamente creciente según su dominio funcional")
        void routesMustMaintainAscendingPriorityOrder() {
            List<Route> routes = getActiveRoutes();
            Map<String, Route> routeMap = routes.stream()
                    .collect(Collectors.toMap(Route::getId, r -> r));

            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_SECURITY_AUTH).getOrder())
                    .isEqualTo(100);
            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_SECURITY_USERS).getOrder())
                    .isEqualTo(110);
            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_COURIER_COURIERS).getOrder())
                    .isEqualTo(200);
            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_COURIER_INTERNAL).getOrder())
                    .isEqualTo(210);
            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_SHIPMENT_SHIPMENTS).getOrder())
                    .isEqualTo(300);
            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_SHIPMENT_CUSTOMERS).getOrder())
                    .isEqualTo(310);
        }
    }

    @Nested
    @DisplayName("4. Predicados y Endpoints Existentes de la Plataforma")
    class ExistingEndpointsPredicatesTests {

        @Test
        @DisplayName("Los predicados deben capturar la convención de endpoints existente /api/v1/...")
        void predicatesMustMatchExistingVersionedEndpoints() {
            List<Route> routes = getActiveRoutes();
            Map<String, Route> routeMap = routes.stream()
                    .collect(Collectors.toMap(Route::getId, r -> r));

            // Verificamos que el predicado de cada ruta contenga el patrón /api/v1/...
            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_SECURITY_AUTH).getPredicate().toString())
                    .contains("/api/v1/auth/**");

            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_SECURITY_USERS).getPredicate().toString())
                    .contains("/api/v1/users/**");

            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_COURIER_COURIERS).getPredicate().toString())
                    .contains("/api/v1/couriers/**");

            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_COURIER_INTERNAL).getPredicate().toString())
                    .contains("/api/v1/internal/couriers/**");

            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_SHIPMENT_SHIPMENTS).getPredicate().toString())
                    .contains("/api/v1/shipments/**");

            assertThat(routeMap.get(GatewayRoutesConfig.ROUTE_SHIPMENT_CUSTOMERS).getPredicate().toString())
                    .contains("/api/v1/customers/**");
        }
    }

    @Nested
    @DisplayName("5. Consulta de Rutas Dinámicas a través de Spring Boot Actuator")
    class ActuatorGatewayEndpointTests {

        @Test
        @DisplayName("El endpoint /actuator/gateway/routes debe responder exitosamente exponiendo las rutas activas")
        void testActuatorGatewayRoutesEndpoint() {
            webTestClient.get()
                    .uri("/actuator/gateway/routes")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$").isArray()
                    .jsonPath("$[?(@.route_id == 'security-service-auth')].uri")
                    .isEqualTo(List.of(GatewayRoutesConfig.URI_SECURITY))
                    .jsonPath("$[?(@.route_id == 'courier-service-couriers')].uri")
                    .isEqualTo(List.of(GatewayRoutesConfig.URI_COURIER))
                    .jsonPath("$[?(@.route_id == 'shipment-service-shipments')].uri")
                    .isEqualTo(List.of(GatewayRoutesConfig.URI_SHIPMENT));
        }
    }
}
