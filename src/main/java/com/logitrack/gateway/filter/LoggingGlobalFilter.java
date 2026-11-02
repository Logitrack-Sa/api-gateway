package com.logitrack.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * Filtro global reactivo para la auditor&iacute;a y observabilidad del tr&aacute;fico perimetral.
 *
 * <p>Intercepta todas las solicitudes HTTP que ingresan a trav&eacute;s del API Gateway hacia
 * cualquiera de los microservicios de LogiTrack (como {@code shipment-service},
 * {@code courier-service} o {@code security-service}).</p>
 *
 * <h2>Pol&iacute;ticas de Seguridad:</h2>
 * <ul>
 *   <li>Se registra el m&eacute;todo HTTP, la URI solicitada y la direcci&oacute;n remota de origen.</li>
 *   <li><b>Nunca</b> se registran tokens JWT completos ni credenciales en texto plano para cumplir
 *       con los lineamientos de seguridad y privacidad del proyecto. Se audita &uacute;nicamente
 *       la presencia o ausencia del encabezado de autorizaci&oacute;n.</li>
 * </ul>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(LoggingGlobalFilter.class);

    /**
     * Intercepta la petici&oacute;n reactiva entrante, registra su informaci&oacute;n esencial de auditor&iacute;a
     * y contin&uacute;a con la cadena de filtros del Gateway.
     *
     * @param exchange Contexto de la solicitud y respuesta HTTP actual en el stack reactivo.
     * @param chain Cadena de filtros reactivos de Spring Cloud Gateway.
     * @return {@link Mono} que se completa cuando la solicitud downstream ha sido procesada.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        String clientIp = (remoteAddress != null && remoteAddress.getAddress() != null)
                ? remoteAddress.getAddress().getHostAddress()
                : "desconocido";

        boolean hasAuth = request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION);

        log.info("[Gateway Request] {} {} | IP: {} | AuthHeader: {}",
                method, path, clientIp, hasAuth ? "PRESENTE" : "AUSENTE");

        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpStatusCode statusCode = response.getStatusCode();
            long duration = System.currentTimeMillis() - startTime;

            log.info("[Gateway Response] {} {} -> Status: {} ({} ms)",
                    method, path, statusCode != null ? statusCode.value() : "N/A", duration);
        }));
    }

    /**
     * Define la precedencia de ejecuci&oacute;n de este filtro dentro de la cadena global.
     *
     * <p>Un valor bajo asegura que se ejecute temprano en el ciclo de vida de la petici&oacute;n.</p>
     *
     * @return Nivel de orden ({@code -1} para alta precedencia).
     */
    @Override
    public int getOrder() {
        return -1;
    }
}
