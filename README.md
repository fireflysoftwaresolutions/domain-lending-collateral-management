<a href="https://fireflysoftware.io/openplatform/">
<picture>
  <source media="(max-width: 640px) and (prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-dark.svg">
  <source media="(max-width: 640px)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-narrow-light.svg">
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-dark.svg">
  <img src="https://raw.githubusercontent.com/fireflysoftwaresolutions/.github/main/profile/assets/banner-light.svg" alt="Firefly OpenPlatform - Domain Lending - Collateral Management" width="100%">
</picture>
</a>

# Firefly OpenPlatform - Domain Lending - Collateral Management

Developed by the Firefly Software Foundation. [dev@fireflysoftware.io](mailto:dev@fireflysoftware.io)

[Website](https://fireflysoftware.io/) · [OpenPlatform](https://fireflysoftware.io/openplatform/) · [Module catalogue](https://fireflysoftware.io/openplatform/modules/)

Domain layer microservice for collateral cases within the Firefly platform. This service orchestrates create, update, status change, close, and query against `core-lending-collateral-management` with compensating saga steps.

Repository: [https://github.com/fireflysoftwaresolutions/domain-lending-collateral-management](https://github.com/fireflysoftwaresolutions/domain-lending-collateral-management)

---

## Overview

`domain-lending-collateral-management` sits between experience services and `core-lending-collateral-management`. Write operations run through the FireflyFramework saga engine. Queries use the CQRS query bus. The domain depends on `core-lending-collateral-management-sdk` only. It does not import another core's internal packages. Identifiers that point at parties, contracts, or products stay UUIDs owned by those cores.

It creates, updates, changes status, and closes collateral cases through `core-lending-collateral-management`. Assets, liens, valuations, documents, and guarantee records stay on the core. This saga covers the collateral case.

### Key features

- **Create** -- register a collateral case through `core-lending-collateral-management`, with compensation that deletes the core write
- **Update** -- read the current collateral case and write the new field values through `update5` on `core-lending-collateral-management`. Status is left unchanged. Compensation writes the previous record back.
- **Status change** -- read the current collateral case and update its status through `core-lending-collateral-management`. The collateral core has no case status history resource, so the status saga does not append one.
- **Close** -- Close runs the status saga and writes RELEASED. The core has no cancel status. Remove deletes the case, which the core allows for every status. **Close** writes `RELEASED` when the core lifecycle allows that move.
- **Query** -- read one collateral case by identifier
- **Compensation** -- a failed later step asks the core to undo the work already accepted
- **CQRS** -- commands and queries travel on separate buses
- **Reactive I/O** -- Spring WebFlux with virtual threads enabled

## Architecture

### Module structure

| Module | Purpose |
|--------|---------|
| `domain-lending-collateral-management-core` | Saga workflows, commands, handlers, and the application service |
| `domain-lending-collateral-management-interfaces` | Request contracts for a status change and a closing reason |
| `domain-lending-collateral-management-infra` | SDK client factory and downstream base-path configuration |
| `domain-lending-collateral-management-web` | Spring Boot application, REST API, actuator, and OpenAPI |
| `domain-lending-collateral-management-sdk` | Java client generated from the OpenAPI document |

### Dependency flow

```
web --> interfaces --> core --> infra --> core-lending-collateral-management-sdk
sdk (generated from openapi.yml)
```

### Saga map

| Saga | Core calls | Compensation |
|------|------------|--------------|
| `RegisterCollateralCaseSaga` | `create5` on `CollateralCaseApi` | Deletes the record |
| `UpdateCollateralCaseSaga` | Reads the current record, then writes field changes through `update5` on `CollateralCaseApi`. Status is left unchanged. | Writes the previous record back through `update5` |
| `UpdateCollateralCaseStatusSaga` | Reads the current record, updates its status through `update5` | Writes the previous status back through the core |
| Query | `getById5` on `CollateralCaseApi` | None. A read has nothing to undo. |

Status values accepted by the core: `PENDING`, `UNDER_REVIEW`, `ACTIVE`, `RELEASED`.

## Setup

### Prerequisites

- Java 25
- Apache Maven 3.9+
- `core-lending-collateral-management-sdk` 26.10.5
- Apache Kafka on `localhost:9092` when event publishing is left at the default

### Environment

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_ADDRESS` | `localhost` | Bind address |
| `SERVER_PORT` | `8047` | HTTP port |
| `ENDPOINT_CORE_LENDING_COLLATERAL` | `http://localhost:8080` | Base URL of `core-lending-collateral-management` |

### Build and test

```bash
mvn clean verify
```

### Run

```bash
ENDPOINT_CORE_LENDING_COLLATERAL=http://localhost:8080 mvn spring-boot:run -pl domain-lending-collateral-management-web
```

Swagger UI (non-production): [http://localhost:8047/swagger-ui.html](http://localhost:8047/swagger-ui.html)

## API

| Method | Path | Behavior |
|--------|------|----------|
| `POST` | `/api/v1/collateral-cases` | Runs `RegisterCollateralCaseSaga` |
| `GET` | `/api/v1/collateral-cases/{collateralCaseId}` | Queries the core |
| `PUT` | `/api/v1/collateral-cases/{collateralCaseId}/status` | Runs `UpdateCollateralCaseStatusSaga` |
| `PUT` | `/api/v1/collateral-cases/{collateralCaseId}` | Runs `UpdateCollateralCaseSaga` |
| `POST` | `/api/v1/collateral-cases/{collateralCaseId}/close` | Close collateral case. Runs the status saga when `core-lending-collateral-management` allows `RELEASED`. |
| `DELETE` | `/api/v1/collateral-cases/{collateralCaseId}` | Deletes the record through `core-lending-collateral-management` when that core allows removal |

The status request body is `status` plus an optional `statusReason`.

Close writes RELEASED. The collateral core does not restrict which status can follow another and has no cancel status. Remove deletes any case because the core delete has no status guard.
A closing command body is an optional `statusReason`.
