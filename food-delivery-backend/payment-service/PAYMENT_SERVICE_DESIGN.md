# 💳 Payment Service — Requirements & System Design

## 📌 Overview

The **Payment Service** is a core microservice in the Food Delivery Backend responsible for processing
payments after an order is placed. It is decoupled from the Order Service via **Apache Kafka**, ensuring
fault-tolerance and async processing. It uses the **Strategy Pattern** for payment methods and the
**Adapter Pattern** for external payment provider integration.

---

## 🎯 Business Requirements

### Functional Requirements

| # | Requirement |
|---|---|
| FR-01 | Process payment for a placed order (UPI, Card, Cash on Delivery) |
| FR-02 | Support multiple payment methods via Strategy Pattern |
| FR-03 | Record every payment attempt with full audit trail |
| FR-04 | Handle payment success — emit `payment.confirmed` event |
| FR-05 | Handle payment failure — emit `payment.failed` event with reason |
| FR-06 | Allow fetching payment status by `orderId` or `paymentId` |
| FR-07 | Support refund initiation for cancelled orders |
| FR-08 | Idempotent payment: prevent duplicate charges for same order |

### Non-Functional Requirements

| # | Requirement |
|---|---|
| NFR-01 | Async processing via Kafka — Order Service is never blocked |
| NFR-02 | Payment secrets (API keys) stored in environment variables / K8s secrets |
| NFR-03 | All payment transactions persisted in MySQL with full status history |
| NFR-04 | Service must be independently deployable and testable |
| NFR-05 | Retry mechanism for transient failures |

---

## 🔗 Service Interactions

```
┌─────────────────────────────────────────────────────────┐
│                    API Gateway (8080)                   │
└─────────────────────┬───────────────────────────────────┘
                      │ JWT-authenticated REST
                      ▼
┌─────────────────────────────────────────────────────────┐
│                   Payment Service (8084)                │
│                                                         │
│  REST Controller → PaymentService → PaymentStrategy     │
│                                   → PaymentRepository   │
│                                   → KafkaProducer       │
└───────────────┬─────────────────────────┬───────────────┘
                │ Consumes                │ Produces
                ▼                         ▼
     ┌──────────────────┐      ┌──────────────────────────┐
     │ order.placed     │      │ payment.confirmed        │
     │ (from Order Svc) │      │ payment.failed           │
     └──────────────────┘      └──────────────────────────┘
                                          │
                          ┌───────────────┼──────────────┐
                          ▼               ▼              ▼
                    Order Service   Restaurant Svc  Notification Svc
                    (status update) (start prep)    (send receipt)
```

---

## 🗂️ Kafka Topics

| Topic | Producer | Consumers | Payload |
|---|---|---|---|
| `order.placed` | Order Service | Payment Service | `OrderPlacedEvent` |
| `payment.confirmed` | Payment Service | Order Service, Restaurant Service | `PaymentConfirmedEvent` |
| `payment.failed` | Payment Service | Order Service | `PaymentFailedEvent` |

### Event Schemas

#### `OrderPlacedEvent` (Consumed by Payment Service)
```json
{
  "orderId": 101,
  "userId": 42,
  "restaurantId": 7,
  "totalAmount": 450.00,
  "paymentMethod": "UPI",
  "timestamp": "2026-04-19T10:00:00"
}
```

#### `PaymentConfirmedEvent` (Produced by Payment Service)
```json
{
  "paymentId": 55,
  "orderId": 101,
  "userId": 42,
  "amount": 450.00,
  "paymentMethod": "UPI",
  "transactionId": "TXN-123456",
  "timestamp": "2026-04-19T10:00:05"
}
```

#### `PaymentFailedEvent` (Produced by Payment Service)
```json
{
  "paymentId": 56,
  "orderId": 101,
  "userId": 42,
  "amount": 450.00,
  "reason": "Insufficient balance",
  "timestamp": "2026-04-19T10:00:05"
}
```

---

## 🗃️ Database Design

### Table: `payments`

| Column | Type | Constraints | Description |
|---|---|---|---|
| `id` | BIGINT | PK, AUTO_INCREMENT | Payment ID |
| `order_id` | BIGINT | NOT NULL, UNIQUE | One payment per order |
| `user_id` | BIGINT | NOT NULL | Customer who paid |
| `amount` | DECIMAL(10,2) | NOT NULL | Total payment amount |
| `payment_method` | VARCHAR(50) | NOT NULL | UPI, CARD, COD |
| `status` | VARCHAR(50) | NOT NULL | PENDING, SUCCESS, FAILED, REFUNDED |
| `transaction_id` | VARCHAR(255) | NULLABLE | External provider txn ID |
| `failure_reason` | VARCHAR(500) | NULLABLE | Reason if FAILED |
| `created_at` | DATETIME | AUTO | Audit timestamp |
| `updated_at` | DATETIME | AUTO | Last status update |

---

## 🏗️ Package Structure

