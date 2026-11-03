# 📘 Documento de Arquitectura y Diseño - API Gateway de LogiTrack

**Proyecto:** LogiTrack  
**Módulo:** `api-gateway`  
**Autor:** @GonzaCarrillo  
**Sprint:** Sprint 3  
**Fecha:** Octubre 2026  
**Relación con Issue:** [[SPRINT 3][GATEWAY] Crear el API Gateway de LogiTrack #80](https://github.com/Logitrack-Sa/shipment-service/issues/80)

---

## 1. Contexto y Objetivos del Microservicio

En el marco de la evolución del ecosistema de LogiTrack hacia una arquitectura distribuida perimetral (Sprint 3), el **API Gateway** asume la responsabilidad de ser el punto único de entrada para todas las solicitudes provenientes del exterior (clientes web, móviles, sistemas externos).

### 🎯 Objetivos Clave
1. **Punto Único de Entrada:** Eliminar la exposición directa de puertos de los microservicios de dominio (`shipment-service`, `courier-service`, `security-service`), centralizando el tráfico en el puerto estándar `8080`.
2. **Desacoplamiento Topológico:** Permitir que los clientes desconozcan las direcciones IP físicas o puertos efímeros de los microservicios de dominio.
3. **Descubrimiento Dinámico de Servicios:** Integrarse con Eureka Server para localizar instancias de microservicios usando nombres lógicos (`lb://SERVICE-NAME`).
4. **Configuración Centralizada:** Sincronizar su configuración perimetral en tiempo de arranque desde `config-server`.
5. **CORS Unificado:** Gestionar preflights y cabeceras de origen cruzado para todos los microservicios en un único lugar.
6. **Stack Reactivo de Alto Rendimiento:** Garantizar máxima concurrencia y bajo consumo de recursos mediante un modelo no bloqueante.

---

## 2. Mapa de Dependencias del Sprint 3

Para la issue `#80`, se analizó la relación con el resto del tablero de tareas de GitHub:

| Issue ID | Nombre de Tarea | Responsable | Estado | Relación con Issue #80 |
| :--- | :--- | :--- | :--- | :--- |
| **#77 (ISSUE 1)** | `[SPRINT 3][EUREKA] Implementar servidor de descubrimiento de servicios` | `@4lexxe` | `OPEN` | **Dependencia directa:** Requiere que Eureka Server esté disponible para validar el registro y resolución de servicios downstream. |
| **#79 (ISSUE 3)** | `[SPRINT 3][CONFIG] Externalizar la configuración de Eureka Server` | `@4lexxe` | `OPEN` | **Dependencia directa:** Requiere externalizar las propiedades de Eureka en `config-repo`. |
| **#80 (ISSUE 4)** | `[SPRINT 3][GATEWAY] Crear el API Gateway de LogiTrack` | `@GonzaCarrillo` | `CLOSED` | Creación del módulo, dependencias reactivas, Dockerfile, Javadoc y documentación. |
| **#81 (ISSUE 5)** | `[SPRINT 3][GATEWAY] Implementar rutas dinámicas hacia los microservicios` | `@GonzaCarrillo` | `CLOSED` | Rutas dinámicas lb://, predicados /api/v1/..., ordenamiento, Spring Cloud LoadBalancer, tests y Postman. |
| **#82 (ISSUE 6)** | `[SPRINT 3][SEGURIDAD] Gestionar JWT y roles desde el API Gateway` | `@GonzaCarrillo` | `CLOSED` | Validación RS256 de JWT, RBAC (ADMIN, CLIENTE, REPARTIDOR), 401/403 reactivos, tests y doc. |
| **#83 (ISSUE 7)** | `[SPRINT 3][GATEWAY] Configurar CORS y centralizar propiedades` | `@GonzaCarrillo` | `CLOSED` | Centralización en `config-repo` y `config-server`, filtro reactivo CORS perimetral (`CorsWebFilter`), soporte preflight OPTIONS y deduplicación con `DedupeResponseHeader`. |

> 💡 **Nota de Independencia:** Para evitar bloquear el desarrollo del Sprint 3 mientras `@4lexxe` finaliza la implementación de `#77` y `#79`, el módulo `api-gateway` se diseñó con perfiles y configuración desacoplada (`optional:configserver:...` y perfiles de prueba herméticos), permitiendo construir, compilar, testear y contenerizar el servicio de forma totalmente independiente.

---

## 3. Justificación Técnica: Stack Reactivo vs. Stack Bloqueante

Uno de los requerimientos explícitos de la tarea es:
> *"Evitar incluir dependencias de servidor web incompatibles con el stack reactivo de Spring Cloud Gateway."*

### ⚠️ El Problema con `spring-boot-starter-web` (Spring WebMVC):
* `spring-boot-starter-web` incluye embebido el servidor **Apache Tomcat** o Jetty, el cual opera bajo el modelo **Servlet bloqueante clásico (Thread-per-request)**: cada solicitud entrante reserva un hilo del pool hasta que la respuesta completa retorna desde el microservicio downstream.
* En un Gateway con alto volumen de llamadas y alta concurrencia, este modelo agota rápidamente los hilos disponibles en memoria y genera contención.
* Si coexisten las dependencias de WebMVC y WebFlux en el mismo ClassPath, Spring Cloud Gateway arroja un error crítico en el arranque (`BeanDefinitionOverrideException` o incompatibilidad de contexto `ReactiveWebServerApplicationContext`).

### ✅ La Solución con `spring-cloud-starter-gateway` (Spring WebFlux):
* Basado en **Project Reactor** y **Netty**.
* Utiliza el patrón **Event Loop**: un número reducido de hilos atiende eventos de E/S no bloqueantes (Non-blocking I/O).
* Permite gestionar miles de conexiones concurrentes con una huella de memoria significativamente menor.

---

## 4. Clases Implementadas y Cobertura de Javadoc

Todas las clases del módulo fueron documentadas exhaustivamente según las directivas del estándar JavaDoc:

1. **`ApiGatewayApplication.java`**:
   * Anotada con `@SpringBootApplication` y `@EnableDiscoveryClient`.
   * Javadoc documenta el rol de la aplicación perimetral, la naturaleza reactiva de Netty y la integración con Eureka y Config Server.
2. **`config.CorsConfig.java`**:
   * Declara el bean reactivo `CorsWebFilter` sobre `UrlBasedCorsConfigurationSource`.
   * Javadoc describe la centralización perimetral de cabeceras CORS, soporte para preflights `OPTIONS`, métodos HTTP autorizados y tiempos de caché.
3. **`config.GatewayRoutesConfig.java`**:
   * Monitorea e imprime en tiempo de arranque el inventario de rutas registradas en el `RouteLocator`.
   * Javadoc detalla la integración entre rutas programáticas y descriptores externos.
4. **`filter.LoggingGlobalFilter.java`**:
   * Implementa `GlobalFilter` y `Ordered` (precedencia `-1`).
   * Registra auditoría reactiva (método, ruta, IP, duración) garantizando no registrar tokens JWT completos para preservar la seguridad.
5. **`controller.FallbackController.java`**:
   * Retorna respuestas `Mono<ResponseEntity<Map<String, Object>>>` con código HTTP 503 ante fallos de conexión hacia los microservicios downstream.

---

## 5. Integración con Docker

Se diseñaron dos configuraciones de despliegue mediante Docker:

1. **`api-gateway/Dockerfile` (Autocontenido multietapa):**
   * **Etapa 1 (Build):** Imagen base `maven:3-eclipse-temurin-25` para compilar y empaquetar con optimización de caché de dependencias.
   * **Etapa 2 (Run):** Imagen ligera `eclipse-temurin:25-jre` ejecutando como usuario no-root (`app` / UID 10001).
   * **Flags de rendimiento:** `-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`.
2. **`deploy/docker/Dockerfile.gateway` (Despliegue centralizado):**
   * Diseñado para el pipeline de orquestación global en `deploy/docker-compose.yml`.

---

## 6. Verificación de Criterios de Aceptación de la Issue #80

| Criterio de Aceptación | Estado | Implementación / Evidencia |
| :--- | :---: | :--- |
| **El Gateway inicia correctamente** | ✅ Cumplido | Configuración reactiva en Netty sin servlets bloqueantes. Contexto verificado mediante pruebas con `WebTestClient`. |
| **Se registra en Eureka** | ✅ Cumplido | Dependencia `spring-cloud-starter-netflix-eureka-client` configurada con `@EnableDiscoveryClient` y `eureka.client.service-url.defaultZone`. |
| **Obtiene la configuración correspondiente desde Config Server** | ✅ Cumplido | Dependencia `spring-cloud-starter-config` y propiedad `spring.config.import=optional:configserver:...` activa, más plantillas `api-gateway*.properties` generadas en `config-repo`. |
| **Puede recibir solicitudes HTTP externas** | ✅ Cumplido | Expone puerto `8080`, filtros globales y endpoint de verificación `/actuator/health`. |
| **Está preparado para enrutar solicitudes hacia los microservicios** | ✅ Cumplido | Rutas definidas en `application.yml` hacia `lb://SECURITY-SERVICE`, `lb://SHIPMENT-SERVICE` y `lb://COURIER-SERVICE`. |
