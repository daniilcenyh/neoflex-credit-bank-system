# Neoflex Bank Credit System

Микросервисная система для расчета кредитных предложений и скоринга клиентов с полным стеком мониторинга.

## Содержание
- [Требования](#требования)
- [Структура проекта](#структура-проекта)
- [Быстрый запуск](#быстрый-запуск)
- [Команды Makefile](#команды-makefile)
- [Сервисы и порты](#сервисы-и-порты)
- [API методы](#api-методы)
- [Мониторинг](#мониторинг)
- [Проверка работоспособности](#проверка-работоспособности)
- [Устранение проблем](#устранение-проблем)

## Требования

- Docker 24.0+
- Docker Compose 2.20+
- Make (опционально, для использования Makefile)
- 8 GB RAM минимум
- 20 GB свободного места на диске

## Структура проекта

```
neoflex-bank-credit-system/
├── calculator-service/              # Микросервис калькулятора
│   ├── src/
│   ├── Dockerfile
│   └── build.gradle.kts
├── infrastructure/                  # Конфигурация инфраструктуры
│   ├── prometheus/
│   │   └── prometheus.yml          # Конфигурация Prometheus
│   ├── grafana/
│   │   ├── provisioning/
│   │   │   ├── datasources/        # Источники данных
│   │   │   └── dashboards/         # Конфигурация дашбордов
│   │   └── dashboards/              # JSON дашбордов
│   ├── loki/
│   │   └── loki-config.yaml        # Конфигурация Loki
│   ├── tempo/
│   │   └── tempo.yaml              # Конфигурация Tempo
│   └── alloy/
│       └── config.alloy            # Конфигурация Alloy
├── docker-compose.yaml              # Docker Compose конфигурация
├── Makefile                         # Команды для управления
└── README.md                        # Документация
```

## Быстрый запуск

### 1. Клонирование репозитория
```bash
git clone <repository-url>
cd neoflex-bank-credit-system
```

### 2. Запуск всех сервисов
```bash
# Вариант 1: через Makefile
make all

# Вариант 2: напрямую через docker-compose
docker-compose up -d
```

### 3. Проверка статуса
```bash
docker-compose ps
```

## Команды Makefile

| Команда | Описание |
|---------|----------|
| `make all` | Полный запуск всех сервисов |
| `make up` | Запуск только Nexus |
| `make build-calculator` | Сборка и запуск calculator-api |
| `make start` | Запуск всех сервисов |
| `make stop` | Остановка всех сервисов |
| `make clean` | Полная очистка (контейнеры + тома) |
| `make logs` | Просмотр логов всех сервисов |
| `make infra` | Запуск только инфраструктуры |
| `make infra-stop` | Остановка инфраструктуры |
| `make rebuild` | Пересборка с нуля |

## Сервисы и порты

После запуска становятся доступны следующие сервисы:

| Сервис | URL | Доступ | Назначение |
|--------|-----|--------|------------|
| **Calculator API** | http://localhost:8092 | - | Основной микросервис |
| **Nexus** | http://localhost:8081 | admin/admin | Хранилище артефактов |
| **Prometheus** | http://localhost:9090 | - | Сбор метрик |
| **Grafana** | http://localhost:3000 | admin/admin | Визуализация |
| **Loki** | http://localhost:3100 | - | Хранение логов |
| **Tempo** | http://localhost:3200 | - | Хранение трейсов |
| **Alloy** | http://localhost:9080 | - | Сбор телеметрии |

## API методы

### Health Check
```bash
curl http://localhost:8092/actuator/health
```

### Метрики Prometheus
```bash
curl http://localhost:8092/actuator/prometheus
```

### Расчет предложений
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

### Полный расчет кредита
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

## Мониторинг

### Grafana дашборды
1. Открыть http://localhost:3000
2. Логин: admin, пароль: admin
3. Перейти в Dashboards → Browse
4. Выбрать "Calculator API - Minimal Dashboard"

Доступные метрики:
- Статус сервиса
- Количество запросов
- Ошибки 5xx
- Бизнес-метрики (offers/credit)
- Время ответа
- Память и CPU

### Prometheus targets
1. Открыть http://localhost:9090/targets
2. Проверить статусы:
    - calculator-api: UP
    - prometheus: UP

### Loki логи
В Grafana:
1. Explore → выбрать Loki
2. Выполнить запрос:
   ```
   {container="calculator-api"}
   ```

### Tempo трейсы
В Grafana:
1. Explore → выбрать Tempo
2. Поиск по trace ID или сервису

## Проверка работоспособности

### 1. Статус контейнеров
```bash
docker-compose ps
```
Все контейнеры должны быть в статусе `Up` и `healthy`.

### 2. Доступность API
```bash
curl http://localhost:8092/actuator/health
```
Ожидаемый ответ: `{"status":"UP"}`

### 3. Метрики в Prometheus
Открыть http://localhost:9090 и выполнить запрос:
```
up{job="calculator-api"}
```
Ожидаемое значение: `1`

### 4. Данные в Grafana
Открыть дашборд и убедиться, что:
- Статус сервиса = 1
- Появляются значения после выполнения запросов к API

## Устранение проблем

### Проблема: контейнеры не стартуют
```bash
# Проверить логи
docker-compose logs [service-name]

# Перезапустить с очисткой
docker-compose down -v
docker-compose up -d
```

### Проблема: calculator-api не виден в Prometheus
```bash
# Проверить доступность метрик
docker exec -it prometheus wget -O- http://calculator-api:8092/actuator/prometheus

# Проверить сеть
docker network inspect neoflex-bank-credit
```

### Проблема: Grafana не видит источники данных
1. Открыть http://localhost:3000
2. Configuration → Data Sources
3. Выбрать Prometheus
4. Нажать "Save & Test"
5. Ожидаемый результат: "Data source is working"

### Проблема: нет данных в дашбордах
```bash
# Выполнить тестовые запросы к API (примеры выше)
# Подождать 30 секунд
# Проверить наличие метрик в Prometheus:
# - calculator_offers_calculated_total
# - calculator_credit_calculated_total
# - http_server_requests_seconds_count
```

### Проблема: не хватает памяти
```bash
# Проверить использование памяти
docker stats

# При необходимости увеличить лимиты в Docker Desktop
# Settings → Resources → Memory
```

### Полный сброс системы
```bash
# Остановить все контейнеры
docker-compose down -v

# Удалить неиспользуемые тома
docker volume prune -f

# Пересобрать и запустить
docker-compose up -d --build
```

## Примечания

- Первый запуск Nexus может занять 2-3 минуты
- Для сбора бизнес-метрик необходимо выполнить хотя бы один запрос к API
- Все метрики собираются с интервалом 15 секунд
- Логи хранятся в Loki 24 часа (конфигурируется)
- Трейсы хранятся в Tempo 7 дней (конфигурируется)
