# Local Setup, Databases & Kafka Guide

This document covers **(A)** how to run the whole system locally, **(B)** the exact databases, tables and full SQL, and **(C)** a beginner-friendly Kafka introduction plus the concrete next steps for your plan.

---

# PART A — Running Locally

## Prerequisites

| Tool | Version | Check |
|---|---|---|
| Java (JDK) | 17+ | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| MySQL | 8.x | `mysql --version` |
| (later) Docker + Compose | latest | `docker -v` / `docker compose version` |

> All services currently use **MySQL** with `username=root`, `password=root`. Change these in each service's `application.properties` if your MySQL differs.

## Services, Ports & Databases (current, verified from configs)

| Service | Port | Base Path | Database | Tables |
|---|---|---|---|---|
| Customer-Service | `9293` | `/API/customer/v1` | `CustomerDetails` | `customer_details`, `addresses` |
| Resturant | `9191` | `/API/resturant/v1` | `resturant` | `resturant_details` |
| MenuItemsService | `9292` | `/API/menuitems/v1` | `MenuItems` | `menu_items` |
| Order_Service | `9294` | `/API/Order/v1` | `OrderService` | `orders`, `order_items` |
| Payment-Service | `9295` | `/API/Payments/v1` | `PaymentService` | `payments` |

> Note: `ddl-auto=update` means Hibernate **auto-creates/updates tables** on startup. The full SQL in Part B is provided for reference or if you prefer to create schemas manually.

## Step 1 — Start MySQL

macOS (Homebrew):
```bash
brew services start mysql
```
Verify you can connect:
```bash
mysql -u root -proot -e "SELECT VERSION();"
```

## Step 2 — Create the databases

Each service owns its own DB (microservice rule — no shared DB):
```bash
mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS CustomerDetails;"
mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS resturant;"
mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS MenuItems;"
mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS OrderService;"
mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS PaymentService;"
```
(Payment-Service already appends `?createDatabaseIfNotExist=true`, so it self-creates.)

## Step 3 — Run each service

Open a **separate terminal per service**. Start in dependency order (a service should be up before something calls it).

**1) Resturant (9191)**
```bash
cd "/Users/vdeekond/Desktop/Spring_-_Springboot/Microservies(openfeignclint)Sync/Resturant"
mvn spring-boot:run
```
**2) MenuItemsService (9292)**
```bash
cd "/Users/vdeekond/Desktop/Spring_-_Springboot/Microservies(openfeignclint)Sync/MenuItemsService"
mvn spring-boot:run
```
**3) Customer-Service (9293)**
```bash
cd "/Users/vdeekond/Desktop/Spring_-_Springboot/Microservies(openfeignclint)Sync/Customer-Service"
mvn spring-boot:run
```
**4) Order_Service (9294)**
```bash
cd "/Users/vdeekond/Desktop/Spring_-_Springboot/Microservies(openfeignclint)Sync/Order_Service"
mvn spring-boot:run
```
**5) Payment-Service (9295)**
```bash
cd "/Users/vdeekond/Desktop/Spring_-_Springboot/Microservies(openfeignclint)Sync/Payment-Service"
mvn spring-boot:run
```

> Tip: build without running tests using `mvn clean package -DskipTests`, then run the jar with `java -jar target/<artifact>.jar`.

## Step 4 — Smoke test

```bash
# 1. customer
curl -X POST http://localhost:9293/API/customer/v1/CreateCustomer \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Johnathan","lastName":"Doe","email":"johnathan@example.com","phone":"9876500000"}'

# 2. restaurant (auto OPEN)
curl -X POST http://localhost:9191/API/resturant/v1/AddResturntdetails \
  -H "Content-Type: application/json" \
  -d '{"resturantName":"Harsh Resturant","city":"Hyderabad"}'

# 3. order
curl -X POST http://localhost:9294/API/Order/v1/CreateOrder \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"restaurantId":1,"deliveryAddressId":1,"items":[{"menuItemId":1,"quantity":2}]}'

# 4. payment
curl -X POST http://localhost:9295/API/Payments/v1/payment \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"customerId":1,"amount":932.50,"paymentMethod":"CARD","status":"SUCCESS"}'
```

## Useful DB inspection commands

```bash
mysql -u root -proot OrderService  --vertical -e "SELECT * FROM orders;"
mysql -u root -proot OrderService  --vertical -e "SELECT * FROM order_items;"
mysql -u root -proot PaymentService --vertical -e "SELECT * FROM payments;"
```

