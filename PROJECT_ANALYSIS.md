# 📊 QHSE-Analytics - Complete Project Analysis

## 🏗️ Project Architecture

### Tech Stack

#### Backend
- **Framework**: Spring Boot (Java)
- **Build Tool**: Maven
- **Database**: SQL (Flyway migrations)
- **Authentication**: Security Config (JWT/OAuth)
- **API Documentation**: OpenAPI/Swagger
- **Async Processing**: Spring Async
- **CORS**: Configured

#### Frontend
- **Framework**: Angular 21.2.8
- **Styling**: SCSS + Angular Material
- **HTTP Client**: Angular HttpClient with Interceptors
- **State Management**: Services
- **Build**: Vite
- **Package Manager**: npm
- **UI Components**: Material Design

#### DevOps
- **Containerization**: Docker (Both Backend & Frontend)
- **Reverse Proxy**: Nginx
- **Container Orchestration**: Ready for Kubernetes

---

## 📁 Backend Structure (Spring Boot)

```
QHSEAnalytics/
├── src/main/java/com/QHSEAnalytics/
│   ├── QhseAnalyticsApplication.java          # Main Application Entry
│   │
│   ├── auth/                                   # Authentication Module
│   │   ├── controller/                        # Auth Endpoints
│   │   ├── dto/                               # Auth DTOs
│   │   ├── entity/                            # User/Role Entities
│   │   ├── exception/                         # Auth Exceptions
│   │   ├── handler/                           # Exception Handlers
│   │   ├── repository/                        # DB Repositories
│   │   └── service/                           # Auth Business Logic
│   │
│   ├── config/                                # Configuration Classes
│   │   ├── AdminInitializer.java             # Init Default Admin
│   │   ├── AsyncConfig.java                  # Async Task Config
│   │   ├── CorsConfig.java                   # CORS Configuration
│   │   ├── ImportSessionModeConstraintUpdater.java
│   │   ├── ImportSessionStatutConstraintUpdater.java
│   │   ├── OpenApiConfig.java                # Swagger/OpenAPI
│   │   └── SecurityConfig.java               # Spring Security
│   │
│   ├── controller/                            # REST Controllers
│   │   ├── AnalyseIaController.java          # AI Analysis Endpoints
│   │   ├── AnalysisController.java           # General Analysis
│   │   ├── DashboardAdminController.java     # Admin Dashboard
│   │   ├── DashboardAnalysteController.java  # Analyst Dashboard
│   │   ├── ExportController.java             # Export Data
│   │   ├── ImportProcessingController.java   # Import Processing
│   │   ├── ImportSessionController.java      # Session Management
│   │   ├── KpiController.java                # KPI Operations
│   │   └── MappingController.java            # Data Mapping
│   │
│   ├── dto/                                   # Data Transfer Objects
│   │   ├── Request DTOs                      # For API Requests
│   │   ├── Response DTOs                     # For API Responses
│   │   └── Ollama DTOs                       # AI Integration
│   │
│   ├── entity/                                # JPA Entities (Database Models)
│   │   ├── User Entity
│   │   ├── Role Entity
│   │   ├── KPI Entity
│   │   ├── ImportSession Entity
│   │   ├── Analysis Entity
│   │   └── ... Other Domain Entities
│   │
│   ├── enums/                                 # Enumeration Classes
│   │   ├── UserRole (ADMIN, ANALYSTE)
│   │   ├── ImportSessionStatus
│   │   ├── KPIStatus
│   │   └── RiskLevel (FAIBLE, MODERE, ELEVE)
│   │
│   ├── exception/                             # Custom Exceptions
│   │   ├── BusinessException
│   │   ├── NotFoundException
│   │   └── ValidationException
│   │
│   ├── initializer/                           # Data Initialization
│   │   └── Database Seeders
│   │
│   ├── repository/                            # Spring Data JPA
│   │   ├── UserRepository
│   │   ├── KpiRepository
│   │   ├── ImportSessionRepository
│   │   ├── AnalysisRepository
│   │   └── Custom Query Repositories
│   │
│   ├── security/                              # Security Filters/Handlers
│   │   ├── JWT Handler
│   │   └── Auth Providers
│   │
│   └── service/                               # Business Logic Layer
│       ├── AuthService
│       ├── KpiService
│       ├── ImportService
│       ├── AnalysisService (AI)
│       ├── DashboardService
│       ├── ExportService
│       └── processing/                        # Async Processing
│
└── src/main/resources/
    ├── application.properties                 # Configuration
    └── db/migration/                          # Flyway Migrations
        └── V1__*.sql                          # Database Schemas
```

