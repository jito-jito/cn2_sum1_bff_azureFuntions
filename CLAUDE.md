# Proyecto: BFF (Spring Boot) + Azure Functions (Java)

Monorepo con dos componentes independientes que juntos sirven al frontend:
un **BFF** en Spring Boot que orquesta llamadas HTTP, y **Azure Functions**
en Java que contienen la lógica de dominio y el acceso a Oracle. Este
documento es la fuente de verdad de las decisiones de arquitectura y stack;
actualízalo a medida que el proyecto avance.

## Estructura del repositorio

Dos carpetas de primer nivel, cada una con su propio ciclo de build/deploy
— no comparten proceso en runtime, solo se comunican por HTTP:

```
/
├── bff/                 → Spring Boot. Se dockeriza y corre en EC2.
├── azure-functions/     → Java Azure Functions. Se despliega en Azure, conecta a Oracle.
└── CLAUDE.md
```

No son un build multi-módulo Maven (no comparten `pom.xml` padre ni se
compilan juntos): son dos proyectos Maven independientes que solo comparten
el contrato HTTP entre sí. Esto es intencional — tienen ciclos de release y
targets de despliegue distintos (EC2 vs Azure).

## Arquitectura

- **Patrón**: Backend For Frontend (BFF). No contiene lógica de negocio pesada;
  orquesta, agrega y adapta respuestas de servicios upstream para las
  necesidades del frontend.
- **Servicios upstream**: Azure Functions (HTTP-triggered). El BFF actúa como
  cliente HTTP de estas funciones — no las hostea ni las despliega.
- **Base de datos**: Oracle. La conexión a la base de datos la maneja la
  capa de Azure Functions, no el BFF. El BFF no tiene acceso directo a
  Oracle salvo que surja una necesidad puntual (cache, auditoría propia),
  a definir más adelante.
- **Runtime de despliegue**: contenedor Docker corriendo en una instancia
  EC2 de AWS (no ECS/EKS por ahora — despliegue directo en la instancia).

```
Frontend  →  BFF (Spring Boot, EC2/Docker)  →  Azure Functions  →  Oracle DB
```

### Arquitectura interna del BFF

Capas claras y BFF sin estado (stateless), para poder escalar horizontalmente
en EC2 (varias instancias detrás de un load balancer) sin rediseñar nada:

1. **Controller** — capa HTTP. Solo mapea request/response, valida input,
   delega al service. Sin lógica de orquestación aquí.
2. **Service** — orquestación. Llama a uno o más clients, combina/agrega
   resultados y mapea al DTO que consume el frontend. Aquí vive la única
   "lógica" propia del BFF (composición, no reglas de negocio de dominio —
   esas viven en las Azure Functions).
3. **Client** — un cliente por Azure Function (o por dominio de Functions),
   con su propio timeout/retry/circuit breaker (Resilience4j). Si una
   Function falla o está lenta, no debe arrastrar a las demás.
4. **Mapper** — traduce el contrato de cada Azure Function al DTO interno
   del BFF. Aísla al frontend de cambios en el contrato de las Functions.
5. **DTO** — modelos de entrada/salida de la API del BFF, independientes de
   los contratos de las Functions.

**Organización de paquetes: por dominio/feature, no por capa global.** Con
pocas Functions integradas cualquier estructura sirve, pero organizar por
dominio es lo que evita que `controller/`, `service/` y `client/` se vuelvan
carpetas gigantes de 30 archivos sin relación entre sí a medida que se
suman integraciones:

```
com.empresa.bff
├── config                    (RestClient/WebClient, Resilience4j, seguridad, CORS)
├── common
│   ├── exception             (GlobalExceptionHandler, contrato de error único)
│   └── web                   (filtro de correlation-id, logging)
├── pedidos                   (ejemplo de dominio)
│   ├── controller
│   ├── service
│   ├── client                (interfaz + impl hacia la Azure Function de pedidos)
│   ├── mapper
│   └── dto
└── clientes                  (otro dominio)
    ├── controller
    ├── service
    ├── client
    ├── mapper
    └── dto
```

Cada dominio nuevo (o cada Azure Function nueva) agrega un paquete, no
archivos sueltos en carpetas compartidas. Esto es lo que hace que la
estructura escale sin reescribirse.

**Principios transversales que mantienen esto escalable:**

- **Sin estado**: nada de sesión en memoria del BFF; si se necesita auth de
  frontend, usar JWT stateless. Así se puede pasar de 1 a N instancias EC2
  detrás de un load balancer sin cambios de código.
- **Llamadas paralelas cuando son independientes**: si un endpoint del BFF
  necesita datos de 2+ Functions no dependientes entre sí, llamarlas en
  paralelo (`WebClient` reactivo o `CompletableFuture`) en vez de
  secuencial, para que la latencia no crezca linealmente con cada
  integración nueva.
- **Aislamiento de fallos por client**: timeout/retry/circuit breaker
  configurados por client individual (no uno global), para que una Function
  lenta no degrade endpoints que no dependen de ella.
