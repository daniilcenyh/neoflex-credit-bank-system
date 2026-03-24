DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'deal') THEN
       CREATE DATABASE deal;
END IF;
END $$;