# MultiTenant SaaS POS System

A robust, multi-tenant Point of Sale (POS) backend system tailored for retailers, designed to run seamlessly on the cloud. Built with Spring Boot, it provides comprehensive features for inventory management, branch operations, shift tracking, and robust payment integrations, supporting diverse payment methods including global cards and regional mobile money.

## 🚀 Key Features

*   **Multi-Tenancy Architecture:** Secure and isolated data and configuration management for multiple retailer tenants operating within a single application instance.
*   **Branch & Stock Management:** Centralized control over product catalogs, with real-time stock synchronization and visibility across different branches, admin panels, and customer storefronts.
*   **Order & Sales Tracking:** Streamlined order processing workflows and comprehensive multi-shift reporting capabilities (`ShiftReport`).
*   **Robust Security & Auth:** Stateless, JWT-based authentication combined with Spring Security for fine-grained role and access management.
*   **Payment Gateway Integrations:**
    *   **Stripe & Razorpay:** Out-of-the-box support for global credit/debit card processing.
    *   **Paypack Payments:** Native integration for Mobile Money transactions (e.g., MTN MoMo, Airtel Money), specifically optimized for the Rwandan and regional East African markets.
*   **Email Notifications:** Integrated email services for receipts, alerts, and user communications.
*   **High Performance Data Caching:** Incorporates Redis for caching frequently accessed catalog/branch data, significantly reducing database load and latency.
*   **Interactive API Documentation:** Full API exploration and testing available via built-in Swagger/OpenAPI UI.
*   **Cloud-Native & Container-Ready:** Pre-configured for deployment using Docker, and scalable orchestration via Kubernetes (K3s) and Helm.

## 🛠️ Technology Stack

*   **Core:** Java 21, Spring Boot (Web, Data JPA, Security, Mail, Actuator)
*   **Database:** PostgreSQL (with HikariCP/commons-pool2)
*   **Caching:** Redis
*   **Security:** JSON Web Tokens (JJWT 0.13.0)
*   **API Specs:** Springdoc OpenAPI (Swagger 3.0.1)
*   **Build Tool:** Maven
*   **Utilities:** Lombok, Jackson (JSR310, Hibernate7), Apache Commons Lang3
*   **Deployment:** Docker, Kubernetes (K3s), Helm

## ⚙️ Prerequisites

*   **Java Development Kit (JDK) 21**
*   **Maven 3.8+**
*   **PostgreSQL 14+**
*   **Redis Server**
*   *(Optional)* Docker, Kubernetes (K3s), and Helm for production-like local testing

## 🚀 Getting Started

### 1. Database Setup

Create a PostgreSQL database for the application to connect to:
```sql
CREATE DATABASE msp_pos_db;
```

### 2. Application Configuration

Update the `src/main/resources/application.properties` (or `.yml`) file with your respective environment variables, database configuration, Redis, and payment gateway credentials:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/msp_pos_db
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Security / JWT Config
jwt.secret=your_super_secret_jwt_key
jwt.expiration=86400000

# Payment Gateways (Stripe, Razorpay, Paypack)
stripe.api.key=sk_test_...
razorpay.api.key=rzp_test_...
paypack.client.id=your_paypack_client_id
paypack.client.secret=your_paypack_client_secret
```

### 3. Build the Application

Use the Maven wrapper to quickly download dependencies and build the project cleanly:

```bash
./mvnw clean install -DskipTests
```

*(Note: To run tests, simply remove the `-DskipTests` flag).*

### 4. Running the Application locally

Start the Spring Boot application using the provided wrapper:

```bash
./mvnw spring-boot:run
```

The server will initialize and begin accepting connections by default on port `8080`.

## 📚 API Documentation

Once the backend is up and running, you can access the self-documenting API interface:

*   **Swagger UI (Interactive Console):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
*   **OpenAPI v3 JSON Specs:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## 🐳 Deployment Info

This application is packaged to be cloud-ready and easily deployable to any standard Kubernetes cluster.

**Building the Docker container:**
```bash
docker build -t msp-pos-backend:latest .
```

**Kubernetes (K3s) Deployment:**
The repository contains Helm charts or standard manifests within the `k8s/` directory. Assuming you have configured your context:
```bash
# Example helm command
helm upgrade --install msp-pos ./k8s/chart
```

## Load testing

k6 scripts live in the sibling `load-test/` folder (see [`../load-test/README.md`](../load-test/README.md)).

**Local conclusion (17 Aug 2026):** with this API on port **5000**, PostgreSQL, and Redis, public catalog and authenticated customer/store-admin reads stayed healthy at 5–10 concurrent users. Median latency was ~15–20 ms; p95 stayed under 100 ms. That is a **local smoke/load check**, not a production capacity rating.

Shop orders require a `customers` row as well as a `users` row. The demo customer seed now creates that profile. Concurrent add-to-cart on the **same** demo user showed a ~0.5% failure rate (2 requests); reads were clean.

```powershell
# API must already be running on http://localhost:5000
.\load-test\run-all.ps1 `
  -CustomerEmail "customer@posify.demo" `
  -CustomerPassword "Demo!123" `
  -AdminEmail "manager@posify.demo" `
  -AdminPassword "Demo!123"
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/NewEndpoint`)
3. Commit your changes (`git commit -m 'Add new REST endpoint for inventory'`)
4. Push to the branch (`git push origin feature/NewEndpoint`)
5. Open a Pull Request