```
payment-service/
└── src/main/java/com/payment_service/
    ├── PaymentServiceApplication.java
    ├── config/
    │   ├── KafkaConsumerConfig.java
    │   └── KafkaProducerConfig.java
    ├── controller/
    │   └── PaymentController.java          ← REST: GET payment status, refund
    ├── dto/
    │   ├── PaymentDto.java                 ← Response DTO
    │   ├── PaymentRequestDto.java          ← REST request (manual pay trigger)
    │   └── event/
    │       ├── OrderPlacedEvent.java       ← Kafka consumed
    │       ├── PaymentConfirmedEvent.java  ← Kafka produced
    │       └── PaymentFailedEvent.java     ← Kafka produced
    ├── entity/
    │   └── Payment.java
    ├── kafka/
    │   ├── PaymentEventConsumer.java       ← Listens to order.placed
    │   └── PaymentEventProducer.java       ← Sends payment.confirmed / failed
    ├── repository/
    │   └── PaymentRepository.java
    ├── service/
    │   ├── PaymentService.java             ← Interface
    │   └── PaymentServiceImpl.java         ← Orchestrator
    └── strategy/
        ├── PaymentStrategy.java            ← Interface
        ├── UpiPaymentStrategy.java         ← UPI implementation
        ├── CardPaymentStrategy.java        ← Card implementation
        ├── CodPaymentStrategy.java         ← COD implementation
        └── PaymentStrategyFactory.java     ← Factory resolves strategy
```

---

## 🎨 Design Patterns

### 1. Strategy Pattern — Payment Methods

```
           «interface»
          PaymentStrategy
         ┌──────────────┐
         │ + pay(amount)│
         └──────┬───────┘
                │
    ┌───────────┼───────────┐
    ▼           ▼           ▼
UpiStrategy  CardStrategy  CodStrategy
```

Each strategy independently handles the payment logic for its method. Adding a new payment provider (e.g., Wallet) requires only adding a new strategy — no existing code changes.

### 2. Factory Pattern — Strategy Resolution

```java
// PaymentStrategyFactory resolves the correct strategy at runtime
PaymentStrategy strategy = factory.getStrategy(paymentMethod); // "UPI" → UpiStrategy
strategy.pay(order.getTotalAmount());
```

### 3. Adapter Pattern — External Provider (Future)

```
PaymentStrategy → ExternalProviderAdapter → RazorpayClient / StripeClient
```

The adapter translates internal payment requests to the external provider's SDK format, isolating external API changes from business logic.

---

## 🔄 Payment Lifecycle / State Machine

```
                  ┌──────────┐
                  │  PENDING │  ← Created when order.placed consumed
                  └────┬─────┘
           ┌───────────┴───────────┐
           ▼                       ▼
     ┌──────────┐           ┌──────────┐
     │ SUCCESS  │           │  FAILED  │
     └────┬─────┘           └────┬─────┘
          │                       │
          ▼                       ▼
  payment.confirmed          payment.failed
     (→ Order,                (→ Order
    Restaurant)               cancels)
          │
    ┌─────┴───────┐
    ▼             ▼
 (Normal)     REFUNDED ← If order cancelled after payment
```

---

## 🌐 REST API Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/api/payments/{paymentId}` | JWT | Get payment details by ID |
| `GET` | `/api/payments/order/{orderId}` | JWT | Get payment for an order |
| `GET` | `/api/payments/user/{userId}` | JWT | Get all payments for a user |
| `POST` | `/api/payments/{paymentId}/refund` | JWT (ADMIN) | Initiate refund |

> **Note:** Payment creation is **NOT triggered via REST**. It is triggered automatically when the
> `order.placed` Kafka event is consumed. This ensures the Order Service is never coupled to Payment Service.

---

## ⚙️ Configuration (`application.yml`)

```yaml
server:
  port: 8084

spring:
  application:
    name: payment-service
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/payment_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:Vishal@90}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: payment-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

---

## 🐳 Docker Compose Addition

```yaml
  payment-service:
    build:
      context: ./payment-service
      dockerfile: Dockerfile
    image: vishalsharma/payment-service:latest
    container_name: payment_service
    restart: on-failure
    ports:
      - "8084:8084"
    environment:
      - MYSQL_HOST=mysql-db
      - MYSQL_PORT=3306
      - MYSQL_USER=root
      - MYSQL_PASSWORD=Vishal@90
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
    depends_on:
      - mysql-db
      - kafka
```

---

## 🧪 Test Plan

| Test | Type | Verifies |
|---|---|---|
| `processPayment_UPI_success` | Unit | UPI strategy returns SUCCESS |
| `processPayment_Card_failure` | Unit | Card strategy handles decline |
| `onOrderPlaced_triggersPayment` | Unit | Kafka consumer triggers payment |
| `idempotency_sameOrderNoDoubleCharge` | Unit | Duplicate event ignored |
| `paymentConfirmedEvent_published` | Integration | Kafka producer publishes event |
| `getPaymentByOrderId_returns200` | Integration | REST endpoint works |

---

## 📦 Maven Dependencies (pom.xml additions)

```xml
<!-- Spring Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- Spring Boot Web, JPA, Validation, MySQL -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

---

## 🚦 Inter-Service Kafka Flow (End-to-End)

```
User places order
      │
      ▼
Order Service
 createOrder() ──────────────────────────────► Kafka: order.placed
      │                                               │
      │                                    ┌──────────┘
      │                                    ▼
      │                            Payment Service
      │                           onOrderPlaced()
      │                          getStrategy(method)
      │                           strategy.pay()
      │                                    │
      │                      ┌─────────────┼──────────────┐
      │                      ▼             ▼              ▼
      │              payment.confirmed  payment.failed
      │                      │
      │          ┌───────────┴──────────────┐
      │          ▼                          ▼
      │   Order Service             Restaurant Service
      │  updateStatus(CONFIRMED)   (order confirmed, start prep)
      │
      ▼
User sees CONFIRMED order
```

---

*Document Version: 1.0 | Author: Vishal Sharma | Date: 2026-04-19*
