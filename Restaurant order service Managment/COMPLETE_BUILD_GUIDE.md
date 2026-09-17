# Complete Build Guide — Async Services (Kitchen · Notification · Delivery)

This guide takes the project **from the current state (sync services + Payment) to a full event-driven system** using **Kafka + MongoDB**. It contains every file you need, in the order to create them.

> You already have (sync, MySQL): **Customer (9293)**, **Resturant (9191)**, **Menu (9292)**, **Order (9294)**, **Payment (9295)**.
> You will add (async, MongoDB): **Kitchen (9296)**, **Notification (9297)**, **Delivery (9298)**.

---

## 0. Big Picture

```
CUSTOMER
   | POST /orders
   v
ORDER SERVICE ---REST(Feign)---> Customer / Restaurant / Menu / Payment   (SYNC: needs answers now)
   | save order
   | publish ORDER_PLACED
   v
 [ Kafka: order-events ]
   |-------------------------------+-----------------------------+
   v                               v                             v
KITCHEN SERVICE               NOTIFICATION SERVICE          (Delivery waits for ORDER_READY)
 create RECEIVED ticket        send "order placed" msg
   | PATCH status PREPARING/READY
   | publish ORDER_READY
   v
 [ Kafka: kitchen-events ]
   |-------------------------------+
   v                               v
DELIVERY SERVICE              NOTIFICATION SERVICE
 create delivery, assign driver    "food ready"
   | PATCH PICKED_UP / ON_THE_WAY / DELIVERED
   | publish ORDER_DELIVERED
   v
 [ Kafka: delivery-events ] --> NOTIFICATION SERVICE ("delivered")
```

### Event & Topic catalog

| Topic | Event types | Produced by | Consumed by |
|---|---|---|---|
| `order-events` | `ORDER_PLACED` | Order | Kitchen, Notification |
| `payment-events` | `PAYMENT_SUCCESS`, `PAYMENT_FAILED` | Payment | Notification |
| `kitchen-events` | `ORDER_PREPARING`, `ORDER_READY` | Kitchen | Delivery, Notification |
| `delivery-events` | `ORDER_PICKED_UP`, `ORDER_ON_THE_WAY`, `ORDER_DELIVERED` | Delivery | Notification |

### Sync vs Async rule

- **Sync (Feign REST):** Order → Customer / Restaurant / Menu / Payment (Order needs the response before continuing).
- **Async (Kafka):** Order → Kitchen/Notification, Kitchen → Delivery/Notification, Delivery → Notification, Payment → Notification.

---

## 1. Infrastructure — Kafka + MongoDB (Docker)

Create `docker-compose-infra.yml` at the repo root:

```yaml
version: "3.8"
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports: ["2181:2181"]

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    depends_on: [zookeeper]
    ports: ["9092:9092"]
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    depends_on: [kafka]
    ports: ["8090:8080"]
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092

  mongodb:
    image: mongo:7
    ports: ["27017:27017"]
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: root
    volumes:
      - mongo_data:/data/db

  mongo-express:
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

Start & verify:
```bash
docker compose -f docker-compose-infra.yml up -d
docker compose -f docker-compose-infra.yml ps
```
- Kafka UI: http://localhost:8090
- Mongo UI: http://localhost:8091

---

## 2. Make Order Service a Producer

### 2.1 `Order_Service/pom.xml` — add dependency
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### 2.2 `Order_Service/src/main/resources/application.properties` — add
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

### 2.3 New: `Order_Service/.../Event/OrderPlacedEvent.java`
```java
package com.example.Order_Service.Event;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderPlacedEvent {
    private String eventId;
    private String eventType;   // ORDER_PLACED
    private Long orderId;
    private Long customerId;
    private Long restaurantId;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
```

### 2.4 New: `Order_Service/.../Event/OrderEventPublisher.java`
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

### 2.5 Edit `OrderService.createOrder(...)` — publish after saving
```java
// field:
@Autowired private OrderEventPublisher orderEventPublisher;

// after: OrderDetails saved = orderDetailesRepo.save(orderDetails); (and items saved)
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

**Verify:** restart Order Service, place an order, see the message in Kafka-UI → `order-events`.

---

## 3. (Optional) Payment Service publishes PAYMENT_SUCCESS

Same 3 pieces as Order:
- add `spring-kafka` + producer properties,
- `PaymentEvent` DTO (`eventType`, `paymentId`, `orderId`, `customerId`, `amount`, `status`),
- publisher sending to topic `payment-events` at the end of `createPayment(...)`.

```java
// PaymentEvent.java
@Data
public class PaymentEvent {
    private String eventType;   // PAYMENT_SUCCESS | PAYMENT_FAILED
    private Long paymentId;
    private Long orderId;
    private Long customerId;
    private java.math.BigDecimal amount;
    private String status;
}
```

---

## 4. Kitchen Service (Kafka consumer + MongoDB) — Port 9296

Create a new Spring Boot project `Kitchen-Service` with base package `com.example.Kitchen_Service`.

### 4.1 `pom.xml` dependencies
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 4.2 `application.properties`
```properties
spring.application.name=Kitchen-Service
server.port=9296

# MongoDB (own database)
spring.data.mongodb.uri=mongodb://root:root@localhost:27017/kitchen_db?authSource=admin

# Kafka consumer
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=kitchen-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*

# Kafka producer (to publish ORDER_READY)
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

### 4.3 Main class
```java
package com.example.Kitchen_Service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class KitchenServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(KitchenServiceApplication.class, args);
    }
}
```

### 4.4 Event DTOs (`.../Event/`)
```java
// OrderPlacedEvent.java  (incoming)
package com.example.Kitchen_Service.Event;
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
```java
// KitchenEvent.java  (outgoing)
package com.example.Kitchen_Service.Event;
import lombok.Data;

@Data
public class KitchenEvent {
    private String eventType;   // ORDER_PREPARING | ORDER_READY
    private Long orderId;
    private Long restaurantId;
}
```

