# Order Processing Service

**Event-driven microservice** demonstrating production-grade patterns:
Kafka · MongoDB · Redis Cache · JWT Security · Spring Boot 3.3 · Java 21

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        ORDER PROCESSING SERVICE                          │
│                                                                           │
│  ┌──────────┐    JWT     ┌────────────────┐    saves    ┌─────────────┐ │
│  │  Client  │──────────►│ OrderController │────────────►│   MongoDB   │ │
│  └──────────┘           └───────┬────────┘             └─────────────┘ │
│                                 │                                        │
│                         ┌───────▼────────┐    cache    ┌─────────────┐ │
│                         │  OrderService  │◄───────────►│    Redis    │ │
│                         └───────┬────────┘             └─────────────┘ │
│                                 │ publish                               │
│                         ┌───────▼────────────────┐                     │
│                         │     Kafka Producer      │                     │
│                         │  topic: order-events    │                     │
│                         └───────┬────────────────┘                     │
│                                 │                                        │
│                    ┌────────────▼──────────────────┐                   │
│                    │       Kafka Consumer           │                   │
│                    │  group: order-processing-group │                   │
│                    │                                │                   │
│                    │  ORDER_CREATED  ──► PROCESSING │                   │
│                    │  PROCESSING     ──► COMPLETED  │                   │
│                    └────────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Kafka Event Flow

```
POST /api/orders  (JWT required)
        │
        ▼
  OrderService.createOrder()
        │
        ├──► Save Order{status=CREATED} ──────────────► MongoDB
        │
        └──► KafkaProducer.publish(OrderEvent)
                    │
                    │  topic: order-events
                    │  key:   orderId  (ensures same order → same partition)
                    ▼
             Kafka Broker
                    │
                    ▼
             KafkaConsumer (group: order-processing-group)
                    │
                    ├──► Update status: CREATED → PROCESSING  → MongoDB
                    ├──► Run business logic (payment, inventory...)
                    └──► Update status: PROCESSING → COMPLETED → MongoDB
                    
             Manual ACK: offset committed only after successful processing
```

---

## Key Design Decisions

| Decision | Choice | Why |
|---|---|---|
| **Kafka key = orderId** | Same order → same partition | Preserves event ordering per order |
| **Manual ACK** | `AckMode.MANUAL_IMMEDIATE` | No message loss on consumer crash |
| **Idempotent producer** | `enable.idempotence=true` | No duplicate messages on retry |
| **Redis TTL** | orders=10min, customerOrders=5min | Balanced freshness vs performance |
| **@CacheEvict on cancel** | Evicts stale cache on status change | Prevents reading cancelled as active |
| **JWT stateless** | `SessionCreationPolicy.STATELESS` | Scales horizontally, no session store |
| **BCrypt** | Password encoding | Industry standard, adaptive cost factor |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Messaging | Apache Kafka (Confluent CP 7.5) |
| Database | MongoDB 7.0 |
| Cache | Redis 7.2 |
| Security | Spring Security + JWT (JJWT 0.12.6) |
| Build | Maven |

---

## Running Locally

### 1. Start infrastructure
```bash
docker-compose up -d
```

### 2. Run the application
```bash
mvn spring-boot:run
```

### 3. Get JWT token
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user","password":"password"}'
```

### 4. Create an order
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <YOUR_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "cust-001",
    "productId": "prod-42",
    "quantity": 3,
    "totalAmount": 299.99
  }'
```

### 5. Get order (Redis cached)
```bash
curl http://localhost:8080/api/orders/<ORDER_ID> \
  -H "Authorization: Bearer <YOUR_TOKEN>"
```

### 6. Cancel order (evicts cache)
```bash
curl -X DELETE http://localhost:8080/api/orders/<ORDER_ID> \
  -H "Authorization: Bearer <YOUR_TOKEN>"
```

---

## API Reference

| Method | Endpoint | Role | Description |
|---|---|---|---|
| `POST` | `/api/auth/login` | Public | Get JWT token |
| `POST` | `/api/orders` | USER | Create order → Kafka event |
| `GET` | `/api/orders/{id}` | USER | Get order (Redis cached) |
| `GET` | `/api/orders/customer/{id}` | USER | Get orders by customer |
| `GET` | `/api/orders` | ADMIN | Get all orders |
| `DELETE` | `/api/orders/{id}` | USER | Cancel order + evict cache |

---

## Test Users (Demo)

| Username | Password | Role |
|---|---|---|
| `user` | `password` | USER |
| `admin` | `admin123` | USER + ADMIN |

---

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `order-events` | OrderProducer | OrderConsumer | All order lifecycle events |

### Event Types
- `ORDER_CREATED` — new order placed
- `ORDER_CANCELLED` — order cancelled by user

---

## Monitoring

| URL | Purpose |
|---|---|
| `http://localhost:8090` | Kafka UI — browse topics, messages |
| `http://localhost:8080/actuator/health` | Service health |
| `http://localhost:8080/actuator/metrics` | Micrometer metrics |

---

## Interview Q&A — Kafka Concepts in This Project

**Q: Why is orderId used as the Kafka message key?**
> Kafka routes all messages with the same key to the same partition. Using orderId as key guarantees all events for one order (CREATED → PROCESSING → COMPLETED) are in the same partition and processed in order. Without it, events could land on different partitions and be processed out of sequence.

**Q: Why manual acknowledgment instead of auto-commit?**
> With auto-commit, offsets are committed on a timer regardless of processing success. If the consumer crashes after auto-commit but before finishing the business logic, that message is permanently lost. Manual ack (`acknowledgment.acknowledge()`) commits the offset only after successful processing — guaranteeing at-least-once delivery.

**Q: What does `enable.idempotence=true` do on the producer?**
> Without idempotence, a producer retry (on network failure) can write the same message twice — duplicate orders. Idempotent producer assigns each message a sequence number; the broker deduplicates retries automatically. Combined with `acks=all`, this gives effectively-once delivery from the producer side.

**Q: How does Redis caching work here?**
> `@Cacheable(value="orders", key="#orderId")` on `getOrderById` intercepts the call. First request → cache miss → hits MongoDB → stores result in Redis with 10-min TTL → returns data. Second request → cache hit → returns from Redis in ~0.5ms instead of ~20ms MongoDB round trip. `@CacheEvict` on `cancelOrder` removes the stale entry so the next fetch gets fresh data.
