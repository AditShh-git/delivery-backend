# 🚚 Delivery Platform — Multi-Tenant Logistics Backend

> A production-style multi-tenant logistics backend built with Spring Boot, PostgreSQL, Redis, and Docker.  
> Not a tutorial project. Built to solve real delivery problems.

---

## 💡 Why I Built This

I've personally faced three frustrating problems as a customer:

1. **Too many apps** — food delivery, courier, electronics returns all use different platforms. Why can't one backend handle all of them?
2. **The introvert problem** — when an order is out for delivery, the rider randomly calls. Some customers don't pick up because they're anxious about unexpected calls. The order fails — not because the customer was unavailable, but because they weren't prepared. **Scheduled slots fix this.**
3. **False no-show claims** — riders sometimes mark an order as attempted when they never showed up. Customers get charged. There's no proof either way. **OTP verification and delivery proof solve this** (Week 7).

This platform is my attempt to solve all three in one system.

---

## 🏗 Architecture Overview

```
Controller → Service → Repository
     ↓
Policy Enforcement Layer (CompanyPolicy per product + delivery type)
     ↓
State Machine Lifecycle (10 statuses, enforced transitions)
     ↓
Capacity-Safe Rider Dispatch (zone match, duty check, concurrency limit)
     ↓
Scheduler Layer (SLA automation, confirmation flow, auto-cancel)
     ↓
Analytics + Caching Layer (Redis, native SQL, projections)
```

Controllers are thin HTTP adapters — all business logic lives in service classes.

---

## ✅ Current Status

| Week | Focus | Status |
|------|-------|--------|
| 1–2 | Foundation — JWT, roles, entities, migrations | ✅ Done |
| 3 | Order Engine — state machine, slots, WhatsApp, SLA | ✅ Done |
| 4 | Analytics — dashboards, Redis caching, projections | ✅ Done |
| 5 | SLA Automation — scheduler, trend API, zone heatmap | ✅ Done |
| 5+ | Clean Architecture — refactored controllers to thin HTTP adapters | ✅ Done |
| 6 | Auto-Dispatch + RunSheet | 🔜 Planned |
| 7 | OTP + Proof + Notifications | 🔜 Planned |
| 8–9 | Intelligence — incentive engine, confidence scoring | 🔜 Future |
| 10–11 | Scale — partner API, webhooks, rate limiting, K8s | 🔜 Future |

---

## 🔑 Key Features (Weeks 1–5)

### Auth & Roles
- JWT authentication with BCrypt password hashing
- 4 roles: `ADMIN`, `COMPANY`, `RIDER`, `CUSTOMER`
- Role-scoped data isolation — COMPANY sees only its own data
- `@PreAuthorize` on all endpoints, custom 401/403 handlers

### 4 Delivery Models

| Model | SLA Deadline | Slot Required | Dispatch Style |
|-------|-------------|---------------|----------------|
| `INSTANT` | 30 minutes | No | Zone-based, auto-dispatched |
| `PARCEL` | 48 hours | Date only | WhatsApp-confirmed |
| `SCHEDULED` | 24 hours | Date + time window | WhatsApp-confirmed |
| `PICKUP_RETURN` | 48 hours | No | WhatsApp-confirmed |

#### 🏎 INSTANT (Zone-Based Dispatch)
For time-critical deliveries like food or same-hour courier. The order is assigned to the **nearest available rider in the same zone** and must be delivered within **30 minutes**. No customer confirmation step — the order goes live immediately after creation.

#### 📦 PARCEL & 🔄 PICKUP_RETURN (WhatsApp-Driven)

These models use a **two-step WhatsApp confirmation flow** before dispatch:

**Step 1 — 6AM Booking Confirmation:**  
At 6AM on the delivery date, `ConfirmationScheduler` sets the order to `CONFIRMATION_PENDING` and simulates sending a WhatsApp message to the customer:

> *"Your order is scheduled for delivery today. Please choose:*  
> *1️⃣ `CONFIRM` — Yes, deliver today*  
> *2️⃣ `CANCEL` — Cancel this order"*

- **`CONFIRM`** → Customer is prompted to pick a time slot next
- **`CANCEL`** → Order cancelled immediately

**Step 2 — Slot Selection (after CONFIRM):**  
Once the customer confirms, they must choose a delivery window:

> *"Please select time slot:*  
> *1️⃣ `SLOT_9_12` — 9AM to 12PM*  
> *2️⃣ `SLOT_12_3` — 12PM to 3PM*  
> *3️⃣ `SLOT_3_6` — 3PM to 6PM"*

- Slot availability is checked against `SlotCapacity` (company + zone + date)
- On success → order moves to `CONFIRMED`, slot `bookedCount` incremented

**If No Response — Auto-Retry & Auto-Reschedule:**
- After **12 hours** with no reply → a same-day reminder is sent
- After a **full day ignored** → order is automatically rescheduled to the next day (`slotDate + 1`)
- After **3 consecutive ignored days** → order is auto-cancelled (`autoCancelled = true`)

> **Planned (not yet implemented):** customer-triggered `RESCHEDULE` keyword, delivery-day reminder message, and automated company notification on cancellation.

