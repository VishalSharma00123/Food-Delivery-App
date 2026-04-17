# API Gateway Service

The API Gateway serves as the centralized entry point for the Food Delivery Application microservices architecture. It acts as a resilient routing mechanism, filtering and validating all global incoming requests before proxying them to their respective internal services (e.g., `auth-service`, `restaurant-service`, etc.).

## 🚀 Key Features

*   **Centralized Routing**: Intelligent request forwarding to downstream microservices.
*   **Reactive Stack**: Built on Spring WebFlux and Spring Cloud Gateway for immense non-blocking scalability.
*   **Global Exception Handling**: Returns consistent, typed JSON responses (intercepts internal exceptions like `ConnectException`).
*   **Cross-Origin Settings (CORS)**: Centrally manages frontend request permissions.
*   **Circuit Breaker (Resilience4j)**: Safely degrades and prevents cascaded failures if backend services fail or time out.
*   **Kubernetes Ready**: Easily deployable to any standard K8s cluster (includes `LoadBalancer` mapping).

## 🛠 Tech Stack

*   **Java 17**
*   **Spring Boot 3.2.x** (WebFlux)
*   **Spring Cloud Gateway 2023.0.x**
*   **Docker & Minikube**

## 🧩 Architecture

```text
[ Frontend Client ]
        |
        v
+-----------------+
|   API Gateway   |  -- Validates routing patterns and token presence (if needed)
+--------+--------+
         |
  +------+-------+
  |              |
  v              v
[Auth]      [Restaurant]
```

## 📦 Containerization & Deployment

To successfully boot and deploy the API Gateway on Kubernetes locally, follow these steps:

### 1. Build the Artifact & Image
```bash
# Compile and build locally
./mvnw clean package -DskipTests
docker build -t food-delivery-backend-api-gateway:latest .
```

### 2. Connect to Minikube
Load the compiled image directly into your cluster's local daemon:
```bash
minikube image load food-delivery-backend-api-gateway:latest
```

### 3. Deploy
Apply all Kubernetes configurations:
```bash
kubectl apply -f k8s/
# Force the pods to pick up your latest image code
kubectl rollout restart deployment api-gateway
```

## 🐛 Debugging Tips
*   **Reactive Server vs Tomcat**: API Gateway uses **Netty**, not Tomcat. Avoid injecting Spring Web MVC dependencies (`spring-boot-starter-web`), as they strictly conflict with WebFlux!
*   To check if requests are passing through successfully, view the real-time Kubernetes logs:
    ```bash
    kubectl logs deploy/api-gateway -f
    ```