---

# PART B — Databases (Full SQL)

These `CREATE TABLE` statements mirror the JPA entities (Hibernate naming: camelCase → snake_case). Use them only if creating schema manually.

## Customer-Service — `CustomerDetails`

```sql
CREATE DATABASE IF NOT EXISTS CustomerDetails;
USE CustomerDetails;

CREATE TABLE IF NOT EXISTS customer_details (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    first_name  VARCHAR(255),
    last_name   VARCHAR(255),
    email       VARCHAR(255),
    phone       VARCHAR(255),
    created_at  DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS addresses (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    customer_id   BIGINT,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city          VARCHAR(255),
    state         VARCHAR(255),
    postal_code   VARCHAR(255),
    address_type  VARCHAR(255),
    PRIMARY KEY (id)
);
```

## Resturant — `resturant`

```sql
CREATE DATABASE IF NOT EXISTS resturant;
USE resturant;

CREATE TABLE IF NOT EXISTS resturant_details (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    resturant_name VARCHAR(255),
    description   VARCHAR(255),
    phone         VARCHAR(255),
    email         VARCHAR(255),
    address       VARCHAR(255),
    city          VARCHAR(255),
    opening_time  TIME(6),
    closing_time  TIME(6),
    status        VARCHAR(255),  -- enum: OPEN | CLOSED | TEMPORARILY_CLOSED
    created_at    DATETIME(6),
    updated_at    DATETIME(6),
    PRIMARY KEY (id)
);
```

## MenuItemsService — `MenuItems`

```sql
CREATE DATABASE IF NOT EXISTS MenuItems;
USE MenuItems;

CREATE TABLE IF NOT EXISTS menu_items (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    name         VARCHAR(255),
    description  VARCHAR(255),
    price        DOUBLE       NOT NULL,
    category     VARCHAR(255),
    veg_non_veg  VARCHAR(255),
    resturant_id BIGINT,
    PRIMARY KEY (id)
);
```

## Order_Service — `OrderService`

```sql
CREATE DATABASE IF NOT EXISTS OrderService;
USE OrderService;

CREATE TABLE IF NOT EXISTS orders (
    id                  BIGINT         NOT NULL AUTO_INCREMENT,
    customer_id         BIGINT,
    restaurant_id       BIGINT,
    delivery_address_id BIGINT,
    status              VARCHAR(255),
    subtotal            DECIMAL(38,2),
    tax                 DECIMAL(38,2),
    delivery_fee        DECIMAL(38,2),
    total_amount        DECIMAL(38,2),
    payment_status      VARCHAR(255),
    created_at          DATETIME(6),
    updated_at          DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    order_id     BIGINT,
    menu_item_id BIGINT,
    quantity     INT,
    PRIMARY KEY (id)
);
```

## Payment-Service — `PaymentService`

```sql
CREATE DATABASE IF NOT EXISTS PaymentService;
USE PaymentService;

CREATE TABLE IF NOT EXISTS payments (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    order_id       BIGINT,
    customer_id    BIGINT,
    amount         DECIMAL(38,2),
    payment_method VARCHAR(255),
    status         VARCHAR(255),  -- SUCCESS | FAILED | REFUNDED
    transaction_id VARCHAR(255),
    created_at     DATETIME(6),
    PRIMARY KEY (id)
);
```

## Future service DBs (from your plan)

```sql
-- Kitchen Service -> kitchen_db
CREATE DATABASE IF NOT EXISTS kitchen_db;
USE kitchen_db;
CREATE TABLE IF NOT EXISTS kitchen_orders (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    order_id      BIGINT,
    restaurant_id BIGINT,
    status        VARCHAR(255),  -- RECEIVED | PREPARING | READY
    received_at   DATETIME(6),
    started_at    DATETIME(6),
    ready_at      DATETIME(6),
    PRIMARY KEY (id)
);

-- Delivery Service -> delivery_db
CREATE DATABASE IF NOT EXISTS delivery_db;
USE delivery_db;
CREATE TABLE IF NOT EXISTS deliveries (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    order_id      BIGINT,
    customer_id   BIGINT,
    restaurant_id BIGINT,
    driver_id     BIGINT,
    status        VARCHAR(255),  -- WAITING_FOR_FOOD | READY_FOR_PICKUP | PICKED_UP | ON_THE_WAY | DELIVERED
    pickup_time   DATETIME(6),
    delivery_time DATETIME(6),
    created_at    DATETIME(6),
    PRIMARY KEY (id)
);

-- Notification Service -> notification_db (optional; console is fine initially)
CREATE DATABASE IF NOT EXISTS notification_db;
USE notification_db;
CREATE TABLE IF NOT EXISTS notifications (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    customer_id BIGINT,
    order_id    BIGINT,
    type        VARCHAR(255),
    message     VARCHAR(1000),
    status      VARCHAR(255),
    created_at  DATETIME(6),
    PRIMARY KEY (id)
);
```

