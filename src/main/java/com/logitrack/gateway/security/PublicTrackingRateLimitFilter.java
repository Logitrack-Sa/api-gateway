package com.logitrack.gateway.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.LongSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Presupuesto compartido por instancia; no confía en cabeceras de IP del visitante. */
@Component
public class PublicTrackingRateLimitFilter implements GlobalFilter, Ordered {
    private final SecurityPathMatcher paths = new SecurityPathMatcher();
    private final int limit;
    private final LongSupplier clock;
    private long window;
    private int requests;

    @Autowired
    public PublicTrackingRateLimitFilter(@Value("${logitrack.tracking.requests-per-minute:120}") int limit) {
        this(limit, System::nanoTime);
    }

    PublicTrackingRateLimitFilter(int limit, LongSupplier clock) {
        if (limit < 1) throw new IllegalArgumentException("El límite debe ser positivo");
        this.limit = limit;
        this.clock = clock;
        this.window = clock.getAsLong();
    }

    private synchronized boolean allow() {
        long now = clock.getAsLong();
        if (now - window >= Duration.ofMinutes(1).toNanos()) {
            window = now;
            requests = 0;
        }
        if (requests >= limit) return false;
        requests++;
        return true;
    }

    @Override
    public int getOrder() { return -60; }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        if (!paths.isPublicTracking(request.getPath().value(), request.getMethod()) || allow()) {
            return chain.filter(exchange);
        }
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set("Retry-After", "60");
        response.getHeaders().setCacheControl("no-store");
        byte[] body = "{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"Esperá un minuto antes de volver a consultar.\"}"
                .getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }
}
