#DOCKER_COMPOSE = docker-compose
#NEXUS_URL      = http://localhost:8081
#NEXUS_API      = $(NEXUS_URL)/service/rest/v1/status
#NEXUS_ARTIFACT = $(NEXUS_URL)/repository/maven-snapshots/net/proselyte/calculator-api/1.0.0-SNAPSHOT/maven-metadata.xml
##NEXUS_ARTIFACT = $(NEXUS_URL)/repository/maven-snapshots/com/neoflex/calculator-api/1.0.0-SNAPSHOT/maven-metadata.xml
#
#INFRA_SERVICES ?= nexus keycloak person-postgres keycloak-postgres prometheus grafana tempo loki alloy
#
#.PHONY: all up build-persons wait-artifact build-api start stop clean logs rebuild infra infra-logs infra-stop
#
## ─────────────────────────────────────────────
## Wait commands — use curl (works on Git Bash, Linux, macOS)
## ─────────────────────────────────────────────
#
#WAIT_NEXUS = \
#	echo 'Waiting for Nexus to be ready...'; \
#	until curl -sf $(NEXUS_API) > /dev/null 2>&1; do \
#		echo 'Nexus not ready, retrying in 5s...'; sleep 5; \
#	done; \
#	echo 'Nexus is ready!'
#
#WAIT_ARTIFACT = \
#	echo 'Waiting for person-api artifact in Nexus...'; \
#	until curl -sf "$(NEXUS_ARTIFACT)" > /dev/null 2>&1; do \
#		echo 'Artifact not yet available, retrying in 5s...'; sleep 5; \
#	done; \
#	echo 'person-api artifact is available in Nexus!'
#
## ─────────────────────────────────────────────
## Main targets
## ─────────────────────────────────────────────
#
### Full startup: infra → persons-api (publishes artifact) → wait → api → rest
#all: up build-persons wait-artifact build-api start
#	@echo "=== All services are up ==="
#
### Step 1: Start Nexus first and wait until it is healthy
#up:
#	@echo ">>> Starting Nexus..."
#	$(DOCKER_COMPOSE) up -d nexus
#	@$(WAIT_NEXUS)
#
### Step 2: Build persons-api image — this also publishes person-api JAR to Nexus
#build-persons:
#	@echo ">>> Building persons-api (will publish calculator-api artifact to Nexus)..."
#	$(DOCKER_COMPOSE) build --no-cache calculator-api
#	@echo ">>> Starting persons-api container..."
#	$(DOCKER_COMPOSE) up -d calculator-api
#
### Step 3: Poll Nexus until the person-api artifact actually appears
#wait-artifact:
#	@$(WAIT_ARTIFACT)
#
### Step 4: Build the api image — person-api artifact is guaranteed to exist in Nexus now
##build-api:
##	@echo ">>> Building api image..."
##	$(DOCKER_COMPOSE) build --no-cache api
#
### Step 5: Start all remaining services
#start:
#	@echo ">>> Starting all services..."
#	$(DOCKER_COMPOSE) up -d
#
## ─────────────────────────────────────────────
## Utility targets
## ─────────────────────────────────────────────
#
#stop:
#	$(DOCKER_COMPOSE) down
#
#clean: stop
#	$(DOCKER_COMPOSE) rm -f
#	docker volume rm $$(docker volume ls -qf dangling=true) 2>/dev/null || true
#	rm -rf ./calculator-service/build
#
#logs:
#	$(DOCKER_COMPOSE) logs -f --tail=200
#
### Start only infrastructure services (no app services)
#infra:
#	@echo ">>> Starting infrastructure: $(INFRA_SERVICES)"
#	$(DOCKER_COMPOSE) up -d $(INFRA_SERVICES)
#	@$(WAIT_NEXUS)
#	@echo ">>> Infrastructure is ready."
#
#infra-logs:
#	$(DOCKER_COMPOSE) logs -f --tail=200 $(INFRA_SERVICES)
#
#infra-stop:
#	$(DOCKER_COMPOSE) stop $(INFRA_SERVICES)
#
### Full teardown and rebuild from scratch
#rebuild: clean all



