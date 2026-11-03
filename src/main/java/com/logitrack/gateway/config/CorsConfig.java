package com.logitrack.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Configuraci&oacute;n reactiva de Cross-Origin Resource Sharing (CORS) a nivel del API Gateway.
 *
 * <p>Centralizar CORS en el per&iacute;metro de la arquitectura evita duplicaci&oacute;n de encabezados
 * {@code Access-Control-Allow-*} en los microservicios aguas abajo y garantiza una respuesta
 * homog&eacute;nea ante las solicitudes preflight {@code OPTIONS} provenientes de aplicaciones web
 * y clientes frontend.</p>
 *
 * <p>Al tratarse de una aplicaci&oacute;n reactiva basada en Spring WebFlux, se emplea
 * {@link CorsWebFilter} en lugar de filtros Servlet cl&aacute;sicos de WebMVC.</p>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@Configuration
public class CorsConfig {

    /**
     * Constructor predeterminado de la clase de configuraci&oacute;n de CORS.
     */
    public CorsConfig() {
    }

    /**
     * Define el filtro reactivo de CORS para interceptar todas las rutas procesadas por el Gateway.
     *
     * <p>Permite:</p>
     * <ul>
     *   <li>Or&iacute;genes permitidos: configurables, permitiendo por defecto tr&aacute;fico desde entornos web de LogiTrack.</li>
     *   <li>M&eacute;todos HTTP: {@code GET}, {@code POST}, {@code PUT}, {@code DELETE}, {@code PATCH}, {@code OPTIONS}.</li>
     *   <li>Encabezados permitidos: {@code Authorization}, {@code Content-Type}, {@code Accept}, entre otros.</li>
     *   <li>Respuestas preflight en cach&eacute; durante 3600 segundos para minimizar la sobrecarga de red.</li>
     * </ul>
     *
     * @return Instancia configurada de {@link CorsWebFilter}.
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Permite cualquier origen de forma segura o configurable por variable
        corsConfig.setAllowedOriginPatterns(Collections.singletonList("*"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        corsConfig.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        corsConfig.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
