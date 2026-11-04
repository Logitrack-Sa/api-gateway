package com.logitrack.gateway;

import com.logitrack.gateway.config.DiscoveryReadinessHealthIndicator;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscoveryReadinessTest {
    private final EurekaClient client = mock(EurekaClient.class);
    private final DiscoveryReadinessHealthIndicator health = new DiscoveryReadinessHealthIndicator(client);

    @BeforeEach void registerAvailableServices() {
        for (String name : new String[]{"SECURITY-SERVICE", "COURIER-SERVICE", "SHIPMENT-SERVICE"}) {
            when(client.getApplication(name)).thenReturn(application(name, InstanceInfo.InstanceStatus.UP));
        }
    }

    @Test void readyWhenAllDestinationsAreInTheLocalRegistry() {
        assertThat(health.health().getStatus()).isEqualTo(Status.UP);
    }

    @ParameterizedTest @ValueSource(strings = {"SECURITY-SERVICE", "COURIER-SERVICE", "SHIPMENT-SERVICE"})
    void waitsForEachDestination(String name) {
        when(client.getApplication(name)).thenReturn(null);
        assertThat(health.health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test void aStartingInstanceIsNotReady() {
        when(client.getApplication("SHIPMENT-SERVICE"))
                .thenReturn(application("SHIPMENT-SERVICE", InstanceInfo.InstanceStatus.STARTING));
        assertThat(health.health().getStatus()).isEqualTo(Status.DOWN);
    }

    private static Application application(String name, InstanceInfo.InstanceStatus status) {
        var application = new Application(name);
        application.addInstance(InstanceInfo.Builder.newBuilder().setInstanceId(name)
                .setAppName(name).setHostName(name).setStatus(status).build());
        return application;
    }
}