---

## 🎨 Frontend Structure (Angular)

```
frontend/
├── src/
│   ├── app/
│   │   ├── app.config.ts                     # App Configuration
│   │   ├── app.routes.ts                     # Routing Configuration
│   │   ├── app.ts                            # Root Component
│   │   │
│   │   ├── core/                             # Singleton Services
│   │   │   ├── guards/                       # Route Guards
│   │   │   │   ├── admin.guard.ts           # Admin Protection
│   │   │   │   ├── analyste.guard.ts        # Analyste Protection
│   │   │   │   └── auth.guard.ts            # General Auth
│   │   │   │
│   │   │   ├── interceptors/                # HTTP Interceptors
│   │   │   │   └── auth.interceptor.ts      # Auto JWT Token
│   │   │   │
│   │   │   ├── models/                      # TypeScript Interfaces
│   │   │   │   ├── analyse-ia.model.ts      # AI Analysis
│   │   │   │   ├── dashboard.model.ts       # Dashboard Data
│   │   │   │   └── import-session.model.ts  # Import Session
│   │   │   │
│   │   │   └── services/                    # Core Services
│   │   │       ├── admin.service.ts         # Admin Operations
│   │   │       ├── ai-analysis.service.ts   # AI Analysis Calls
│   │   │       ├── auth.service.ts          # Authentication
│   │   │       ├── dashboard.service.ts     # Dashboard Data
│   │   │       ├── import.service.ts        # Import Operations
│   │   │       ├── import-upload-state.service.ts
│   │   │       └── token.service.ts         # Token Management
│   │   │
│   │   ├── features/                        # Feature Modules
│   │   │   ├── admin/                       # Admin Portal
│   │   │   │   ├── admin-shell.component.*  # Shell Layout
│   │   │   │   ├── models/
│   │   │   │   │   └── admin.models.ts
│   │   │   │   └── pages/
│   │   │   │       ├── historique/          # History Page
│   │   │   │       ├── kpis/                # KPI Management
│   │   │   │       ├── overview/            # Overview Page
│   │   │   │       ├── profile/             # User Profile
│   │   │   │       └── users/               # User Management
│   │   │   │
│   │   │   ├── analyste/                    # Analyste Portal
│   │   │   │   ├── analyste-shell.component.*
│   │   │   │   └── pages/
│   │   │   │       ├── dashboard/           # Analytics Dashboard
│   │   │   │       ├── historique/          # History
│   │   │   │       ├── ia-insights/         # AI Insights
│   │   │   │       ├── import/              # Import Data
│   │   │   │       └── import-mapping/      # Field Mapping
│   │   │   │
│   │   │   └── auth/                        # Auth Module
│   │   │       ├── models/
│   │   │       │   └── auth.models.ts
│   │   │       └── pages/
│   │   │           ├── dashboard/
│   │   │           ├── forgot-password/
│   │   │           ├── home/
│   │   │           ├── login/
│   │   │           ├── otp/
│   │   │           ├── register/
│   │   │           ├── reset-password/
│   │   │           └── verify-account/
│   │   │
│   │   ├── shared/                          # Shared Resources
│   │   │   ├── material.module.ts           # Material Module
│   │   │   └── components/
│   │   │       ├── risk-summary-panel.component.*
│   │   │       └── charts/
│   │   │           ├── bar-comparison.component.ts
│   │   │           ├── pie-distribution.component.ts
│   │   │           └── radar-performance.component.ts
│   │   │
│   │   └── environments/                    # Environment Config
│   │       ├── environment.ts               # Development
│   │       └── environment.prod.ts          # Production
│   │
│   ├── index.html                           # Main HTML
│   ├── main.ts                              # Bootstrap
│   ├── material-theme.scss                  # Material Theme
│   └── styles.css                           # Global Styles
│
├── package.json                             # Dependencies
├── angular.json                             # Angular Config
├── tsconfig.json                            # TypeScript Config
├── proxy.conf.json                          # Dev Proxy
├── Dockerfile                               # Container Image
├── nginx.conf                               # Nginx Config
└── README.md
```

---

## 🔄 API Endpoints Overview

### Authentication Endpoints
```
POST   /api/auth/register           # User Registration
POST   /api/auth/login              # User Login
POST   /api/auth/verify-otp         # OTP Verification
POST   /api/auth/forgot-password    # Password Recovery
POST   /api/auth/reset-password     # Reset Password
GET    /api/auth/me                 # Get Current User
POST   /api/auth/refresh            # Refresh Token
```

