FROM pgvector/pgvector:pg16

# Configurações padrão de ambiente para o banco de dados
ENV POSTGRES_DB=credit_analysis
ENV POSTGRES_USER=postgres
ENV POSTGRES_PASSWORD=postgres

# Garante a extensão pgvector na inicialização do banco
RUN echo "CREATE EXTENSION IF NOT EXISTS vector;" > /docker-entrypoint-initdb.d/01-init-pgvector.sql

# Porta padrão do PostgreSQL
EXPOSE 5432
