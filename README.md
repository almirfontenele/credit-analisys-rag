# Credit Analysis RAG

Sistema de análise de crédito usando RAG (Retrieval-Augmented Generation) com Spring Boot 3, Java 21, OpenAI e pgvector.

## Requisitos
- Java 21
- PostgreSQL 16+ com pgvector
- OpenAI API Key

## Usuários e Autenticação (HTTP Basic)
| Usuário | Senha | Perfil (Role) | Endpoints |
|---|---|---|---|
| `admin` | `admin123` | `ADMIN` | `/api/admin/**` (ex: regras de negócio) |
| `operator` | `operator123` | `OPERATOR` | `/api/operators/**` (ex: clientes e análise) |
| `customer` | `customer123` | `CUSTOMER` | `/api/customers/me`, `/api/credit-analysis/solicitar` |

## Testes Automatizados e de Integração
Para executar toda a suíte de testes com PostgreSQL e pgvector:
```bash
mvn test
```
Para mais informações sobre as suítes e cobertura dos testes de integração, consulte [`docs/testes-de-integracao.md`](docs/testes-de-integracao.md).

## Documentação da API (Swagger)
- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) ou [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

> Utilize o botão **Authorize** no topo do Swagger UI para autenticar com os usuários `admin` ou `operator`.

### Endpoint de Solicitação de Crédito (POST)
- **Rota:** `POST /api/operators/credit-analysis` (ou `/api/credit-analysis`)
- **Headers:** `Content-Type: application/json`, `Authorization: Basic b3BlcmF0b3I6b3BlcmF0b3IxMjM=` (operator:operator123)
- **Payload:**
```json
{
  "cpf": "10861913825",
  "salario": 10000.00,
  "valor-credito-solicitado": 3000.00
}
```
- **Regra de Validação:**
  - Se aprovado: retorna status HTTP `200 OK` com os detalhes da análise e aprovação.
  - Se incompatível com as regras de negócio / salário: retorna status HTTP `402 Payment Required` com a mensagem `"valor solicitado incompativel com salario"`.

## Setup

1. Configure a variável de ambiente:
```bash
export OPENAI_API_KEY="sua-chave-aqui"
```

2. Suba o banco PostgreSQL com a extensão pgvector:
```bash
docker compose up -d
```

3. Execute a aplicação:
```bash
mvn spring-boot:run
```