### 4.5 Document + Repository (`.../Model/`, `.../Repo/`)
```java
// KitchenOrder.java
package com.example.Kitchen_Service.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import java.time.LocalDateTime;

@Document(collection = "kitchen_orders")
@Data
public class KitchenOrder {
    @Id
    private String id;
    @Indexed
    private Long orderId;
    private Long restaurantId;
    private String status;      // RECEIVED | PREPARING | READY
    private LocalDateTime receivedAt;
    private LocalDateTime startedAt;
    private LocalDateTime readyAt;
}
```
```java
// KitchenOrderRepo.java
package com.example.Kitchen_Service.Repo;

import com.example.Kitchen_Service.Model.KitchenOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface KitchenOrderRepo extends MongoRepository<KitchenOrder, String> {
    Optional<KitchenOrder> findByOrderId(Long orderId);
}
```

### 4.6 Publisher (`.../Event/KitchenEventPublisher.java`)
```java
package com.example.Kitchen_Service.Event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KitchenEventPublisher {
    private static final String TOPIC = "kitchen-events";

    @Autowired
    private KafkaTemplate<String, KitchenEvent> kafkaTemplate;

    public void publish(KitchenEvent event) {
        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);
    }
}
```

### 4.7 Consumer (`.../Listener/OrderEventConsumer.java`)
```java
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

    @Autowired
    private KitchenOrderRepo kitchenOrderRepo;

    @KafkaListener(topics = "order-events", groupId = "kitchen-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        KitchenOrder ticket = new KitchenOrder();
        ticket.setOrderId(event.getOrderId());
        ticket.setRestaurantId(event.getRestaurantId());
        ticket.setStatus("RECEIVED");
        ticket.setReceivedAt(LocalDateTime.now());
        kitchenOrderRepo.save(ticket);
        System.out.println("KITCHEN: created RECEIVED ticket for order " + event.getOrderId());
    }
}
```

### 4.8 Service + Controller (status updates + publish ORDER_READY)
```java
// KitchenService.java
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
        KitchenOrder order = repo.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Kitchen order not found: " + orderId));
        order.setStatus(status);
        if ("PREPARING".equalsIgnoreCase(status)) order.setStartedAt(LocalDateTime.now());
        if ("READY".equalsIgnoreCase(status))     order.setReadyAt(LocalDateTime.now());
        KitchenOrder saved = repo.save(order);

        KitchenEvent event = new KitchenEvent();
        event.setOrderId(saved.getOrderId());
        event.setRestaurantId(saved.getRestaurantId());
        event.setEventType("PREPARING".equalsIgnoreCase(status) ? "ORDER_PREPARING" : "ORDER_READY");
        if ("PREPARING".equalsIgnoreCase(status) || "READY".equalsIgnoreCase(status)) {
            publisher.publish(event);
        }
        return saved;
    }
}
```
```java
// KitchenController.java
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
    public KitchenOrder updateStatus(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        return kitchenService.updateStatus(orderId, body.get("status"));
    }
}
```

**Kitchen flow test:**
```bash
# place an order (Order Service) -> Kitchen auto-creates RECEIVED
curl -X PATCH http://localhost:9296/api/kitchen/orders/1/status -H "Content-Type: application/json" -d '{"status":"PREPARING"}'
curl -X PATCH http://localhost:9296/api/kitchen/orders/1/status -H "Content-Type: application/json" -d '{"status":"READY"}'
# READY publishes ORDER_READY to kitchen-events
```

