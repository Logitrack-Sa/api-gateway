package com.logitrack.gateway.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Validador criptogr&aacute;fico y extractor de identidad de tokens JWT para Spring Cloud Gateway.
 *
 * <p>Verifica que los tokens cumplan estrictamente con las especificaciones de LogiTrack:</p>
 * <ul>
 *   <li>Algoritmo de firma digital asim&eacute;trica {@code RS256}.</li>
 *   <li>Verificaci&oacute;n criptogr&aacute;fica con la clave p&uacute;blica RSA oficial de LogiTrack.</li>
 *   <li>No haber expirado (considerando tolerancia a clock skew).</li>
 *   <li>Existencia de un claim {@code sub} v&aacute;lido en formato UUID.</li>
 *   <li>Extracci&oacute;n confiable de la lista de roles ({@code ADMIN}, {@code CLIENTE}, {@code REPARTIDOR}).</li>
 * </ul>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 */
@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final RsaPublicKeyProvider keyProvider;
    private final JwtSecurityProperties properties;

    /**
     * Construye el validador de tokens JWT inyectando el proveedor de clave y propiedades.
     *
     * @param keyProvider Proveedor de clave p&uacute;blica RSA.
     * @param properties  Propiedades de seguridad JWT.
     */
    public JwtTokenValidator(RsaPublicKeyProvider keyProvider, JwtSecurityProperties properties) {
        this.keyProvider = keyProvider;
        this.properties = properties;
    }

    /**
     * Valida la firma, vigencia y estructura de un token JWT compacto y retorna la identidad autenticada.
     *
     * @param token Cadena compacta del JWT recibida en el encabezado {@code Authorization: Bearer <token>}.
     * @return Instancia de {@link AuthenticatedUser} con los datos de identidad y roles extra&iacute;dos.
     * @throws IllegalArgumentException Si el token es nulo, inv&aacute;lido, manipulado, corrupto o ha expirado.
     */
    public AuthenticatedUser validateAndExtractUser(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("El token JWT no puede ser nulo o vacío.");
        }

        try {
            SignedJWT signedJwt = SignedJWT.parse(token);

            // 1. Validar Algoritmo RS256
            if (!JWSAlgorithm.RS256.equals(signedJwt.getHeader().getAlgorithm())) {
                throw new IllegalArgumentException("Algoritmo de token no permitido: " + signedJwt.getHeader().getAlgorithm());
            }

            // 2. Verificar Firma Criptográfica con clave pública RSA
            if (!properties.getKeyId().equals(signedJwt.getHeader().getKeyID())) {
                throw new IllegalArgumentException("Identificador de clave JWT no confiable.");
            }
            RSASSAVerifier verifier = new RSASSAVerifier(keyProvider.getPublicKey());
            if (!signedJwt.verify(verifier)) {
                throw new IllegalArgumentException("Firma criptográfica del token inválida o adulterada.");
            }

            JWTClaimsSet claimsSet = signedJwt.getJWTClaimsSet();
            Instant now = Instant.now();
            long clockSkew = properties.getClockSkewSeconds();

            // 3. Validar Expiración
            Date expirationTime = claimsSet.getExpirationTime();
            if (expirationTime == null) {
                throw new IllegalArgumentException("El token JWT requiere fecha de expiración.");
            } else {
                Instant expInstant = expirationTime.toInstant();
                if (now.isAfter(expInstant.plusSeconds(clockSkew))) {
                    throw new IllegalArgumentException("El token JWT ha expirado.");
                }
            }

            if (claimsSet.getNotBeforeTime() != null &&
                    claimsSet.getNotBeforeTime().toInstant().isAfter(now.plusSeconds(clockSkew))) {
                throw new IllegalArgumentException("El token JWT todavía no es válido.");
            }
            if (claimsSet.getAudience().stream().noneMatch(properties.getAudiences()::contains)) {
                throw new IllegalArgumentException("Audiencia del token JWT no permitida.");
            }

            // 4. Validar Fecha de Emisión (No emisión en el futuro)
            Date issueTime = claimsSet.getIssueTime();
            if (issueTime != null) {
                Instant iatInstant = issueTime.toInstant();
                if (iatInstant.isAfter(now.plusSeconds(clockSkew))) {
                    throw new IllegalArgumentException("Fecha de emisión del token no válida (emitido en el futuro).");
                }
            }

            // 5. Validar Emisor (si está configurado)
            if (properties.getIssuer() != null && !properties.getIssuer().isBlank()) {
                String issuer = claimsSet.getIssuer();
                if (!properties.getIssuer().equals(issuer)) {
                    throw new IllegalArgumentException("Emisor de token no confiable: " + issuer);
                }
            }

            // 6. Validar Subject (sub debe ser UUID)
            String sub = claimsSet.getSubject();
            if (sub == null || sub.isBlank()) {
                throw new IllegalArgumentException("El claim 'sub' es requerido en el token JWT.");
            }
            UUID userId;
            try {
                userId = UUID.fromString(sub);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("El claim 'sub' debe ser un UUID válido.", ex);
            }

            // 7. Extraer Roles
            Set<String> roles = extractRoles(claimsSet);

            // 8. Extraer claims de dominio opcionales
            UUID customerId = parseOptionalUuidClaim(claimsSet, "customerId");
            UUID courierId = parseOptionalUuidClaim(claimsSet, "courierId");

            return new AuthenticatedUser(userId, roles, customerId, courierId);

        } catch (ParseException | JOSEException ex) {
            log.warn("[Security] Error al procesar token JWT: {}", ex.getMessage());
            throw new IllegalArgumentException("Token JWT con formato o codificación inválida.", ex);
        }
    }

    /**
     * Extrae el conjunto de roles del claim {@code roles}.
     *
     * @param claimsSet Conjunto de claims del JWT.
     * @return Conjunto inmutable de nombres de roles normalizados.
     */
    private Set<String> extractRoles(JWTClaimsSet claimsSet) {
        Object rolesObj = claimsSet.getClaim("roles");
        if (rolesObj instanceof List<?> list) {
            Set<String> set = new HashSet<>();
            for (Object item : list) {
                if (item instanceof String roleName) {
                    set.add(roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName);
                }
            }
            return Collections.unmodifiableSet(set);
        }
        return Collections.emptySet();
    }

    /**
     * Parsea un claim opcional como UUID.
     *
     * @param claimsSet Conjunto de claims del token.
     * @param claimName Nombre del claim.
     * @return Instancia de {@link UUID} o {@code null} si no est&aacute; presente.
     */
    private UUID parseOptionalUuidClaim(JWTClaimsSet claimsSet, String claimName) {
        try {
            String val = claimsSet.getStringClaim(claimName);
            if (val != null && !val.isBlank()) {
                return UUID.fromString(val);
            }
        } catch (ParseException | IllegalArgumentException ex) {
            log.debug("[Security] Claim {} con valor no-UUID: {}", claimName, ex.getMessage());
        }
        return null;
    }
}
