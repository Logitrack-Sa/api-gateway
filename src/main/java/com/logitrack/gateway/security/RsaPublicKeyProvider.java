package com.logitrack.gateway.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Proveedor y cargador seguro de la clave p&uacute;blica RSA (RS256) para el API Gateway.
 *
 * <p>Permite validar criptogr&aacute;ficamente la firma de los tokens emitidos por {@code security-service}
 * sin realizar llamadas HTTP remotas por cada solicitud, desacoplando los servicios y garantizando
 * alto rendimiento y resiliencia en la capa perimetral.</p>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@Component
public class RsaPublicKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(RsaPublicKeyProvider.class);

    private final JwtSecurityProperties properties;
    private final ResourceLoader resourceLoader;

    private RSAPublicKey publicKey;

    /**
     * Construye el proveedor de clave p&uacute;blica RSA inyectando las propiedades y el cargador de recursos.
     *
     * @param properties     Propiedades de seguridad JWT.
     * @param resourceLoader Cargador de recursos de Spring.
     */
    public RsaPublicKeyProvider(JwtSecurityProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Inicializa y decodifica la clave p&uacute;blica RSA al arrancar el contexto reactivo.
     */
    @PostConstruct
    public void init() {
        this.publicKey = loadPublicKey();
        log.info("[Security] Clave pública RSA cargada exitosamente para validación perimetral de JWT (RS256)");
    }

    /**
     * Obtiene la instancia de {@link RSAPublicKey} activa para verificaci&oacute;n criptogr&aacute;fica.
     *
     * @return Clave p&uacute;blica RSA cargada.
     */
    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    /**
     * Establece de forma program&aacute;tica la clave p&uacute;blica (utilizado primordialmente en pruebas unitarias).
     *
     * @param publicKey Instancia de {@link RSAPublicKey} a utilizar.
     */
    public void setPublicKey(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    /**
     * Carga el material criptogr&aacute;fico desde la ubicaci&oacute;n configurada o sus rutas de reserva.
     *
     * @return Instancia de {@link RSAPublicKey}.
     */
    private RSAPublicKey loadPublicKey() {
        String location = properties.getPublicKeyLocation();
        if (location == null || location.isBlank()) {
            throw new IllegalStateException("Configurar la clave pública RSA del emisor.");
        }
        try {
            Resource resource = resourceLoader.getResource(location);
            return parsePublicKeyFromPem(resource.getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException | GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("No se pudo cargar la clave pública RSA configurada.", ex);
        }
    }

    /**
     * Parsea y decodifica una clave p&uacute;blica RSA en formato PEM est&aacute;ndar X.509.
     *
     * @param pem Cadena en formato PEM.
     * @return Instancia de {@link RSAPublicKey}.
     * @throws GeneralSecurityException Si el formato criptogr&aacute;fico es inv&aacute;lido.
     */
    public static RSAPublicKey parsePublicKeyFromPem(String pem) throws GeneralSecurityException {
        String clean = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] decoded = Base64.getDecoder().decode(clean);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
    }
}