---

## 5. Notification Service (multi-topic consumer) — Port 9297

Base package `com.example.Notification_Service`. It consumes **all** topics and prints (and optionally stores) notifications.

### 5.1 `pom.xml`
Same as Kitchen (web + data-mongodb + spring-kafka + lombok). MongoDB is optional here — start console-only.

### 5.2 `application.properties`
```properties
spring.application.name=Notification-Service
server.port=9297

spring.data.mongodb.uri=mongodb://root:root@localhost:27017/notification_db?authSource=admin

spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
# accept different DTO class names across services:
spring.kafka.consumer.properties.spring.json.use.type.headers=false
spring.kafka.consumer.properties.spring.json.value.default.type=com.example.Notification_Service.Event.GenericEvent
```

### 5.3 Generic event (accepts any event shape)
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

### 5.4 Listener (one method per topic)
```java
package com.example.Notification_Service.Listener;

import com.example.Notification_Service.Event.GenericEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    @KafkaListener(topics = {"order-events","payment-events","kitchen-events","delivery-events"},
                   groupId = "notification-service")
    public void onEvent(GenericEvent event) {
        String msg = switch (event.getEventType() == null ? "" : event.getEventType()) {
            case "ORDER_PLACED"     -> "Your order #" + event.getOrderId() + " has been placed.";
            case "PAYMENT_SUCCESS"  -> "Payment successful for order #" + event.getOrderId() + ".";
            case "PAYMENT_FAILED"   -> "Payment failed for order #" + event.getOrderId() + ".";
            case "ORDER_PREPARING"  -> "Your food for order #" + event.getOrderId() + " is being prepared.";
            case "ORDER_READY"      -> "Your food for order #" + event.getOrderId() + " is ready.";
            case "ORDER_PICKED_UP"  -> "Your order #" + event.getOrderId() + " was picked up.";
            case "ORDER_ON_THE_WAY" -> "Your order #" + event.getOrderId() + " is on the way.";
            case "ORDER_DELIVERED"  -> "Your order #" + event.getOrderId() + " was delivered.";
            default                 -> "Update for order #" + event.getOrderId();
        };
        System.out.println("""
                ====================================
                EMAIL NOTIFICATION
                Customer: %s
                Order: %s
                %s
                ====================================""".formatted(event.getCustomerId(), event.getOrderId(), msg));
    }
}
```

> Later, add a `Notification` document + repo to persist these (fields: `id, customerId, orderId, type, message, status, createdAt`).

---

## 6. Delivery Service (Kafka + MongoDB + REST) — Port 9298

Base package `com.example.Delivery_Service`. Consumes `ORDER_READY`, manages delivery lifecycle, publishes `ORDER_DELIVERED`.

### 6.1 `pom.xml`
Same as Kitchen (web + data-mongodb + spring-kafka + lombok).

### 6.2 `application.properties`
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

### 6.3 Document + Repo
```java
// Delivery.java
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
// DeliveryRepo.java
package com.example.Delivery_Service.Repo;

import com.example.Delivery_Service.Model.Delivery;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface DeliveryRepo extends MongoRepository<Delivery, String> {
    Optional<Delivery> findByOrderId(Long orderId);
}
```

### 6.4 Events
```java
// KitchenEvent.java (incoming)
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
// DeliveryEvent.java (outgoing)
package com.example.Delivery_Service.Event;
import lombok.Data;
@Data
public class DeliveryEvent {
    private String eventType;   // ORDER_PICKED_UP | ORDER_ON_THE_WAY | ORDER_DELIVERED
    private Long orderId;
    private Long driverId;
}
```

### 6.5 Publisher, Consumer, Service, Controller
```java
// DeliveryEventPublisher.java
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
// KitchenEventConsumer.java
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
        if (!"ORDER_READY".equals(event.getEventType())) return;   // ignore PREPARING
        Delivery d = new Delivery();
        d.setOrderId(event.getOrderId());
        d.setRestaurantId(event.getRestaurantId());
        d.setStatus("READY_FOR_PICKUP");
        d.setCreatedAt(LocalDateTime.now());
        repo.save(d);
        System.out.println("DELIVERY: created READY_FOR_PICKUP for order " + event.getOrderId());
    }
}
```
```java
// DeliveryService.java
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
        Delivery d = get(orderId);
        d.setDriverId(driverId);
        return repo.save(d);
    }

    public Delivery updateStatus(Long orderId, String status) {
        Delivery d = get(orderId);
        d.setStatus(status);
        if ("PICKED_UP".equalsIgnoreCase(status)) d.setPickupTime(LocalDateTime.now());
        if ("DELIVERED".equalsIgnoreCase(status)) d.setDeliveryTime(LocalDateTime.now());
        Delivery saved = repo.save(d);

        DeliveryEvent e = new DeliveryEvent();
        e.setOrderId(orderId);
        e.setDriverId(saved.getDriverId());
        e.setEventType(switch (status.toUpperCase()) {
            case "PICKED_UP"  -> "ORDER_PICKED_UP";
            case "ON_THE_WAY" -> "ORDER_ON_THE_WAY";
            case "DELIVERED"  -> "ORDER_DELIVERED";
            default            -> "DELIVERY_UPDATE";
        });
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
// DeliveryController.java
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
    public Delivery assign(@PathVariable Long orderId, @RequestBody Map<String, Long> body) {
        return service.assignDriver(orderId, body.get("driverId"));
    }

    @PatchMapping("/{orderId}/status")
    public Delivery updateStatus(@PathVariable Long orderId, @RequestBody Map<String, String> body) {
        return service.updateStatus(orderId, body.get("status"));
    }
}
```

