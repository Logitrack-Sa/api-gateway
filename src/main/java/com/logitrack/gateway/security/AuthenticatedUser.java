package com.logitrack.gateway.security;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Representa la identidad inmutable y los privilegios de un usuario autenticado mediante JWT en el API Gateway.
 *
 * <p>Encapsula el identificador de usuario ({@code sub}), el conjunto de roles autorizados
 * ({@code ADMIN}, {@code CLIENTE}, {@code REPARTIDOR}) y los posibles identificadores de dominio
 * vinculados ({@code customerId}, {@code courierId}).</p>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 */
public final class AuthenticatedUser {

    private final UUID userId;
    private final Set<String> roles;
    private final UUID customerId;
    private final UUID courierId;

    /**
     * Construye una instancia inmutable de un usuario autenticado.
     *
     * @param userId     Identificador UUID del sujeto (sub).
     * @param roles      Conjunto de roles asignados al usuario.
     * @param customerId Identificador UUID opcional de cliente asociado.
     * @param courierId  Identificador UUID opcional de repartidor asociado.
     */
    public AuthenticatedUser(UUID userId, Set<String> roles, UUID customerId, UUID courierId) {
        this.userId = Objects.requireNonNull(userId, "El userId no puede ser nulo");
        this.roles = roles == null ? Collections.emptySet() : Collections.unmodifiableSet(roles);
        this.customerId = customerId;
        this.courierId = courierId;
    }

    /**
     * Obtiene el identificador UUID del usuario (claim {@code sub}).
     *
     * @return UUID del usuario.
     */
    public UUID getUserId() {
        return userId;
    }

    /**
     * Obtiene el conjunto inmutable de roles del usuario.
     *
     * @return Conjunto de nombres de rol (ej. {@code ADMIN}, {@code CLIENTE}, {@code REPARTIDOR}).
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Obtiene el identificador de cliente vinculado si existe.
     *
     * @return UUID de cliente o {@code null}.
     */
    public UUID getCustomerId() {
        return customerId;
    }

    /**
     * Obtiene el identificador de repartidor vinculado si existe.
     *
     * @return UUID de repartidor o {@code null}.
     */
    public UUID getCourierId() {
        return courierId;
    }

    /**
     * Comprueba si el usuario posee un rol espec&iacute;fico (soporta nombres con o sin prefijo {@code ROLE_}).
     *
     * @param role Nombre del rol a evaluar.
     * @return {@code true} si el usuario posee el rol; {@code false} en caso contrario.
     */
    public boolean hasRole(String role) {
        if (role == null) {
            return false;
        }
        String cleanRole = role.startsWith("ROLE_") ? role.substring(5) : role;
        return roles.contains(cleanRole) || roles.contains("ROLE_" + cleanRole);
    }

    /**
     * Comprueba si el usuario posee al menos uno de los roles provistos.
     *
     * @param expectedRoles Lista de roles a verificar.
     * @return {@code true} si posee al menos un rol coincidente.
     */
    public boolean hasAnyRole(String... expectedRoles) {
        if (expectedRoles == null) {
            return false;
        }
        for (String role : expectedRoles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Comprueba si el usuario cuenta con rol de Administrador.
     *
     * @return {@code true} si cuenta con rol {@code ADMIN}.
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Comprueba si el usuario cuenta con rol de Cliente.
     *
     * @return {@code true} si cuenta con rol {@code CLIENTE}.
     */
    public boolean isCliente() {
        return hasRole("CLIENTE");
    }

    /**
     * Comprueba si el usuario cuenta con rol de Repartidor.
     *
     * @return {@code true} si cuenta con rol {@code REPARTIDOR}.
     */
    public boolean isRepartidor() {
        return hasRole("REPARTIDOR");
    }

    @Override
    public String toString() {
        return "AuthenticatedUser{" +
                "userId=" + userId +
                ", roles=" + roles +
                ", customerId=" + customerId +
                ", courierId=" + courierId +
                '}';
    }
}