### KPI Management
```
GET    /api/kpis                    # List All KPIs
GET    /api/kpis/{id}               # Get KPI Details
POST   /api/kpis                    # Create KPI
PUT    /api/kpis/{id}               # Update KPI
DELETE /api/kpis/{id}               # Delete KPI
GET    /api/kpis/category/{cat}     # Filter by Category
```

### Import/Export
```
POST   /api/import/upload           # Upload Import File
GET    /api/import/sessions         # List Import Sessions
GET    /api/import/sessions/{id}    # Session Details
POST   /api/import/process/{id}     # Process Import
GET    /api/import/preview/{id}     # Preview Before Import

POST   /api/export/kpis             # Export KPIs
POST   /api/export/analysis         # Export Analysis
POST   /api/export/report           # Generate Report
```

### AI Analysis
```
POST   /api/analyse-ia              # Trigger AI Analysis
GET    /api/analyse-ia/kpi/{id}     # Get AI Analysis for KPI
GET    /api/analyse-ia/batch        # Batch Analysis Status
POST   /api/analyse-ia/8d-method    # 8D Method Analysis
```

### Dashboard
```
GET    /api/dashboard/admin/stats   # Admin Stats
GET    /api/dashboard/analyste/metrics  # Analyste Metrics
GET    /api/dashboard/kpis/by-risk  # KPIs by Risk Level
GET    /api/dashboard/trends        # Trend Analysis
```

### Data Mapping
```
GET    /api/mapping/fields          # Get Available Fields
POST   /api/mapping/save            # Save Field Mapping
GET    /api/mapping/templates       # Get Mapping Templates
```

---

## 🔐 Authentication & Security

### Flow
```
1. User Registration/Login
2. JWT Token Generation
3. Token Stored in LocalStorage
4. Auth Interceptor Adds Token to Headers
5. Backend Validates JWT
6. Role-Based Access Control (RBAC)
   - ADMIN: Full System Access
   - ANALYSTE: Dashboard & Analysis Access
```

### Guards
- **auth.guard.ts**: Ensures User is Logged In
- **admin.guard.ts**: Ensures ADMIN Role
- **analyste.guard.ts**: Ensures ANALYSTE Role

---

## 📊 Database Schema

### Key Tables
```sql
-- Users & Roles
users
├── id (PK)
├── email (UNIQUE)
├── password (HASHED)
├── firstName
├── lastName
├── role (FK to roles)
└── timestamps

roles
├── id (PK)
├── name (ADMIN, ANALYSTE)
└── permissions

-- KPI Data
kpis
├── id (PK)
├── name
├── category
├── unit
├── definition
├── value_n (2023)
├── value_n1 (2022)
├── status
├── variation_percent
├── ecart
└── timestamps

-- Import Management
import_sessions
├── id (PK)
├── user_id (FK)
├── file_name
├── status (PENDING, PROCESSING, SUCCESS, ERROR)
├── mode
├── file_path
└── timestamps

import_session_lines
├── id (PK)
├── session_id (FK)
├── kpi_data (JSON)
└── status

-- AI Analysis
analyses
├── id (PK)
├── kpi_id (FK)
├── risk_level (FAIBLE, MODERE, ELEVE)
├── issue_detected
├── issue_type
├── corrective_action
├── preventive_action
├── immediate_action
├── priority_level
├── requires_8d (BOOLEAN)
├── eight_d_details (JSON)
├── ai_note
└── timestamps
```

---

## 🤖 AI Integration

### Current Setup
- **Provider**: Ollama (Local LLM)
- **Model**: Configurable
- **Integration Points**:
  - `AnalyseIaController.java` - API Endpoints
  - `AIAnalysisService` - Business Logic
  - DTOs for Ollama Communication

### Features
1. Per-KPI Analysis
2. Risk Detection
3. Issue Identification
4. 8D Method Generation
5. Batch Processing
6. RAG Knowledge Base (Ready to Implement)

---

## 🚀 Docker & Deployment

### Backend Dockerfile
```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
EXPOSE 8080
```

### Frontend Dockerfile
```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist/frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
```

### Running with Docker
```bash
# Backend
docker build -t qhse-analytics-backend .
docker run -p 8080:8080 qhse-analytics-backend

# Frontend
docker build -f frontend/Dockerfile -t qhse-analytics-frontend ./frontend
docker run -p 80:80 qhse-analytics-frontend
```

---

## 🎯 Key Features Implemented

