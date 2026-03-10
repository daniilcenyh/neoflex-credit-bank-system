DOCKER_COMPOSE = docker-compose
NEXUS_URL      = http://localhost:8081
NEXUS_API      = $(NEXUS_URL)/service/rest/v1/status
NEXUS_ARTIFACT = $(NEXUS_URL)/repository/maven-snapshots/net/proselyte/calculator-api/1.0.0-SNAPSHOT/maven-metadata.xml

INFRA_SERVICES ?= nexus keycloak person-postgres keycloak-postgres prometheus grafana tempo loki alloy

.PHONY: all up build-persons wait-artifact build-api start stop clean logs rebuild infra infra-logs infra-stop

# ─────────────────────────────────────────────
# Wait commands — use curl (works on Git Bash, Linux, macOS)
# ─────────────────────────────────────────────

WAIT_NEXUS = \
	echo 'Waiting for Nexus to be ready...'; \
	until curl -sf $(NEXUS_API) > /dev/null 2>&1; do \
		echo 'Nexus not ready, retrying in 5s...'; sleep 5; \
	done; \
	echo 'Nexus is ready!'

WAIT_ARTIFACT = \
	echo 'Waiting for person-api artifact in Nexus...'; \
	until curl -sf "$(NEXUS_ARTIFACT)" > /dev/null 2>&1; do \
		echo 'Artifact not yet available, retrying in 5s...'; sleep 5; \
	done; \
	echo 'person-api artifact is available in Nexus!'

# ─────────────────────────────────────────────
# Main targets
# ─────────────────────────────────────────────

## Full startup: infra → persons-api (publishes artifact) → wait → api → rest
all: up build-persons wait-artifact build-api start
	@echo "=== All services are up ==="

## Step 1: Start Nexus first and wait until it is healthy
up:
	@echo ">>> Starting Nexus..."
	$(DOCKER_COMPOSE) up -d nexus
	@$(WAIT_NEXUS)

## Step 2: Build persons-api image — this also publishes person-api JAR to Nexus
build-persons:
	@echo ">>> Building persons-api (will publish calculator-api artifact to Nexus)..."
	$(DOCKER_COMPOSE) build --no-cache calculator-api
	@echo ">>> Starting persons-api container..."
	$(DOCKER_COMPOSE) up -d calculator-api

## Step 3: Poll Nexus until the person-api artifact actually appears
wait-artifact:
	@$(WAIT_ARTIFACT)

## Step 4: Build the api image — person-api artifact is guaranteed to exist in Nexus now
#build-api:
#	@echo ">>> Building api image..."
#	$(DOCKER_COMPOSE) build --no-cache api

## Step 5: Start all remaining services
start:
	@echo ">>> Starting all services..."
	$(DOCKER_COMPOSE) up -d

# ─────────────────────────────────────────────
# Utility targets
# ─────────────────────────────────────────────

stop:
	$(DOCKER_COMPOSE) down

clean: stop
	$(DOCKER_COMPOSE) rm -f
	docker volume rm $$(docker volume ls -qf dangling=true) 2>/dev/null || true
	rm -rf ./calculator-service/build

logs:
	$(DOCKER_COMPOSE) logs -f --tail=200

## Start only infrastructure services (no app services)
infra:
	@echo ">>> Starting infrastructure: $(INFRA_SERVICES)"
	$(DOCKER_COMPOSE) up -d $(INFRA_SERVICES)
	@$(WAIT_NEXUS)
	@echo ">>> Infrastructure is ready."

infra-logs:
	$(DOCKER_COMPOSE) logs -f --tail=200 $(INFRA_SERVICES)

infra-stop:
	$(DOCKER_COMPOSE) stop $(INFRA_SERVICES)

## Full teardown and rebuild from scratch
rebuild: clean all