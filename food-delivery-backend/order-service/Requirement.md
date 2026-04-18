# Order Service Requirements

## Overview
The Order Service is responsible for managing the entire lifecycle of customer orders within the Food Delivery Application. It handles order creation, price verification against the Restaurant Service, and status tracking.

## Core Features
1. **Order Placement**
   - Create new orders comprising multiple menu items from a specific restaurant.
   - Synchronously verify item prices and availability by communicating with the **Restaurant Service**.
   - Calculate total order cost (including dummy taxes or fees if applicable).

2. **Order Management**
   - Track order status transitions (e.g., `PENDING` -> `CONFIRMED` -> `PREPARING` -> `OUT_FOR_DELIVERY` -> `DELIVERED` -> `CANCELLED`).
   - Allow restaurant owners to update the status of active orders.
   - Allow customers to cancel an order strictly if the status is still `PENDING`.

3. **Order Retrieval**
   - Retrieve full order history for a particular Customer/User.
   - Retrieve all active/past orders for a specific Restaurant.

## Integration & APIs
- **Restaurant Service**: Required to fetch up-to-date `.price` and `.isAvailable` metrics for items in the cart before placing the order to prevent price tampering.
- **REST APIs**: Public-facing APIs protected by API Gateway JWT validation. Role-Based constraints enforce that only owners can update statuses.
