# Restaurant Ordering & Kitchen Management System — Architecture

## Order Placement Flow (Synchronous)

```
CUSTOMER
   |
   | POST /orders
   v
ORDER SERVICE
   |
   +---- REST ----> CUSTOMER SERVICE
   |                  Validate customer
   |
   +---- REST ----> RESTAURANT SERVICE
   |                  Restaurant open?
   |
   +---- REST ----> MENU SERVICE
   |                  Item available?
   |                  Current price?
   |
   +---- REST ----> PAYMENT SERVICE
   |                  Process payment
   |
   v
Save Order
```

Order Service acts as the orchestrator for order creation. It synchronously calls
each dependent service via **OpenFeign** to validate the request end-to-end before
persisting the order:

- **Customer Service** — confirms the customer exists and validates the delivery address.
- **Restaurant Service** — confirms the restaurant exists and is `OPEN`.
- **Menu Service** — confirms each ordered item exists, is available, and fetches the
  current price (source of truth for pricing, never trust client-submitted prices).
- **Payment Service** — processes payment for the calculated total (not yet implemented).

Only after all validations pass does Order Service persist the order and its items.

## Post-Order Event Flow (Asynchronous via Kafka)

```
Save Order
   |
   | Kafka
   |
   +---------- ORDER_PLACED ----------+
                                      |
                     +----------------+----------------+
                     |                |                |
                     v                v                v
                 KITCHEN         NOTIFICATION      DELIVERY
                 SERVICE            SERVICE         SERVICE
                     |
               PREPARING
                     |
                  READY
                     |
              ORDER_READY
                     |
                     v
                   Kafka
                     |
               Delivery Service
                     |
                 PICKED_UP
                     |
                ON_THE_WAY
                     |
                 DELIVERED
                     |
                     v
              ORDER_DELIVERED
                     |
                    Kafka
                     |
              Notification Service
```

Once an order is saved, Order Service publishes an `ORDER_PLACED` event to Kafka.
Multiple downstream services consume this event **independently and asynchronously**
(no direct coupling between them):

- **Kitchen Service** — begins preparing the order (`PREPARING` -> `READY`), then
  publishes `ORDER_READY`.
- **Notification Service** — notifies the customer that their order was placed
  (and later, on delivery).
- **Delivery Service** — consumes `ORDER_READY`, assigns a delivery, and tracks
  status transitions (`PICKED_UP` -> `ON_THE_WAY` -> `DELIVERED`), publishing
  `ORDER_DELIVERED` when complete.
- **Notification Service** (again) — consumes `ORDER_DELIVERED` to notify the
  customer of final delivery.

## Why REST for Order Creation but Kafka for Everything After

- **Order creation validation must be synchronous** — the customer needs an
  immediate success/failure response (e.g. "restaurant closed" or "item
  unavailable") before the order is confirmed.
- **Post-order workflow is naturally asynchronous** — kitchen prep, delivery
  tracking, and notifications don't block each other and don't need to block
  the customer's initial request. Kafka decouples these services so any one of
  them can be slow, restart, or fail without affecting the others.

## Sync vs Async — Decision Rule (Important for Interviews)

**Use synchronous REST for:**

- `Order -> Customer` — "Does this customer exist?"
- `Order -> Restaurant` — "Is restaurant currently open?"
- `Order -> Menu` — "Are these items available and what are their prices?"
- `Order -> Payment` — "Was the payment successful?"

Because Order Service needs the response immediately before continuing.

**Use Kafka async events for:**

- `Order -> Kitchen`
- `Order -> Notification`
- `Kitchen -> Delivery`
- `Delivery -> Notification`
- `Payment -> Notification`

Because the request doesn't need to wait for those operations.

## API Gateway

Instead of exposing:
```
localhost:8081
localhost:8082
localhost:8083
localhost:8084
```
the client uses a single entry point:
```
localhost:8080
```

**Gateway routes:**

```
/api/restaurants/**  -> restaurant-service
/api/menu/**         -> menu-service
/api/customers/**    -> customer-service
/api/orders/**       -> order-service
/api/payments/**     -> payment-service
/api/kitchen/**      -> kitchen-service
/api/deliveries/**   -> delivery-service
```

## Technology Stack

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Cloud OpenFeign
- Spring Cloud Gateway
- Apache Kafka
- PostgreSQL
- Docker
- Docker Compose
- Swagger / OpenAPI
- Resilience4j
- JUnit 5
- Mockito
- Testcontainers

Don't introduce Kubernetes initially. First get everything working with Docker Compose.

## Very Important Microservice Rule — Database Per Service

Do not use one database shared by all services.

**Avoid:**
```
              PostgreSQL
                  ^
       +----------+---------+
       |          |         |
     Order      Menu     Customer
```

**Instead:**
```
Order Service
     |
order_db


Menu Service
     |
menu_db


Customer Service
     |
customer_db


Payment Service
     |
payment_db
```

Each service owns its data. That will make this project much closer to actual
microservice architecture.

## Development Order

Don't create everything together. Build it in this order:

**PHASE 1**
```
Restaurant Service
        ↓
Menu Service
```
Get CRUD APIs working.

**PHASE 2**
```
Customer Service
```

**PHASE 3**
```
Order Service
     |
     +----> Menu Service
     +----> Restaurant Service
     +----> Customer Service

using OpenFeign
```
This is where you learn synchronous communication.

**PHASE 4**
```
Payment Service
```

**PHASE 5**
```
Kafka

Order Service
     |
     +---- ORDER_PLACED ----> Kafka
```

**PHASE 6**
```
Kitchen Service
Notification Service
```

**PHASE 7**
```
Delivery Service
```

**PHASE 8**
```
API Gateway
Docker Compose
Resilience4j
Swagger
Testing
Tracing
```

The first coding target is therefore **Restaurant Service + Menu Service**, not the
complete architecture at once. Once those two work, the rest becomes much easier.

## Current Implementation Status

| Service | Status |
|---|---|
| Restaurant Service | Implemented |
| Menu Service | Implemented |
| Customer Service | Implemented |
| Order Service (REST orchestration) | Implemented |
| Payment Service | Not yet implemented |
| Kafka event flow (ORDER_PLACED, etc.) | Not yet implemented |
| Kitchen Service | Not yet implemented |
| Delivery Service | Not yet implemented |
| Notification Service | Not yet implemented |
| API Gateway | Not yet implemented |

**Note:** the current implementation uses MySQL and a manually configured
`Resturantclient`/`CustomerClinet`/`Menuitemsclinet` (OpenFeign) rather than
PostgreSQL — this deviates from the recommended stack above but doesn't block
progress. Consider migrating to PostgreSQL + Docker Compose when reaching Phase 8.
