# app

Multi-module Java + Spring Boot + Spring Cloud microservices project.

- **groupId**: `com.example`
- **Modules**:
- **eureka-server** (EUREKA_SERVER) — http://localhost:28678
- **gateway-service** (GATEWAY) — http://localhost:27193
- **hall-service** (BUSINESS_SERVICE) — http://localhost:26444
- **vendor-service** (BUSINESS_SERVICE) — http://localhost:25485
- **user-service** (BUSINESS_SERVICE) — http://localhost:24800
- **proposal-service** (BUSINESS_SERVICE) — http://localhost:27035
- **event-service** (BUSINESS_SERVICE) — http://localhost:23021

## Build

```bash
mvn clean install -DskipTests
```

## Run (in separate terminals, in this order)

```bash
cd eureka-server && mvn spring-boot:run   # port 28678
cd hall-service && mvn spring-boot:run   # port 26444
cd vendor-service && mvn spring-boot:run   # port 25485
cd user-service && mvn spring-boot:run   # port 24800
cd proposal-service && mvn spring-boot:run   # port 27035
cd event-service && mvn spring-boot:run   # port 23021
cd gateway-service && mvn spring-boot:run   # port 27193
```

## Access the API

**Call the services through the gateway — that's the intended entry point,
not each service's own port.** The gateway (gateway-service, port 27193)
discovers every registered service via Eureka and routes to it by lower-cased
service name:

- **hall-service**: `http://localhost:27193/hall-service/api/v1/...`
- **vendor-service**: `http://localhost:27193/vendor-service/api/v1/...`
- **user-service**: `http://localhost:27193/user-service/api/v1/...`
- **proposal-service**: `http://localhost:27193/proposal-service/api/v1/...`
- **event-service**: `http://localhost:27193/event-service/api/v1/...`

- **Aggregated Swagger docs**: `http://localhost:27193/docs`

Every business service's own `http://localhost:<port>` listed above under
Modules is reachable directly too (and Docker Compose publishes it), but
that's there for local debugging one module in isolation — a real client, or
anything calling more than one service, should go through the gateway so
routing, discovery and any cross-cutting gateway config stay in one place.