---

# PART C — Kafka (Brief Intro + Next Steps)

## What is Kafka? (in 60 seconds)

Kafka is a **distributed event log**. Instead of Service A directly calling Service B (synchronous REST), Service A **publishes an event** to a Kafka **topic**, and any number of services **subscribe** and react — without A waiting or even knowing who listens.

**Core terms:**

- **Producer** — a service that publishes events (e.g., Order Service publishes `ORDER_PLACED`).
- **Topic** — a named stream/log of events (e.g., `order-events`). Split into **partitions** for parallelism.
- **Consumer** — a service that reads events from a topic.
- **Consumer Group** — consumers sharing a `group.id`. Each event is delivered to **one** consumer per group, but **every group** gets its own copy. So Kitchen, Notification, and Delivery each use a **different** group and all receive the same `ORDER_PLACED`.
- **Broker** — a Kafka server. **ZooKeeper/KRaft** coordinates the cluster.
- **Offset** — the position a consumer has read up to (lets it resume after restart).

## Why async here?

| Communication | Style | Why |
|---|---|---|
| Order → Customer / Restaurant / Menu / Payment | **Sync REST (Feign)** | Order needs the answer *immediately* to proceed |
| Order → Kitchen / Notification | **Async Kafka** | Fire-and-forget; order shouldn't wait |
| Kitchen → Delivery, Delivery → Notification, Payment → Notification | **Async Kafka** | Independent, non-blocking reactions |

## Event flow you're building (Phase 5+)

```
Order Service --(publish ORDER_PLACED)--> [ order-events ] --> Kitchen / Notification / Delivery
Kitchen Service --(publish ORDER_READY)--> [ kitchen-events ] --> Delivery / Notification
Delivery Service --(publish ORDER_DELIVERED)--> [ delivery-events ] --> Notification
```

## Step 1 — Run Kafka locally with Docker Compose

Create `docker-compose-kafka.yml` at the repo root:
```yaml
version: "3.8"
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on: [zookeeper]
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    depends_on: [kafka]
    ports:
      - "8090:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```
Start it:
```bash
docker compose -f docker-compose-kafka.yml up -d
```
Open the UI at http://localhost:8090 to watch topics/messages.

## Step 2 — Add the Spring Kafka dependency

Add to the `pom.xml` of **Order_Service** (producer) and every consumer service (Kitchen/Notification/Delivery):
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

## Step 3 — Producer config (Order_Service `application.properties`)

```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

## Step 4 — Define the event DTO (shared shape)

```java
public class OrderPlacedEvent {
    private String eventId;      // e.g. UUID
    private String eventType;    // "ORDER_PLACED"
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private java.math.BigDecimal totalAmount;
    private java.time.LocalDateTime createdAt;
    // getters/setters or @Data
}
```

## Step 5 — Publish from Order Service (after order is saved)

```java
@Service
public class OrderEventPublisher {

    private static final String TOPIC = "order-events";

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void publishOrderPlaced(OrderPlacedEvent event) {
        // key = orderId keeps all events of one order in the same partition (ordering)
        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);
    }
}
```
Call `publishOrderPlaced(...)` at the end of `createOrder(...)` (ideally after payment succeeds).

## Step 6 — Consumer config + listener (e.g., Kitchen Service)

`application.properties`:
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=kitchen-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
```
Listener:
```java
@Component
public class OrderEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "kitchen-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        // create a kitchen ticket with status RECEIVED
        System.out.println("Kitchen received order: " + event.getOrderId());
    }
}
```
Notification and Delivery services do the same but with **different `group-id`s** (`notification-service`, `delivery-service`) so all three receive every event.

## Step 7 — Enable Kafka & verify

