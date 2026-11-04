package com.logitrack.gateway.support;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Utilidad de soporte para generaci&oacute;n y firma de tokens JWT en suites de pruebas unitarias e integraci&oacute;n.
 *
 * <p>Permite sintetizar tokens firmados con la clave privada real del ecosistema ({@code RS256}),
 * as&iacute; como tokens expirados, adulterados o firmados con claves ajenas para validaci&oacute;n
 * exhaustiva de la seguridad perimetral.</p>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
public final class JwtTestHelper {

    private static final RSAPrivateKey TEST_PRIVATE_KEY;

    static {
        try {
            ClassPathResource resource = new ClassPathResource("certs/private.pem");
            String pem = resource.getContentAsString(StandardCharsets.UTF_8);
            String clean = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = Base64.getDecoder().decode(clean);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            TEST_PRIVATE_KEY = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (IOException | GeneralSecurityException ex) {
            throw new ExceptionInInitializerError("No se pudo cargar private.pem para pruebas: " + ex.getMessage());
        }
    }

    private JwtTestHelper() {
    }

    /**
     * Genera un token JWT v&aacute;lido firmado con RS256.
     *
     * @param userId     UUID del usuario (claim sub).
     * @param roles      Lista de roles asignados (ej. "ADMIN", "CLIENTE", "REPARTIDOR").
     * @param customerId UUID opcional de cliente.
     * @param courierId  UUID opcional de repartidor.
     * @return Cadena serializada compacta del JWT.
     */
    public static String generateToken(UUID userId, List<String> roles, UUID customerId, UUID courierId) {
        return buildToken(userId, roles, customerId, courierId, 3600, "https://security-service", TEST_PRIVATE_KEY);
    }

    /**
     * Genera un token JWT expirado firmado con RS256.
     *
     * @param userId UUID del usuario.
     * @param roles  Lista de roles.
     * @return Token expirado.
     */
    public static String generateExpiredToken(UUID userId, List<String> roles) {
        return buildToken(userId, roles, null, null, -3600, "https://security-service", TEST_PRIVATE_KEY);
    }

    /**
     * Genera un token firmado con una clave privada RSA ajena/falsa para simular falsificaci&oacute;n de firma.
     *
     * @param userId UUID del usuario.
     * @param roles  Lista de roles.
     * @return Token firmado con clave espuria.
     */
    public static String generateUntrustedToken(UUID userId, List<String> roles) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair foreignKeyPair = kpg.generateKeyPair();
            return buildToken(userId, roles, null, null, 3600, "https://security-service", (RSAPrivateKey) foreignKeyPair.getPrivate());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Error al generar par de claves foráneo para test", ex);
        }
    }

    private static String buildToken(UUID userId, List<String> roles, UUID customerId, UUID courierId,
                                      long ttlSeconds, String issuer, RSAPrivateKey signingKey) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .issuer(issuer)
                .audience(List.of("security-service", "courier-service", "shipment-service"))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .claim("roles", roles);

        if (customerId != null) {
            builder.claim("customerId", customerId.toString());
        }
        if (courierId != null) {
            builder.claim("courierId", courierId.toString());
        }

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID("logitrack-main")
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT signedJwt = new SignedJWT(header, builder.build());

        try {
            RSASSASigner signer = new RSASSASigner(signingKey);
            signedJwt.sign(signer);
            return signedJwt.serialize();
        } catch (JOSEException ex) {
            throw new IllegalStateException("Error al firmar token de prueba", ex);
        }
    }
}