---

## 7. Full Run Order

1. **Infra:** `docker compose -f docker-compose-infra.yml up -d`
2. **MySQL sync services:** Resturant (9191), Menu (9292), Customer (9293), Order (9294), Payment (9295)
3. **Async services:** Kitchen (9296), Notification (9297), Delivery (9298)

Each service: `mvn spring-boot:run` in its own terminal.

---

## 8. End-to-End Test (the whole story)

```bash
# 1) Seed data (customer, address, restaurant, menu) as in API_Documentation.md, then:

# 2) Place order  -> Order publishes ORDER_PLACED
curl -X POST http://localhost:9294/API/Order/v1/CreateOrder \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"restaurantId":1,"deliveryAddressId":1,"items":[{"menuItemId":1,"quantity":2}]}'
#   => Kitchen console: "created RECEIVED ticket"
#   => Notification console: "order has been placed"

# 3) Pay (optional PAYMENT_SUCCESS)
curl -X POST http://localhost:9295/API/Payments/v1/payment \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"customerId":1,"amount":932.50,"paymentMethod":"CARD","status":"SUCCESS"}'

# 4) Kitchen prepares then ready -> publishes ORDER_READY
curl -X PATCH http://localhost:9296/api/kitchen/orders/1/status -H "Content-Type: application/json" -d '{"status":"PREPARING"}'
curl -X PATCH http://localhost:9296/api/kitchen/orders/1/status -H "Content-Type: application/json" -d '{"status":"READY"}'
#   => Delivery console: "created READY_FOR_PICKUP"
#   => Notification console: "food is ready"

# 5) Delivery lifecycle -> publishes ORDER_DELIVERED
curl -X POST  http://localhost:9298/api/deliveries/1/assign-driver -H "Content-Type: application/json" -d '{"driverId":77}'
curl -X PATCH http://localhost:9298/api/deliveries/1/status -H "Content-Type: application/json" -d '{"status":"PICKED_UP"}'
curl -X PATCH http://localhost:9298/api/deliveries/1/status -H "Content-Type: application/json" -d '{"status":"ON_THE_WAY"}'
curl -X PATCH http://localhost:9298/api/deliveries/1/status -H "Content-Type: application/json" -d '{"status":"DELIVERED"}'
#   => Notification console: "picked up" / "on the way" / "delivered"
```

Watch: **Kafka-UI (8090)** for messages on every topic, **service consoles** for reactions, **mongo-express (8091)** for `kitchen_db` / `delivery_db` documents.

---

## 9. Build Order Checklist

- [ ] Phase 5a — Infra up (Kafka + Mongo); Order publishes `ORDER_PLACED`; verify in Kafka-UI.
- [ ] Phase 5b — Kitchen consumes `ORDER_PLACED`, saves `RECEIVED`; `PATCH status`; publishes `ORDER_READY`.
- [ ] Phase 6 — Notification consumes all topics; console emails.
- [ ] Phase 7 — Delivery consumes `ORDER_READY`; lifecycle; publishes `ORDER_DELIVERED`.
- [ ] Phase 8 — API Gateway, Resilience4j, Swagger, tests, tracing.

## 10. Common Pitfalls

- **Consumer sees nothing** → check `auto-offset-reset=earliest` and that the topic name matches exactly.
- **Deserialization error** → set `spring.json.trusted.packages=*`; for cross-service DTOs use `use.type.headers=false` + `value.default.type` (as shown in Notification/Delivery).
- **Publish before commit** → always publish *after* the DB save succeeds.
- **Shared DB** → never; each service owns `kitchen_db` / `notification_db` / `delivery_db`.
- **Order matters** → start infra before services; a consumer with `earliest` will still catch messages it missed.
