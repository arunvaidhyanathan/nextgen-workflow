# NextGen Workflow Application

A comprehensive microservices-based workflow management system designed for case management, investigations, and business process automation. Built with Spring Boot, React, and Flowable BPMN engine.

## 🏗️ Architecture Overview

This application follows a microservices architecture with:

- **API Gateway**: Central entry point with session-based authentication
- **Case Management Service**: Core business logic for case handling
- **Workflow Service**: Flowable BPMN-based process automation
- **User Management Service**: Authentication and authorization
- **React Frontend**: Modern UI built with TypeScript and Tailwind CSS

## 🚀 Key Features

- **Session-Based Authentication**: Simple, secure authentication with X-Session-Id headers
- **Case Management**: Complete lifecycle management for investigation cases
- **Workflow Automation**: BPMN 2.0 compliant business process execution
- **Role-Based Access Control**: Fine-grained permissions with Cerbos policy engine
- **Multi-Department Support**: HR, Legal, Security, and Investigation workflows
- **RESTful APIs**: Comprehensive OpenAPI 3.0 specification
- **Real-Time Notifications**: Task assignments and status updates

## 🛠️ Technology Stack

### Backend
- **Java 17** with Spring Boot 3.x
- **PostgreSQL** databases per service
- **Flowable BPMN Engine** for workflow automation
- **Cerbos** for policy-based authorization
- **Docker** containerization

### Frontend
- **React 18** with TypeScript
- **Vite** build tool
- **Tailwind CSS** + shadcn/ui components
- **TanStack Query** for data fetching
- **React Router** for navigation

## 📦 Project Structure

```
├── api-gateway-service/          # Central API Gateway with authentication
├── case-management-service/      # Core case management microservice
├── workflow-service/             # Flowable BPMN workflow engine
├── user-management-service/      # User authentication and management
├── CMS-UI-App/                   # React frontend application
├── docker-compose.yml            # Multi-service orchestration
├── nextgen-workflow.md           # Comprehensive architecture documentation
├── nextgen-workflow-openapi-session-auth.yaml  # OpenAPI specification
└── NextGen-Workflow-API-Collection.json        # Postman testing collection
```

## 🏁 Quick Start

### Prerequisites

- **Java 17+**
- **Node.js 18+**
- **PostgreSQL 13+**
- **Docker & Docker Compose**

### 1. Clone Repository

```bash
git clone <repository-url>
cd NextGen-Workflow-Application
```

### 2. Start Backend Services

```bash
# Start all services with Docker Compose
docker-compose up -d

# Or start individual services:
cd api-gateway-service && ./mvnw spring-boot:run
cd case-management-service && ./mvnw spring-boot:run
cd workflow-service && ./mvnw spring-boot:run
cd user-management-service && ./mvnw spring-boot:run
```

### 3. Start Frontend

```bash
cd CMS-UI-App
npm install
npm run dev
```

### 4. Access Application

- **Frontend**: http://localhost:3000
- **API Gateway**: http://localhost:8080/api
- **API Documentation**: http://localhost:8080/swagger-ui.html

## 📚 Documentation

- **[Architecture Guide](nextgen-workflow.md)**: Comprehensive system architecture and design decisions
- **[API Documentation](nextgen-workflow-openapi-session-auth.yaml)**: OpenAPI 3.0 specification
- **[Frontend Guide](CMS-UI-App/CLAUDE.md)**: React application structure and development guide

## 🔐 Authentication

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

## 🧪 Testing

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

## 📊 Sample Data

The application includes pre-configured test users:

| Username | Password | Role | Department |
|----------|----------|------|------------|
| alice.intake | password123 | Intake Analyst | Intake |
| edward.inv | password123 | Investigator | Investigation |
| sarah.legal | password123 | Legal Counsel | Legal |
| mike.admin | password123 | Administrator | IT |

## 🚀 Deployment

### Development
```bash
docker-compose -f docker-compose.dev.yml up
```

### Production
```bash
docker-compose -f docker-compose.prod.yml up
```

## 📈 Monitoring & Observability

- **Health Checks**: `/actuator/health` for all services
- **Metrics**: Prometheus-compatible metrics at `/actuator/prometheus`
- **Logging**: Structured JSON logging with correlation IDs

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

For questions and support:
- **Documentation**: See [nextgen-workflow.md](nextgen-workflow.md)
- **API Issues**: Check OpenAPI specification
- **Bug Reports**: Create GitHub issues
- **Feature Requests**: Submit enhancement proposals

---

**NextGen Workflow Application** - Streamlining case management and business process automation.