### ✅ Authentication
- User Registration & Login
- JWT Token Management
- OTP Verification
- Password Reset
- Role-Based Access Control

### ✅ KPI Management
- CRUD Operations
- Category Filtering
- Status Tracking
- Historical Data

### ✅ Import/Export
- File Upload Support
- Field Mapping
- Batch Processing
- Export Reports

### ✅ AI Analysis (Ollama)
- Per-KPI Analysis
- Risk Detection
- 8D Method Generation
- Batch Processing

### ✅ Dashboard
- Admin Overview
- Analyste Metrics
- Risk Visualization
- Trend Analysis
- Charts & Graphs

### ✅ Data Visualization
- Material Chart Components
- Bar Comparison
- Pie Distribution
- Radar Performance

---

## 📋 Current Issues & TODOs

### Backend
- [ ] Implement RAG Knowledge Base
- [ ] Add Caching Layer (Redis)
- [ ] Implement Data Validation
- [ ] Add Audit Logging
- [ ] Unit Test Coverage

### Frontend
- [ ] Complete Analyste Dashboard
- [ ] Add Real-time Notifications
- [ ] Implement Chart Interactions
- [ ] Add Export Functionality
- [ ] Mobile Responsiveness

### DevOps
- [ ] Kubernetes Deployment Config
- [ ] CI/CD Pipeline (GitHub Actions)
- [ ] Environment-specific Config
- [ ] Database Backup Strategy

---

## 📚 Dependencies

### Backend (Maven)
```xml
<!-- Spring Boot & Web -->
spring-boot-starter-web
spring-boot-starter-security
spring-boot-starter-data-jpa

<!-- Database -->
mysql-connector-java
flyway-core

<!-- API Documentation -->
springdoc-openapi-starter-webmvc-ui

<!-- Authentication -->
jjwt

<!-- Async -->
spring-boot-starter-data-redis

<!-- Testing -->
spring-boot-starter-test
```

### Frontend (NPM)
```json
{
  "@angular/core": "^21.2.8",
  "@angular/material": "^21.x",
  "@angular/cdk": "^21.x",
  "chart.js": "^4.x",
  "ng2-charts": "^4.x",
  "rxjs": "^7.x"
}
```

---

## 🔧 Configuration Files

### Backend: application.properties
```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/qhse_db
spring.datasource.username=root
spring.datasource.password=password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# JWT
jwt.secret=your-secret-key
jwt.expiration=86400000

# Ollama AI
ollama.api.url=http://localhost:11434
ollama.model=llama2

# CORS
cors.allowed-origins=http://localhost:4200

# Async
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
```

### Frontend: environment.ts
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  aiServiceUrl: 'http://localhost:11434'
};
```

---

## 🚀 Getting Started

### Backend Setup
```bash
cd QHSEAnalytics

# Build
mvn clean install

# Run
mvn spring-boot:run

# Access API Docs
http://localhost:8080/swagger-ui.html
```

### Frontend Setup
```bash
cd frontend

# Install Dependencies
npm install

# Development Server
ng serve