- **Correlation-id de punta a punta**: un filtro genera/propaga un
  request-id desde el frontend hacia cada llamada a Azure Functions, para
  poder trazar un request a través de todo el flujo cuando haya varias
  integraciones en juego.
- **Health checks por dependencia**: Actuator con un `HealthIndicator` por
  cada Azure Function crítica, no solo el health genérico de la app.
- **Cache opcional vía abstracción de Spring (`@Cacheable`)**: si el
  volumen crece, se puede pasar de cache local (Caffeine) a distribuida
  (Redis) sin tocar la capa de service, siempre que se use la abstracción
  desde el día uno en vez de cachear "a mano".

## Stack técnico del BFF

- **Lenguaje**: Java 21 (LTS)
- **Framework**: Spring Boot 3.3.x
- **Build tool**: Maven
- **Cliente HTTP hacia Azure Functions**: `RestClient` (Spring 6) o
  `WebClient` si se requiere no bloqueante. Evitar `RestTemplate` (deprecado).
- **Acceso a datos Oracle**: no aplica en el BFF — la conexión a Oracle vive
  en las Azure Functions. Si en el futuro el BFF necesita persistencia
  propia, documentar aquí driver (`ojdbc11`) y pool (HikariCP).
- **Autenticación hacia Azure Functions**: Function Key (`authLevel =
  FUNCTION`), enviada por el BFF en el header `x-functions-key` (ver
  `AzureFunctionsClientConfig`). El valor viene de la property
  `azure-functions.function-key`, resuelta desde la env var
  `AZURE_FUNCTIONS_KEY` — nunca hardcodeada. En `dev` tiene default vacío
  (Core Tools local no valida la key); en `prod` es obligatoria.

## Base de datos Oracle

- La conexión a Oracle es responsabilidad de las Azure Functions, **no** del
  BFF. El BFF solo ve los datos ya procesados/expuestos por las Functions
  a través de sus endpoints HTTP.
- El BFF no debe incluir driver de Oracle, datasource ni configuración de
  persistencia relacional mientras esto se mantenga así.
- Si más adelante el BFF requiere acceso directo a Oracle (ej. reportes,
  auditoría propia), documentar aquí: driver (`ojdbc11`), variables de
  entorno del datasource, manejo de wallet/TLS y herramienta de migraciones
  (Flyway/Liquibase).
- El detalle de cómo las Azure Functions implementan esta conexión vive en
  la sección "Azure Functions" más abajo.

## Integración BFF → Azure Functions

Esta sección describe la integración **desde el punto de vista del BFF**
(cliente). Para cómo se construyen las Functions en sí, ver la sección
"Azure Functions".

- El BFF consume Azure Functions HTTP-triggered como servicios externos.
- Configurar URLs base de las Functions vía variables de entorno /
  `application.yml` por perfil (`dev`, `staging`, `prod`), nunca hardcodeadas.
- Aplicar timeouts, retries y circuit breaker (ej. Resilience4j) en los
  clientes hacia las Functions, ya que son un servicio remoto fuera de la
  red del BFF.
- Registrar aquí cada Function consumida, su contrato (request/response) y
  su propósito a medida que se integren.

## Azure Functions

Proyecto Java independiente en `azure-functions/`. Es el componente que
contiene la lógica de dominio y el **único** con acceso a Oracle — el BFF
solo lo consume por HTTP (ver sección anterior).

### Stack técnico

- **Lenguaje**: Java 21.
- **Runtime**: Azure Functions Java Worker, programming model v4
  (anotaciones `@FunctionName` + `@HttpTrigger`). Sin Spring Boot aquí: en
  un runtime serverless, levantar un `ApplicationContext` completo en cada
  cold start es un costo que no vale la pena para functions HTTP simples.
- **Build tool**: Maven, con `azure-functions-maven-plugin` (empaqueta y
  despliega directo a Azure).
- **Acceso a datos Oracle**: JDBC directo (o un micro-ORM ligero, a
  definir) con driver `ojdbc11` + pool HikariCP.

### Estructura interna

Organizada por dominio, en espejo con los dominios del BFF, para que cada
integración sea fácil de rastrear de un lado a otro:

```
azure-functions/src/main/java/com/empresa/functions
├── pedidos
│   ├── PedidosFunction.java     (@FunctionName + @HttpTrigger)
│   ├── service
│   ├── repository               (JDBC hacia Oracle)
│   └── dto
└── clientes
    ├── ClientesFunction.java
    ├── service
    ├── repository
    └── dto
```

### Consideraciones específicas del runtime serverless

- **Reutilizar el pool de conexiones entre invocaciones**: Azure Functions
  reutiliza el proceso entre invocaciones "calientes" (warm start). El
  `DataSource`/pool de Hikari debe crearse como `static`/singleton a nivel
  de clase — **nunca** dentro del método de la function — o cada
  invocación abre conexiones nuevas y agota el límite de conexiones de
  Oracle rápidamente.
- **Cold start**: mantener el classpath liviano; evitar frameworks con
  arranque costoso (de ahí no usar Spring en este proyecto).
