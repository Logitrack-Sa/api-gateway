package com.logitrack.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Microservicio API Gateway del ecosistema LogiTrack.
 *
 * <p>Esta clase constituye el punto de inicio de la aplicaci&oacute;n perimetral
 * construida sobre <b>Spring Cloud Gateway</b> y el stack reactivo de <b>Project Reactor</b>
 * y <b>Netty</b>.</p>
 *
 * <h2>Responsabilidades del API Gateway:</h2>
 * <ul>
 *   <li>Punto &uacute;nico de entrada (Single Point of Entry) para todo el tr&aacute;fico externo
 *       hacia la plataforma LogiTrack.</li>
 *   <li>Enrutamiento din&aacute;mico de solicitudes hacia los microservicios de dominio
 *       ({@code shipment-service}, {@code courier-service}, {@code security-service})
 *       mediante resoluci&oacute;n de nombres l&oacute;gicos v&iacute;a Eureka (esquema {@code lb://}).</li>
 *   <li>Integraci&oacute;n con <b>Config Server</b> para obtenci&oacute;n centralizada
 *       de configuraciones perimetrales y de red.</li>
 *   <li>Gesti&oacute;n centralizada de pol&iacute;ticas transversales como CORS, auditor&iacute;a de tr&aacute;fico
 *       y resiliencia ante ca&iacute;das de servicios downstream.</li>
 * </ul>
 *
 * <h2>Stack Reactivo vs. Bloqueante:</h2>
 * <p>A diferencia de los microservicios tradicionales basados en Spring WebMVC y contenedores
 * Tomcat/Jetty (basados en el modelo de un hilo por petici&oacute;n o Thread-per-request),
 * este microservicio utiliza el paradigma reactivo no bloqueante basado en eventos (Event-Loop).
 * Por esta raz&oacute;n, se excluye deliberadamente cualquier dependencia con {@code spring-boot-starter-web}
 * para evitar colisiones y asegurar el m&aacute;ximo rendimiento con baja huella de memoria.</p>
 *
 * @author GonzaCarrillo
 * @version 1.0.0
 * @since Sprint 3
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    /**
     * Constructor predeterminado de la clase de configuraci&oacute;n y arranque de la aplicaci&oacute;n.
     */
    public ApiGatewayApplication() {
    }

    /**
     * Punto de entrada principal que inicializa el contexto de la aplicaci&oacute;n Spring Boot.
     *
     * @param args Argumentos pasados en la l&iacute;nea de comandos al arrancar el servicio.
     */
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
