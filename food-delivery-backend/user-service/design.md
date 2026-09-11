# User Service Design

## 1. Architecture
- **Presentation**: Spring Web REST controllers under `/api/users`.
- **Application**: Service layer for profile and address rules (default address uniqueness).
- **Persistence**: Spring Data JPA + MySQL 8.
- **Messaging**: `KafkaTemplate` publishing JSON events to `user.profile.changed`.

## 2. Tech stack
- Spring Boot 3.2.x, Java 17
- MySQL 8, Hibernate DDL `update` for dev
- Spring Kafka
- JUnit 5, Mockito, `spring-kafka-test` (optional embedded broker for integration tests; unit tests mock `KafkaTemplate`)

## 3. Data model

### `user_profiles`
| Column        | Type        | Notes                          |
|---------------|-------------|--------------------------------|
| id            | BIGINT PK   | Surrogate                      |
| auth_user_id  | BIGINT      | Unique, maps to auth user      |
| display_name  | VARCHAR     | Not null                       |
| phone         | VARCHAR     | Nullable                       |
| contact_email | VARCHAR     | Nullable                       |
| created_at    | TIMESTAMP   |                                |
| updated_at    | TIMESTAMP   |                                |

### `delivery_addresses`
| Column           | Type        | Notes                    |
|------------------|-------------|--------------------------|
| id               | BIGINT PK   |                          |
| user_profile_id  | BIGINT FK   | → user_profiles.id       |
| label            | VARCHAR     | e.g. HOME, WORK          |
| line1            | VARCHAR     |                          |
| line2            | VARCHAR     | Nullable                 |
| city             | VARCHAR     |                          |
| postal_code      | VARCHAR     |                          |
| default_address  | BOOLEAN     | At most one true per profile |

## 4. REST surface (v1)
- `POST   /api/users/profiles` — create profile
- `GET    /api/users/profiles/{id}` — by profile id
- `GET    /api/users/profiles/by-auth/{authUserId}` — by auth user id
- `PUT    /api/users/profiles/{id}` — update profile fields
- `POST   /api/users/profiles/{profileId}/addresses` — add address
- `GET    /api/users/profiles/{profileId}/addresses` — list addresses
- `PUT    /api/users/addresses/{addressId}` — update address
- `DELETE /api/users/addresses/{addressId}` — delete address
- `PATCH  /api/users/addresses/{addressId}/default` — set default

## 5. Kafka
- **Topic**: `user.profile.changed` (configurable via `kafka.topics.user-profile-changed`).
- **Payload**: `type` (`CREATED` \| `UPDATED`), `profileId`, `authUserId`, `occurredAt` (ISO-8601).

## 6. Deployment
- **Docker**: multi-stage build (Maven + Temurin 17 JRE).
- **Kubernetes**: Deployment + ClusterIP Service on port **8086**.
