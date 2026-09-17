# Feature: Update Order's Payment Status (Async via Kafka)

**Goal:** When a payment succeeds, the order's `paymentStatus` should change from `PENDING` to `SUCCESS` — automatically, without Payment Service calling Order directly.

**How:** Payment Service **publishes** an event → Order Service **listens** and updates its own order.

```
Payment Service  --publish "PAYMENT_SUCCESS"-->  [ Kafka topic: payment-events ]  --listen-->  Order Service
   (PRODUCER)                                                                          (updates paymentStatus)
```

> Prerequisite: Kafka must be running (see `KAFKA_FROM_SCRATCH.md`, Section 3 — `docker compose -f docker-compose-infra.yml up -d`).

---

# PROGRESS CHECKLIST

**Payment Service (the sender)**
- [x] Step 1 — Add `spring-kafka` dependency to `pom.xml`  *(already done)*
- [x] Step 2 — Add producer config to `application.properties`  *(already done)*
- [ ] Step 3 — Create `PaymentEvent` class
- [ ] Step 4 — Create `PaymentEventPublisher` class
- [ ] Step 5 — Publish the event inside `createPayment(...)`

**Order Service (the receiver)**
- [ ] Step 6 — Add `spring-kafka` dependency to `pom.xml`
- [ ] Step 7 — Add consumer config to `application.properties`
- [ ] Step 8 — Create `PaymentEvent` class (a copy)
- [ ] Step 9 — Create `PaymentEventConsumer` class
- [ ] Step 10 — Test it

---

# PART 1 — PAYMENT SERVICE (sends the event)

## Step 1 — Dependency  ✅ DONE
This is already in `Payment-Service/pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

## Step 2 — Producer config  ✅ DONE
This is already in `Payment-Service/src/main/resources/application.properties`:
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```
**What this means:** tell Spring where Kafka is (port 9092) and to convert our event object into JSON before sending.

## Step 3 — Create the event class
This is the message we will send. Create a new file:

**File:** `Payment-Service/src/main/java/com/example/Payment_Service/Event/PaymentEvent.java`
```java
package com.example.Payment_Service.Event;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentEvent {
    private String eventType;   // "PAYMENT_SUCCESS" or "PAYMENT_FAILED"
    private Long paymentId;
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String status;
}
```
**What this means:** just a simple data holder describing what happened.

## Step 4 — Create the publisher
This is the tool that actually sends the event to Kafka.

**File:** `Payment-Service/src/main/java/com/example/Payment_Service/Event/PaymentEventPublisher.java`
```java
package com.example.Payment_Service.Event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {

    private static final String TOPIC = "payment-events";   // the channel name

    @Autowired
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void publish(PaymentEvent event) {
        // orderId is used as the "key" so events for one order stay in order
        kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);
    }
}
```
**What this means:** `kafkaTemplate.send(...)` drops the event on the `payment-events` channel and returns immediately (does NOT wait for Order Service).

## Step 5 — Publish inside `createPayment(...)`
Open `Payment-Service/.../Service/PaymentService.java`.

**5a.** Add this field near the other `@Autowired` fields at the top of the class:
```java
@Autowired
private com.example.Payment_Service.Event.PaymentEventPublisher paymentEventPublisher;
```

**5b.** Find where the payment is saved (a line like `PaymentDetails saved = paymentDetailsRepo.save(payment);`). **After** that line, and **before** the method returns, add:
```java
PaymentEvent event = new PaymentEvent();
event.setEventType("SUCCESS".equalsIgnoreCase(saved.getStatus()) ? "PAYMENT_SUCCESS" : "PAYMENT_FAILED");
event.setPaymentId(saved.getId());
event.setOrderId(saved.getOrderId());
event.setCustomerId(saved.getCustomerId());
event.setAmount(saved.getAmount());
event.setStatus(saved.getStatus());
paymentEventPublisher.publish(event);
```
> Note: use the exact variable name your code uses for the saved payment (it may be `saved`, `payment`, etc.). Match your repository/getter names.

**Payment Service is done.** It now announces every payment.

---

# PART 2 — ORDER SERVICE (receives the event)

## Step 6 — Add dependency
**File:** `Order_Service/pom.xml` — add inside `<dependencies>`:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

