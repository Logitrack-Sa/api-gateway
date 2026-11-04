package com.logitrack.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Propiedades de configuraci&oacute;n reactivas para Cross-Origin Resource Sharing (CORS)
 * en el API Gateway de LogiTrack.
 *
 * <p>Esta clase se enlaza autom&aacute;ticamente con el prefijo {@code logitrack.gateway.cors}
 * provisto por Spring Cloud Config Server o por el archivo de propiedades local del Gateway.</p>
 *
 * <p>Permite externalizar:</p>
 * <ul>
 *   <li>{@code enabled}: Interruptor global para activar o desactivar la pol&iacute;tica CORS perimetral.</li>
 *   <li>{@code allowed-origins}: Lista de or&iacute;genes web autorizados (ej. frontends en React, Vite o Angular).</li>
 *   <li>{@code allowed-methods}: M&eacute;todos HTTP admitidos (GET, POST, PUT, DELETE, PATCH, OPTIONS).</li>
 *   <li>{@code allowed-headers}: Encabezados aceptados en las solicitudes de clientes.</li>
 *   <li>{@code exposed-headers}: Encabezados que el navegador web puede leer en la respuesta HTTP.</li>
 *   <li>{@code allow-credentials}: Flag para admitir credenciales, cookies o encabezados {@code Authorization}.</li>
 *   <li>{@code max-age}: Duraci&oacute;n en segundos para el almacenamiento en cach&eacute; del preflight OPTIONS.</li>
 * </ul>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@ConfigurationProperties(prefix = "logitrack.gateway.cors")
public class CorsProperties {

    /**
     * Indica si el filtro CORS perimetral se encuentra habilitado en el Gateway.
     */
    private boolean enabled = true;

    /**
     * Lista de or&iacute;genes autorizados para realizar solicitudes de origen cruzado.
     */
    private List<String> allowedOrigins = new ArrayList<>(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:4200",
            "http://localhost:8080",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173"
    ));

    /**
     * Lista de m&eacute;todos HTTP permitidos para las solicitudes CORS.
     */
    private List<String> allowedMethods = new ArrayList<>(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    ));

    /**
     * Lista de encabezados HTTP permitidos en las solicitudes entrantes.
     */
    private List<String> allowedHeaders = new ArrayList<>(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
    ));

    /**
     * Lista de encabezados HTTP expuestos hacia las aplicaciones frontend.
     */
    private List<String> exposedHeaders = new ArrayList<>(Arrays.asList(
            "Authorization",
            "Content-Disposition",
            "X-User-Id",
            "X-User-Roles"
    ));

    /**
     * Indica si se permiten credenciales en las solicitudes de origen cruzado.
     */
    private boolean allowCredentials = true;

    /**
     * Tiempo m&aacute;ximo de almacenamiento en cach&eacute; (en segundos) para solicitudes preflight.
     */
    private long maxAge = 3600L;

    /**
     * Constructor por defecto requerido por Spring Boot para el enlace de propiedades de configuraci&oacute;n.
     */
    public CorsProperties() {
    }

    /**
     * Obtiene el estado de activaci&oacute;n de CORS.
     *
     * @return {@code true} si CORS est&aacute; habilitado, {@code false} en caso contrario.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Establece el estado de activaci&oacute;n de CORS.
     *
     * @param enabled {@code true} para habilitar CORS perimetral.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Obtiene la lista inmutable de or&iacute;genes autorizados.
     *
     * @return Lista de or&iacute;genes permitidos.
     */
    public List<String> getAllowedOrigins() {
        return Collections.unmodifiableList(allowedOrigins);
    }

    /**
     * Asigna la lista de or&iacute;genes autorizados.
     *
     * @param allowedOrigins Lista de or&iacute;genes permitidos.
     */
    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins != null ? new ArrayList<>(allowedOrigins) : new ArrayList<>();
    }

    /**
     * Obtiene la lista inmutable de m&eacute;todos HTTP autorizados.
     *
     * @return Lista de m&eacute;todos HTTP permitidos.
     */
    public List<String> getAllowedMethods() {
        return Collections.unmodifiableList(allowedMethods);
    }

    /**
     * Asigna la lista de m&eacute;todos HTTP autorizados.
     *
     * @param allowedMethods Lista de m&eacute;todos permitidos.
     */
    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods != null ? new ArrayList<>(allowedMethods) : new ArrayList<>();
    }

    /**
     * Obtiene la lista inmutable de encabezados HTTP permitidos.
     *
     * @return Lista de encabezados permitidos.
     */
    public List<String> getAllowedHeaders() {
        return Collections.unmodifiableList(allowedHeaders);
    }

    /**
     * Asigna la lista de encabezados HTTP permitidos.
     *
     * @param allowedHeaders Lista de encabezados admitidos.
     */
    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders != null ? new ArrayList<>(allowedHeaders) : new ArrayList<>();
    }

    /**
     * Obtiene la lista inmutable de encabezados expuestos a los clientes web.
     *
     * @return Lista de encabezados expuestos.
     */
    public List<String> getExposedHeaders() {
        return Collections.unmodifiableList(exposedHeaders);
    }

    /**
     * Asigna la lista de encabezados expuestos a los clientes web.
     *
     * @param exposedHeaders Lista de encabezados expuestos.
     */
    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders != null ? new ArrayList<>(exposedHeaders) : new ArrayList<>();
    }

    /**
     * Indica si las solicitudes CORS permiten credenciales (cookies, encabezados {@code Authorization}).
     *
     * @return {@code true} si las credenciales est&aacute;n permitidas.
     */
    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    /**
     * Asigna si se permiten credenciales en solicitudes CORS.
     *
     * @param allowCredentials {@code true} para admitir credenciales.
     */
    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    /**
     * Obtiene el tiempo m&aacute;ximo de almacenamiento en cach&eacute; del preflight en segundos.
     *
     * @return Tiempo de cach&eacute; en segundos.
     */
    public long getMaxAge() {
        return maxAge;
    }

    /**
     * Asigna el tiempo m&aacute;ximo de almacenamiento en cach&eacute; del preflight en segundos.
     *
     * @param maxAge Tiempo en segundos.
     */
    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }
}
