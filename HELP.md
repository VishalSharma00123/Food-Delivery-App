# Getting Started

### Reference Documentation
For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/4.0.5/maven-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/4.0.5/maven-plugin/build-image.html)

### Maven Parent overrides

Due to Maven's design, elements are inherited from the parent POM to the project POM.
While most of the inheritance is fine, it also inherits unwanted elements like `<license>` and `<developers>` from the parent.
To prevent this, the project POM contains empty overrides for these elements.
If you manually switch to a different parent and actually want the inheritance, you need to remove those overrides.


# 🍔 Food Delivery Backend (Microservices Architecture)

## 📌 Overview

This project is a **Food Delivery Backend System** built using **Microservices Architecture**.
It is designed to be scalable, modular, and production-ready by leveraging modern backend technologies and design patterns.

Instead of building everything at once, the system follows a **step-by-step modular development approach**, where each service is developed and integrated progressively.

---

## 🚀 Project Structure

```
food-delivery-backend/
│
├── api-gateway/
├── auth-service/
├── restaurant-service/
├── order-service/
├── payment-service/
├── notification-service/
│
├── docker-compose.yml
└── k8s/
```

---

## 🛠️ Tech Stack

* **Java 17**
* **Spring Boot 3.x**
* **Spring Security**
* **Spring Data JPA**
* **PostgreSQL**
* **Spring Cloud Gateway**
* **OpenFeign**
* **Lombok**
* **MapStruct**
* **Docker**
* **Kubernetes**

---

## 🧩 Microservices (MVP)

The following core services are implemented first:

### 🔹 API Gateway

* Central entry point
* Routes requests to respective services

### 🔹 Auth Service

* User registration & login
* JWT token generation
* Role-based authentication

### 🔹 Restaurant Service

* Restaurant CRUD operations
* Menu management
* Owner access control

### 🔹 Order Service

* Place orders
* Manage order lifecycle/status
* Fetch user orders

### 🔹 Payment Service

* Payment processing
* Strategy-based payment handling
* External provider integration (Adapter)

### 🔹 Notification Service

* In-app notifications from order and payment Kafka events
* List and mark-read APIs for users

---

## 🔮 Future Services

* Delivery Service
* Cart Service
* User Service

---

## 🔐 Role-Based Access Control (RBAC)

```java
public enum RoleName {
    ROLE_CUSTOMER,
    ROLE_RESTAURANT_OWNER,
    ROLE_ADMIN,
    ROLE_DELIVERY_AGENT
}
```

---

## 🧠 Design Patterns Used

### 🔹 Common Across Services

* Layered Architecture
* DTO Pattern
* Mapper Pattern

### 🔹 Order Service

* State Pattern (order lifecycle)
* Chain of Responsibility (order processing)
* Builder Pattern (order creation)

### 🔹 Payment Service

* Strategy Pattern (payment methods)
* Factory Pattern (payment creation)
* Adapter Pattern (external payment providers)

---

## 📁 Example: Auth Service Structure

```
auth-service
└── src/main/java/com/food/auth
    ├── controller
    ├── dto
    ├── entity
    ├── repository
    ├── security
    ├── service
    ├── config
    └── exception
```

---

## 🐳 Deployment

### Docker

* Each service is containerized
* Managed via `docker-compose.yml`

### Kubernetes

* Deployment manifests available under `/k8s`
* Supports scalable production deployment

---

## ⚡ Development Approach

1. Build **core services first (MVP)**
2. Integrate via **API Gateway**
3. Implement **JWT + RBAC**
4. Add advanced services (delivery, etc.)
5. Deploy using **Docker & Kubernetes**

---

## 🎯 Key Features

* Microservices-based scalable architecture
* Secure authentication with JWT
* Role-based authorization (RBAC)
* Clean separation of concerns
* Production-ready design patterns
* Containerized deployment

---

## 🧠 Philosophy

> “Build scalable systems step by step — not all at once.”

---

## 📌 Next Steps

* Implement each service module independently
* Add inter-service communication (Feign)
* Introduce logging & monitoring
* Optimize performance and security

---

## 👨‍💻 Author

**Vishal Sharma**

---

## ⭐ Notes

This project is designed for:

* Learning Microservices Architecture
* Practicing System Design
* Preparing for Backend/Java Interviews

---
