# Restaurant Service - High Level Design

## 1. System Architecture
The Restaurant Service follows a standard microservice layered design:
- **Presentation Layer (Controllers):** Exposes REST APIs, handles HTTP requests/responses, and input validation.
- **Business Logic Layer (Services):** Contains core business rules (e.g., verifying menu ownership, updating operating statuses).
- **Data Access Layer (Repositories):** Integrates with the database using an ORM layer (e.g., Spring Data JPA).

### Inter-Service Communication
- **Synchronous (REST/HTTP):** 
  - Used by the API Gateway to route client traffic.
  - Used by the Order Service to synchronously verify item availability/pricing before order creation.
- **Asynchronous (Messaging - e.g., Kafka / RabbitMQ):** 
  - Used to emit domain events like `RestaurantStatusChanged` or `MenuItemUpdated`. This allows the Search Service to asynchronously update its indexes without tightly coupling to this service.

## 2. Tech Stack Recommended
- **Language/Framework:** Java 17+, Spring Boot
- **Database:** PostgreSQL or MySQL (Relational database preferred due to structured entity relationships).
- **Caching:** Redis (Crucial for scaling read-heavy menu operations).
- **Security:** Spring Security (for role-based access control based on JWTs).

## 3. Database Design & Relationships
The data model relies on a strict relational hierarchy:
- `Restaurant (1) ----- (*) MenuCategory`
- `MenuCategory (1) ----- (*) MenuItem`

**Indexes:** 
- B-Tree index on `restaurant_id` in `MenuItem` and `MenuCategory` for fast lookups.
- Index on `status` in the `Restaurant` table to quickly filter open/closed locations.

## 4. Key Design Patterns
- **DTO (Data Transfer Object) Pattern:** Used alongside tools like MapStruct to prevent exposing internal database Entity classes directly to clients.
- **Builder Pattern:** Simplifies the instantiation of complex Entity and DTO objects (often via Lombok `@Builder`).
- **Global Exception Handling:** Utilizing Spring's `@ControllerAdvice` to provide standardized error responses (e.g., `404 Not Found` for missing restaurants, `403 Forbidden` for unauthorized edits).

## 5. Caching Strategy
Since reading menus is a high-traffic operation, we implement a caching caching layer:
- **Cache Target:** The `GET /api/public/restaurants/{id}/menu` response is cached.
- **Cache Invalidation:** Cache is evicted or updated whenever an admin makes a `PUT`, `POST`, or `PATCH` request to update the menu structure or item availability.

## 6. Security Considerations
- **Gateway Validation:** The API Gateway ensures all requests have a valid JWT token. 
- **Role-Based Access Control (RBAC):**
  - Read APIs (`GET`) are accessible to `ROLE_CUSTOMER`.
  - Write APIs (`POST`, `PUT`, `DELETE`) require `ROLE_RESTAURANT_OWNER` or `ROLE_ADMIN`.
- **Data Ownership Authorization:** Before updating a menu item, the service must verify that the `owner_id` embedded in the JWT token matches the `owner_id` associated with the target restaurant.
