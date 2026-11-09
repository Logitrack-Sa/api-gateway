package com.logitrack.gateway.security;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.assertThat;

class PublicTrackingRateLimitFilterTest {
    @Test void onlyThePublicGetIsExemptFromLogin() {
        var paths = new SecurityPathMatcher();
        String path = "/api/v1/shipments/public-tracking/LT-A1B2C3D4E5";
        assertThat(paths.isPublic(path, HttpMethod.GET)).isTrue();
        assertThat(paths.isPublic(path, HttpMethod.POST)).isFalse();
        assertThat(paths.isPublic(path + "/history", HttpMethod.GET)).isFalse();
        assertThat(paths.isPublic("/api/v1/shipments/tracking/LT-A1B2C3D4E5", HttpMethod.GET)).isFalse();
    }

    @Test void limitsPublicQueriesAndRestoresBudgetAfterOneMinute() {
        var clock = new AtomicLong();
        var filter = new PublicTrackingRateLimitFilter(2, clock::get);
        for (int index = 0; index < 3; index++) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
                    "/api/v1/shipments/public-tracking/LT-A1B2C3D4E5"));
            filter.filter(exchange, e -> { e.getResponse().setStatusCode(HttpStatus.OK); return Mono.empty(); }).block();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(index < 2 ? HttpStatus.OK : HttpStatus.TOO_MANY_REQUESTS);
            if (index == 2) assertThat(exchange.getResponse().getHeaders().getFirst("Retry-After")).isEqualTo("60");
        }
        clock.addAndGet(Duration.ofMinutes(1).toNanos());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get(
                "/api/v1/shipments/public-tracking/LT-A1B2C3D4E5"));
        filter.filter(exchange, e -> { e.getResponse().setStatusCode(HttpStatus.OK); return Mono.empty(); }).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
