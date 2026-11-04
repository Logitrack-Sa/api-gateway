package com.logitrack.gateway.config;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/** Espera el catalogo local de Eureka antes de aceptar trafico de negocio. */
@Component
@ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "true")
public class DiscoveryReadinessHealthIndicator implements HealthIndicator {
    private final EurekaClient client;

    /** @param client Cliente que mantiene el catalogo usado por LoadBalancer. */
    public DiscoveryReadinessHealthIndicator(EurekaClient client) {
        this.client = client;
    }

    /** @return UP cuando los tres destinos tienen una instancia disponible en la cache local. */
    @Override
    public Health health() {
        for (String name : List.of("SECURITY-SERVICE", "COURIER-SERVICE", "SHIPMENT-SERVICE")) {
            var application = client.getApplication(name);
            if (application == null || application.getInstances().stream()
                    .noneMatch(instance -> instance.getStatus() == InstanceInfo.InstanceStatus.UP)) {
                return Health.down().build();
            }
        }
        return Health.up().build();
    }
}
