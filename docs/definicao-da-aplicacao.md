# Definição da Aplicação: Análise de Crédito com IA (RAG & Agentes)

## 1. Visão Geral

Esta aplicação tem como objetivo solucionar o processo de validação e análise de crédito utilizando Inteligência Artificial baseada em agentes e arquitetura **RAG** (*Retrieval-Augmented Generation*).

---

## 2. Perfis de Usuário e Permissões

### 👤 Cliente (Customer)
- **Gestão Cadastral**: Cadastro e manutenção das suas próprias informações (CRUD) armazenadas em banco de dados **PostgreSQL**.
- **Solicitação de Crédito**: Pode submeter solicitações para análise de crédito.

### 🛡️ Administrador (Admin)
- **Gestão de Regras de Negócio**: Cadastro, atualização e manutenção das políticas/regras de crédito armazenadas em base vetorial (**pgvector**).
- **Análise de Crédito**: Pode disparar e consultar análises de crédito no sistema.

---

## 3. Funcionalidades e Regras de Negócio

### 3.1. Armazenamento e Bancos de Dados
- **PostgreSQL (Relacional)**:
  - Armazenamento cadastral dos clientes (CRUD).
  - Histórico e registro dos pedidos/solicitações de crédito avaliados (status de aprovação/reprovação e motivos).
- **pgvector (Base Vetorial)**:
  - Armazenamento das regras e políticas de crédito cadastradas pelos administradores em formato de embeddings para consulta contextual.

### 3.2. Fluxo da Análise de Crédito

A análise pode ser iniciada tanto por **Customer** quanto por **Admin**.

1. **Parâmetros de Entrada**:
   - CPF do solicitante
   - Valor de crédito solicitado
   - Salário atual

2. **Etapas de Validação e Processamento**:
   - **Validação Cadastral**: Verifica se o CPF informado possui cadastro ativo no banco PostgreSQL.
   - **Consulta e Avaliação RAG com Agentes**:
     - Agentes de IA realizam busca semântica (*Retrieval*) das regras de negócio cadastradas no **pgvector**.
     - O agente analisa se a solicitação (valor pretendido x salário x regras de crédito) está aderente às diretrizes.

3. **Resultado da Análise**:
   - **Aprovado**:
     - Registra a solicitação na base PostgreSQL com o status de **Aprovada**.
     - Retorna confirmação de sucesso ao solicitante.
   - **Reprovado**:
     - Retorna notificação de que o crédito **não pôde ser aprovado**.
     - Apresenta e detalha a **motivação da recusa** com base nas regras avaliadas.
