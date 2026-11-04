package com.logitrack.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de configuraci&oacute;n de seguridad y validaci&oacute;n de tokens JWT para el API Gateway.
 *
 * <p>Permite parametrizar de forma din&aacute;mica mediante variables de entorno o archivos de
 * propiedades la ubicaci&oacute;n de la clave p&uacute;blica RSA (formato PEM), el emisor confiable
 * y la tolerancia temporal de reloj (clock skew).</p>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@Component
@ConfigurationProperties(prefix = "logitrack.security.jwt")
public class JwtSecurityProperties {

    /**
     * Ubicaci&oacute;n del archivo PEM con la clave p&uacute;blica RSA (X.509).
     * Soporta prefijos {@code classpath:} y {@code file:}.
     */
    private String publicKeyLocation = "classpath:certs/public.pem";

    /**
     * Emisor v&aacute;lido de los tokens JWT emitidos por {@code security-service}.
     */
    private String issuer = "https://security-service";

    /**
     * Clave identificadora del par criptogr&aacute;fico (kid) esperada en el encabezado JWT.
     */
    private String keyId = "logitrack-main";

    /**
     * Tolerancia temporal en segundos para discrepancias de reloj entre servidores (clock skew).
     */
    private long clockSkewSeconds = 30;

    /**
     * Constructor predeterminado de las propiedades de seguridad JWT.
     */
    public JwtSecurityProperties() {
    }

    /**
     * Obtiene la ubicaci&oacute;n de la clave p&uacute;blica RSA.
     *
     * @return Ruta del recurso de la clave p&uacute;blica.
     */
    public String getPublicKeyLocation() {
        return publicKeyLocation;
    }

    /**
     * Establece la ubicaci&oacute;n de la clave p&uacute;blica RSA.
     *
     * @param publicKeyLocation Ruta del recurso de la clave p&uacute;blica.
     */
    public void setPublicKeyLocation(String publicKeyLocation) {
        this.publicKeyLocation = publicKeyLocation;
    }

    /**
     * Obtiene el emisor confiable de tokens JWT.
     *
     * @return Cadena que identifica al emisor esperado.
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Establece el emisor confiable de tokens JWT.
     *
     * @param issuer Identificador del emisor esperado.
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * Obtiene el identificador de clave (kid) esperado.
     *
     * @return Identificador de clave.
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * Establece el identificador de clave (kid) esperado.
     *
     * @param keyId Identificador de clave.
     */
    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    /**
     * Obtiene la tolerancia temporal de reloj en segundos.
     *
     * @return N&uacute;mero de segundos de tolerancia.
     */
    public long getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    /**
     * Establece la tolerancia temporal de reloj en segundos.
     *
     * @param clockSkewSeconds N&uacute;mero de segundos de tolerancia.
     */
    public void setClockSkewSeconds(long clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }
}