- **Un Function App vs. varios**: para empezar, un solo Function App
  agrupando todas las functions (un solo deploy, operación más simple). Si
  más adelante un dominio necesita escalar o versionarse de forma
  independiente, separarlo en su propio Function App — la organización por
  paquete ya deja esa puerta abierta sin reescribir código.
- **Secretos y configuración**: `local.settings.json` es solo para
  desarrollo local y **nunca se commitea**. En Azure, la cadena de
  conexión a Oracle y demás secretos van en Application Settings o Azure
  Key Vault.
- **Migraciones de esquema**: al ser la única capa con acceso a Oracle,
  las migraciones (Flyway/Liquibase, herramienta a definir) corren desde
  este proyecto.

### Despliegue

- Se despliega en **Azure** (Function App), no en la instancia EC2 — ese
  runtime es exclusivo del BFF.
- Vía `azure-functions-maven-plugin` (`mvn clean package azure-functions:deploy`,
  con `ORACLE_JDBC_URL`/`ORACLE_DB_USER`/`ORACLE_DB_PASSWORD` como variables
  de entorno del shell que despliega — nunca hardcodeadas en el `pom.xml`).
- **Plan de hosting: Consumption.** Decidido — pago por uso, acorde a una
  suscripción de estudiante/dev. Trade-off aceptado: cold start en la
  primera invocación tras inactividad.
- **Región: East US.** La región ideal por latencia hacia la Oracle ADB
  (en `sa-santiago-1`, Chile) sería `brazilsouth`, pero la suscripción
  "Azure for Students" la bloquea por política
  (`RequestDisallowedByAzure`) — solo permite un set restringido de
  regiones. `eastus` funcionó sin problema.
- **Function App desplegado**: `fn-usuarios-roles` en el resource group
  `rg-usuarios-roles`, URL `https://fn-usuarios-roles.azurewebsites.net`.
  Function key requerida vía `?code=` o header `x-functions-key` (authLevel
  FUNCTION).
- **Networking hacia Oracle — pendiente de endurecer**: la Autonomous
  Database usa una Access Control List (ACL) por IP/CIDR. El plan
  Consumption sobre Linux no expone un set pequeño y confiable de IPs de
  salida (`possibleOutboundIpAddresses` no fue suficiente en la práctica
  aun cubriendo matemáticamente todo el rango reportado). Por ahora la
  ACL quedó abierta (`0.0.0.0/0`) para desbloquear el desarrollo — la DB
  sigue protegida por usuario/password + TLS, pero esto es temporal.
  Opciones para endurecerlo más adelante: Function App en plan Premium
  con integración VNET + NAT Gateway (IP de salida fija y chica), o
  Oracle Private Endpoint dentro de una VCN peered con Azure.

## Dockerización del BFF

- Imagen base: `eclipse-temurin:21-jre-alpine` (o similar, ligera, JRE-only
  para runtime).
- Build multi-stage: una etapa con JDK + Maven para compilar el jar, otra
  con solo el JRE para ejecutar.
- La imagen no debe contener credenciales ni el wallet de Oracle "horneado";
  estos se inyectan en runtime (env vars / volumen montado).
- Exponer el puerto de la app vía variable de entorno `SERVER_PORT`
  (default 8080).

## Despliegue del BFF en EC2

- La instancia EC2 corre Docker (o Docker Compose si se suman servicios
  auxiliares).
- Pendiente de definir: estrategia de despliegue (manual `docker run`,
  `docker compose`, CI/CD hacia EC2), gestión de secretos en la instancia,
  y si habrá un reverse proxy (Nginx/Caddy) delante del contenedor para
  TLS.

## Convenciones de código

- Paquetes por dominio/feature (ver "Arquitectura interna del BFF" arriba),
  no por capa global — evitar carpetas `controller/`, `service/`, `client/`
  compartidas por toda la app.
- Dentro de cada dominio: `controller`, `service`, `client`, `mapper`, `dto`.
- DTOs de la API del BFF separados de los contratos de las Azure Functions
  — nunca exponer el payload crudo de una Function directamente al frontend.
- Perfiles de Spring (`application-dev.yml`, `application-prod.yml`) para
  separar configuración por ambiente.

## Pendientes a definir con el equipo

- [ ] Herramienta de migraciones de base de datos (Flyway vs Liquibase),
      a ejecutar desde `azure-functions/`.
- [ ] Estrategia de CI/CD hacia la instancia EC2 (BFF) y hacia Azure
      (Functions) — son pipelines separados.
- [ ] Si el BFF expone autenticación propia hacia el frontend (JWT, sesión)
      o delega en un servicio externo.
- [ ] JDBC directo vs. micro-ORM para el acceso a Oracle desde las
      Functions.
- [ ] Un Function App único vs. uno por dominio.
- [ ] **Endurecer el acceso de red a Oracle** — hoy la ACL de la ADB está
      abierta (`0.0.0.0/0`) porque el plan Consumption no da un set chico
      de IPs de salida confiable. Evaluar Premium+VNET o Private Endpoint
      antes de ir a un ambiente con datos reales.
