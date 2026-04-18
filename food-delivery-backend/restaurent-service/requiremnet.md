# Restaurant Service Requirements

## Overview
The Restaurant Service is a core microservice in the Food Delivery Application responsible for managing restaurant profiles, menus, operating hours, and their availability status.

## Core Features

1. **Restaurant Profile Management**
   - Create, update, and retrieve restaurant profiles.
   - Manage standard details: name, description, address, contact information, average preparation time, and minimum order value.
   - Toggle restaurant operational status (Online / Offline / Closed).

2. **Menu Management**
   - Manage menu categories (e.g., Starters, Main Course, Beverages, Desserts).
   - CRUD (Create, Read, Update, Delete) operations for menu items.
   - Track item attributes: name, description, price, dietary preferences (veg/non-veg/vegan), and images.
   - Real-time availability toggling for individual menu items (In-Stock / Out-of-Stock).

3. **Operating Hours Management**
   - Define daily operating hours and handle temporary closures.

4. **Integration & Internal APIs**
   - Provide APIs for the **Order Service** to fetch exact item details and pricing during order placement.
   - Provide APIs for the **API Gateway** / **Search Service** to fetch and filter lists of restaurants (e.g., by nearest location or cuisine).

## Proposed API Endpoints

### Public Ends (Customer Facing)
- `GET /api/public/restaurants` - List available restaurants (support pagination/filters).
- `GET /api/public/restaurants/{id}` - Get a specific restaurant's profile.
- `GET /api/public/restaurants/{id}/menu` - Get the menu for a restaurant organized by category.

### Private/Admin Ends (Restaurant Owner / Admin Panel)
- `POST /api/restaurants` - Onboard a new restaurant.
- `PUT /api/restaurants/{id}` - Update restaurant details.
- `PATCH /api/restaurants/{id}/status` - Update restaurant status (open/close).
- `POST /api/restaurants/{id}/categories` - Add a new menu category.
- `POST /api/restaurants/{id}/items` - Add a new menu item.
- `PUT /api/items/{itemId}` - Update menu item details.
- `PATCH /api/items/{itemId}/availability` - Mark an item as available/unavailable.

## Database Entities

- **Restaurant**: `id`, `owner_id`, `name`, `description`, `address`, `phone`, `status`, `created_at`, `updated_at`
- **MenuCategory**: `id`, `restaurant_id`, `name`, `description`
- **MenuItem**: `id`, `restaurant_id`, `category_id`, `name`, `description`, `price`, `food_type`, `is_available`, `image_url`

## Non-Functional Requirements
- **High Availability**: The menu fetching APIs (`GET`) will be read-heavy and need rapid response times.
- **Data Integrity**: Price data provided to the Order Service must be immediately consistent.
