# NextGen Workflow Application

A comprehensive microservices-based workflow management system designed for case management, investigations, and business process automation. Built with Spring Boot, React, and Flowable BPMN engine.

## Architecture Overview

This application follows a microservices architecture with:

- **API Gateway**: Central entry point with session-based authentication
- **Case Management Service**: Core business logic for case handling
- **Workflow Service**: Flowable BPMN-based process automation
- **User Management Service**: Authentication and authorization
- **React Frontend**: Modern UI built with TypeScript and Tailwind CSS

## Key Features

- **Session-Based Authentication**: Simple, secure authentication with X-Session-Id headers
- **Case Management**: Complete lifecycle management for investigation cases
- **Workflow Automation**: BPMN 2.0 compliant business process execution
- **Role-Based Access Control**: Fine-grained permissions with Cerbos policy engine
- **Multi-Department Support**: HR, Legal, Security, and Investigation workflows
- **RESTful APIs**: Comprehensive OpenAPI 3.0 specification
- **Real-Time Notifications**: Task assignments and status updates

## Technology Stack

### Backend
- **Java 21** with Spring Boot 3.3.4
- **PostgreSQL 16.x** databases per service
- **Flowable BPMN Engine 7.2.0** for workflow automation
- **Cerbos 0.14.0** for policy-based authorization
- **Docker** containerization

### Frontend
- **React 18** with TypeScript
- **Vite** build tool
- **Tailwind CSS** + shadcn/ui components
- **TanStack Query** for data fetching
- **React Router** for navigation

## Project Structure

```
├── api-gateway/                  # Central API Gateway with authentication
├── onecms-service/              # Core case management microservice
├── flowable-wrapper-v2/         # Flowable BPMN workflow engine
├── entitlement-service/         # User authentication and management
├── service-registry/            # Eureka service discovery
├── CMS-UI-App/                  # React frontend application
├── database/                    # Centralized Liquibase migrations
├── cerbos/                      # Cerbos policy configuration
├── scripts/                     # Database initialization scripts
├── docker-compose-infrastructure.yml  # Infrastructure services
├── nextgen-workflow.md          # Comprehensive architecture documentation
├── nextgen-workflow-openapi-session-auth.yaml  # OpenAPI specification
└── NextGen-Workflow-API-Collection.json        # Postman testing collection
```

## Quick Start

### Prerequisites

- **Java 21+**
- **Node.js 18+**
- **PostgreSQL 16.x**
- **Docker & Docker Compose**
- **Cerbos 0.14.0** (via Docker)

### 1. Clone Repository

```bash
git clone <repository-url>
cd NextGen-Workflow-Application
```

### 2. Start Infrastructure Components

```bash
# Start PostgreSQL and Cerbos
docker-compose -f docker-compose-infrastructure.yml up -d

# Wait for services to be ready
docker-compose -f docker-compose-infrastructure.yml ps
```

### 3. Start Backend Services

```bash
# Start Service Registry first
cd service-registry && ./mvnw spring-boot:run

# Then start other services (in separate terminals):
cd entitlement-service && ./mvnw spring-boot:run
cd flowable-wrapper-v2 && ./mvnw spring-boot:run
cd onecms-service && ./mvnw spring-boot:run
cd api-gateway && ./mvnw spring-boot:run
```

### 4. Start Frontend

```bash
cd CMS-UI-App
npm install
npm run dev
```

### 5. Access Application

- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost:8080/api
- **Service Registry (Eureka)**: http://localhost:8761
- **Cerbos Admin**: http://localhost:3593
- **PgAdmin** (if started): http://localhost:5050

### Service Ports
- **Service Registry**: 8761
- **API Gateway**: 8080
- **Entitlement Service**: 8081
- **Flowable Workflow Engine**: 8082
- **OneCMS Service**: 8083
- **PostgreSQL**: 5432
- **Cerbos gRPC**: 3592
- **Cerbos HTTP**: 3593

## 📚 Documentation

- **[Architecture Guide](nextgen-workflow.md)**: Comprehensive system architecture and design decisions
- **[API Documentation](nextgen-workflow-openapi-session-auth.yaml)**: OpenAPI 3.0 specification
- **[Frontend Guide](CMS-UI-App/CLAUDE.md)**: React application structure and development guide

## Authentication

The application uses session-based authentication:

1. **Login**: POST `/auth/login` → Get `sessionId`
2. **Authenticate**: Include `X-Session-Id` header in all requests
3. **Session Management**: 30-minute inactivity timeout

### Sample Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "alice.intake", "password": "password123"}'
```

## Testing

### Backend Testing
```bash
# Run tests for all services
./mvnw test

# Service-specific testing
cd case-management-service && ./mvnw test
```

### Frontend Testing
```bash
cd CMS-UI-App
npm run test
npm run lint
```

### API Testing
Import `NextGen-Workflow-API-Collection.json` into Postman for comprehensive API testing.

## Sample Data

The application includes pre-configured test users:

| Username | Password | Role | Department |
|----------|----------|------|------------|
| alice.intake | password123 | Intake Analyst | Intake |
| edward.inv | password123 | Investigator | Investigation |
| sarah.legal | password123 | Legal Counsel | Legal |
| mike.admin | password123 | Administrator | IT |

## Deployment

### Development
```bash
docker-compose -f docker-compose.dev.yml up
```

### Production
```bash
docker-compose -f docker-compose.prod.yml up
```

## Monitoring & Observability

- **Health Checks**: `/actuator/health` for all services
- **Metrics**: Prometheus-compatible metrics at `/actuator/prometheus`
- **Logging**: Structured JSON logging with correlation IDs

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## Support

For questions and support:
- **Documentation**: See [nextgen-workflow.md](nextgen-workflow.md)
- **API Issues**: Check OpenAPI specification
- **Bug Reports**: Create GitHub issues
- **Feature Requests**: Submit enhancement proposals

---

**NextGen Workflow Application** - Streamlining case management and business process automation.