# Access Application
http://localhost:4200
```

---

## 📞 Support & Documentation

### API Documentation
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### Logs
- Backend: `logs/qhse-analytics.log`
- Frontend: Browser Console

---

## 📝 Version Control

- **Current Branch**: main
- **Repository**: ZeinebBenJeddou/QHSE-Analytics
- **Last Updated**: May 3, 2026

---

## 🎓 Learning Resources

### Backend
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Spring Security: https://spring.io/projects/spring-security
- JPA/Hibernate: https://hibernate.org/orm/

### Frontend
- Angular Documentation: https://angular.io/docs
- Angular Material: https://material.angular.io/
- TypeScript: https://www.typescriptlang.org/docs/

### AI/ML
- Ollama: https://ollama.ai/
- RAG Patterns: https://en.wikipedia.org/wiki/Retrieval-augmented_generation

---

## ✨ Next Steps

1. **Complete AI Analysis Implementation**
   - Implement RAG Knowledge Base
   - Enhance 8D Method Generation
   - Add Batch Processing Queue

2. **Frontend Enhancement**
   - Complete Dashboard Pages
   - Add Real-time Updates
   - Implement Data Export

3. **Testing**
   - Add Unit Tests
   - Integration Tests
   - E2E Tests

4. **DevOps**
   - Setup CI/CD Pipeline
   - Configure Kubernetes
   - Add Monitoring & Logging

5. **Documentation**
   - API Documentation
   - User Manual
   - Developer Guide# 📊 QHSE-Analytics - Complete Project Analysis

## 🏗️ Project Architecture

### Tech Stack

#### Backend
- **Framework**: Spring Boot (Java)
- **Build Tool**: Maven
- **Database**: SQL (Flyway migrations)
- **Authentication**: Security Config (JWT/OAuth)
- **API Documentation**: OpenAPI/Swagger
- **Async Processing**: Spring Async
- **CORS**: Configured

#### Frontend
- **Framework**: Angular 21.2.8
- **Styling**: SCSS + Angular Material
- **HTTP Client**: Angular HttpClient with Interceptors
- **State Management**: Services
- **Build**: Vite
- **Package Manager**: npm
- **UI Components**: Material Design

#### DevOps
- **Containerization**: Docker (Both Backend & Frontend)
- **Reverse Proxy**: Nginx
- **Container Orchestration**: Ready for Kubernetes

---

## 📁 Backend Structure (Spring Boot)

```
QHSEAnalytics/
├── src/main/java/com/QHSEAnalytics/
│   ├── QhseAnalyticsApplication.java          # Main Application Entry
│   │
│   ├── auth/                                   # Authentication Module
│   │   ├── controller/                        # Auth Endpoints
│   │   ├── dto/                               # Auth DTOs
│   │   ├── entity/                            # User/Role Entities
│   │   ├── exception/                         # Auth Exceptions
│   │   ├── handler/                           # Exception Handlers
│   │   ├── repository/                        # DB Repositories
│   │   └── service/                           # Auth Business Logic
│   │
│   ├── config/                                # Configuration Classes
│   │   ├── AdminInitializer.java             # Init Default Admin
│   │   ├── AsyncConfig.java                  # Async Task Config
│   │   ├── CorsConfig.java                   # CORS Configuration
│   │   ├── ImportSessionModeConstraintUpdater.java
│   │   ├── ImportSessionStatutConstraintUpdater.java
│   │   ├── OpenApiConfig.java                # Swagger/OpenAPI
│   │   └── SecurityConfig.java               # Spring Security
│   │
│   ├── controller/                            # REST Controllers
│   │   ├── AnalyseIaController.java          # AI Analysis Endpoints
│   │   ├── AnalysisController.java           # General Analysis
│   │   ├── DashboardAdminController.java     # Admin Dashboard
│   │   ├── DashboardAnalysteController.java  # Analyst Dashboard
│   │   ├── ExportController.java             # Export Data
│   │   ├── ImportProcessingController.java   # Import Processing
│   │   ├── ImportSessionController.java      # Session Management
│   │   ├── KpiController.java                # KPI Operations
│   │   └── MappingController.java            # Data Mapping
│   │
│   ├── dto/                                   # Data Transfer Objects
│   │   ├── Request DTOs                      # For API Requests
│   │   ├── Response DTOs                     # For API Responses
│   │   └── Ollama DTOs                       # AI Integration
│   │
│   ├── entity/                                # JPA Entities (Database Models)
│   │   ├── User Entity
│   │   ├── Role Entity
│   │   ├── KPI Entity
│   │   ├── ImportSession Entity
│   │   ├── Analysis Entity
│   │   └── ... Other Domain Entities
│   │
│   ├── enums/                                 # Enumeration Classes
│   │   ├── UserRole (ADMIN, ANALYSTE)
│   │   ├── ImportSessionStatus
│   │   ├── KPIStatus
│   │   └── RiskLevel (FAIBLE, MODERE, ELEVE)
│   │
│   ├── exception/                             # Custom Exceptions
│   │   ├── BusinessException
│   │   ├── NotFoundException
│   │   └── ValidationException
│   │
│   ├── initializer/                           # Data Initialization
│   │   └── Database Seeders
│   │
│   ├── repository/                            # Spring Data JPA
│   │   ├── UserRepository
│   │   ├── KpiRepository
│   │   ├── ImportSessionRepository
│   │   ├── AnalysisRepository
│   │   └── Custom Query Repositories
│   │
│   ├── security/                              # Security Filters/Handlers
│   │   ├── JWT Handler
│   │   └── Auth Providers
│   │
│   └── service/                               # Business Logic Layer
│       ├── AuthService
│       ├── KpiService
│       ├── ImportService
│       ├── AnalysisService (AI)
│       ├── DashboardService
│       ├── ExportService
│       └── processing/                        # Async Processing
│
└── src/main/resources/
    ├── application.properties                 # Configuration
    └── db/migration/                          # Flyway Migrations
        └── V1__*.sql                          # Database Schemas
