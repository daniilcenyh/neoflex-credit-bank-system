-- Делаем колонки gender, marital_status, dependent_amount nullable
ALTER TABLE deal.clients ALTER COLUMN gender DROP NOT NULL;
ALTER TABLE deal.clients ALTER COLUMN marital_status DROP NOT NULL;
ALTER TABLE deal.clients ALTER COLUMN dependent_amount DROP NOT NULL;