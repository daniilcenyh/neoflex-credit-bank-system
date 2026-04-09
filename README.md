# Neoflex Bank Credit System

Микросервисная система для расчёта кредитных предложений и скоринга клиентов.  
Состоит из двух сервисов: `calculator-api` (кредитный калькулятор) и `deal-api` (работа со сделками).  
Полный стек мониторинга: Prometheus, Grafana, Loki, Tempo, ELK.

---

## Требования

- Docker 24.0+, Docker Compose 2.20+
- Make (рекомендуется) или использование команд docker-compose вручную
- 8 ГБ ОЗУ (минимум), 20 ГБ дискового пространства
- Для **Windows**: запуск терминала (PowerShell, Git Bash, CMD) **от имени администратора**

---

## Структура проекта

```
.
├── calculator-service/           # сервис калькулятора
├── deal-service/                 # сервис сделок
├── infrastructure/
│   ├── prometheus/               # конфиг Prometheus
│   ├── grafana/                  # дашборды и datasource
│   ├── loki/                     # конфиг Loki
│   ├── tempo/                    # конфиг Tempo
│   ├── alloy/                    # конфиг Alloy (otel collector)
│   ├── elk/                      # logstash, filebeat
│   └── databases/deal/           # SQL-скрипты инициализации БД
├── docker-compose.yaml
├── Makefile
└── README.md
```

---

## Быстрый запуск (всё одной командой)

```bash
git clone <repository-url>
cd neoflex-bank-credit-system
make all
```

Через несколько минут будут доступны:
- Calculator API: http://localhost:8092
- Deal API: http://localhost:8093
- Grafana: http://localhost:3000 (admin/admin)
- Остальные сервисы (см. таблицу портов)

---

## Пошаговый запуск

| Шаг | Команда | Что делает |
|-----|---------|-------------|
| 1 | `make start-infra` | Запуск Nexus, Prometheus, Loki, Tempo, Alloy |
| 2 | `make build-calculator` | Сборка образа calculator-api, публикация JAR в Nexus |
| 3 | `make build-deal` | Сборка образа deal-api, публикация JAR в Nexus |
| 4 | `make start-apps` | Запуск PostgreSQL, calculator-api, deal-api, Grafana |
| 5 | `make start-elk` | Запуск Elasticsearch, Logstash, Kibana, Filebeat |

После выполнения всех шагов система полностью готова.

---

## Команды Makefile

| Команда | Описание |
|---------|----------|
| `make all` | Полный запуск (инфра → сборка → приложения → ELK) |
| `make start-infra` | Запуск инфраструктуры (Nexus, Prometheus, Loki, Tempo, Alloy) |
| `make build-calculator` | Сборка calculator-api |
| `make build-deal` | Сборка deal-api |
| `make start-apps` | Запуск БД, сервисов, Grafana |
| `make start-elk` | Запуск ELK-стека |
| `make stop-elk` | Остановка ELK |
| `make stop` | Остановка всех контейнеров |
| `make clean` | Полная очистка (контейнеры, тома, build-директории) |
| `make logs` | Логи всех сервисов |
| `make logs-calculator` | Логи calculator-api |
| `make logs-deal` | Логи deal-api |
| `make status` | Статус контейнеров |
| `make rebuild` | `make clean && make all` |
| `make help` | Справка по командам |

---

## Сервисы и порты

| Сервис | URL / хост:порт | Логин / пароль |
|--------|----------------|----------------|
| Calculator API | http://localhost:8092 | – |
| Deal API | http://localhost:8093 | – |
| Swagger (Calculator) | http://localhost:8092/swagger-ui.html | – |
| Swagger (Deal) | http://localhost:8093/swagger-ui.html | – |
| PostgreSQL (deal) | localhost:5434 | postgres / postgres |
| Nexus | http://localhost:8081 | admin / admin |
| Prometheus | http://localhost:9090 | – |
| Grafana | http://localhost:3000 | admin / admin |
| Loki | http://localhost:3100 | – |
| Tempo | http://localhost:3200 | – |
| Alloy (OTLP) | http://localhost:4318 | – |
| Elasticsearch | http://localhost:9200 | – |
| Kibana | http://localhost:5601 | – |

---

## API примеры

### Calculator API – получить кредитные предложения