```

---

## 🎨 Frontend Structure (Angular)

```
frontend/
├── src/
│   ├── app/
│   │   ├── app.config.ts                     # App Configuration
│   │   ├── app.routes.ts                     # Routing Configuration
│   │   ├── app.ts                            # Root Component
│   │   │
│   │   ├── core/                             # Singleton Services
│   │   │   ├── guards/                       # Route Guards
│   │   │   │   ├── admin.guard.ts           # Admin Protection
│   │   │   │   ├── analyste.guard.ts        # Analyste Protection
│   │   │   │   └── auth.guard.ts            # General Auth
│   │   │   │
│   │   │   ├── interceptors/                # HTTP Interceptors
│   │   │   │   └── auth.interceptor.ts      # Auto JWT Token
│   │   │   │
│   │   │   ├── models/                      # TypeScript Interfaces
│   │   │   │   ├── analyse-ia.model.ts      # AI Analysis
│   │   │   │   ├── dashboard.model.ts       # Dashboard Data
│   │   │   │   └── import-session.model.ts  # Import Session
│   │   │   │
│   │   │   └── services/                    # Core Services
│   │   │       ├── admin.service.ts         # Admin Operations
│   │   │       ├── ai-analysis.service.ts   # AI Analysis Calls
│   │   │       ├── auth.service.ts          # Authentication
│   │   │       ├── dashboard.service.ts     # Dashboard Data
│   │   │       ├── import.service.ts        # Import Operations
│   │   │       ├── import-upload-state.service.ts
│   │   │       └── token.service.ts         # Token Management
│   │   │
│   │   ├── features/                        # Feature Modules
│   │   │   ├── admin/                       # Admin Portal
│   │   │   │   ├── admin-shell.component.*  # Shell Layout
│   │   │   │   ├── models/
│   │   │   │   │   └── admin.models.ts
│   │   │   │   └── pages/
│   │   │   │       ├── historique/          # History Page
│   │   │   │       ├── kpis/                # KPI Management
│   │   │   │       ├── overview/            # Overview Page
│   │   │   │       ├── profile/             # User Profile
│   │   │   │       └── users/               # User Management
│   │   │   │
│   │   │   ├── analyste/                    # Analyste Portal
│   │   │   │   ├── analyste-shell.component.*
│   │   │   │   └── pages/
│   │   │   │       ├── dashboard/           # Analytics Dashboard
│   │   │   │       ├── historique/          # History
│   │   │   │       ├── ia-insights/         # AI Insights
│   │   │   │       ├── import/              # Import Data
│   │   │   │       └── import-mapping/      # Field Mapping
│   │   │   │
│   │   │   └── auth/                        # Auth Module
│   │   │       ├── models/
│   │   │       │   └── auth.models.ts
│   │   │       └── pages/
│   │   │           ├── dashboard/
│   │   │           ├── forgot-password/
│   │   │           ├── home/
│   │   │           ├── login/
│   │   │           ├── otp/
│   │   │           ├── register/
│   │   │           ├── reset-password/
│   │   │           └── verify-account/
│   │   │
│   │   ├── shared/                          # Shared Resources
│   │   │   ├── material.module.ts           # Material Module
│   │   │   └── components/
│   │   │       ├── risk-summary-panel.component.*
│   │   │       └── charts/
│   │   │           ├── bar-comparison.component.ts
│   │   │           ├── pie-distribution.component.ts
│   │   │           └── radar-performance.component.ts
│   │   │
│   │   └── environments/                    # Environment Config
│   │       ├── environment.ts               # Development
│   │       └── environment.prod.ts          # Production
│   │
│   ├── index.html                           # Main HTML
│   ├── main.ts                              # Bootstrap
│   ├── material-theme.scss                  # Material Theme
│   └── styles.css                           # Global Styles
│
├── package.json                             # Dependencies
├── angular.json                             # Angular Config
├── tsconfig.json                            # TypeScript Config
├── proxy.conf.json                          # Dev Proxy
├── Dockerfile                               # Container Image
├── nginx.conf                               # Nginx Config
└── README.md
```

---

## 🔄 API Endpoints Overview

### Authentication Endpoints
```
POST   /api/auth/register           # User Registration
POST   /api/auth/login              # User Login
POST   /api/auth/verify-otp         # OTP Verification
POST   /api/auth/forgot-password    # Password Recovery
POST   /api/auth/reset-password     # Reset Password
GET    /api/auth/me                 # Get Current User
POST   /api/auth/refresh            # Refresh Token
```

### KPI Management
```
GET    /api/kpis                    # List All KPIs
GET    /api/kpis/{id}               # Get KPI Details
POST   /api/kpis                    # Create KPI
PUT    /api/kpis/{id}               # Update KPI
DELETE /api/kpis/{id}               # Delete KPI
GET    /api/kpis/category/{cat}     # Filter by Category
```

### Import/Export
```
POST   /api/import/upload           # Upload Import File
GET    /api/import/sessions         # List Import Sessions
GET    /api/import/sessions/{id}    # Session Details
POST   /api/import/process/{id}     # Process Import
GET    /api/import/preview/{id}     # Preview Before Import

