package com.logitrack.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuraci&oacute;n y registro complementario de rutas para Spring Cloud Gateway.
 *
 * <p>El API Gateway soporta la definici&oacute;n declarativa de rutas mediante descriptores
 * en archivos YAML/Properties (alimentados din&aacute;micamente por Config Server), as&iacute; como
 * la construcci&oacute;n program&aacute;tica a trav&eacute;s de beans {@link RouteLocator}.</p>
 *
 * <p>Esta clase centraliza la inspecci&oacute;n y trazabilidad de las rutas cargadas en el Gateway
 * al iniciar el ciclo de vida del contenedor de Spring.</p>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@Configuration
public class GatewayRoutesConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutesConfig.class);

    /**
     * Registra un ejecutor que imprime en el log el diagn&oacute;stico de rutas activas
     * registradas en el {@link RouteLocator} una vez que el contexto reactivo est&aacute; listo.
     *
     * @param routeLocator El localizador de rutas reactivo inyectado por Spring Cloud Gateway.
     * @return Una instancia de {@link CommandLineRunner} lista para ejecutarse al arranque.
     */
    @Bean
    public CommandLineRunner logActiveRoutes(RouteLocator routeLocator) {
        return args -> routeLocator.getRoutes()
                .subscribe(route -> log.info("[Gateway Route] ID: {} | URI: {} | Predicates: {}",
                        route.getId(),
                        route.getUri(),
                        route.getPredicate()));
    }
}
