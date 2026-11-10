package com.logitrack.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuraci&oacute;n y centralizaci&oacute;n del enrutamiento din&aacute;mico para Spring Cloud Gateway.
 *
 * <p>Esta clase define las convenciones estructurales, identificadores y nombres l&oacute;gicos
 * de los microservicios downstream registrados en Eureka Server, posibilitando la resoluci&oacute;n
 * din&aacute;mica de instancias mediante el balanceador de carga del lado del cliente
 * ({@code Spring Cloud LoadBalancer}) a trav&eacute;s del prefijo de esquema {@code lb://}.</p>
 *
 * <h2>Microservicios de Destino y Nombres L&oacute;gicos en Eureka:</h2>
 * <ul>
 *   <li><b>Security Service:</b> {@code SECURITY-SERVICE} &rarr; Rutas {@code /api/v1/auth/**} y {@code /api/v1/users/**}.</li>
 *   <li><b>Courier Service:</b> {@code COURIER-SERVICE} &rarr; Rutas {@code /api/v1/couriers/**} e {@code /api/v1/internal/couriers/**}.</li>
 *   <li><b>Shipment Service:</b> {@code SHIPMENT-SERVICE} &rarr; Rutas {@code /api/v1/shipments/**} y {@code /api/v1/customers/**}.</li>
 * </ul>
 *
 * <h2>Garant&iacute;a de No Conflictos:</h2>
 * <p>Se asignan prioridades expl&iacute;citas (orden num&eacute;rico ascendente) y predicados de ruta
 * disjuntos para garantizar que las peticiones se despachen al microservicio adecuado sin
 * ambig&uuml;edades ni solapamientos topol&oacute;gicos.</p>
 *
 * @author GonzaCarrillo
 * @version 1.1.0
 */
@Configuration
public class GatewayRoutesConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutesConfig.class);

    /**
     * Constructor predeterminado de la clase de configuraci&oacute;n de rutas din&aacute;micas.
     */
    public GatewayRoutesConfig() {
    }

    /**
     * Nombre l&oacute;gico del microservicio de autenticaci&oacute;n y usuarios registrado en Eureka.
     */
    public static final String SERVICE_SECURITY = "SECURITY-SERVICE";

    /**
     * URI din&aacute;mica balanceada hacia el microservicio de autenticaci&oacute;n y usuarios.
     */
    public static final String URI_SECURITY = "lb://" + SERVICE_SECURITY;

    /**
     * Nombre l&oacute;gico del microservicio de repartidores registrado en Eureka.
     */
    public static final String SERVICE_COURIER = "COURIER-SERVICE";

    /**
     * URI din&aacute;mica balanceada hacia el microservicio de repartidores.
     */
    public static final String URI_COURIER = "lb://" + SERVICE_COURIER;

    /**
     * Nombre l&oacute;gico del microservicio de env&iacute;os y clientes registrado en Eureka.
     */
    public static final String SERVICE_SHIPMENT = "SHIPMENT-SERVICE";

    /**
     * URI din&aacute;mica balanceada hacia el microservicio de env&iacute;os y clientes.
     */
    public static final String URI_SHIPMENT = "lb://" + SERVICE_SHIPMENT;

    /**
     * Nombre l&oacute;gico del microservicio de chatbot ag&eacute;ntico registrado en Eureka.
     */
    public static final String SERVICE_CHATBOT = "CHATBOT-SERVICE";

    /**
     * URI din&aacute;mica balanceada hacia el microservicio de chatbot ag&eacute;ntico.
     */
    public static final String URI_CHATBOT = "lb://" + SERVICE_CHATBOT;

    /**
     * Identificador de la ruta de autenticaci&oacute;n p&uacute;blica y credenciales.
     */
    public static final String ROUTE_SECURITY_AUTH = "security-service-auth";

    /**
     * Identificador de la ruta de gesti&oacute;n de identidades y usuarios administrados.
     */
    public static final String ROUTE_SECURITY_USERS = "security-service-users";

    /**
     * Identificador de la ruta de administraci&oacute;n y consulta de repartidores.
     */
    public static final String ROUTE_COURIER_COURIERS = "courier-service-couriers";

    /**
     * Identificador de la ruta interna de estado operativo m&iacute;nimo de repartidores.
     */
    public static final String ROUTE_COURIER_INTERNAL = "courier-service-internal";

    /**
     * Identificador de la ruta de creaci&oacute;n, consulta y transiciones de env&iacute;os.
     */
    public static final String ROUTE_SHIPMENT_SHIPMENTS = "shipment-service-shipments";

    /**
     * Identificador de la ruta de gesti&oacute;n y consulta de clientes.
     */
    public static final String ROUTE_SHIPMENT_CUSTOMERS = "shipment-service-customers";

    /**
     * Identificador de la ruta de asistente conversacional chatbot.
     */
    public static final String ROUTE_CHATBOT_CHAT = "chatbot-service-chat";

    /**
     * Registra un ejecutor de diagn&oacute;stico que audita en los registros del sistema
     * las rutas activas cargadas en el {@link RouteLocator} una vez inicializado el contexto reactivo.
     *
     * @param routeLocator El localizador de rutas reactivo prove&iacute;do por Spring Cloud Gateway.
     * @return Una instancia de {@link CommandLineRunner} lista para ejecutarse al arranque.
     */
    @Bean
    public CommandLineRunner logActiveRoutes(RouteLocator routeLocator) {
        return args -> routeLocator.getRoutes()
                .subscribe(route -> log.info("[Gateway Dynamic Route] ID: {} | URI: {} | Predicates: {} | Order: {}",
                        route.getId(),
                        route.getUri(),
                        route.getPredicate(),
                        route.getOrder()));
    }
}