POST   /api/export/kpis             # Export KPIs
POST   /api/export/analysis         # Export Analysis
POST   /api/export/report           # Generate Report
```

### AI Analysis
```
POST   /api/analyse-ia              # Trigger AI Analysis
GET    /api/analyse-ia/kpi/{id}     # Get AI Analysis for KPI
GET    /api/analyse-ia/batch        # Batch Analysis Status
POST   /api/analyse-ia/8d-method    # 8D Method Analysis
```

### Dashboard
```
GET    /api/dashboard/admin/stats   # Admin Stats
GET    /api/dashboard/analyste/metrics  # Analyste Metrics
GET    /api/dashboard/kpis/by-risk  # KPIs by Risk Level
GET    /api/dashboard/trends        # Trend Analysis
```

### Data Mapping
```
GET    /api/mapping/fields          # Get Available Fields
POST   /api/mapping/save            # Save Field Mapping
GET    /api/mapping/templates       # Get Mapping Templates
```

---

## 🔐 Authentication & Security

### Flow
```
1. User Registration/Login
2. JWT Token Generation
3. Token Stored in LocalStorage
4. Auth Interceptor Adds Token to Headers
5. Backend Validates JWT
6. Role-Based Access Control (RBAC)
   - ADMIN: Full System Access
   - ANALYSTE: Dashboard & Analysis Access
```

### Guards
- **auth.guard.ts**: Ensures User is Logged In
- **admin.guard.ts**: Ensures ADMIN Role
- **analyste.guard.ts**: Ensures ANALYSTE Role

---

## 📊 Database Schema

### Key Tables
```sql
-- Users & Roles
users
├── id (PK)
├── email (UNIQUE)
├── password (HASHED)
├── firstName
├── lastName
├── role (FK to roles)
└── timestamps

roles
├── id (PK)
├── name (ADMIN, ANALYSTE)
└── permissions

-- KPI Data
kpis
├── id (PK)
├── name
├── category
├── unit
├── definition
├── value_n (2023)
├── value_n1 (2022)
├── status
├── variation_percent
├── ecart
└── timestamps

-- Import Management
import_sessions
├── id (PK)
├── user_id (FK)
├── file_name
├── status (PENDING, PROCESSING, SUCCESS, ERROR)
├── mode
├── file_path
└── timestamps

import_session_lines
├── id (PK)
├── session_id (FK)
├── kpi_data (JSON)
└── status

-- AI Analysis
analyses
├── id (PK)
├── kpi_id (FK)
├── risk_level (FAIBLE, MODERE, ELEVE)
├── issue_detected
├── issue_type
├── corrective_action
├── preventive_action
├── immediate_action
├── priority_level
├── requires_8d (BOOLEAN)
├── eight_d_details (JSON)
├── ai_note
└── timestamps
```

---

## 🤖 AI Integration

### Current Setup
- **Provider**: Ollama (Local LLM)
- **Model**: Configurable
- **Integration Points**:
  - `AnalyseIaController.java` - API Endpoints
  - `AIAnalysisService` - Business Logic
  - DTOs for Ollama Communication

### Features
1. Per-KPI Analysis
2. Risk Detection
3. Issue Identification
4. 8D Method Generation
5. Batch Processing
6. RAG Knowledge Base (Ready to Implement)

---

## 🚀 Docker & Deployment

### Backend Dockerfile
```dockerfile
FROM openjdk:17-slim
WORKDIR /app
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
EXPOSE 8080
```

### Frontend Dockerfile
```dockerfile
FROM node:18-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist/frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 80
```

### Running with Docker
```bash
# Backend
docker build -t qhse-analytics-backend .
docker run -p 8080:8080 qhse-analytics-backend

