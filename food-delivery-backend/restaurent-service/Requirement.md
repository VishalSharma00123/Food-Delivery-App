# Restaurant Service — Requirements

## Purpose

The Restaurant service owns restaurant master data, menu categories, and menu items. It exposes a **public, unauthenticated** menu API used by the Order service (Feign) so customers can build carts from live prices and availability. It also exposes **authenticated** APIs for owners/operators to manage restaurants and menus.

## Actors

- **Guest / Order service**: reads menu data for a restaurant (no JWT).
- **Customer app (via API Gateway)**: may list restaurants and read details and menus (public GET, as configured at the gateway).
- **Restaurant owner / Admin**: creates and updates restaurants, categories, and menu items (JWT from Auth service).

## Functional Requirements

1. **Public menu (Order integration)**  
   - `GET /api/public/restaurants/{restaurantId}/menu` returns a JSON object whose keys are **category names** and whose values are arrays of menu items.  
   - Each menu item includes at least: `id`, `restaurantId`, `categoryId`, `name`, `description`, `price`, `foodType`, `isAvailable`, `imageUrl` (compatible with the Order service `MenuItemDto`).

2. **Restaurant catalog (read)**  
   - List all restaurants with basic fields (name, status, city, cuisine).  
   - Get restaurant by id with the same basic fields.

3. **Restaurant management (write)**  
   - Create, update, and delete a restaurant (authenticated).  
   - Restaurant has a lifecycle/status (e.g. ACTIVE / INACTIVE).

4. **Category management**  
   - For a given restaurant: list, create, update, and delete categories (authenticated).  
   - Category names are unique per restaurant.

5. **Menu item management**  
   - For a given restaurant: list, create, update, delete menu items (authenticated).  
   - Menu items belong to one category and one restaurant; price and availability are editable.  
   - Food type is classified (e.g. VEG, NON_VEG, VEGAN, OTHER) for display and filtering in clients.

6. **Security**  
   - Public paths: `/api/public/**` and `GET /api/restaurants/**` (read-only discovery and nested reads).  
   - All mutating calls under `/api/restaurants/**` require a valid **Bearer JWT** issued by Auth service (same signing secret as other services in this repo).  
   - JWT must be validated (signature + expiry); roles are attached to the security context for future fine-grained rules.

7. **Persistence**  
   - Data is stored in MySQL database `restaurant_db`.  
   - Schema may be created via JPA (`ddl-auto: update`) in development; SQL scripts are provided for explicit setup and sample data.

## Non-Functional Requirements

- **Port**: `8082` (aligned with API Gateway and Docker Compose).  
- **Observability**: Spring Boot default logging; actuator may be added later.  
- **Container**: Dockerfile produces a runnable JAR image on port 8082.  
- **Kubernetes**: Deployment + ClusterIP Service, configurable MySQL host and JWT secret via env.

## Out of Scope (v1)

- Geo search, opening hours, and ratings.  
- Image upload storage (only `imageUrl` string).  
- Multi-tenant billing or subscription plans.
