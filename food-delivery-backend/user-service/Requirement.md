# User Service Requirements

## Overview
The User Service owns **customer profile and delivery address** data for the Food Delivery Application. It is separate from **Auth Service**, which handles credentials and JWT issuance. This service stores enrichments keyed by the **auth user id** (`UserCredential.id` from auth-service).

## Core Features

### 1. User profile
- Create a profile for a registered user (`authUserId` must be unique).
- Read profile by internal profile id or by `authUserId`.
- Update display name, phone, and optional contact email used for delivery notifications.
- Reject duplicate profile creation for the same `authUserId`.

### 2. Delivery addresses
- Add multiple addresses per profile (label, lines, city, postal code).
- List addresses for a profile.
- Update or delete an address.
- Mark exactly one address as **default** for delivery; clearing others when a new default is set.

### 3. Events (Kafka)
- Emit **`user.profile.changed`** after profile create or update so notification or analytics services can react without synchronous coupling.

## APIs
- Base path: `/api/users` (behind API Gateway; JWT required at gateway).
- REST JSON over HTTP.

## Integrations
- **Auth Service**: No direct HTTP call in v1; callers pass `authUserId` (typically aligned with JWT subject / user id from auth). Future: consume `user.registered` events if auth publishes them.
- **Kafka**: Producer for profile lifecycle events.
- **MySQL**: Dedicated schema `user_db`.

## Non-goals (v1)
- Password or role management (Auth Service).
- Payment instruments.
