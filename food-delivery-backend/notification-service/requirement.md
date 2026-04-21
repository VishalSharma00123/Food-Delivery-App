# Notification Service — Requirements

## Problem statement

Customers need a single place to see **in-app** updates about orders and payments (order placed, payment succeeded, payment failed). This service ingests domain events from Kafka, persists notification records, and exposes HTTP APIs for clients to list and mark notifications as read.

## Actors

- **End users (customers)** — consume notifications via the API (typically through the mobile or web app behind the API Gateway).
- **Order service** — publishes `order.placed` when an order is created.
- **Payment service** — publishes `payment.confirmed` and `payment.failed` after processing outcomes.

## Functional requirements

1. **Event ingestion** — Subscribe to Kafka topics `order.placed`, `payment.confirmed`, and `payment.failed` with consumer group `notification-service-group`, deserialize JSON payloads compatible with existing producer DTOs, and create one notification per meaningful event for the affected user.
2. **Persistence** — Store notifications in MySQL database `notification_db` with enough metadata to display a title and body, associate to `user_id` and optional `order_id`, and track read state (`is_read`).
3. **Query API** — `GET /api/notifications/user/{userId}` returns notifications for that user, newest first, with optional filter `unreadOnly=true`.
4. **Read state** — `PATCH /api/notifications/user/{userId}/{notificationId}/read` marks a single notification as read only if it belongs to that user.

## Non-functional requirements

- **Port:** `8085` (local and container).
- **Configuration:** Same environment variable pattern as other services: `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER`, `MYSQL_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`.
- **Idempotency (MVP):** If the same logical event is replayed (same user, type, and order id), the service may skip creating a duplicate row to avoid clutter on consumer retries.

## Out of scope (MVP)

- Push notifications (APNs/FCM), email, or SMS delivery.
- Admin UI or cross-tenant management APIs.
- Service-level JWT validation (aligned with existing order/payment services; gateway may still protect routes).

## References

- Design: [design.md](design.md)