### Order State Machine
```
CREATED → CONFIRMATION_PENDING → CONFIRMED → ASSIGNED → IN_TRANSIT → DELIVERED
                                                              ↓
                                                           FAILED → retry → auto-unassign
```
Invalid transitions are **blocked at the service layer**. An order cannot jump to `ASSIGNED` before `CONFIRMED`. This keeps rider analytics and KPI dashboards accurate.

### SLA Automation
- `SlaBreachScheduler` runs every **5 minutes** via `@Scheduled`
- Finds orders past their SLA deadline with `slaBreached = false`
- Auto-flags them and evicts the Redis dashboard cache
- Closes the gap where manual rider updates could leave breached orders undetected

### Analytics Dashboards
- **Admin KPI** — 9 executive metrics: total orders, delivered, failed, success rate, SLA breached, active riders, companies
- **Company KPI** — same metrics, isolated per company (multi-tenant)
- **Rider Analytics** — success rate, failure grouped by reason, zone + date filtered
- **Rider Ranking** — sorted by success rate
- **Order Trend API** — `GET /api/admin/reports/order-trend?interval=DAY|WEEK|MONTH`
- **Zone Heatmap** — `GET /api/admin/reports/zone-heatmap` — which zones are failing, which are overloaded

All dashboards use **native SQL aggregation + typed Spring projections** (no `Object[]` raw queries).

### Redis Caching
- `@Cacheable` on admin and company dashboards
- `@CacheEvict` on every order create, status update, and rider assignment
- All cache names registered in `CacheConfig` to prevent `Cannot find cache named` errors
- Prevents heavy analytics queries from hitting the DB on every request

### WhatsApp Webhook Engine
- Inbound replies handled by `WhatsAppWebhookService` (interface + `WhatsAppWebhookServiceImpl`)
- Webhook endpoint: `POST /api/webhook/whatsapp?orderId={id}&action={action}` — secured with `X-Webhook-Secret` header
- Supported actions: `CONFIRM` → slot prompt, `SLOT_9_12` / `SLOT_12_3` / `SLOT_3_6` → book slot, `CANCEL` → cancel order
- `ConfirmationScheduler` fires at **6AM daily** to send booking confirmations; retries every hour; auto-reschedules after 1 ignored day; auto-cancels after 3 ignored days
- All outbound messaging is currently **logged** (real WhatsApp API integration is wired in as the next step)

### Slot Capacity System
- Per-date, per-window slot capacity management via `SlotCapacity` entity
- `SlotController` exposes slots management for admins
- Capacity enforced at order confirmation time

### Zone Enforcement
- Riders can only be assigned to orders in their zone (hard constraint)
- Null-safe zone check in `assignRider()` — orders without a zone skip enforcement
- Zone heatmap shows failure rates and order volume per zone
- DB indexes on zone, status, SLA breach for fast analytics queries

### Company Onboarding
- Companies onboard with full policy setup per product + delivery type
- Email-based user resolution for company linking
- Company status tracking (active/inactive)

---

## 🗄 Database

- **20 Flyway migrations** — full schema versioning from V1 to V20
- Indexes on: `zone`, `status`, `sla_breached`, `company_id`, `created_at`
- Key tables: `orders`, `riders`, `users`, `companies`, `attempt_history`, `slot_capacities`, `zones`

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| Security | Spring Security + JWT |
| Database | PostgreSQL |
| Caching | Redis |
| Migrations | Flyway |
| Containerization | Docker + docker-compose |
| Build | Maven |

---

## ▶️ How To Run

**Prerequisites:** Docker and Docker Compose installed.

```bash
# 1. Clone the repository
git clone https://github.com/AditShh-git/delivery-platform

# 2. Create a .env file
cp .env.example .env
# Fill in your values

# 3. Start everything
docker-compose up --build
```

The app will start on `http://localhost:8080`.  
PostgreSQL on `5432`, Redis on `6379`.

---

## 📁 Project Structure

```
src/
├── controller/       # Thin REST adapters — routing, auth, request/response mapping only
├── service/          # Business logic, state machine, policy enforcement
│   └── impl/         # Service implementations
├── slot/             # Slot capacity entity, repository, service, and controller
├── repository/       # Spring Data JPA + native SQL queries
├── scheduler/        # SlaBreachScheduler, ConfirmationScheduler
├── entity/           # JPA entities
├── dto/              # Request/response records
│   ├── request/
│   └── response/
├── projection/       # Typed Spring projection interfaces
├── config/           # CacheConfig, SecurityConfig, custom auth entry points
├── security/         # JWT filter, token provider
├── utils/            # Shared utilities
└── exception/        # Global exception handler
db/migration/         # V1–V20 Flyway SQL migrations
```

---

## 🔜 Coming Next (Week 6)

- **AutoDispatchScheduler** — INSTANT orders self-assign to the best available rider every 5 seconds
- **RunSheet System** — admin creates a sorted delivery list for PARCEL riders, route ordered by nearest-neighbor GPS sort, exportable as CSV

---

*Built independently to simulate production SaaS backend architecture.*