package com.logitrack.gateway.security;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Contrato de respuesta uniforme de error de seguridad emitido por el API Gateway.
 *
 * <p>Estandariza los mensajes reactivos devueltos ante fallos de autenticaci&oacute;n (HTTP 401)
 * o denegaci&oacute;n de permisos por rol (HTTP 403), respetando el formato com&uacute;n de error
 * del ecosistema LogiTrack.</p>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SecurityErrorResponse {

    private final String timestamp;
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final String path;

    /**
     * Construye una respuesta estructurada de error de seguridad perimetral.
     *
     * @param status  C&oacute;digo de estado HTTP (401 o 403).
     * @param error   Descripci&oacute;n corta del estado HTTP (ej. "Unauthorized", "Forbidden").
     * @param code    C&oacute;digo mnem&oacute;nico de negocio del error (ej. "UNAUTHORIZED", "ACCESS_DENIED").
     * @param message Detalle explicativo de la causa del rechazo.
     * @param path    Ruta de la solicitud rechazada.
     */
    public SecurityErrorResponse(int status, String error, String code, String message, String path) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.code = code;
        this.message = message;
        this.path = path;
    }

    /**
     * Obtiene el momento UTC en el que se gener&oacute; el error.
     *
     * @return Marca temporal ISO-8601.
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Obtiene el c&oacute;digo de estado HTTP num&eacute;rico.
     *
     * @return Valor num&eacute;rico del estado HTTP.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Obtiene la descripci&oacute;n est&aacute;ndar del estado HTTP.
     *
     * @return Nombre de estado HTTP.
     */
    public String getError() {
        return error;
    }

    /**
     * Obtiene el c&oacute;digo mnem&oacute;nico de negocio.
     *
     * @return C&oacute;digo de error de seguridad.
     */
    public String getCode() {
        return code;
    }

    /**
     * Obtiene el mensaje explicativo para el cliente.
     *
     * @return Mensaje de error.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Obtiene la ruta del recurso solicitado.
     *
     * @return URI de la solicitud.
     */
    public String getPath() {
        return path;
    }
}
