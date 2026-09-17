# Kafka From Scratch — Learn It, Then Build the Whole Project

**Read this top to bottom.** It assumes you know **nothing** about Kafka or async messaging. By the end you will understand Kafka *and* have built Kitchen, Notification, and Delivery services for your food-delivery project.

> Related files in this repo: `API_Documentation.md` (existing REST APIs), `SETUP_AND_KAFKA.md` (DB/SQL + short Kafka intro), `COMPLETE_BUILD_GUIDE.md` (condensed code reference). **This file is the full teaching version — start here.**

---

# TABLE OF CONTENTS

1. What problem does Kafka solve?
2. Kafka concepts explained simply
3. Install & run Kafka (Docker)
4. Learn Kafka with the terminal (no code)
5. How Spring Boot talks to Kafka
6. Producer explained line-by-line
7. Consumer explained line-by-line
8. Serialization (turning objects into messages)
9. The project plan (what we build)
10. Build Step A — Infra
11. Build Step B — Order Service becomes a Producer
12. Build Step C — Kitchen Service (first consumer)
13. Build Step D — Notification Service
14. Build Step E — Delivery Service
15. Run everything & full test
16. Troubleshooting (read when stuck)
17. Glossary

---

# 1. What problem does Kafka solve?

Right now your **Order Service** calls other services directly and **waits** for each reply (this is "synchronous", using Feign/REST):

```
Order --> Customer   (wait)
Order --> Restaurant (wait)
Order --> Menu       (wait)
Order --> Payment    (wait)
```

That is fine when Order *needs* the answer to continue (e.g. "is payment successful?").

But after the order is placed, lots of things must happen: the **kitchen** must start cooking, the **customer** must get a notification, a **delivery** must be arranged. Order Service should **not** wait for all of those. If it did:
- It would be slow.
- If the Notification Service is down, the whole order fails. Bad!

**Kafka fixes this.** Order Service just **announces** "ORDER_PLACED" once, and walks away. Any number of other services **listen** and react on their own time. Order Service doesn't know or care who listens.

This is **asynchronous** (async) communication.

### Real-world analogy
Kafka is like a **notice board** (or a group chat):
- Order Service **pins a note**: "Order #1 was placed."
- Kitchen, Notification, and Delivery all **read the board** and react.
- The person pinning the note doesn't wait for anyone. The note stays on the board so even a latecomer can read it.

---

# 2. Kafka concepts explained simply

Learn these 7 words. Everything else is detail.

| Word | Simple meaning | Analogy |
|---|---|---|
| **Broker** | The Kafka server that stores messages. | The notice board itself |
| **Topic** | A named channel of messages. | One specific board, e.g. "order-events" |
| **Partition** | A topic is split into parts for speed/scaling. | Multiple columns on the board |
| **Offset** | The position/number of a message in a partition. | "You've read up to note #42" |
| **Producer** | A program that **sends** messages to a topic. | The person pinning notes |
| **Consumer** | A program that **reads** messages from a topic. | The person reading notes |
| **Consumer Group** | A team of consumers sharing a `group.id`. | A department reading the board |

### The most important rule (consumer groups)

- **Each consumer group gets its OWN copy** of every message.
- Within **one group**, a message is handled by only **one** member.

So for `ORDER_PLACED`:
- Kitchen (group `kitchen-service`) gets it.
- Notification (group `notification-service`) gets it.
- Delivery (group `delivery-service`) gets it.

All three receive the same event **because they use different group names**. If you accidentally gave them the same group name, only ONE of them would get each message. **Remember: one group.id per service.**

### What is an "event"?
An **event** is just a small JSON message describing something that happened:
```json
{
  "eventType": "ORDER_PLACED",
  "orderId": 1,
  "customerId": 1,
  "restaurantId": 1,
  "totalAmount": 932.50,
  "createdAt": "2026-08-18T01:00:00"
}
```

---

# 3. Install & run Kafka (Docker)

