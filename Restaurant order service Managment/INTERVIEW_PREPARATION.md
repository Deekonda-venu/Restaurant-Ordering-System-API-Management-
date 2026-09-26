# Interview Preparation: Restaurant Ordering Microservices

Use this as a speaking guide, not a script to memorize. Be precise about what is implemented today and distinguish it from possible future improvements.

## 1. Quick introduction

> I built a restaurant ordering system as a set of Spring Boot microservices. Customer, Restaurant, Menu, Order, and Payment provide REST APIs and use separate MySQL schemas. OpenFeign is used for synchronous service-to-service calls when Order needs validation data immediately. After an order is saved, Kafka events drive the asynchronous Kitchen, Notification, and Delivery workflows. Kitchen and Delivery keep their workflow documents in MongoDB. The services and infrastructure can be run locally, with Kafka and MongoDB managed by Docker Compose.

Keep the first answer to about 30–45 seconds. If the interviewer asks for detail, explain one request from API to database to Kafka consumer.

## 2. Project at a glance

| Area | Implementation |
|---|---|
| Language/runtime | Java 17 |
| Application framework | Spring Boot 4.1.x in the current service builds |
| Service communication | REST with Spring Cloud OpenFeign for synchronous calls; Kafka for asynchronous events |
| Relational storage | MySQL, with a separate schema configured for each synchronous service |
| Document storage | MongoDB for Kitchen and Delivery workflow records |
| Event infrastructure | Apache Kafka with local Docker Compose infrastructure |
| Build tools | Maven wrappers for Customer, Restaurant, Menu, Order, and Payment; Gradle wrappers for Kitchen, Notification, and Delivery |

The project currently has no API Gateway. Clients call each service directly using its configured local port. Notification currently consumes Kafka events and logs notification messages; it does not expose a REST endpoint or persist notifications through a repository.

## 3. Current service map

| Service | Responsibility | Port | Storage / communication |
|---|---|---:|---|
| Customer | Customers and addresses | 9293 | MySQL `CustomerDetails` |
| Restaurant | Restaurant data and operating status | 9191 | MySQL `resturant` |
| Menu | Menu items and restaurant lookup | 9292 | MySQL `MenuItems`; Feign to Restaurant |
| Order | Validates order references, computes totals, saves orders | 9999 | MySQL `OrderService`; Feign to Customer, Restaurant, and Menu; publishes Kafka |
| Payment | Records payments/refunds and publishes payment events | 9295 | MySQL `PaymentService`; Feign to Customer and Order |
| Kitchen | Consumes placed orders and tracks preparation | 9296 | MongoDB `kitchen_db`; Kafka consumer and producer |
| Notification | Converts events into console messages | 9297 | Kafka consumer; no notification persistence currently implemented |
| Delivery | Creates delivery records when food is ready and tracks delivery status | 9298 | MongoDB `delivery_db`; Kafka consumer and producer |

## 4. Explain one order end to end

1. The caller creates a customer and address, restaurant, and menu items through their REST APIs.
2. The caller sends `POST http://localhost:9999/API/Order/v1/CreateOrder` with customer, restaurant, address, and item IDs/quantities.
3. Order uses Feign to check the customer/address, restaurant, and menu items. It gets prices from Menu rather than trusting a submitted price, computes the total, saves the order to MySQL, and publishes `ORDER_PLACED` on `order-events`.
4. Kitchen consumes `order-events`, ignores unrelated or invalid events, and creates a `RECEIVED` ticket in MongoDB. Notification consumes the order event and prints a message.
5. A kitchen operator calls `PATCH /api/kitchen/orders/{orderId}/status` with `PREPARING` or `READY`. Kitchen saves the state and publishes `ORDER_PREPARING` or `ORDER_READY` on `kitchen-events`.
6. Delivery consumes `ORDER_READY` and creates a `READY_FOR_PICKUP` record in MongoDB. An operator can assign a driver and update delivery to `PICKED_UP`, `ON_THE_WAY`, and `DELIVERED` through Delivery APIs.
7. Delivery publishes lifecycle events on `delivery-events`; Notification consumes them and prints corresponding updates. Payment is created separately through its API and publishes a success/failure event on `payment-events`.

A short version of the design choice: use REST when the caller needs an immediate answer to continue; use Kafka when downstream work can happen independently after the main request has succeeded.

## 5. APIs worth remembering

- Create order: `POST /API/Order/v1/CreateOrder` on port `9999`
- Read order: `GET /API/Order/v1/GetOrderById/{OrderId}` on port `9999`
- Create payment: `POST /API/Payments/v1/payment` on port `9295`
- Update kitchen state: `PATCH /api/kitchen/orders/{orderId}/status` on port `9296`
- Assign driver: `POST /api/deliveries/{orderId}/assign-driver` on port `9298`
- Update delivery state: `PATCH /api/deliveries/{orderId}/status` on port `9298`

The detailed APIs and example bodies are in [API_Documentation.md](API_Documentation.md) and [RUNNING_AND_API_GUIDE.md](RUNNING_AND_API_GUIDE.md).

