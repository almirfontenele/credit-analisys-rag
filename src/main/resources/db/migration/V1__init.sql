CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE business_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_name VARCHAR(255) NOT NULL,
    rule_description TEXT NOT NULL,
    rule_content TEXT NOT NULL,
    embedding vector(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    cpf VARCHAR(11) UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    birth_date DATE NOT NULL,
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE income_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    monthly_income DECIMAL(15,2) NOT NULL,
    employment_type VARCHAR(100),
    employer_name VARCHAR(255),
    employment_time_years INTEGER,
    other_income DECIMAL(15,2) DEFAULT 0,
    existing_debts DECIMAL(15,2) DEFAULT 0,
    credit_score INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE credit_analysis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    income_data_id UUID NOT NULL REFERENCES income_data(id) ON DELETE CASCADE,
    status VARCHAR(50) NOT NULL,
    approved_limit DECIMAL(15,2),
    analysis_details TEXT,
    rag_context TEXT,
    ai_response TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_business_rules_embedding ON business_rules 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
