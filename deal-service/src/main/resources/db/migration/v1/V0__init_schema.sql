-- Создание схемы deal (если её нет)
CREATE SCHEMA IF NOT EXISTS deal;

-- Устанавливаем поисковый путь
SET search_path TO deal;

-- =====================================================
-- Таблица clients
-- =====================================================
CREATE TABLE IF NOT EXISTS clients (
                                       client_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    middle_name VARCHAR(255),
    email VARCHAR(255) NOT NULL UNIQUE,
    birth_date DATE NOT NULL,
    account_number VARCHAR(255),
    gender VARCHAR(20) NOT NULL,
    marital_status VARCHAR(20),
    dependent_amount INTEGER,
    passport_id UUID,
    employment_id UUID,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
    );

-- =====================================================
-- Таблица passports
-- =====================================================
CREATE TABLE IF NOT EXISTS passports (
                                         passport_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    series VARCHAR(4),
    number VARCHAR(6),
    issue_branch VARCHAR(255),
    issue_date DATE,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
    );

-- =====================================================
-- Таблица employments
-- =====================================================
CREATE TABLE IF NOT EXISTS employments (
                                           employment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employment_status VARCHAR(50),
    employer_inn VARCHAR(12),
    salary NUMERIC(19, 2),
    position VARCHAR(50),
    work_experience_total INTEGER,
    work_experience_current INTEGER,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
    );

-- =====================================================
-- Таблица credits
-- =====================================================
CREATE TABLE IF NOT EXISTS credits (
                                       credit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount NUMERIC(19, 2),
    term INTEGER,
    monthly_payment NUMERIC(19, 2),
    rate NUMERIC(5, 2),
    psk NUMERIC(5, 2),
    is_insurance_enabled BOOLEAN,
    is_salary_client BOOLEAN,
    payment_schedule JSONB,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
    );

-- =====================================================
-- Таблица statements
-- =====================================================
CREATE TABLE IF NOT EXISTS statements (
                                          statement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID,
    credit_id UUID,
    status VARCHAR(50) NOT NULL,
    creation_date TIMESTAMP,
    applied_offer JSONB,
    sign_date TIMESTAMP,
    ses_code VARCHAR(255),
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
    );

-- =====================================================
-- Таблица status_histories
-- =====================================================
CREATE TABLE IF NOT EXISTS status_histories (
                                                status_history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    statement_id UUID,
    status VARCHAR(50) NOT NULL,
    time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    change_type VARCHAR(20) NOT NULL,
    created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC'),
    updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT (CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
    );

-- =====================================================
-- Внешние ключи
-- =====================================================

ALTER TABLE clients
    ADD CONSTRAINT fk_clients_passports
        FOREIGN KEY (passport_id)
            REFERENCES passports(passport_id)
            ON DELETE SET NULL;

ALTER TABLE clients
    ADD CONSTRAINT fk_clients_employments
        FOREIGN KEY (employment_id)
            REFERENCES employments(employment_id)
            ON DELETE SET NULL;

ALTER TABLE statements
    ADD CONSTRAINT fk_statements_clients
        FOREIGN KEY (client_id)
            REFERENCES clients(client_id)
            ON DELETE CASCADE;

ALTER TABLE statements
    ADD CONSTRAINT fk_statements_credits
        FOREIGN KEY (credit_id)
            REFERENCES credits(credit_id)
            ON DELETE SET NULL;

ALTER TABLE status_histories
    ADD CONSTRAINT fk_status_histories_statements
        FOREIGN KEY (statement_id)
            REFERENCES statements(statement_id)
            ON DELETE CASCADE;

-- =====================================================
-- Индексы для оптимизации
-- =====================================================

CREATE INDEX IF NOT EXISTS idx_clients_email ON clients(email);
CREATE INDEX IF NOT EXISTS idx_clients_birth_date ON clients(birth_date);
CREATE INDEX IF NOT EXISTS idx_statements_client_id ON statements(client_id);
CREATE INDEX IF NOT EXISTS idx_statements_status ON statements(status);
CREATE INDEX IF NOT EXISTS idx_statements_creation_date ON statements(creation_date);
CREATE INDEX IF NOT EXISTS idx_status_histories_statement_id ON status_histories(statement_id);
CREATE INDEX IF NOT EXISTS idx_status_histories_time ON status_histories(time);

-- =====================================================
-- Триггер для автоматического обновления поля updated
-- =====================================================

CREATE OR REPLACE FUNCTION update_updated_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated = CURRENT_TIMESTAMP AT TIME ZONE 'UTC';
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Триггеры для всех таблиц
CREATE TRIGGER update_clients_updated
    BEFORE UPDATE ON clients
    FOR EACH ROW EXECUTE FUNCTION update_updated_column();

CREATE TRIGGER update_passports_updated
    BEFORE UPDATE ON passports
    FOR EACH ROW EXECUTE FUNCTION update_updated_column();

CREATE TRIGGER update_employments_updated
    BEFORE UPDATE ON employments
    FOR EACH ROW EXECUTE FUNCTION update_updated_column();

CREATE TRIGGER update_credits_updated
    BEFORE UPDATE ON credits
    FOR EACH ROW EXECUTE FUNCTION update_updated_column();

CREATE TRIGGER update_statements_updated
    BEFORE UPDATE ON statements
    FOR EACH ROW EXECUTE FUNCTION update_updated_column();

CREATE TRIGGER update_status_histories_updated
    BEFORE UPDATE ON status_histories
    FOR EACH ROW EXECUTE FUNCTION update_updated_column();

-- =====================================================
-- Комментарии к таблицам
-- =====================================================

COMMENT ON TABLE clients IS 'Клиенты системы';
COMMENT ON TABLE passports IS 'Паспортные данные клиентов';
COMMENT ON TABLE employments IS 'Данные о занятости клиентов';
COMMENT ON TABLE credits IS 'Кредитные предложения';
COMMENT ON TABLE statements IS 'Заявки на кредит';
COMMENT ON TABLE status_histories IS 'История изменения статусов заявок';