package com.logitrack.gateway.security;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

/**
 * Gestor de reglas de autorizaci&oacute;n por roles (RBAC) y clasificaci&oacute;n de rutas p&uacute;blicas y protegidas.
 *
 * <p>Centraliza la matriz de control de acceso del ecosistema LogiTrack definida en el Sprint 2:</p>
 * <ul>
 *   <li><b>P&uacute;blicas:</b> Autenticaci&oacute;n ({@code /api/v1/auth/register}, {@code /api/v1/auth/login},
 *       {@code /api/v1/auth/jwks}, {@code /api/v1/auth/public-key}), Actuator, contingencias (fallback) y OpenAPI.</li>
 *   <li><b>Exclusivas ADMIN:</b> Gesti&oacute;n de usuarios e identidades ({@code /api/v1/users/**}),
 *       gesti&oacute;n y CRUD de repartidores ({@code /api/v1/couriers/**}), gesti&oacute;n de clientes
 *       ({@code /api/v1/customers/**}), asignaci&oacute;n de repartidores y cancelaci&oacute;n de env&iacute;os.</li>
 *   <li><b>Permitidas para CLIENTE:</b> Creaci&oacute;n de env&iacute;os ({@code POST /api/v1/shipments}) y consulta de env&iacute;os.</li>
 *   <li><b>Permitidas para REPARTIDOR:</b> Transiciones de estado de env&iacute;os ({@code PATCH /api/v1/shipments/{id}/status}),
 *       consulta de estado m&iacute;nimo de repartidor ({@code GET /api/v1/internal/couriers/**}) y consulta de env&iacute;os.</li>
 * </ul>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@Component
public class SecurityPathMatcher {

    /**
     * Constructor predeterminado del clasificador y evaluador de rutas de seguridad.
     */
    public SecurityPathMatcher() {
    }

    /**
     * Determina si la ruta y m&eacute;todo HTTP solicitados corresponden a un endpoint p&uacute;blico.
     *
     * @param path   Ruta de la solicitud HTTP (ej. {@code /api/v1/auth/login}).
     * @param method M&eacute;todo HTTP de la solicitud.
     * @return {@code true} si la ruta es p&uacute;blica y no requiere encabezado de autorizaci&oacute;n.
     */
    public boolean isPublic(String path, HttpMethod method) {
        if (path == null) {
            return false;
        }

        // 1. Solicitudes de preflight CORS (OPTIONS)
        if (HttpMethod.OPTIONS.equals(method)) {
            return true;
        }

        // 2. Endpoints de autenticación, verificación y claves públicas
        if (HttpMethod.POST.equals(method) &&
                (path.equals("/api/v1/auth/login") || path.equals("/api/auth/login") ||
                 path.equals("/api/v1/auth/register") || path.equals("/api/auth/register") ||
                 path.startsWith("/api/v1/auth/verify-email") || path.startsWith("/api/auth/verify-email") ||
                 path.equals("/api/v1/auth/resend-verification") || path.equals("/api/auth/resend-verification"))) {
            return true;
        }

        if (HttpMethod.GET.equals(method) &&
                (path.equals("/api/v1/auth/jwks") || path.equals("/api/auth/jwks") ||
                 path.equals("/api/v1/auth/public-key") || path.equals("/api/auth/public-key"))) {
            return true;
        }

        // 3. Endpoints de salud y métricas de Actuator
        if (path.startsWith("/actuator/") || path.equals("/actuator")) {
            return true;
        }

        // 4. Endpoints de degradación elegante (Fallback)
        if (path.startsWith("/fallback/") || path.equals("/fallback")) {
            return true;
        }

        // 5. Documentación OpenAPI y Swagger UI
        return path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.equals("/swagger-ui.html") ||
                path.startsWith("/webjars/");
    }

    /**
     * Eval&uacute;a si el usuario autenticado cuenta con los roles y permisos necesarios para acceder a la ruta.
     *
     * @param user   Usuario autenticado y verificado criptogr&aacute;ficamente.
     * @param path   Ruta de la solicitud HTTP.
     * @param method M&eacute;todo HTTP de la solicitud.
     * @return {@code true} si el usuario est&aacute; autorizado; {@code false} si debe denegarse con HTTP 403 Forbidden.
     */
    public boolean isAuthorized(AuthenticatedUser user, String path, HttpMethod method) {
        if (user == null || path == null) {
            return false;
        }

        // El rol ADMIN posee acceso irrestricto a todas las rutas protegidas
        if (user.isAdmin()) {
            return true;
        }

        // 1. Perfil propio del usuario autenticado (ADMIN, CLIENTE, REPARTIDOR)
        if (path.equals("/api/v1/auth/me") || path.equals("/api/auth/me")) {
            return true;
        }

        // 2. Endpoints de administración de identidades y usuarios: Exclusivo ADMIN
        if (path.startsWith("/api/v1/users") || path.startsWith("/api/users")) {
            return false;
        }

        // 3. Endpoints de clientes: Exclusivo ADMIN
        if (path.startsWith("/api/v1/customers") || path.startsWith("/api/customers")) {
            return false;
        }

        // 4. Endpoints de repartidores (courier-service)
        if (path.startsWith("/api/v1/internal/couriers") || path.startsWith("/api/internal/couriers")) {
            // Estado mínimo interno: ADMIN o REPARTIDOR
            return user.isRepartidor();
        }
        if (path.startsWith("/api/v1/couriers") || path.startsWith("/api/couriers")) {
            // CRUD de repartidores: Exclusivo ADMIN (CLIENTE y REPARTIDOR denegados)
            return false;
        }

        // 5. Endpoints de envíos (shipment-service)
        if (path.startsWith("/api/v1/shipments") || path.startsWith("/api/shipments")) {
            // Asignación de repartidor: Exclusivo ADMIN
            if (path.endsWith("/courier") && HttpMethod.POST.equals(method)) {
                return false;
            }

            // Cancelación de envío: Exclusivo ADMIN
            if (path.endsWith("/cancellation") && HttpMethod.POST.equals(method)) {
                return false;
            }

            // Cambio de estado: ADMIN o REPARTIDOR (CLIENTE denegado)
            if (path.endsWith("/status") && HttpMethod.PATCH.equals(method)) {
                return user.isRepartidor();
            }

            // Creación de envío: ADMIN o CLIENTE (REPARTIDOR denegado)
            if (HttpMethod.POST.equals(method) &&
                    (path.equals("/api/v1/shipments") || path.equals("/api/shipments") ||
                     path.equals("/api/v1/shipments/") || path.equals("/api/shipments/"))) {
                return user.isCliente();
            }

            // Consulta, tracking e historial de envíos (GET): Permitido a usuarios autenticados
            if (HttpMethod.GET.equals(method)) {
                return true;
            }
        }

        // 6. Asistente Conversacional IA (chatbot-service): Permitido a todos los usuarios autenticados
        if (path.startsWith("/api/v1/chat") || path.startsWith("/api/chat")) {
            return true;
        }

        // Por política de seguridad por defecto: si no coincide con permisos conocidos, se deniega
        return false;
    }
}
