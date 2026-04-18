# Order Service Design

## 1. System Architecture
The Order Service is a standard Java/Spring Boot microservice communicating over REST. 
- **Presentation**: REST Controllers.
- **Service Layer**: Business logic handling complex state validation and cost aggregations.
- **Integration Layer**: Uses `OpenFeign` (or `RestTemplate`) to perform synchronous RPCs to the `restaurant-service` to validate incoming item requests.
- **Data Access Layer**: JPA handling persistence to a dedicated MySQL schema.

## 2. Tech Stack
- **Framework**: Spring Boot 3 / Java 17
- **Database**: MySQL 8.0
- **HTTP Client**: Spring Cloud OpenFeign
- **Testing**: JUnit 5, Mockito

## 3. Database Design
- **`orders` table**:
  - `id` (PK)
  - `user_id` (Extracted from Auth/Gateway)
  - `restaurant_id` (Mapped to Restaurant Service)
  - `total_amount` (Decimal)
  - `status` (String/Enum: PENDING, CONFIRMED, DELIVERED, CANCELLED)
  - `created_at`, `updated_at` (Timestamps)
- **`order_items` table**:
  - `id` (PK)
  - `order_id` (FK to `orders`)
  - `menu_item_id` (Mapped to Restaurant Service)
  - `quantity` (Integer)
  - `price_at_purchase` (Decimal - captured at request time to prevent historical loss if prices change later).

## 4. Communication Strategy
- **Synchronous Call Flow**:
  1. Customer submits POST `/api/orders` with list of `menu_item_id` and `quantity`.
  2. Order Service calls `GET http://restaurant-service:8082/api/public/restaurants/{id}/menu`.
  3. Order Service ensures items exist, are available, and calculates the true `totalAmount` based on the fetched catalog.
  4. Order is persisted as PENDING.

## 5. Caching Strategy
No strict caching for Order processing since pricing validation requires real-time fidelity, and orders are highly dynamic.