The easiest way to run Kafka locally is **Docker**. Install **Docker Desktop** first (https://www.docker.com/products/docker-desktop). Verify:
```bash
docker -v
docker compose version
```

Create a file named `docker-compose-infra.yml` in the project root with this content:

```yaml
version: "3.8"
services:
  zookeeper:                          # Kafka's helper for coordination
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports: ["2181:2181"]

  kafka:                              # the broker (the notice board)
    image: confluentinc/cp-kafka:7.6.0
    depends_on: [zookeeper]
    ports: ["9092:9092"]             # apps connect here
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  kafka-ui:                           # a website to SEE messages
    image: provectuslabs/kafka-ui:latest
    depends_on: [kafka]
    ports: ["8090:8080"]
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092

  mongodb:                            # database for the new services
    image: mongo:7
    ports: ["27017:27017"]
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: root
    volumes: ["mongo_data:/data/db"]

  mongo-express:                      # a website to SEE the database
    image: mongo-express:latest
    depends_on: [mongodb]
    ports: ["8091:8081"]
    environment:
      ME_CONFIG_MONGODB_ADMINUSERNAME: root
      ME_CONFIG_MONGODB_ADMINPASSWORD: root
      ME_CONFIG_MONGODB_URL: mongodb://root:root@mongodb:27017/

volumes:
  mongo_data:
```

Start everything:
```bash
docker compose -f docker-compose-infra.yml up -d
```
Check it's running:
```bash
docker compose -f docker-compose-infra.yml ps
```
Open in your browser:
- **Kafka UI:** http://localhost:8090  (watch messages here)
- **Mongo UI:** http://localhost:8091  (watch database here)

Stop everything later with:
```bash
docker compose -f docker-compose-infra.yml down
```

---

# 4. Learn Kafka with the terminal (NO code yet)

Before touching Spring, play with Kafka directly so the concepts click.

**Open a shell inside the Kafka container:**
```bash
docker exec -it $(docker ps -qf name=kafka) bash
```

**Create a topic** called `practice`:
```bash
kafka-topics --create --topic practice --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
```

**List topics:**
```bash
kafka-topics --list --bootstrap-server localhost:9092
```

**Start a producer** (you type messages, press Enter to send):
```bash
kafka-console-producer --topic practice --bootstrap-server localhost:9092
> hello
> my first kafka message
```
Leave it running. Open a **second** terminal and enter the container again (`docker exec ...`), then:

**Start a consumer** (reads from the beginning):
```bash
kafka-console-consumer --topic practice --from-beginning --bootstrap-server localhost:9092
```
You will see `hello` and `my first kafka message` appear. Now type more lines in the producer terminal — they appear instantly in the consumer. **That is Kafka.** A producer sends, a consumer receives, through a topic.

Type `exit` to leave the container. Now let's do the same thing from Spring Boot.

---

# 5. How Spring Boot talks to Kafka

Spring gives you two simple tools:
- **`KafkaTemplate`** — to **send** messages (producer).
- **`@KafkaListener`** — to **receive** messages (consumer).

You add one dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```
And a few config lines in `application.properties` (explained next). That's the whole integration.

---

# 6. Producer explained line-by-line

**Config** (goes in `application.properties` of the sender service):
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```
- `bootstrap-servers` → where Kafka is (from Step 3, port 9092).
- `key-serializer` → the message **key** is text (we use orderId as text).
- `value-serializer` → the message **value** (your object) is converted to **JSON**.

**Code:**
```java
@Component
public class OrderEventPublisher {

    private static final String TOPIC = "order-events";   // which board to pin on

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;  // the "send" tool

    public void publishOrderPlaced(OrderPlacedEvent event) {
        // key = orderId  => all events for the same order keep their order
        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);
    }
}
```
- `KafkaTemplate<String, OrderPlacedEvent>` → keys are `String`, values are your event object.
- `.send(topic, key, value)` → publishes the message. **Fire and forget** — it does not wait for consumers.
- **Why a key?** Messages with the same key go to the same partition, preserving order for that order.

---

# 7. Consumer explained line-by-line

**Config** (goes in `application.properties` of the receiver service):
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=kitchen-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
```
- `group-id` → this service's team name. **Must be unique per service.**
- `auto-offset-reset=earliest` → when a brand-new group starts, read messages **from the beginning** (so you don't miss ones already on the board). If `latest`, it would only read *new* messages.
- `value-deserializer=JsonDeserializer` → turn incoming JSON back into your Java object.
- `spring.json.trusted.packages=*` → allow Spring to build any class from JSON (required, or you get an error).

**Code:**
```java
@Component
public class OrderEventConsumer {

    @KafkaListener(topics = "order-events", groupId = "kitchen-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        // this method runs automatically every time a message arrives
        System.out.println("Received order: " + event.getOrderId());
    }
}
```
- `@KafkaListener(topics=..., groupId=...)` → "call this method for every message on this topic."
- The method parameter (`OrderPlacedEvent`) is the deserialized message. Spring fills it in for you.

---

# 8. Serialization (turning objects into messages)

Kafka only moves **bytes**. So:
- **Producer** turns your `OrderPlacedEvent` object → **JSON text** → bytes. (`JsonSerializer`)
- **Consumer** turns bytes → JSON text → your object. (`JsonDeserializer`)

**Cross-service gotcha:** the producer stamps the message with its Java class name (e.g. `com.example.Order_Service.Event.OrderPlacedEvent`). If the consumer's class is in a **different package**, deserialization can fail. Two fixes (use in Notification/Delivery which receive events from many services):
```properties
spring.kafka.consumer.properties.spring.json.use.type.headers=false
spring.kafka.consumer.properties.spring.json.value.default.type=com.example.YourService.Event.YourEventClass
```
- `use.type.headers=false` → ignore the sender's class name.
- `value.default.type=...` → always build THIS class instead. Give it a class with matching field names.

That's all the theory. Now we build.

---

# 9. The project plan (what we build)

You already have (sync, MySQL): Customer 9293, Resturant 9191, Menu 9292, Order 9294, Payment 9295.

We add (async, MongoDB):
- **Kitchen 9296** — listens for `ORDER_PLACED`, makes a cooking ticket, can mark `PREPARING`/`READY`, publishes `ORDER_READY`.
- **Notification 9297** — listens for everything, prints "emails".
- **Delivery 9298** — listens for `ORDER_READY`, manages delivery, publishes `ORDER_DELIVERED`.

**Topics & events:**

| Topic | Events | Produced by | Consumed by |
|---|---|---|---|
| `order-events` | `ORDER_PLACED` | Order | Kitchen, Notification |
| `kitchen-events` | `ORDER_PREPARING`, `ORDER_READY` | Kitchen | Delivery, Notification |
| `delivery-events` | `ORDER_PICKED_UP`, `ORDER_ON_THE_WAY`, `ORDER_DELIVERED` | Delivery | Notification |
| `payment-events` (optional) | `PAYMENT_SUCCESS`, `PAYMENT_FAILED` | Payment | Notification |

**Flow:**
```
Order --ORDER_PLACED--> Kitchen (RECEIVED) --ORDER_READY--> Delivery (READY_FOR_PICKUP) --ORDER_DELIVERED--> (done)
                         Notification hears every step and prints a message.
```

---

# 10. Build Step A — Infra

Do **Section 3** now: create `docker-compose-infra.yml`, run `docker compose -f docker-compose-infra.yml up -d`, and confirm Kafka UI (8090) and Mongo UI (8091) open. Don't continue until these work.

---

# 11. Build Step B — Order Service becomes a Producer

### B1. Add dependency to `Order_Service/pom.xml`
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### B2. Add to `Order_Service/src/main/resources/application.properties`
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

### B3. New file: `Order_Service/.../Event/OrderPlacedEvent.java`
```java
package com.example.Order_Service.Event;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderPlacedEvent {
    private String eventId;
    private String eventType;
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
```

### B4. New file: `Order_Service/.../Event/OrderEventPublisher.java`
```java
package com.example.Order_Service.Event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {
    private static final String TOPIC = "order-events";

    @Autowired
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void publishOrderPlaced(OrderPlacedEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);
    }
}
```

### B5. Publish inside `OrderService.createOrder(...)`
Add the field near the other `@Autowired` fields:
```java
@Autowired private com.example.Order_Service.Event.OrderEventPublisher orderEventPublisher;
```
After the order (and items) are saved, before `return`:
```java
OrderPlacedEvent event = new OrderPlacedEvent();
event.setEventId(java.util.UUID.randomUUID().toString());
event.setEventType("ORDER_PLACED");
event.setOrderId(saved.getId());
event.setCustomerId(saved.getCustomerId());
event.setRestaurantId(saved.getRestaurantId());
event.setTotalAmount(saved.getTotalAmount());
event.setCreatedAt(saved.getCreatedAt());
orderEventPublisher.publishOrderPlaced(event);
```

### B6. VERIFY (important!)
1. Make sure infra is up.
2. Start Order Service: `mvn spring-boot:run` in the `Order_Service` folder.
3. Place an order:
```bash
curl -X POST http://localhost:9294/API/Order/v1/CreateOrder \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"restaurantId":1,"deliveryAddressId":1,"items":[{"menuItemId":1,"quantity":2}]}'
```
4. Open Kafka UI (8090) → Topics → `order-events` → Messages. **You should see your event.**

If you see it, you are officially producing events. No consumer needed yet.

---

# 12. Build Step C — Kitchen Service (first consumer)

You need a **new Spring Boot project**. Easiest ways to create one:
- **IntelliJ:** File → New → Project → Spring Boot (Spring Initializr). Group `com.example`, Artifact `Kitchen-Service`, Java 17, add dependencies: **Spring Web**, **Spring for Apache Kafka**, **Spring Data MongoDB**, **Lombok**.
- **Or website:** https://start.spring.io with the same choices, download, unzip into this repo folder.

Then create these files (package base `com.example.Kitchen_Service`):

### C1. `application.properties`
```properties
spring.application.name=Kitchen-Service
server.port=9296

spring.data.mongodb.uri=mongodb://root:root@localhost:27017/kitchen_db?authSource=admin

spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=kitchen-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*

spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

### C2. Incoming event `.../Event/OrderPlacedEvent.java`
```java
package com.example.Kitchen_Service.Event;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class OrderPlacedEvent {
    private String eventId;
    private String eventType;
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
```
> Because this class is in a different package than Order's, also add to `application.properties`:
> ```properties
> spring.kafka.consumer.properties.spring.json.use.type.headers=false
> spring.kafka.consumer.properties.spring.json.value.default.type=com.example.Kitchen_Service.Event.OrderPlacedEvent
> ```

### C3. Outgoing event `.../Event/KitchenEvent.java`
```java
package com.example.Kitchen_Service.Event;
import lombok.Data;

@Data
public class KitchenEvent {
    private String eventType;   // ORDER_PREPARING | ORDER_READY
    private Long orderId;
    private Long restaurantId;
}
```

### C4. Document + Repo
```java
// .../Model/KitchenOrder.java
package com.example.Kitchen_Service.Model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Document(collection = "kitchen_orders")
@Data
public class KitchenOrder {
    @Id private String id;
    @Indexed private Long orderId;
    private Long restaurantId;
    private String status;      // RECEIVED | PREPARING | READY
    private LocalDateTime receivedAt;
    private LocalDateTime startedAt;
    private LocalDateTime readyAt;
}
```
```java
// .../Repo/KitchenOrderRepo.java
package com.example.Kitchen_Service.Repo;
import com.example.Kitchen_Service.Model.KitchenOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface KitchenOrderRepo extends MongoRepository<KitchenOrder, String> {
    Optional<KitchenOrder> findByOrderId(Long orderId);
}
```

### C5. Publisher
```java
// .../Event/KitchenEventPublisher.java
package com.example.Kitchen_Service.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KitchenEventPublisher {
    private static final String TOPIC = "kitchen-events";
    @Autowired private KafkaTemplate<String, KitchenEvent> kafkaTemplate;
    public void publish(KitchenEvent e) { kafkaTemplate.send(TOPIC, String.valueOf(e.getOrderId()), e); }
}
```

### C6. Consumer (creates the ticket)
```java
// .../Listener/OrderEventConsumer.java
package com.example.Kitchen_Service.Listener;
import com.example.Kitchen_Service.Event.OrderPlacedEvent;
import com.example.Kitchen_Service.Model.KitchenOrder;
import com.example.Kitchen_Service.Repo.KitchenOrderRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class OrderEventConsumer {
    @Autowired private KitchenOrderRepo repo;

    @KafkaListener(topics = "order-events", groupId = "kitchen-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        KitchenOrder ticket = new KitchenOrder();
        ticket.setOrderId(event.getOrderId());
        ticket.setRestaurantId(event.getRestaurantId());
        ticket.setStatus("RECEIVED");
        ticket.setReceivedAt(LocalDateTime.now());
        repo.save(ticket);
        System.out.println("KITCHEN: RECEIVED ticket for order " + event.getOrderId());
    }
}
```

### C7. Service + Controller (mark PREPARING/READY, publish ORDER_READY)
```java
// .../Service/KitchenService.java
package com.example.Kitchen_Service.Service;
import com.example.Kitchen_Service.Event.KitchenEvent;
import com.example.Kitchen_Service.Event.KitchenEventPublisher;
import com.example.Kitchen_Service.Model.KitchenOrder;
import com.example.Kitchen_Service.Repo.KitchenOrderRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class KitchenService {
    @Autowired private KitchenOrderRepo repo;
    @Autowired private KitchenEventPublisher publisher;

    public KitchenOrder updateStatus(Long orderId, String status) {
        KitchenOrder o = repo.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Kitchen order not found: " + orderId));
        o.setStatus(status);
        if ("PREPARING".equalsIgnoreCase(status)) o.setStartedAt(LocalDateTime.now());
        if ("READY".equalsIgnoreCase(status))     o.setReadyAt(LocalDateTime.now());
        KitchenOrder saved = repo.save(o);

        if ("PREPARING".equalsIgnoreCase(status) || "READY".equalsIgnoreCase(status)) {
            KitchenEvent e = new KitchenEvent();
            e.setOrderId(saved.getOrderId());
            e.setRestaurantId(saved.getRestaurantId());
            e.setEventType("READY".equalsIgnoreCase(status) ? "ORDER_READY" : "ORDER_PREPARING");
            publisher.publish(e);
        }
        return saved;
    }
}
```
```java
// .../Controller/KitchenController.java
package com.example.Kitchen_Service.Controller;
import com.example.Kitchen_Service.Model.KitchenOrder;
import com.example.Kitchen_Service.Service.KitchenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/kitchen")
public class KitchenController {
    @Autowired private KitchenService kitchenService;

    @PatchMapping("/orders/{orderId}/status")
    public KitchenOrder updateStatus(@PathVariable Long orderId, @RequestBody Map<String,String> body) {
        return kitchenService.updateStatus(orderId, body.get("status"));
    }
}
```

### C8. VERIFY
1. Start Kitchen Service (`mvn spring-boot:run`).
2. Place an order again (Step B6). **Kitchen console prints** "RECEIVED ticket".
3. Check Mongo UI (8091) → `kitchen_db` → `kitchen_orders` → your document.
4. Move it forward:
```bash
curl -X PATCH http://localhost:9296/api/kitchen/orders/1/status -H "Content-Type: application/json" -d '{"status":"PREPARING"}'
curl -X PATCH http://localhost:9296/api/kitchen/orders/1/status -H "Content-Type: application/json" -d '{"status":"READY"}'
```
5. In Kafka UI, topic `kitchen-events` now has an `ORDER_READY` message. **That's a consumer that also produces — the chain has begun.**

---

# 13. Build Step D — Notification Service

New Spring Boot project (base `com.example.Notification_Service`), dependencies: Spring Web, Spring for Apache Kafka, Lombok (MongoDB optional — start console-only).

### D1. `application.properties`
```properties
spring.application.name=Notification-Service
server.port=9297

spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
spring.kafka.consumer.properties.spring.json.use.type.headers=false
spring.kafka.consumer.properties.spring.json.value.default.type=com.example.Notification_Service.Event.GenericEvent
```

### D2. Generic event (accepts any event shape)
```java
package com.example.Notification_Service.Event;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class GenericEvent {
    private String eventType;
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private String status;
}
```

### D3. Listener (subscribes to all topics)
```java
package com.example.Notification_Service.Listener;
import com.example.Notification_Service.Event.GenericEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    @KafkaListener(topics = {"order-events","payment-events","kitchen-events","delivery-events"},
                   groupId = "notification-service")
    public void onEvent(GenericEvent e) {
        String type = e.getEventType() == null ? "" : e.getEventType();
        String msg = switch (type) {
            case "ORDER_PLACED"     -> "Your order #" + e.getOrderId() + " has been placed.";
            case "PAYMENT_SUCCESS"  -> "Payment successful for order #" + e.getOrderId() + ".";
            case "PAYMENT_FAILED"   -> "Payment failed for order #" + e.getOrderId() + ".";
            case "ORDER_PREPARING"  -> "Your food for order #" + e.getOrderId() + " is being prepared.";
            case "ORDER_READY"      -> "Your food for order #" + e.getOrderId() + " is ready.";
            case "ORDER_PICKED_UP"  -> "Order #" + e.getOrderId() + " was picked up.";
            case "ORDER_ON_THE_WAY" -> "Order #" + e.getOrderId() + " is on the way.";
            case "ORDER_DELIVERED"  -> "Order #" + e.getOrderId() + " was delivered.";
            default                 -> "Update for order #" + e.getOrderId();
        };
        System.out.println("=== EMAIL to customer " + e.getCustomerId() + " | order " + e.getOrderId() + " ===\n" + msg + "\n=========================");
    }
}
```

### D4. VERIFY
Start Notification Service. Repeat the order + kitchen steps — you'll see printed "emails" for each event. Done.

---

# 14. Build Step E — Delivery Service

New Spring Boot project (base `com.example.Delivery_Service`), dependencies: Spring Web, Spring for Apache Kafka, Spring Data MongoDB, Lombok.

### E1. `application.properties`
```properties
spring.application.name=Delivery-Service
server.port=9298

spring.data.mongodb.uri=mongodb://root:root@localhost:27017/delivery_db?authSource=admin

spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=delivery-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
spring.kafka.consumer.properties.spring.json.use.type.headers=false
spring.kafka.consumer.properties.spring.json.value.default.type=com.example.Delivery_Service.Event.KitchenEvent

spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

### E2. Events
```java
// .../Event/KitchenEvent.java  (incoming)
package com.example.Delivery_Service.Event;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class KitchenEvent {
    private String eventType;   // ORDER_READY
    private Long orderId;
    private Long restaurantId;
}
```
```java
// .../Event/DeliveryEvent.java  (outgoing)
package com.example.Delivery_Service.Event;
import lombok.Data;
@Data
public class DeliveryEvent {
    private String eventType;   // ORDER_PICKED_UP | ORDER_ON_THE_WAY | ORDER_DELIVERED
    private Long orderId;
    private Long driverId;
}
```

### E3. Document + Repo
```java
// .../Model/Delivery.java
package com.example.Delivery_Service.Model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Document(collection = "deliveries")
@Data
public class Delivery {
    @Id private String id;
    @Indexed private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private Long driverId;
    private String status;   // WAITING_FOR_FOOD | READY_FOR_PICKUP | PICKED_UP | ON_THE_WAY | DELIVERED
    private LocalDateTime pickupTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime createdAt;
}
```
```java
// .../Repo/DeliveryRepo.java
package com.example.Delivery_Service.Repo;
import com.example.Delivery_Service.Model.Delivery;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;
public interface DeliveryRepo extends MongoRepository<Delivery, String> {
    Optional<Delivery> findByOrderId(Long orderId);
}
```

### E4. Publisher + Consumer + Service + Controller
```java
// .../Event/DeliveryEventPublisher.java
package com.example.Delivery_Service.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
@Component
public class DeliveryEventPublisher {
    private static final String TOPIC = "delivery-events";
    @Autowired private KafkaTemplate<String, DeliveryEvent> kafkaTemplate;
    public void publish(DeliveryEvent e) { kafkaTemplate.send(TOPIC, String.valueOf(e.getOrderId()), e); }
}
```
```java
// .../Listener/KitchenEventConsumer.java
package com.example.Delivery_Service.Listener;
import com.example.Delivery_Service.Event.KitchenEvent;
import com.example.Delivery_Service.Model.Delivery;
import com.example.Delivery_Service.Repo.DeliveryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class KitchenEventConsumer {
    @Autowired private DeliveryRepo repo;

    @KafkaListener(topics = "kitchen-events", groupId = "delivery-service")
    public void onKitchenEvent(KitchenEvent event) {
        if (!"ORDER_READY".equals(event.getEventType())) return;  // ignore PREPARING
        Delivery d = new Delivery();
        d.setOrderId(event.getOrderId());
        d.setRestaurantId(event.getRestaurantId());
        d.setStatus("READY_FOR_PICKUP");
        d.setCreatedAt(LocalDateTime.now());
        repo.save(d);
        System.out.println("DELIVERY: READY_FOR_PICKUP for order " + event.getOrderId());
    }
}
```
```java
// .../Service/DeliveryService.java
package com.example.Delivery_Service.Service;
import com.example.Delivery_Service.Event.DeliveryEvent;
import com.example.Delivery_Service.Event.DeliveryEventPublisher;
import com.example.Delivery_Service.Model.Delivery;
import com.example.Delivery_Service.Repo.DeliveryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class DeliveryService {
    @Autowired private DeliveryRepo repo;
    @Autowired private DeliveryEventPublisher publisher;

    public Delivery assignDriver(Long orderId, Long driverId) {
        Delivery d = get(orderId); d.setDriverId(driverId); return repo.save(d);
    }
    public Delivery updateStatus(Long orderId, String status) {
        Delivery d = get(orderId);
        d.setStatus(status);
        if ("PICKED_UP".equalsIgnoreCase(status)) d.setPickupTime(LocalDateTime.now());
        if ("DELIVERED".equalsIgnoreCase(status)) d.setDeliveryTime(LocalDateTime.now());
        Delivery saved = repo.save(d);
        DeliveryEvent e = new DeliveryEvent();
        e.setOrderId(orderId); e.setDriverId(saved.getDriverId());
        e.setEventType(switch (status.toUpperCase()) {
            case "PICKED_UP" -> "ORDER_PICKED_UP";
            case "ON_THE_WAY" -> "ORDER_ON_THE_WAY";
            case "DELIVERED" -> "ORDER_DELIVERED";
            default -> "DELIVERY_UPDATE"; });
        publisher.publish(e);
        return saved;
    }
    private Delivery get(Long orderId) {
        return repo.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Delivery not found for order: " + orderId));
    }
}
```
```java
// .../Controller/DeliveryController.java
package com.example.Delivery_Service.Controller;
import com.example.Delivery_Service.Model.Delivery;
import com.example.Delivery_Service.Service.DeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/deliveries")
public class DeliveryController {
    @Autowired private DeliveryService service;

    @PostMapping("/{orderId}/assign-driver")
    public Delivery assign(@PathVariable Long orderId, @RequestBody Map<String,Long> body) {
        return service.assignDriver(orderId, body.get("driverId"));
    }
    @PatchMapping("/{orderId}/status")
    public Delivery updateStatus(@PathVariable Long orderId, @RequestBody Map<String,String> body) {
        return service.updateStatus(orderId, body.get("status"));
    }
}
```

---

# 15. Run everything & full test

**Start order:**
1. `docker compose -f docker-compose-infra.yml up -d`  (Kafka + Mongo)
2. Sync services: Resturant 9191, Menu 9292, Customer 9293, Order 9294, Payment 9295
3. Async services: Kitchen 9296, Notification 9297, Delivery 9298

**The whole story (run these in order):**
```bash
# place order -> ORDER_PLACED
curl -X POST http://localhost:9294/API/Order/v1/CreateOrder -H "Content-Type: application/json" \
  -d '{"customerId":1,"restaurantId":1,"deliveryAddressId":1,"items":[{"menuItemId":1,"quantity":2}]}'
#   Kitchen: RECEIVED ticket | Notification: "order placed"

# kitchen cooks -> ORDER_READY
curl -X PATCH http://localhost:9296/api/kitchen/orders/1/status -H "Content-Type: application/json" -d '{"status":"PREPARING"}'
curl -X PATCH http://localhost:9296/api/kitchen/orders/1/status -H "Content-Type: application/json" -d '{"status":"READY"}'
#   Delivery: READY_FOR_PICKUP | Notification: "food ready"

# delivery -> ORDER_DELIVERED
curl -X POST  http://localhost:9298/api/deliveries/1/assign-driver -H "Content-Type: application/json" -d '{"driverId":77}'
curl -X PATCH http://localhost:9298/api/deliveries/1/status -H "Content-Type: application/json" -d '{"status":"PICKED_UP"}'
curl -X PATCH http://localhost:9298/api/deliveries/1/status -H "Content-Type: application/json" -d '{"status":"ON_THE_WAY"}'
curl -X PATCH http://localhost:9298/api/deliveries/1/status -H "Content-Type: application/json" -d '{"status":"DELIVERED"}'
#   Notification: "picked up" / "on the way" / "delivered"
```
Watch **Kafka UI (8090)** for messages on each topic and **service consoles** for reactions.

---

# 16. Troubleshooting (read when stuck)

| Symptom | Cause & Fix |
|---|---|
| App won't start: "connection refused localhost:9092" | Kafka isn't running. `docker compose -f docker-compose-infra.yml up -d` |
| Consumer prints nothing | Wrong topic name, or `auto-offset-reset` not `earliest`. Also confirm the producer actually published (check Kafka UI). |
| Error: "not in trusted packages" | Add `spring.kafka.consumer.properties.spring.json.trusted.packages=*` |
| Deserialization error / wrong class | Add `spring.json.use.type.headers=false` and `spring.json.value.default.type=<your event class>` |
| Only ONE service receives an event that many should | They share the same `group-id`. Give each service a unique `group-id`. |
| Mongo auth error | URI must end with `?authSource=admin` and use `root/root` from the compose file. |
| Two apps fail to start on same port | Every service needs a unique `server.port`. |
| Event stored but consumer missed it | Restart consumer; with `earliest` it re-reads from the start of the topic for a new group. |

---

# 17. Glossary

- **Broker** — the Kafka server storing messages.
- **Topic** — a named stream of messages (e.g. `order-events`).
- **Partition** — a sub-log of a topic; enables parallelism and ordering per key.
- **Offset** — a message's position number in a partition.
- **Producer** — sends messages (`KafkaTemplate.send`).
- **Consumer** — reads messages (`@KafkaListener`).
- **Consumer Group** (`group.id`) — a team; each group gets every message once; unique per service.
- **Serializer/Deserializer** — converts objects ↔ JSON bytes.
- **Event** — a JSON message describing something that happened.
- **Sync (REST/Feign)** — caller waits for a reply (Order→Payment).
- **Async (Kafka)** — caller fires an event and moves on (Order→Kitchen).

---

## Final advice
Build in this exact order and **verify each step before the next**:
**A** infra → **B** Order producer (see it in Kafka UI) → **C** Kitchen (see the ticket in Mongo) → **D** Notification (see the printed email) → **E** Delivery (see the full chain). If a step fails, fix it using Section 16 before continuing. You've got everything you need in this file.