## Step 7 — Add consumer config
**File:** `Order_Service/src/main/resources/application.properties` — add:
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=order-service
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
spring.kafka.consumer.properties.spring.json.use.type.headers=false
spring.kafka.consumer.properties.spring.json.value.default.type=com.example.Order_Service.Event.PaymentEvent
```
**What the tricky lines mean:**
- `group-id=order-service` → this service's unique team name (needed so it receives messages).
- `auto-offset-reset=earliest` → read messages from the start (so you don't miss any during testing).
- `trusted.packages=*` → allow Spring to build the object from JSON.
- `use.type.headers=false` + `value.default.type=...` → the sender (Payment) has its `PaymentEvent` in a different package; these two lines tell Order to just build **its own** `PaymentEvent` class instead.

## Step 8 — Create the event class (a copy)
**File:** `Order_Service/src/main/java/com/example/Order_Service/Event/PaymentEvent.java`
```java
package com.example.Order_Service.Event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class PaymentEvent {
    private String eventType;
    private Long paymentId;
    private Long orderId;
    private Long customerId;
    private BigDecimal amount;
    private String status;
}
```
> Same fields as Payment's version. `@JsonIgnoreProperties(ignoreUnknown = true)` means "don't crash if there are extra fields".

## Step 9 — Create the consumer
This method runs automatically whenever a payment event arrives.

**File:** `Order_Service/src/main/java/com/example/Order_Service/Listener/PaymentEventConsumer.java`
```java
package com.example.Order_Service.Listener;

import com.example.Order_Service.Event.PaymentEvent;
import com.example.Order_Service.Model.OrderDetails;
import com.example.Order_Service.Repositary.OrderDetailesRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class PaymentEventConsumer {

    @Autowired
    private OrderDetailesRepo orderDetailesRepo;

    @KafkaListener(topics = "payment-events", groupId = "order-service")
    public void onPaymentEvent(PaymentEvent event) {
        OrderDetails order = orderDetailesRepo.findById(event.getOrderId()).orElse(null);
        if (order == null) {
            System.out.println("ORDER: no order found for id " + event.getOrderId());
            return;
        }
        if ("PAYMENT_SUCCESS".equals(event.getEventType())) {
            order.setPaymentStatus("SUCCESS");
        } else {
            order.setPaymentStatus("FAILED");
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderDetailesRepo.save(order);
        System.out.println("ORDER: order " + order.getId() + " paymentStatus -> " + order.getPaymentStatus());
    }
}
```
**What this means:** find the order by id, set its `paymentStatus`, and save it. `@KafkaListener` makes this run for every message on `payment-events`.

> Your repository is `OrderDetailesRepo` in package `com.example.Order_Service.Repositary` — the imports above already match your project.

---

# PART 3 — TEST IT (Step 10)

1. **Start Kafka:**
   ```bash
   docker compose -f docker-compose-infra.yml up -d
   ```
2. **Start Order Service (9294)** and **Payment Service (9295)** — `mvn spring-boot:run` in each folder.
3. **Create an order** (note the `orderId`; its `paymentStatus` will be `PENDING`):
   ```bash
   curl -X POST http://localhost:9294/API/Order/v1/CreateOrder \
     -H "Content-Type: application/json" \
     -d '{"customerId":1,"restaurantId":1,"deliveryAddressId":1,"items":[{"menuItemId":1,"quantity":2}]}'
   ```
4. **Make a payment** for that order:
   ```bash
   curl -X POST http://localhost:9295/API/Payments/v1/payment \
     -H "Content-Type: application/json" \
     -d '{"orderId":1,"customerId":1,"amount":932.50,"paymentMethod":"CARD","status":"SUCCESS"}'
   ```
5. **Watch the Order Service console** — it should print:
   ```
   ORDER: order 1 paymentStatus -> SUCCESS
   ```
6. **Confirm** the order changed:
   ```bash
   curl http://localhost:9294/API/Order/v1/GetOrderById/1
   ```
   `paymentStatus` should now be `SUCCESS`.
7. (Optional) Open **Kafka UI** at http://localhost:8090 → topic `payment-events` → see your message.

---

# IF SOMETHING GOES WRONG

| Problem | Fix |
|---|---|
| App won't start: "connection refused :9092" | Kafka isn't running. `docker compose -f docker-compose-infra.yml up -d` |
| Order console prints nothing | Check the topic name is exactly `payment-events` in both services; check `auto-offset-reset=earliest`. |
| Error "not in trusted packages" | Confirm `spring.kafka.consumer.properties.spring.json.trusted.packages=*` in Order. |
| Deserialization / class error | Confirm the two lines `use.type.headers=false` and `value.default.type=com.example.Order_Service.Event.PaymentEvent` in Order. |
| `paymentStatus` stays PENDING | Make sure Step 5 (publish) actually runs, and the `orderId` in the payment matches an existing order. |

---

# THE BIG IDEA (remember this)

- **Payment** doesn't call **Order**. It just **announces** "payment happened".
- **Order** listens and updates itself.
- If you later add **Notification Service** with a *different* `group-id`, it can listen to the **same** `payment-events` and send an email — without changing Payment at all. That's the power of async.