- Ensure the main application class scans your Kafka components (default component scan is fine).
- Start Kafka (Step 1), then start Order_Service + Kitchen Service.
- Place an order → you should see the Kitchen consumer log the event, and the message in Kafka-UI under `order-events`.

## Recommended build order for the Kafka phase

1. **Phase 5a** — Get Kafka running in Docker; add producer to Order_Service; publish `ORDER_PLACED`; verify in Kafka-UI.
2. **Phase 5b** — Build **Kitchen Service**; consume `ORDER_PLACED`; create `kitchen_orders (RECEIVED)`; expose `PATCH /api/kitchen/orders/{orderId}/status`; publish `ORDER_READY`.
3. **Phase 6** — Build **Notification Service**; consume `ORDER_PLACED`, `PAYMENT_SUCCESS`, `ORDER_READY`, etc.; print console emails (DB later).
4. **Phase 7** — Build **Delivery Service**; consume `ORDER_READY`; manage delivery lifecycle; publish `ORDER_DELIVERED`.
5. **Phase 8** — API Gateway (Spring Cloud Gateway), Resilience4j, Swagger, tests, tracing.

## Kafka gotchas for beginners

- **Different `group.id` per service** — same group would split events instead of broadcasting.
- **`auto-offset-reset=earliest`** — so a newly started consumer reads existing messages during dev.
- **`spring.json.trusted.packages=*`** — required for the JSON deserializer to build your DTO.
- **Don't publish before the DB commit** — publish the event *after* the order is safely saved.
- **Topic auto-creation** is on by default in dev; for prod, create topics explicitly with proper partitions/replication.

---

## Target Tech Stack (from your plan)

Java 21 · Spring Boot 3.x · Spring Web · Spring Data JPA · Spring Validation · Spring Cloud OpenFeign · Spring Cloud Gateway · Apache Kafka · PostgreSQL · Docker / Docker Compose · Swagger/OpenAPI · Resilience4j · JUnit 5 · Mockito · Testcontainers.

> You currently run **MySQL + Java 17**. That's perfectly fine to continue; migrate to PostgreSQL/Java 21 only if you specifically want to match the target stack. Keep **one database per service** either way.

---

# PART D — MongoDB for the Kafka Services (Beginner Roadmap)

> Note: the technology is **MongoDB** (often nicknamed "Mongo"). "Mango" = MongoDB. 🙂

## Why use MongoDB for the event-driven services?

Your Kafka consumers (**Kitchen**, **Notification**, **Delivery**) mostly **store event data and status history**. That data is flexible and document-shaped, so MongoDB is a natural fit:

- Events arrive as JSON → MongoDB stores JSON-like **documents** directly (no rigid schema / no table migrations).
- Great for append-heavy, "log this event" workloads like notifications.
- Easy to evolve fields as your events grow.

You can keep **MySQL for the sync services** (Customer, Restaurant, Menu, Order, Payment) and use **MongoDB for the async services**. That's a realistic polyglot-persistence setup — still **one database per service**.

## MongoDB in 60 seconds (vs SQL you already know)

| SQL (MySQL) | MongoDB | Meaning |
|---|---|---|
| Database | Database | Same concept |
| Table | **Collection** | A group of records |
| Row | **Document** | One record, stored as BSON/JSON |
| Column | **Field** | A key in the document |
| Primary key `id` | `_id` (`ObjectId` or your own) | Unique identifier |
| `JOIN` | Usually **embed** data or reference by id | Mongo prefers denormalization |
| SQL query | Query by JSON filter, e.g. `{ "orderId": 10001 }` | No SQL language |

Key idea: **no fixed schema**. Two documents in the same collection can have different fields. You model data the way your app reads it.

## Step 1 — Run MongoDB locally (Docker)

Add to your `docker-compose-kafka.yml` (or a separate compose file):
```yaml
  mongodb:
    image: mongo:7
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: root
    volumes:
      - mongo_data:/data/db

  mongo-express:            # optional web UI at http://localhost:8091
    image: mongo-express:latest
    depends_on: [mongodb]
    ports:
      - "8091:8081"
    environment:
      ME_CONFIG_MONGODB_ADMINUSERNAME: root
      ME_CONFIG_MONGODB_ADMINPASSWORD: root
      ME_CONFIG_MONGODB_URL: mongodb://root:root@mongodb:27017/

volumes:
  mongo_data:
```
Start it:
```bash
docker compose -f docker-compose-kafka.yml up -d
```