## 6. Common interview questions

### Why did you choose microservices?

The domain has distinct responsibilities: customer, restaurant, catalog, ordering, payment, kitchen, notification, and delivery. Separate services can own their code and data, and asynchronous events let post-order workflows evolve without making order creation wait for them. The tradeoff is more operational complexity: service discovery/configuration, observability, retries, and distributed consistency all need deliberate handling.

### Why use both REST and Kafka?

Order creation needs immediate validation results from Customer, Restaurant, and Menu, so those calls use REST through Feign. After the order is saved, Kitchen and Notification can react independently. Kafka decouples the producer from those consumers and allows consumers to process events at their own pace.

### What is OpenFeign doing here?

Feign defines a typed Java client for another service's HTTP API. The Order service uses Feign clients to retrieve customer/address, restaurant, and menu data. This keeps HTTP request construction out of the business service, but the calls remain synchronous and can fail if a dependency is unavailable.

### How does Kafka help this system?

The producer writes an event to a topic, and consumer groups process it independently. For example, Kitchen and Notification each consume `order-events`. The Order service does not need to call either one directly or wait for their work to finish.

### What happens if a consumer is offline?

Kafka retains events according to topic retention, so a consumer can resume from its committed offset when it reconnects. Exact recovery behavior depends on the consumer group, offset, retention, and error-handling configuration. For production, I would add explicit retry/backoff and dead-letter handling, monitor lag, and make consumers idempotent.

### What does idempotency mean here?

A repeated event should not create duplicate workflow records. Kitchen checks whether a ticket already exists for an order before inserting; Delivery similarly checks for an existing delivery by order ID. In production I would also enforce unique indexes and design updates so replaying an event is safe.

### Why MySQL and MongoDB?

The transactional customer, catalog, order, and payment records are relational and fit MySQL. Kitchen and Delivery use workflow documents with status/timestamp fields, which are stored in MongoDB in this implementation. This is a design choice, not a claim that MongoDB is inherently better for all workflow data.

### How are order totals calculated?

The Order service loads menu prices from Menu and calculates line totals from quantity. In the current API documentation, tax is 5% and the delivery fee is a flat 40; the total is subtotal plus tax plus delivery fee. A production implementation should represent money with `BigDecimal`, define rounding rules, and keep pricing rules configurable/versioned.

### Is payment part of the order-creation request?

No. Payment has its own API and is invoked separately. Do not claim that Order synchronously calls Payment during order creation. Payment publishes a payment event, and consumers can react asynchronously.

### How does a payment result update an order?

Describe only the event consumer behavior currently present in the Order service: it consumes payment events and updates the payment status on a matching order. Mention the event topic and payload contract, and be ready to explain that event delivery and database updates are not one atomic transaction in this implementation.

### What happens if a service port is occupied?

Spring cannot bind to a port already used by another process. I check the process listening on that port, determine whether it is a duplicate service, and stop only the unintended process or configure a distinct port. Order currently runs on `9999`, and Payment's Feign client is configured to call that port.

### What would you improve next?

Good next steps include:

- Add an API Gateway and centralized external API/security boundary.
- Replace hard-coded Feign URLs with service discovery or centralized configuration.
- Add timeouts, retries with backoff, circuit breakers, and fallbacks for synchronous calls.
- Use an outbox pattern so saving an order and publishing its event cannot silently diverge.
- Add Kafka retry topics/dead-letter topics, schema/version management, and event contract tests.
- Add authentication/authorization, request validation, centralized error responses, and observability with correlation IDs, metrics, and tracing.
- Add integration tests with Testcontainers for MySQL, MongoDB, and Kafka, and automate the complete order flow in CI.
- Persist notifications and deliver them through a real channel instead of console output.

## 7. Honest project limitations to explain

Interviewers value accurate scope. Be ready to say:

- Services use hard-coded localhost URLs in Feign configuration; this is suitable for local development but not multi-host production deployment.
- There is no API Gateway in the current running code.
- Notification currently prints messages to the service console rather than sending email/SMS or storing notification history.
- Kafka publishing and relational persistence do not use an outbox transaction, so production reliability needs additional work.
- Some services use different build systems, and the project would benefit from unified build conventions and automated integration testing.
- Local infrastructure uses single-node development configuration and is not production HA/security configuration.

Avoid presenting planned improvements as completed features.

## 8. Closing summary

> The central design is a synchronous validation path followed by an asynchronous event-driven workflow. Order owns the order record and uses Feign for immediate validation; Kafka coordinates Kitchen, Notification, Delivery, and Payment updates. The main thing I learned is that service boundaries improve independent ownership, but they also make failure handling, observability, event consistency, and idempotency essential parts of the design.

## 9. Preparation checklist

Before the interview, practice explaining the order flow without reading, know which service owns each database, be ready to draw the four Kafka topics and their producers/consumers, and run one complete local smoke test. Be prepared to discuss one bug you fixed, the evidence that identified the root cause, and how you verified the fix.
