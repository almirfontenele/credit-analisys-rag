-- V2: Adicionar status cadastral em customers e flexibilizar credit_analysis
ALTER TABLE customers 
ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE NOT NULL;

ALTER TABLE credit_analysis 
ALTER COLUMN income_data_id DROP NOT NULL;

ALTER TABLE credit_analysis 
ADD COLUMN IF NOT EXISTS requested_amount DECIMAL(15,2),
ADD COLUMN IF NOT EXISTS declared_income DECIMAL(15,2);

CREATE INDEX IF NOT EXISTS idx_customers_cpf_active ON customers (cpf, active);