## Step 2 — Add the Spring Data MongoDB dependency

In each Mongo-backed service's `pom.xml` (Kitchen / Notification / Delivery) — **replace** the JPA/MySQL deps with:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```
(Keep `spring-kafka` — these services still consume Kafka.)

## Step 3 — Configure the connection (`application.properties`)

Each service gets its **own database name** (one DB per service):
```properties
# Kitchen Service example
spring.data.mongodb.uri=mongodb://root:root@localhost:27017/kitchen_db?authSource=admin
```
Notification → `notification_db`, Delivery → `delivery_db`.

## Step 4 — Model a document (vs a JPA entity)

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Document(collection = "kitchen_orders")   // like @Table
@Data
public class KitchenOrder {

    @Id
    private String id;            // Mongo _id (String/ObjectId), not auto-increment Long

    private Long orderId;
    private Long restaurantId;
    private String status;        // RECEIVED | PREPARING | READY
    private LocalDateTime receivedAt;
    private LocalDateTime startedAt;
    private LocalDateTime readyAt;
}
```

## Step 5 — Repository (almost identical to JPA)

```java
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface KitchenOrderRepo extends MongoRepository<KitchenOrder, String> {
    Optional<KitchenOrder> findByOrderId(Long orderId);
    List<KitchenOrder> findByStatus(String status);
}
```
Same derived-query method names you already use with JPA — Spring Data works the same way.

## Step 6 — Use it inside a Kafka consumer

```java
@Component
public class KitchenEventConsumer {

    @Autowired
    private KitchenOrderRepo kitchenOrderRepo;

    @KafkaListener(topics = "order-events", groupId = "kitchen-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        KitchenOrder ticket = new KitchenOrder();
        ticket.setOrderId(event.getOrderId());
        ticket.setRestaurantId(event.getRestaurantId());
        ticket.setStatus("RECEIVED");
        ticket.setReceivedAt(java.time.LocalDateTime.now());
        kitchenOrderRepo.save(ticket);      // stored as a document in kitchen_db.kitchen_orders
    }
}
```

## Step 7 — Inspect data (Mongo shell)

```bash
docker exec -it <mongo_container> mongosh -u root -p root --authenticationDatabase admin
```
```javascript
use kitchen_db
db.kitchen_orders.find().pretty()
db.kitchen_orders.find({ orderId: 10001 })
db.kitchen_orders.updateOne({ orderId: 10001 }, { $set: { status: "PREPARING" } })
```
Or use **mongo-express** UI at http://localhost:8091.

## Recommended MongoDB + Kafka roadmap

1. **M1 — Learn basics**: run Mongo in Docker, open mongo-express, manually `insert`/`find`/`update` a few documents to get comfortable with collections vs documents.
2. **M2 — Kitchen Service (first Mongo + Kafka service)**:
   - `spring-boot-starter-data-mongodb` + `spring-kafka`.
   - `@Document KitchenOrder`, `KitchenOrderRepo`.
   - `@KafkaListener` on `order-events` → save `RECEIVED` ticket.
   - REST `PATCH /api/kitchen/orders/{orderId}/status` to move `RECEIVED → PREPARING → READY`.
   - On `READY`, **publish** `ORDER_READY` to Kafka.
3. **M3 — Notification Service**: consume multiple events; store `notifications` documents (or console-only first).
4. **M4 — Delivery Service**: consume `ORDER_READY`; manage delivery lifecycle documents; publish `ORDER_DELIVERED`.
5. **M5 — Polish**: indexes on `orderId`, error handling, and (optional) TTL on old notifications.

## Beginner gotchas

- **`_id` is a String/ObjectId**, not a numeric auto-increment. Keep your business key (e.g. `orderId`) as a separate field.
- **No JOINs** — store what you need in the document, or look up other services via Feign/events.
- **No schema migrations** — but be consistent in code; add an **index** on frequently queried fields:
  ```java
  @Indexed private Long orderId;   // from org.springframework.data.mongodb.core.index.Indexed
  ```
- **Don't mix** `spring-boot-starter-data-jpa` and `spring-boot-starter-data-mongodb` in the same service unless you really need both — it confuses auto-config for beginners.
- Keep **one Mongo database per service** (`kitchen_db`, `notification_db`, `delivery_db`).