DOCKER_COMPOSE = docker-compose
NEXUS_URL      = http://localhost:8081
NEXUS_API      = $(NEXUS_URL)/service/rest/v1/status
NEXUS_ARTIFACT_CALCULATOR = $(NEXUS_URL)/repository/maven-snapshots/com/neoflex/calculator-api/1.0.0-SNAPSHOT/maven-metadata.xml
NEXUS_ARTIFACT_DEAL = $(NEXUS_URL)/repository/maven-snapshots/com/neoflex/deal-api/1.0.0-SNAPSHOT/maven-metadata.xml

INFRA_SERVICES ?= nexus prometheus grafana tempo loki alloy

.PHONY: all up start-infra build-calculator build-deal wait-artifact-calculator wait-artifact-deal start-apps stop clean logs rebuild infra infra-logs infra-stop rebuild-calculator rebuild-deal clean-calculator clean-deal logs-calculator logs-deal status help

# ─────────────────────────────────────────────
# Wait commands
# ─────────────────────────────────────────────

WAIT_NEXUS = \
	echo 'Waiting for Nexus to be ready...'; \
	until curl -sf $(NEXUS_API) > /dev/null 2>&1; do \
		echo 'Nexus not ready, retrying in 5s...'; sleep 5; \
	done; \
	echo 'Nexus is ready!'

WAIT_ARTIFACT_CALCULATOR = \
	echo 'Waiting for calculator-api artifact in Nexus...'; \
	until curl -sf "$(NEXUS_ARTIFACT_CALCULATOR)" > /dev/null 2>&1; do \
		echo 'Calculator artifact not yet available, retrying in 5s...'; sleep 5; \
	done; \
	echo 'calculator-api artifact is available in Nexus!'

WAIT_ARTIFACT_DEAL = \
	echo 'Waiting for deal-api artifact in Nexus...'; \
	until curl -sf "$(NEXUS_ARTIFACT_DEAL)" > /dev/null 2>&1; do \
		echo 'Deal artifact not yet available, retrying in 5s...'; sleep 5; \
	done; \
	echo 'deal-api artifact is available in Nexus!'

# ─────────────────────────────────────────────
# Main targets
# ─────────────────────────────────────────────

## Full startup: start Nexus → build services (auto-publish to Nexus) → wait → start apps
all: start-infra build-calculator build-deal wait-artifact-calculator wait-artifact-deal start-apps
	@echo "=== All services are up ==="
	@echo "Calculator API: http://localhost:8092"
	@echo "Deal API: http://localhost:8093"
	@echo "Nexus: http://localhost:8081"
	@echo "Prometheus: http://localhost:9090"
	@echo "Grafana: http://localhost:3000 (admin/admin)"

## Step 1: Start Nexus and infrastructure services, wait for Nexus to be ready
start-infra:
	@echo ">>> Starting Nexus and infrastructure services..."
	$(DOCKER_COMPOSE) up -d nexus prometheus loki tempo alloy
	@$(WAIT_NEXUS)
	@echo ">>> Infrastructure is ready. Nexus is up and running!"

## Step 2: Build calculator-api image (automatically publishes to Nexus during build)
build-calculator:
	@echo ">>> Building calculator-api image (will publish to Nexus)..."
	$(DOCKER_COMPOSE) build --no-cache calculator-api
	@echo ">>> Calculator-api image built and artifact published to Nexus!"

## Step 3: Build deal-api image (automatically publishes to Nexus during build)
build-deal:
	@echo ">>> Building deal-api image (will publish to Nexus)..."
	$(DOCKER_COMPOSE) build --no-cache deal-api
	@echo ">>> Deal-api image built and artifact published to Nexus!"

## Step 4: Wait for calculator-api artifact to appear in Nexus
wait-artifact-calculator:
	@$(WAIT_ARTIFACT_CALCULATOR)

## Step 5: Wait for deal-api artifact to appear in Nexus
wait-artifact-deal:
	@$(WAIT_ARTIFACT_DEAL)

