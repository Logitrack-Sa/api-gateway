package com.logitrack.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controlador reactivo de degradaci&oacute;n elegante (Fallback) para el API Gateway.
 *
 * <p>Proporciona respuestas estandarizadas en formato JSON cuando un microservicio downstream
 * (como {@code shipment-service}, {@code courier-service} o {@code security-service})
 * experimenta demoras, desconexi&oacute;n o fallos de red transitorios.</p>
 *
 * <p>Al emplear el modelo reactivo de WebFlux, todos los m&eacute;todos retornan instancias de
 * {@link Mono} para preservar la ejecuci&oacute;n no bloqueante.</p>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /**
     * Constructor predeterminado del controlador reactivo de fallback.
     */
    public FallbackController() {
    }

    /**
     * Respuesta de contingencia predeterminada cuando un servicio general no est&aacute; disponible.
     *
     * @return {@link Mono} con {@link ResponseEntity} HTTP 503 y detalle de contingencia.
     */
    @GetMapping
    public Mono<ResponseEntity<Map<String, Object>>> defaultFallback() {
        return buildFallbackResponse("El servicio solicitado no se encuentra disponible temporalmente.");
    }

    /**
     * Respuesta de contingencia cuando {@code shipment-service} no responde a tiempo.
     *
     * @return {@link Mono} con respuesta de error 503 para gesti&oacute;n de env&iacute;os.
     */
    @GetMapping("/shipments")
    public Mono<ResponseEntity<Map<String, Object>>> shipmentsFallback() {
        return buildFallbackResponse("El servicio de envíos (shipment-service) no está respondiendo. Intente más tarde.");
    }

    /**
     * Respuesta de contingencia cuando {@code courier-service} no responde a tiempo.
     *
     * @return {@link Mono} con respuesta de error 503 para repartidores.
     */
    @GetMapping("/couriers")
    public Mono<ResponseEntity<Map<String, Object>>> couriersFallback() {
        return buildFallbackResponse("El servicio de repartidores (courier-service) no está respondiendo. Intente más tarde.");
    }

    /**
     * Construye un mapa estructurado con metadatos del fallo y c&oacute;digo HTTP 503.
     *
     * @param message Mensaje descriptivo del motivo de contingencia.
     * @return {@link Mono} con cuerpo JSON estructurado.
     */
    private Mono<ResponseEntity<Map<String, Object>>> buildFallbackResponse(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase());
        body.put("message", message);
        body.put("gateway", "LogiTrack API Gateway");

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
    }
}
