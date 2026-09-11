# Role-based access (RBAC)

This document describes application roles as stored in the **`roles`** table (auth database), how they appear in JWTs, and what each role is allowed to do. Use it when adding new endpoints or services so rules stay consistent.

## Role catalogue (database)

| id | name | Spring authority (`JwtAuthenticationFilter` / `CustomUserDetails`) |
|----|------|-------------------------------------------------------------------|
| 1 | `USER` | `ROLE_USER` |
| 2 | `ADMIN` | `ROLE_ADMIN` |
| 3 | `RESTAURANT_OWNER` | `ROLE_RESTAURANT_OWNER` |
| 4 | `DELIVERY_PARTNER` | `ROLE_DELIVERY_PARTNER` |
| 5 | `CUSTOMER` | `ROLE_CUSTOMER` |

JWT claim `roles` holds the **names without the `ROLE_` prefix** (for example `["CUSTOMER"]` or `["ADMIN","RESTAURANT_OWNER"]`). Restaurant-service maps each name to a `SimpleGrantedAuthority` with the `ROLE_` prefix so Spring’s `hasAnyRole("ADMIN")` style checks work.

Constants in code:

- Auth service: `com.auth_service.auth_service.util.RoleNames`
- Restaurant service: `com.restaurent_service.restaurent_service.security.AppRoles` and `SecurityRoleUtils`

---

## Role capabilities (product rules)

### ADMIN (id 2)

- **Global operator.** May perform any supported write/delete operation that other roles can perform, **including on resources owned by someone else** (for example any restaurant’s menu in restaurant-service).
- Intended for support, moderation, and emergency fixes.

### RESTAURANT_OWNER (id 3)

- **Catalogue owner.** May create restaurants (they become the `owner_id` on the row), and may create/update/delete **only** restaurants, categories, and menu items **where `restaurants.owner_id` equals their JWT `userId`**.
- Cannot change another owner’s restaurant data (HTTP **403** from service-layer checks).

### CUSTOMER (id 5)

- **Buyer.** Browse menus, place orders (when order-service enforces auth), manage own profile/addresses.
- **No** restaurant catalogue mutations (no POST/PUT/PATCH/DELETE under `/api/restaurants/**` except what is explicitly opened later).

### DELIVERY_PARTNER (id 4)

- **Courier.** Read/update delivery-related state (orders assigned to them, status updates) when those APIs exist; **no** restaurant admin APIs.
- **No** restaurant catalogue mutations unless you deliberately add an exception.

### USER (id 1)

- **Default / legacy base role.** Treat as **read-only** for privileged domains unless combined with another role (JWT can carry multiple roles if your auth model allows it).
- Typically used when an account has not yet picked a primary persona (customer vs owner vs partner).

---

## Implemented enforcement (restaurant-service)

| Area | Rule |
|------|------|
| `GET /api/public/**` | Public, no JWT |
| `GET /api/restaurants/**` | Public, no JWT (browse) |
| `POST`, `PUT`, `PATCH`, `DELETE` under `/api/restaurants/**` | JWT required; Spring Security allows only **`ADMIN`** or **`RESTAURANT_OWNER`** |
| Same mutations, service layer | **`ADMIN`**: any restaurant. **`RESTAURANT_OWNER`**: only if `restaurant.ownerId == jwt userId` |

Other microservices (order, payment, user, notification) should follow the same matrix in their `SecurityFilterChain` / service checks when you add security there.

---

## Suggested matrix for future services (not all enforced in code yet)

| Action | ADMIN | RESTAURANT_OWNER | CUSTOMER | DELIVERY_PARTNER | USER |
|--------|:------:|:----------------:|:----------:|:------------------:|:----:|
| Restaurant catalogue writes | Yes (any) | Yes (own only) | No | No | No |
| Restaurant public reads | Yes | Yes | Yes | Yes | Yes |
| Own orders (create/cancel/view) | Yes | Yes | Yes | No | Yes* |
| Any user’s orders | Yes | No | No | No | No |
| Assigned delivery run | Yes | No | No | Yes | No |
| User profile (self) | Yes | Yes | Yes | Yes | Yes |
| User profile (others) | Yes | No | No | No | No |

\*`USER` only where you treat it as a logged-in customer with the same rules as `CUSTOMER`.

---

## HTTP status conventions

- **401** — missing/invalid JWT on a protected route.
- **403** — valid JWT but role or ownership does not allow the operation.
- **404** — resource missing (do not use 403 to hide existence unless you have a strong privacy requirement).

---

## Changing roles

1. Update the `roles` table and registration/login logic so JWT `roles` match names used here.
2. Update `RoleNames` / `AppRoles` if you add new constants.
3. Update this file and the relevant `SecurityFilterChain` (and any `assertCanManage*` logic).