```bash
curl -X POST http://localhost:8092/api/v1/calculator/offers \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500000,
    "term": 12,
    "firstName": "Ivan",
    "lastName": "Ivanov",
    "email": "ivan@mail.com",
    "birthdate": "1990-01-01",
    "passportSeries": "1234",
    "passportNumber": "123456"
  }'
```

### Calculator API – полный расчёт кредита (со скорингом)

```bash
curl -X POST http://localhost:8092/api/v1/calculator/calc \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500000,
    "term": 12,
    "firstName": "Ivan",
    "lastName": "Ivanov",
    "gender": "MALE",
    "birthdate": "1990-01-01",
    "passportSeries": "1234",
    "passportNumber": "123456",
    "passportIssueDate": "2010-01-01",
    "passportIssueBranch": "Test Branch",
    "maritalStatus": "MARRIED",
    "dependentAmount": 2,
    "employment": {
      "employmentStatus": "EMPLOYED",
      "employerINN": "7707083893",
      "salary": 150000,
      "position": "TOP_MANAGER",
      "workExperienceTotal": 60,
      "workExperienceCurrent": 24
    },
    "accountNumber": "40817810000000000001",
    "isInsuranceEnabled": true,
    "isSalaryClient": true
  }'
```

### Deal API – создать заявку

```bash
curl -X POST http://localhost:8093/api/v1/deal/statement \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500000,
    "term": 12,
    "firstName": "Ivan",
    "lastName": "Ivanov",
    "email": "ivan@mail.com",
    "birthdate": "1990-01-01",
    "passportSeries": "1234",
    "passportNumber": "123456"
  }'
```

---

## Проверка работоспособности

1. **Статус контейнеров**  
   `make status` – все контейнеры должны быть `Up` или `healthy`.

2. **Healthcheck API**
   ```bash
   curl http://localhost:8092/actuator/health
   curl http://localhost:8093/actuator/health
   ```
   Ответ: `{"status":"UP"}`

3. **Метрики Prometheus**  
   Открыть http://localhost:9090 → выполнить запрос `up{job="calculator-api"}` → значение `1`.

4. **Логи в Loki**  
   Grafana → Explore → Loki → запрос `{container="calculator-api"}` → должны быть логи.

5. **Артефакты в Nexus**  
   http://localhost:8081 → Browse → `maven-snapshots/com/neoflex/` → присутствуют `calculator-api` и `deal-api`.

---

## Устранение проблем

### Контейнер не запускается / падает
```bash
docker-compose logs <service-name>
```

### Nexus недоступен при сборке
```bash
make start-infra          # дождаться healthcheck (2-3 минуты)
curl http://localhost:8081/service/rest/v1/status   # должен вернуть "ok"
```

### Deal API не видит PostgreSQL
Проверить переменные окружения в `docker-compose.yaml` для `deal-api`:
```
POSTGRES_HOST=deal-postgres-db
POSTGRES_PORT=5432
```
Проверить, что БД запущена: `make db`

### Нет метрик в Prometheus
Убедиться, что сервисы `calculator-api` и `deal-api` запущены и их `/actuator/prometheus` доступен:
```bash
docker exec -it prometheus wget -O- http://calculator-api:8092/actuator/prometheus
```

### Недостаточно памяти
Увеличить ресурсы Docker Desktop (8+ ГБ). Отключить ELK, если не нужен:
```bash
make stop-elk
```

### Полный сброс и перезапуск
```bash
make clean
make all
```

---

## Примечание для Windows

**Запускайте терминал от имени администратора** (правый клик → "Запуск от имени администратора").  
Без прав администратора сборка Docker-образов завершится ошибкой доступа к временным файлам `C:\Windows\TEMP`.

---

## Файлы конфигурации

- `docker-compose.yaml` – оркестрация всех сервисов
- `Makefile` – цели для управления
- `infrastructure/prometheus/prometheus.yml` – настройка сбора метрик
- `infrastructure/grafana/provisioning/` – автоматическая настройка datasource и дашбордов
- `infrastructure/loki/loki-config.yaml` – конфиг Loki
- `infrastructure/tempo/tempo.yaml` – конфиг Tempo
- `infrastructure/alloy/config.alloy` – приём OTLP и маршрутизация в Prometheus/Loki/Tempo
- `infrastructure/elk/logstash/pipeline/` – конфиги Logstash
- `infrastructure/databases/deal/` – SQL-скрипты инициализации БД

---

**Готово.** После выполнения `make all` система полностью функционирует.  
Для остановки: `make stop`. Для удаления всех данных: `make clean`.
```