# Frontend
docker build -f frontend/Dockerfile -t qhse-analytics-frontend ./frontend
docker run -p 80:80 qhse-analytics-frontend
```

---

## 🎯 Key Features Implemented

### ✅ Authentication
- User Registration & Login
- JWT Token Management
- OTP Verification
- Password Reset
- Role-Based Access Control

### ✅ KPI Management
- CRUD Operations
- Category Filtering
- Status Tracking
- Historical Data

### ✅ Import/Export
- File Upload Support
- Field Mapping
- Batch Processing
- Export Reports

### ✅ AI Analysis (Ollama)
- Per-KPI Analysis
- Risk Detection
- 8D Method Generation
- Batch Processing

### ✅ Dashboard
- Admin Overview
- Analyste Metrics
- Risk Visualization
- Trend Analysis
- Charts & Graphs

### ✅ Data Visualization
- Material Chart Components
- Bar Comparison
- Pie Distribution
- Radar Performance

---

## 📋 Current Issues & TODOs

### Backend
- [ ] Implement RAG Knowledge Base
- [ ] Add Caching Layer (Redis)
- [ ] Implement Data Validation
- [ ] Add Audit Logging
- [ ] Unit Test Coverage

### Frontend
- [ ] Complete Analyste Dashboard
- [ ] Add Real-time Notifications
- [ ] Implement Chart Interactions
- [ ] Add Export Functionality
- [ ] Mobile Responsiveness

### DevOps
- [ ] Kubernetes Deployment Config
- [ ] CI/CD Pipeline (GitHub Actions)
- [ ] Environment-specific Config
- [ ] Database Backup Strategy

---

## 📚 Dependencies

### Backend (Maven)
```xml
<!-- Spring Boot & Web -->
spring-boot-starter-web
spring-boot-starter-security
spring-boot-starter-data-jpa

<!-- Database -->
mysql-connector-java
flyway-core

<!-- API Documentation -->
springdoc-openapi-starter-webmvc-ui

<!-- Authentication -->
jjwt

<!-- Async -->
spring-boot-starter-data-redis

<!-- Testing -->
spring-boot-starter-test
```

### Frontend (NPM)
```json
{
  "@angular/core": "^21.2.8",
  "@angular/material": "^21.x",
  "@angular/cdk": "^21.x",
  "chart.js": "^4.x",
  "ng2-charts": "^4.x",
  "rxjs": "^7.x"
}
```

---

## 🔧 Configuration Files

### Backend: application.properties
```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/qhse_db
spring.datasource.username=root
spring.datasource.password=password

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# JWT
jwt.secret=your-secret-key
jwt.expiration=86400000

# Ollama AI
ollama.api.url=http://localhost:11434
ollama.model=llama2

# CORS
cors.allowed-origins=http://localhost:4200

# Async
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
```

### Frontend: environment.ts
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  aiServiceUrl: 'http://localhost:11434'
};
```

---

## 🚀 Getting Started

### Backend Setup
```bash
cd QHSEAnalytics

# Build
mvn clean install

# Run
mvn spring-boot:run

# Access API Docs
http://localhost:8080/swagger-ui.html
```

### Frontend Setup
```bash
cd frontend

# Install Dependencies
npm install

# Development Server
ng serve

# Access Application
http://localhost:4200
```

---

## 📞 Support & Documentation

### API Documentation
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### Logs
- Backend: `logs/qhse-analytics.log`
- Frontend: Browser Console

---

## 📝 Version Control

- **Current Branch**: main
- **Repository**: ZeinebBenJeddou/QHSE-Analytics
- **Last Updated**: May 3, 2026

---

## 🎓 Learning Resources

### Backend
- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Spring Security: https://spring.io/projects/spring-security
- JPA/Hibernate: https://hibernate.org/orm/

### Frontend
- Angular Documentation: https://angular.io/docs
- Angular Material: https://material.angular.io/
- TypeScript: https://www.typescriptlang.org/docs/

### AI/ML
- Ollama: https://ollama.ai/
- RAG Patterns: https://en.wikipedia.org/wiki/Retrieval-augmented_generation

---

## ✨ Next Steps

1. **Complete AI Analysis Implementation**
   - Implement RAG Knowledge Base
   - Enhance 8D Method Generation
   - Add Batch Processing Queue

2. **Frontend Enhancement**
   - Complete Dashboard Pages
   - Add Real-time Updates
   - Implement Data Export

3. **Testing**
   - Add Unit Tests
   - Integration Tests
   - E2E Tests

4. **DevOps**
   - Setup CI/CD Pipeline
   - Configure Kubernetes
   - Add Monitoring & Logging

5. **Documentation**
   - API Documentation
   - User Manual
   - Developer Guide