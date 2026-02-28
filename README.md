# 🚚 Delivery Backend — Multi-Model Logistics Platform

⚠️ **Status: Under Active Development**

This project is a production-style logistics backend system built using Spring Boot.  
It is being developed in structured weekly milestones.

Current Stable Milestone: **v0.3-week3-stable (Core Dispatch Engine Completed)**

---

## 📌 Project Vision

To build a scalable, multi-tenant logistics backend similar to real-world systems used by:

- E-commerce platforms (electronics, fashion, parcels)
- Courier companies
- Scheduled delivery services
- Reverse logistics providers

This is not a simple CRUD project.  
It is a domain-driven dispatch engine.

---

## 🚀 Implemented Features (Up to Week 3)

### 🔐 Role-Based Access Control
- ADMIN
- COMPANY
- RIDER
- CUSTOMER
- JWT authentication
- Service-level access validation

---

### 📦 Multi Delivery Models
- INSTANT (Quick delivery)
- PARCEL (Bulk courier style)
- SCHEDULED (Time-slot based)
- PICKUP_RETURN (Reverse logistics)

---

### 🔄 Order State Machine
Order lifecycle enforced via transition rules:

CREATED → ASSIGNED → IN_TRANSIT → DELIVERED  
FAILED → Retry → Auto-Unassign (after max attempts)

Invalid transitions are blocked at service layer.

---

### 🧠 Policy-Driven Retry Engine
- Company-level product policies
- DeliveryType-based rules
- Max reschedule limits
- Missed slot tracking
- Attempt history audit table
- Auto-unassign rider on max retry

---

### 👨‍✈️ Rider Capacity & Duty System
- On-duty / Off-duty state
- Max concurrent orders
- Active order count tracking
- Safe increment/decrement logic
- Capacity-based assignment
- Prevents over-allocation

---

### ⏱ SLA Tracking
- Model-based SLA deadlines
- SLA breach detection
- Audit-ready fields

---

### 🕒 Slot Capacity System
- Date + zone + time-window capacity
- Booked count tracking
- Prevents overbooking

---

### 🐳 Infrastructure
- Dockerized setup
- PostgreSQL
- Redis
- Flyway database migrations
- Profile-based configuration (local / docker)

---

## 🏗 Architecture

Controller → Service → Repository  
Policy Enforcement Layer  
State-Machine Based Lifecycle  
Capacity-Safe Rider Dispatch  
Audit & History Tracking  

Designed with production-level separation of concerns.

---

## 🛠 Tech Stack

- Java 17
- Spring Boot
- Spring Security (JWT)
- PostgreSQL
- Redis
- Flyway
- Docker
- Maven

---

## ▶️ How To Run

1. Clone the repository
2. Create a `.env` file with required environment variables
3. Run:

```bash
docker-compose up --build
