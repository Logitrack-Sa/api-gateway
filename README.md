# 🌐 LogiTrack - API Gateway

Microservicio perimetral basado en **Spring Cloud Gateway** que actúa como **punto único de entrada (Single Point of Entry)** para todas las solicitudes externas dirigidas a la plataforma de logística **LogiTrack**.

Este componente centraliza el acceso externo, desacopla a los clientes de las direcciones físicas de los servicios internos y gestiona el enrutamiento dinámico, las políticas de CORS, la auditoría y la degradación elegante (fallback).

---

## 📋 Información de la Tarea / Issue

* **Sprint:** Sprint 3
* **Issue:** [#80 - [SPRINT 3][GATEWAY] Crear el API Gateway de LogiTrack](https://github.com/Logitrack-Sa/shipment-service/issues/80)
* **Responsable:** `@GonzaCarrillo`
* **Dependencias identificadas en el Sprint:**
  * **ISSUE 1** ([#77](https://github.com/Logitrack-Sa/shipment-service/issues/77)): *Implementar servidor de descubrimiento de servicios (Eureka Server)* - Asignada a `@4lexxe` (Estado: `OPEN`).
  * **ISSUE 3** ([#79](https://github.com/Logitrack-Sa/shipment-service/issues/79)): *Externalizar la configuración de Eureka Server en Config Server* - Asignada a `@4lexxe` (Estado: `OPEN`).
  * **ISSUE 5** ([#81](https://github.com/Logitrack-Sa/shipment-service/issues/81)): *Implementar rutas dinámicas hacia los microservicios*.
  * **ISSUE 7** ([#83](https://github.com/Logitrack-Sa/shipment-service/issues/83)): *Configuración centralizada y CORS del Gateway en Config Server*.

---

## 🏛️ Arquitectura y Decisiones de Diseño

```
                                      ┌────────────────────────────────────────────────────────┐
                                      │                     Config Server                      │
                                      │                     (Puerto 8888)                      │
                                      └───────────────────────────▲────────────────────────────┘
                                                                  │ Configuración centralizada
                                                                  │ (api-gateway.properties)
                                                                  │
                                      ┌───────────────────────────▼────────────────────────────┐
                                      │                     Eureka Server                      │
                                      │                     (Puerto 8065)                      │
                                      └───────────────────────────▲────────────────────────────┘
                                                                  │ Descubrimiento dinámico
                                                                  │ (lb://SERVICE-NAME)
                                                                  │
[ Cliente Externo / Web / App ] ──────► ┌─────────────────────────┴────────────────────────────┐
   (HTTP / REST / JSON)                 │                  LogiTrack API Gateway               │
                                        │                      (Puerto 8080)                   │
                                        └──────────────┬────────────────┬──────────────────────┘
                                                       │                │
                        ┌──────────────────────────────┘                └──────────────────────────────┐
                        ▼                                               ▼                              ▼
          ┌───────────────────────────┐                   ┌───────────────────────────┐  ┌───────────────────────────┐
          │     security-service      │                   │     shipment-service      │  │      courier-service      │
          │       (Puerto 8083)       │                   │       (Puerto 8081)       │  │       (Puerto 8082)       │
          └───────────────────────────┘                   └───────────────────────────┘  └───────────────────────────┘
```

### 1. Stack Reactivo No Bloqueante (Evitar Servlets / Tomcat)
* **Spring Cloud Gateway** opera exclusivamente sobre el paradigma reactivo no bloqueante basado en **Project Reactor** y el servidor de eventos **Netty**.
* **Regla estricta aplicada:** Se ha omitido deliberadamente la dependencia `spring-boot-starter-web` (que incluye Tomcat y el modelo bloqueante `Thread-per-request` de Spring MVC), previniendo conflictos de contenedor y garantizando un alto rendimiento con uso mínimo de memoria bajo concurrencia.

### 2. Integración con Eureka Client
* Hace uso de `spring-cloud-starter-netflix-eureka-client` con la propiedad `@EnableDiscoveryClient`.
* El Gateway está preparado para resolver las instancias downstream utilizando balanceo dinámico de carga con el esquema `lb://<SERVICE_NAME>`:
  * `lb://SECURITY-SERVICE`
  * `lb://SHIPMENT-SERVICE`
  * `lb://COURIER-SERVICE`
* Si Eureka Server aún no está disponible (mientras se completa la Issue #77), el Gateway arranca de forma resiliente y se reconecta automáticamente una vez que Eureka inicie.

### 3. Integración con Config Server
* Incorpora `spring-cloud-starter-config` configurado mediante `spring.config.import=optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}`.
* Al usar el prefijo `optional:`, el servicio puede iniciar con valores por defecto locales si Config Server está en mantenimiento o en proceso de arranque.
* En `config-repo`, se definieron los archivos `api-gateway.properties`, `api-gateway-dev.properties` y `api-gateway-prod.properties` para la centralización completa de parámetros perimetrales.

### 4. CORS Reactivo Centralizado (`CorsConfig`)
* Utiliza `CorsWebFilter` y `UrlBasedCorsConfigurationSource` de Spring WebFlux.
* Evita la duplicación de encabezados CORS (`Access-Control-Allow-Origin`) en los microservicios aguas abajo y procesa de forma nativa las peticiones preflight `OPTIONS`.

### 5. Auditoría de Tráfico Seguro (`LoggingGlobalFilter`)
* Intercepta todas las peticiones externas registrando método, URI, IP del cliente y tiempo total de respuesta.
* **Seguridad de datos:** Conforme a los lineamientos del proyecto, **no registra tokens JWT completos en los logs** (solo audita presencia o ausencia del header `Authorization`).

### 6. Controlador de Contingencia (`FallbackController`)
* Expone rutas reactivas bajo `/fallback` para emitir respuestas homogéneas con código HTTP 503 (`Service Unavailable`) cuando un microservicio downstream se encuentre inalcanzable o experimente timeouts.

---

## 📂 Estructura del Módulo `api-gateway`

```
api-gateway/
├── Dockerfile
├── .dockerignore
├── .gitignore
├── pom.xml
├── README.md
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
├── mvnw
├── mvnw.cmd
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/logitrack/gateway/
    │   │       ├── ApiGatewayApplication.java             # Clase principal anotada con Javadoc exhaustivo
    │   │       ├── config/
    │   │       │   ├── CorsConfig.java                    # Filtro reactivo CORS (WebFlux)
    │   │       │   └── GatewayRoutesConfig.java           # Inspección y configuración de rutas
    │   │       ├── controller/
    │   │       │   └── FallbackController.java            # Endpoints reactivos de contingencia (503)
    │   │       └── filter/
    │   │           └── LoggingGlobalFilter.java           # Auditoría perimetral sin fuga de tokens
    │   └── resources/
    │       └── application.yml                            # Rutas lb://, Config Server y Eureka
    └── test/
        ├── java/
        │   └── com/logitrack/gateway/
        │       └── ApiGatewayApplicationTests.java        # Pruebas reactivas con WebTestClient
        └── resources/
            └── application-test.yml                       # Perfil hermético para pruebas unitarias
```

---

## ⚙️ Variables de Entorno y Configuración

| Variable de Entorno | Valor Predeterminado | Descripción |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8080` | Puerto HTTP de entrada para el API Gateway |
| `CONFIG_SERVER_URL` | `http://localhost:8888` | URL del servidor centralizado de configuración |
| `EUREKA_SERVER_URL` | `http://localhost:8065/eureka/` | URL del registro de servicios Eureka |
| `EUREKA_REGISTER_WITH_EUREKA` | `true` | Determina si el Gateway se registra en Eureka |
| `EUREKA_FETCH_REGISTRY` | `true` | Determina si consulta el registro de Eureka para `lb://` |
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil de configuración activo (`dev`, `prod`, `test`) |

---

## 🛠️ Rutas Predeterminadas Configuradas

| ID de Ruta | Predicado (Ruta externa) | Destino Dinámico | Microservicio |
| :--- | :--- | :--- | :--- |
| `security-service-public` | `/api/auth/**` | `lb://SECURITY-SERVICE` | Autenticación y registro |
| `security-service-users` | `/api/users/**` | `lb://SECURITY-SERVICE` | Gestión de usuarios |
| `shipment-service-route` | `/api/shipments/**`, `/api/customers/**` | `lb://SHIPMENT-SERVICE` | Gestión de envíos y clientes |
| `courier-service-route` | `/api/couriers/**` | `lb://COURIER-SERVICE` | Gestión de repartidores |

---

## 🐳 Contenerización con Docker

El módulo cuenta con un **Dockerfile multietapa** basado en los estándares del proyecto (Java 25 Temurin y usuario sin privilegios `app:10001`):

### Construcción de la imagen:
```bash
docker build -t logitrack/api-gateway:latest api-gateway/
```

### Ejecución en contenedor:
```bash
docker run -d \
  --name logitrack-api-gateway \
  -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e CONFIG_SERVER_URL=http://config-server:8888 \
  -e EUREKA_SERVER_URL=http://eureka-server:8065/eureka/ \
  logitrack/api-gateway:latest
```

---

## 🧪 Pruebas y Validación

### Ejecución de Pruebas Unitarias / Integración Reactiva:
```bash
cd api-gateway
./mvnw clean test
```
Las pruebas utilizan `WebTestClient` y un perfil aislado (`application-test.yml`) que no requiere servicios de infraestructura externos en ejecución para validar el contexto y los filtros.

### Endpoints de Monitoreo y Diagnóstico:
* **Salud del Gateway:** `GET http://localhost:8080/actuator/health`
* **Rutas registradas:** `GET http://localhost:8080/actuator/gateway/routes`
* **Endpoint de fallback:** `GET http://localhost:8080/fallback`

## Contrato de registro (#78)

Compose utiliza `SPRING_PROFILES_ACTIVE=prod,sprint3` y `CONFIG_SERVER_URL`.
Config Client es obligatorio; el perfil sprint3 centraliza Eureka y anuncia
`SERVICE_HOST=api-gateway`. Mantener `EUREKA_SERVER_URL=http://eureka-server:8065/eureka/`.
La compatibilidad Boot 3.5 / Cloud 2025.0 se verifica al arrancar.
La implementacion de rutas, JWT y CORS continua en #81-#83.
