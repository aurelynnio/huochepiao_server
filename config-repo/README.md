# VeTau config-repo

This directory is structured for Spring Cloud Config Server.

Files:

- `application.yml`: shared defaults used by every service
- `api-gateway.yml`
- `eureka-server.yml`
- `order-service.yml`
- `payment-service.yml`
- `search-service.yml`
- `ticket-service.yml`
- `user-service.yml`

Notes:

- `discovery-service` uses `spring.application.name: eureka-server`, so its config file must be named `eureka-server.yml`.
- Bootstrap-only settings such as `spring.config.import` and `spring.cloud.config.*` stay inside each service's local `src/main/resources/application.yml`.
- Runtime settings that should live in the external config repository are defined here.

Git-backed config server:

1. Push these files to a standalone Git repository.
2. Update `CONFIG_SERVER_GIT_URI` to the new repository URL.
3. Remove or replace `SPRING_PROFILES_ACTIVE: native` in `server/docker-compose.yml`.
4. Remove the local `./config-repo:/config-repo:ro` volume if you want Config Server to read only from Git.