## Step 6: Start all application services
start-apps:
	@echo ">>> Starting all application services..."
	$(DOCKER_COMPOSE) up -d deal-postgres-db
	sleep 5
	$(DOCKER_COMPOSE) up -d calculator-api deal-api grafana
	@echo ">>> All application services started!"

# ─────────────────────────────────────────────
# Utility targets
# ─────────────────────────────────────────────

## Stop all services
stop:
	$(DOCKER_COMPOSE) down

## Clean everything
clean: stop
	$(DOCKER_COMPOSE) rm -f
	docker volume rm $$(docker volume ls -qf dangling=true) 2>/dev/null || true
	rm -rf ./calculator-service/build
	rm -rf ./deal-service/build

## Follow logs for all services
logs:
	$(DOCKER_COMPOSE) logs -f --tail=200

## Follow logs for calculator-api only
logs-calculator:
	$(DOCKER_COMPOSE) logs -f --tail=200 calculator-api

## Follow logs for deal-api only
logs-deal:
	$(DOCKER_COMPOSE) logs -f --tail=200 deal-api

## Start only infrastructure services
infra:
	@echo ">>> Starting infrastructure: $(INFRA_SERVICES)"
	$(DOCKER_COMPOSE) up -d $(INFRA_SERVICES)
	@$(WAIT_NEXUS)
	@echo ">>> Infrastructure is ready."

## Follow infrastructure logs
infra-logs:
	$(DOCKER_COMPOSE) logs -f --tail=200 $(INFRA_SERVICES)

## Stop infrastructure services
infra-stop:
	$(DOCKER_COMPOSE) stop $(INFRA_SERVICES)

## Start databases only
db:
	@echo ">>> Starting databases..."
	$(DOCKER_COMPOSE) up -d deal-postgres-db

## Follow database logs
db-logs:
	$(DOCKER_COMPOSE) logs -f --tail=200 deal-postgres-db

## Stop database
db-stop:
	$(DOCKER_COMPOSE) stop deal-postgres-db

## Full teardown and rebuild from scratch
rebuild: clean all

## Rebuild only calculator-api
rebuild-calculator: clean-calculator
	$(DOCKER_COMPOSE) build --no-cache calculator-api
	$(DOCKER_COMPOSE) up -d calculator-api

## Rebuild only deal-api
rebuild-deal: clean-deal
	$(DOCKER_COMPOSE) build --no-cache deal-api
	$(DOCKER_COMPOSE) up -d deal-api

## Clean calculator-api
clean-calculator:
	$(DOCKER_COMPOSE) stop calculator-api
	$(DOCKER_COMPOSE) rm -f calculator-api
	rm -rf ./calculator-service/build

## Clean deal-api
clean-deal:
	$(DOCKER_COMPOSE) stop deal-api
	$(DOCKER_COMPOSE) rm -f deal-api
	rm -rf ./deal-service/build

## Show status of all services
status:
	$(DOCKER_COMPOSE) ps

## Show help
help:
	@echo "Available targets:"
	@echo ""
	@echo "Main targets:"
	@echo "  all                    - Full startup (infra → build → wait → start)"
	@echo "  start-infra            - Start Nexus and monitoring stack"
	@echo "  build-calculator       - Build calculator-api (auto-publishes to Nexus)"
	@echo "  build-deal            - Build deal-api (auto-publishes to Nexus)"
	@echo "  start-apps            - Start application services"
	@echo ""
	@echo "Utility targets:"
	@echo "  stop                   - Stop all services"
	@echo "  clean                  - Stop and remove containers, clean builds"
	@echo "  logs                   - Follow logs for all services"
	@echo "  logs-calculator        - Follow logs for calculator-api"
	@echo "  logs-deal             - Follow logs for deal-api"
	@echo "  infra                  - Start infrastructure only"
	@echo "  db                     - Start databases only"
	@echo "  rebuild                - Full rebuild from scratch"
	@echo "  rebuild-calculator     - Rebuild only calculator-api"
	@echo "  rebuild-deal          - Rebuild only deal-api"
	@echo "  status                 - Show container status"
	@echo "  help                   - Show this help"