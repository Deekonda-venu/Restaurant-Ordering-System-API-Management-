# Future Design: Driver Management and Automatic Delivery Assignment

## Goal

Extend the current ordering project so that:

- Delivery drivers can be registered and managed.
- When food becomes ready, the system automatically assigns an available driver.
- The Order Service's MySQL database is the source of truth for driver assignment and delivery progress.
- Customers and operators can retrieve the latest delivery state through the Order Service API.

This is a future design, not a description of functionality already implemented.

## Current implementation

Today, the Delivery service stores delivery documents in MongoDB. It receives `ORDER_READY` from Kitchen, creates a delivery record, and exposes endpoints to manually assign a `driverId` and update the delivery status. There is no Driver entity or driver-registration API. The Order Service stores order/payment fields in its MySQL `OrderService` schema and does not currently store driver assignment or delivery lifecycle fields.

## Recommended ownership and communication

Make the Order Service the owner of driver availability, assignment, and delivery state in its MySQL database. The Delivery service must not connect directly to or write into the Order database. It should request changes through Order Service APIs; this preserves database ownership and allows Order Service to enforce validation and transactions.

```text
Kitchen -- ORDER_READY event --> Kafka
                                  |
                                  v
                         Delivery Service
                                  |
                     POST assign available driver
                                  |
                                  v
                           Order Service
                     MySQL: orders, drivers,
                     delivery assignments/status
                                  |
                 delivery status events (optional)
                                  v
                        Notification Service
```

### Suggested relational tables

Keep the existing `orders` table and add separate tables in the Order Service's `OrderService` schema:

- `drivers`: driver ID, name/contact details, active flag, availability status, and timestamps.
- `deliveries`: delivery ID, unique order ID, driver ID, delivery status, created/updated timestamps, pickup time, and delivered time.

Use foreign keys from delivery to order and driver where appropriate. Store status values consistently, preferably as enums in Java and constrained values in the database. Do not duplicate driver contact details on every order; join by `driver_id` when those details are needed.

Suggested driver states: `AVAILABLE`, `ASSIGNED`, `INACTIVE`.

Suggested delivery states: `WAITING_FOR_FOOD`, `READY_FOR_PICKUP`, `PICKED_UP`, `ON_THE_WAY`, `DELIVERED`, `CANCELLED`.

## Target workflow

1. An administrator registers drivers and activates/deactivates them through a Driver API owned by Order Service.
2. A customer places an order. Order Service validates and saves it as it does today.
3. Kitchen marks the ticket `READY` and publishes `ORDER_READY` to Kafka.
4. Delivery consumes `ORDER_READY` and calls Order Service to request assignment for that order. The request should be safe to retry.
5. In one database transaction, Order Service checks that the order has no existing active delivery, selects an active `AVAILABLE` driver, assigns that driver, creates or updates the delivery row, and changes the driver's state to `ASSIGNED`.
6. Order Service returns the assigned driver and delivery status. Delivery can publish an event for Notification, or Order Service can publish an event after the transaction commits.
7. Delivery progress updates (`PICKED_UP`, `ON_THE_WAY`, `DELIVERED`) go through Order Service APIs. Order Service updates its MySQL records and makes the driver `AVAILABLE` again after successful delivery or cancellation according to the business rule.
8. Notification consumes delivery events and sends or logs customer updates. It should receive `customerId` from the event rather than relying on an optional field in the Kitchen event.

## Random assignment requirements

“Random” should mean selecting uniformly from eligible drivers, not choosing any driver indiscriminately. Only active drivers in `AVAILABLE` state should be eligible. Selection and state change must be atomic to prevent two simultaneous orders from receiving the same driver.

For MySQL 8, one implementation option is to select an eligible row with a transaction and row lock, for example `SELECT ... FOR UPDATE SKIP LOCKED`, then immediately change its status to `ASSIGNED`. Another option is optimistic locking with a version column and retry when an update loses a race. A simple random query without a lock or atomic update is not safe under concurrent requests.

If no eligible driver exists, do not silently assign an unavailable driver. Keep the delivery in `WAITING_FOR_DRIVER` (or `READY_FOR_PICKUP` with an explicit unassigned state), return a clear response, and retry assignment later or notify an operator.

## Suggested API direction

These are proposed endpoints; they do not exist yet.

| Method | Proposed path | Purpose |
|---|---|---|
| `POST` | `/API/Order/v1/drivers` | Register a driver |
| `GET` | `/API/Order/v1/drivers/available` | List active available drivers |
| `PATCH` | `/API/Order/v1/drivers/{driverId}/status` | Activate/deactivate or manually change availability when allowed |
| `POST` | `/API/Order/v1/orders/{orderId}/delivery/assign` | Assign an available driver; can be called by Delivery Service after `ORDER_READY` |
| `GET` | `/API/Order/v1/orders/{orderId}/delivery` | Read the assignment and delivery status |
| `PATCH` | `/API/Order/v1/orders/{orderId}/delivery/status` | Record `PICKED_UP`, `ON_THE_WAY`, or `DELIVERED` |

Example driver registration request:

```json
{
  "name": "Asha Kumar",
  "phone": "9876500012",
  "active": true
}
```

Example assignment response:

```json
{
  "orderId": 16,
  "driver": {
    "driverId": 12,
    "name": "Asha Kumar",
    "phone": "9876500012"
  },
  "deliveryStatus": "READY_FOR_PICKUP",
  "assignedAt": "2026-09-28T10:45:00"
}
```

Example status request:

```json
{
  "status": "ON_THE_WAY"
}
```

## Reliability and data consistency

- Add a unique constraint for one active delivery assignment per order.
- Make assignment idempotent: repeating the request for an already-assigned order returns the existing assignment instead of allocating another driver.
- Use a database transaction for driver selection, assignment creation, and driver-state change.
- Validate allowed status transitions; for example, a delivery cannot move from `READY_FOR_PICKUP` directly to `DELIVERED`.
- Use an outbox pattern if Order Service must reliably publish Kafka events after updating MySQL. A database commit and Kafka publish are not one atomic operation by default.
- Include `orderId`, `customerId`, `driverId`, `eventType`, and an event ID in delivery events. Do not assume Kitchen currently sends `customerId` in `ORDER_READY`.
- Add tests for no available drivers, concurrent assignment, duplicate event/request, invalid status transition, driver release, and event publication.

## Migration from the current code

1. Add driver and delivery entities/repositories to Order Service and database migrations for the new tables.
2. Add Order Service APIs and transactional automatic-assignment logic.
3. Change Delivery Service to call those APIs instead of persisting delivery assignments to its own MongoDB repository.
4. Keep Kafka as the trigger from Kitchen to Delivery; Delivery remains the workflow adapter/orchestrator, while Order Service remains the persistence owner.
5. Backfill any existing MongoDB delivery records into MySQL, checking for duplicate order IDs and missing drivers.
6. Verify the migrated orders and API behavior, then stop using the Mongo delivery collection. Do not delete existing Mongo data until the migration has been backed up and verified.

## Acceptance checklist

- Drivers can be created, listed, activated, and deactivated.
- `ORDER_READY` causes one available driver to be assigned automatically.
- Driver and delivery assignment are stored in the Order Service MySQL schema.
- Two concurrent ready orders cannot claim the same driver.
- Replayed Kafka messages do not create duplicate deliveries or reassign an active order.
- Delivery progress updates are persisted and visible through Order Service APIs.
- Successful delivery releases the driver according to the chosen business rule.
- Notification events include the customer and order identifiers required to notify the correct customer.
