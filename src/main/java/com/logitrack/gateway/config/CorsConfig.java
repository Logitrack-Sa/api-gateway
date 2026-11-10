package com.logitrack.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Configuraci&oacute;n reactiva de Cross-Origin Resource Sharing (CORS) a nivel del API Gateway.
 *
 * <p>Centralizar CORS en el perimetro de la arquitectura evita duplicacion de encabezados
 * {@code Access-Control-Allow-*} en los microservicios aguas abajo y garantiza una respuesta
 * homogenea ante las solicitudes preflight {@code OPTIONS} provenientes de aplicaciones web
 * y clientes frontend.</p>
 *
 * <p>Al tratarse de una aplicacion reactiva basada en Spring WebFlux, se emplea
 * {@link CorsWebFilter} en lugar de filtros Servlet clasicos de WebMVC.</p>
 *
 * <p>Las politicas se administran dinamicamente mediante {@link CorsProperties},
 * lo que permite sincronizar los origenes y metodos autorizados desde Spring Cloud
 * Config Server sin requerir compilaciones adicionales.</p>
 *
 * @author GonzaCarrillo
 * @version 1.1.0
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    private final CorsProperties corsProperties;

    /**
     * Construye la configuracionn de CORS inyectando las propiedades reactivas del Gateway.
     * @param corsProperties Propiedades externalizadas de CORS con origenes y metodos permitidos.
     */
    public CorsConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    /**
     * Define el filtro reactivo de CORS para interceptar todas las rutas procesadas por el Gateway.
     *
     * <p>Configura:</p>
     * <ul>
     *   <li>Origenes permitidos: configurables mediante {@link CorsProperties#getAllowedOrigins()},
     *       soportando patrones de dominio para entornos de desarrollo y producci&oacute;n.</li>
     *   <li>Metodos HTTP: {@code GET}, {@code POST}, {@code PUT}, {@code DELETE}, {@code PATCH}, {@code OPTIONS}.</li>
     *   <li>Encabezados permitidos: {@code Authorization}, {@code Content-Type}, {@code Accept}, entre otros.</li>
     *   <li>Encabezados expuestos: {@code Authorization}, {@code Content-Disposition}, identificadores de usuario.</li>
     *   <li>Respuestas preflight en cach&eacute; durante el tiempo configurado para minimizar la sobrecarga de red.</li>
     * </ul>
     *
     * @return Instancia configurada de {@link CorsWebFilter}.
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        if (!corsProperties.isEnabled()) {
            log.warn("Filtro reactivo CORS deshabilitado por configuración (logitrack.gateway.cors.enabled=false)");
            return new CorsWebFilter(new UrlBasedCorsConfigurationSource());
        }

        log.info("Inicializando filtro reactivo CORS perimetral. Orígenes autorizados: {}",
                corsProperties.getAllowedOrigins());

        CorsConfiguration corsConfig = new CorsConfiguration();

        // Se configuran patrones de orígenes para admitir dominios exactos y subdominios con allowCredentials
        corsConfig.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        corsConfig.setAllowedMethods(corsProperties.getAllowedMethods());
        corsConfig.setAllowedHeaders(corsProperties.getAllowedHeaders());
        corsConfig.setExposedHeaders(corsProperties.getExposedHeaders());
        corsConfig.setAllowCredentials(corsProperties.isAllowCredentials());
        corsConfig.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
