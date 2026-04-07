# Social Graph Microservices Platform

> A production-grade distributed backend inspired by LinkedIn — built with **Spring Boot**, **Apache Kafka**, **Neo4j**, **PostgreSQL**, and **Kubernetes**.

Implements user authentication, social connection graphs, post feeds with media uploads, and real-time notifications across **7 independently deployable microservices** with full Kubernetes manifests for cloud deployment.

---

## Table of Contents

- [System Architecture](#system-architecture)
- [Service Breakdown](#service-breakdown)
- [How It All Works Together — Request Flows](#how-it-all-works-together--request-flows)
- [Kafka Event Bus](#kafka-event-bus)
- [API Gateway — JWT Auth & Routing](#api-gateway--jwt-auth--routing)
- [Kubernetes Deployment](#kubernetes-deployment)
- [Tech Stack](#tech-stack)
- [Local Setup](#local-setup)
- [Full API Reference](#full-api-reference)
- [Project Structure](#project-structure)

---

## System Architecture

```
                          ┌──────────────────────────────────────────┐
                          │            EXTERNAL CLIENT               │
                          │         (Mobile / Browser / Postman)     │
                          └──────────────────┬───────────────────────┘
                                             │ HTTPS
                                             ▼
                          ┌──────────────────────────────────────────┐
                          │           API GATEWAY  :8080             │
                          │                                          │
                          │  1. JWT token validation                 │
                          │  2. Extract userId from token            │
                          │  3. Inject X-User-Id header              │
                          │  4. Route to correct microservice        │
                          │  5. Load balance via Eureka              │
                          └───┬──────────┬──────────────────┬────────┘
                              │          │                  │
                       /api/v1/users  /api/v1/posts     /api/v1/connections
                              │          │                  │
                ┌─────────────▼──┐  ┌────▼───────────┐  ┌───▼──────────────┐
                │  USER SERVICE  │  │ POSTS SERVICE  │  │ CONNECTIONS SVC  │
                │    :9020       │  │    :9040       │  │     :9030        │
                │                │  │                │  │                  │
                │ - Signup/Login │  │ - Create post  │  │ - Send request   │
                │ - JWT issuance │  │ - Like/Unlike  │  │ - Accept/Reject  │
                │ - BCrypt hash  │  │ - Get posts    │  │ - 1st degree     │
                │                │  │                │  │  connections     │
                │ [PostgreSQL]   │  │ [PostgreSQL]   │  │  [Neo4j Graph]   │
                └───────┬────────┘  └──┬──────┬──────┘  └─────────────┬────┘
                        │              │      │                       │       
                        │  Open Feign  │      │   Open Feign          │
                        │   ┌──────────┘      └──────────────┐        │
                        │ upload File                (get 1st degree  │
                        │   │                          connections)   │
                        │   ▼                                │        │
                        │  ┌────────────────────┐            │        │
                        │  │  UPLOADER SERVICE  │            │        │
                        │  │      :9060         │            │        │
                        │  │                    │            │        │
                        │  │ - Upload image     │            │        │
                        │  │ - GCS / Cloudinary │            │        │
                        │  │ - Returns URL      │            │        │
                        │  └────────────────────┘            │        │
                        │                        post_created_topic   │
                        │         APACHE KAFKA   post_liked_topic     │
                        │                                    │        │
                        │                                    │        │
           user_created_topic                                │        │
                        │                                    │      connection_request_topic 
                        │                                    │      connection_accept_topic
                        │                                    │        │
                        └──────────────┬─────────────────────┘        │
                                       │◄─────────────────────────────┘
                                       ▼
                          ┌────────────────────────┐
                          │  NOTIFICATION SERVICE  │
                          │       :9050            │
                          │                        │
                          │  Kafka consumer for:   │
                          │  - connection events   │
                          │  - post events         │
                          │  - like events         │
                          │                        │
                          │  [PostgreSQL]          │
                          └────────────────────────┘

                          ┌────────────────────────┐
                          │   DISCOVER SERVICE     │
                          │   (Eureka)  :8761      │
                          │                        │
                          │  All services register │
                          │  here on startup.      │
                          │  Gateway does load-    │
                          │  balanced routing via  │
                          │  lb://SERVICE-NAME     │
                          └────────────────────────┘
```

---

## Service Breakdown

### 1. API Gateway — `:8080`
The **single entry point** for all client requests. No client ever talks directly to a microservice.

**Responsibilities:**
- Validates JWT on every protected route using a custom `AuthenticationFilter`
- Extracts `userId` from the JWT claims and injects it as `X-User-Id` header on the forwarded request
- Routes requests to downstream services using **Eureka load balancing** (`lb://SERVICE-NAME`)
- Strips `/api/v1/` prefix before forwarding (e.g. `/api/v1/posts/core/1` → `/core/1` at Posts Service)

**Routes configured:**

| Route | Incoming Path | Forwarded To | Auth Required |
|---|---|---|---|
| user-service | `/api/v1/users/**` | `USER-SERVICE` (Eureka) | ❌ No |
| posts-service | `/api/v1/posts/**` | `POSTS-SERVICE` (Eureka) | ✅ Yes |
| connections-service | `/api/v1/connections/**` | `CONNECTIONS-SERVICE` (Eureka) | ✅ Yes |

---

### 2. User Service — `:9020` · PostgreSQL
Owns user identity. The **only service that issues JWTs**.

**Endpoints:**
- `POST /auth/signup` — validates email uniqueness, BCrypt hashes password, saves user, publishes `UserCreatedEvent` to Kafka, returns `UserDto`
- `POST /auth/login` — verifies credentials, returns signed JWT

**Kafka Producer:**
- Publishes `user_created_topic` → `{ userId, name }` on every new signup

---

### 3. Connections Service — `:9030` · **Neo4j**
The social graph engine. Uses **Neo4j graph database** instead of relational tables because connection relationships are inherently graph-shaped — traversal queries (first-degree, second-degree, mutual connections) are native in Cypher and prohibitively expensive in SQL.

**Neo4j Node:** `Person { userId, name }`

**Neo4j Relationships:**
- `(p1)-[:REQUESTED_TO]->(p2)` — pending connection request
- `(p1)-[:CONNECTED_TO]-(p2)` — accepted connection (undirected)

**Cypher Queries Used:**
```cypher
-- Get first-degree connections
MATCH (personA:Person)-[:CONNECTED_TO]-(personB:Person)
WHERE personA.userId = $userId
RETURN DISTINCT personB

-- Send connection request (creates REQUESTED_TO edge)
MATCH (p1:Person), (p2:Person)
WHERE p1.userId = $senderId AND p2.userId = $receiverId
CREATE (p1)-[:REQUESTED_TO]->(p2)

-- Accept request (deletes REQUESTED_TO, creates CONNECTED_TO)
MATCH (p1:Person)-[r:REQUESTED_TO]->(p2:Person)
WHERE p1.userId = $senderId AND p2.userId = $receiverId
DELETE r
CREATE (p1)-[:CONNECTED_TO]->(p2)
```

**Business Logic Guards:**
- Cannot send request to yourself
- Cannot send duplicate request
- Cannot accept if already connected
- Cannot accept a request that doesn't exist

**Kafka Consumer:** Listens on `user_created_topic` → creates a `Person` node in Neo4j for every new user

**Kafka Producer:** Publishes to `connection_request_topic` and `connection_accept_topic` on every relevant action

---

### 4. Posts Service — `:9040` · PostgreSQL
Handles post creation, retrieval, and likes. Orchestrates **two synchronous Feign calls** and **two async Kafka publishes** during post creation.

**Post Creation Flow (detailed):**
1. Extract `userId` from `X-User-Id` header
2. Call **Uploader Service** via Feign → upload image → receive GCS URL
3. Save post to PostgreSQL with `userId` + `imageUrl`
4. Call **Connections Service** via Feign → fetch list of first-degree connections
5. For each connection → publish `PostCreated` event to `post_created_topic`

**Like Flow:**
1. Validate post exists
2. Guard against duplicate likes
3. Save `PostLike { userId, postId }` to PostgreSQL
4. Publish `PostLiked` event to `post_liked_topic`

**Feign Clients:**
- `ConnectionsServiceClient` → calls `/connections/core/{userId}/first-degree`
- `UploaderServiceClient` → calls `/uploads/file` with multipart file

Both Feign clients use `FeignClientInterceptor` to automatically propagate the `X-User-Id` header downstream.

---

### 5. Uploader Service — `:9060` · No Database
A stateless file upload abstraction. Implements a `UploaderService` strategy interface with two concrete backends:

| Implementation | When Active | Storage |
|---|---|---|
| `GCStorageUploaderService` | `@Service` (active) | Google Cloud Storage |
| `CloudinaryUploadService` | `// @Service` (commented out) | Cloudinary |

**Flow:** Receives `MultipartFile` → generates UUID filename → uploads to GCS → returns public `https://storage.googleapis.com/{bucket}/{filename}` URL

---

### 6. Notification Service — `:9050` · PostgreSQL
A **pure Kafka consumer** — no REST API, no direct calls from other services. Fully decoupled.

**Consumes 4 Kafka topics:**

| Topic | Action |
|---|---|
| `connection_request_topic` | Saves notification: "You received a connection request from userId: X" |
| `connection_accept_topic` | Saves notification: "Your connection request was accepted by userId: X" |
| `post_created_topic` | Saves notification for each connection: "Your connection X created a post: ..." |
| `post_liked_topic` | Saves notification for post owner: "User X liked your post Y" |

**Entity:** `Notification { id, userId, message, createdAt }`

---

### 7. Discover Service — `:8761` · Eureka Server
Spring Cloud Netflix Eureka registry. All other services register themselves on startup. The API Gateway uses `lb://SERVICE-NAME` to look up live instances and load balance.

---

## How It All Works Together — Request Flows

### Flow 1: User Signup
```
Client → POST /api/v1/users/auth/signup
  → API Gateway (no auth required, routes to User Service)
  → User Service: hash password → save to PostgreSQL
  → Kafka: publish user_created_topic { userId, name }
  → Connections Service (consumer): create Person node in Neo4j
  ← Return UserDto to client
```

### Flow 2: Send Connection Request
```
Client → POST /api/v1/connections/core/request/{receiverId}
  → API Gateway: validate JWT → inject X-User-Id header
  → Connections Service:
      - Check receiver exists in Neo4j
      - Check no duplicate request
      - Check not already connected
      - CREATE (sender)-[:REQUESTED_TO]->(receiver) in Neo4j
      - Kafka: publish connection_request_topic { senderId, receiverId }
  → Notification Service (consumer): save notification for receiver
  ← Return 204 No Content
```

### Flow 3: Create a Post with Image
```
Client → POST /api/v1/posts/core  (multipart: post JSON + image file)
  → API Gateway: validate JWT → inject X-User-Id header
  → Posts Service:
      - Feign → Uploader Service: upload image → get GCS URL
      - Save Post { userId, content, imageUrl } to PostgreSQL
      - Feign → Connections Service: get all first-degree connections
      - For each connection:
          Kafka: publish post_created_topic { postId, content, userId=connection, ownerUserId }
  → Notification Service (consumer): save notification for each connection
  ← Return PostDto { id, content, imageUrl, userId }
```

### Flow 4: Like a Post
```
Client → POST /api/v1/posts/likes/{postId}
  → API Gateway: validate JWT → inject X-User-Id header
  → Posts Service:
      - Verify post exists
      - Check not already liked
      - Save PostLike { userId, postId } to PostgreSQL
      - Kafka: publish post_liked_topic { likedByUserId, postId, ownerUserId }
  → Notification Service (consumer): save notification for post owner
  ← Return 204 No Content
```

---

## Kafka Event Bus

All async communication flows through Kafka. Services never call each other directly for notifications.

| Topic | Partitions | Producer | Consumer(s) | Payload |
|---|---|---|---|---|
| `user_created_topic` | 1 | User Service | Connections Service | `{ userId, name }` |
| `connection_request_topic` | 3 | Connections Service | Notification Service | `{ senderId, receiverId }` |
| `connection_accept_topic` | 3 | Connections Service | Notification Service | `{ senderId, receiverId }` |
| `post_created_topic` | - | Posts Service | Notification Service | `{ postId, content, userId, ownerUserId }` |
| `post_liked_topic` | - | Posts Service | Notification Service | `{ likedByUserId, postId, ownerUserId }` |

**Why Kafka (not REST calls to Notification Service)?**
If Notification Service is down, posts and connections still work. Kafka buffers the events. When Notification Service comes back up, it processes the backlog. A direct REST call would fail and require retry logic in every producer service.

---

**Why Neo4j over PostgreSQL for connections?**

A `connections` table in PostgreSQL would require a self-join for first-degree lookups and recursive CTEs for second-degree lookups. As the graph grows denser, these queries become exponentially more expensive. Neo4j traverses relationships in O(connections) time regardless of total graph size.

---

## API Gateway — JWT Auth & Routing

**JWT Validation Flow:**
```
Request arrives at Gateway
        │
        ▼
AuthenticationFilter.apply()
        │
        ├── Has "Authorization: Bearer <token>" header?
        │         No → 401 Unauthorized
        │
        ├── Is token valid? (signature + expiry check)
        │         No → 401 Unauthorized
        │
        ▼
Extract userId from JWT claims
        │
        ▼
Mutate request: add header X-User-Id = userId
        │
        ▼
Forward to downstream microservice
```

Downstream services read `userId` from `X-User-Id` header via `AuthContextHolder.getCurrentUserId()` — they never decode the JWT themselves. Auth logic lives in exactly one place.

---

## Kubernetes Deployment

Full `k8s/` manifests for deploying the entire platform on GKE (Google Kubernetes Engine).

### What's in k8s/

| File | Kind | Description |
|---|---|---|
| `api-gateway.yml` | Deployment + Service | API Gateway pod |
| `user-service.yml` | Deployment + Service | User Service pod |
| `user-db.yml` | Deployment + Service | Dedicated PostgreSQL for User Service |
| `posts-service.yml` | Deployment + Service | Posts Service pod |
| `posts-db.yml` | Deployment + Service | Dedicated PostgreSQL for Posts Service |
| `connections-service.yml` | Deployment + Service | Connections Service pod |
| `connections-db.yml` | Deployment + Service | Dedicated PostgreSQL for Connections |
| `notification-service.yml` | Deployment + Service | Notification Service pod |
| `notification-db.yml` | Deployment + Service | Dedicated PostgreSQL for Notifications |
| `uploader-service.yml` | Deployment + Service | Uploader Service pod |
| `kafka.yml` | **StatefulSet** + Service | Kafka cluster (2 replicas, KRaft mode) |
| `ingress.yml` | Ingress | GCE Ingress → routes all traffic to API Gateway |
| `secrets.yml` | Secret | DB passwords, JWT secret key |

### Key Design Decisions in K8s

**Per-service database isolation** — each microservice has its own PostgreSQL pod. No service can query another's database. This enforces strict data ownership.

**Kafka as StatefulSet** — Kafka requires stable network identities and persistent storage. StatefulSet provides both. KRaft mode (no ZooKeeper) keeps the setup simpler.

**Secrets via K8s Secrets** — DB passwords and JWT secret are injected as environment variables from `secrets.yml`, not hardcoded in any config file.

**Spring Profile `k8s`** — every service has `application-k8s.properties` with cluster-internal service DNS names (e.g. `user-db` instead of `localhost`). Activated by setting `SPRING_PROFILES_ACTIVE=k8s` in the Deployment spec.

**Resource limits on every pod:**
```yaml
resources:
  limits:
    memory: "400Mi"
    cpu: "200m"
  requests:
    memory: "200Mi"
    cpu: "100m"
```

### GCE Ingress
```
Internet → GCE Load Balancer → Ingress (myingress)
                                    │
                                    └── path: "/"
                                         └── api-gateway Service :80
```

All traffic enters through one IP, hits the GCE Ingress, goes to the API Gateway, which then routes internally.

---

## Tech Stack

| Layer | Technology | Why |
|---|---|---|
| Framework | Spring Boot 3 | Production-grade Java backend |
| API Gateway | Spring Cloud Gateway | Reactive gateway with filter chain |
| Service Discovery | Eureka (Spring Cloud Netflix) | Dynamic service registration + load balancing |
| Sync Communication | OpenFeign | Declarative HTTP client between services |
| Async Communication | Apache Kafka | Decoupled event-driven notifications |
| Auth | JWT | Stateless, scalable token auth |
| Password Hashing | BCrypt | Industry-standard one-way hashing |
| Relational DB | PostgreSQL | User, post, like, notification data |
| Graph DB | Neo4j | Social connection graph traversal |
| File Storage | Google Cloud Storage | Post image uploads |
| File Storage (alt) | Cloudinary | Pluggable alternative via strategy pattern |
| Containerization | Docker | Each service as independent container |
| Orchestration | Kubernetes | GKE-ready, full cluster deployment |
| Build | Maven (multi-module) | Single `mvn clean install` builds all services |

---

## Local Setup

### Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL (running on `localhost:5432`)
- Neo4j (running on `bolt://localhost:7687`)
- Apache Kafka (running on `localhost:9092`)

### Required Environment Variables

```bash
# Shared
JWT_SECRET_KEY=your_jwt_secret_at_least_32_chars

# User Service
POSTGRES_USERNAME=postgres
POSTGRES_PASSWORD=your_postgres_password

# Connections Service
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=your_neo4j_password

# Uploader Service (choose one)
GCP_BUCKET_NAME=your_gcs_bucket_name
# or configure Cloudinary credentials in application.yml
```

### Databases to Create

```sql
-- Run in PostgreSQL
CREATE DATABASE userDB;
CREATE DATABASE postsDB;
CREATE DATABASE notificationDB;
```

Neo4j: No schema setup needed. Nodes and relationships are created automatically.

### Build All Services

```bash
# From root directory (where parent pom.xml is)
mvn clean install
```

### Startup Order

Services must start in this sequence — each depends on Eureka being available:

```
Step 1:  DiscoverService      (Eureka must be up first)
Step 2:  UserService
Step 3:  ConnectionsService
Step 4:  PostsService
Step 5:  UploaderService
Step 6:  NotificationService
Step 7:  APIGateway           (must start last — needs all services registered in Eureka)
```

### Run Each Service

```bash
cd DiscoverService && mvn spring-boot:run
cd userService && mvn spring-boot:run
# ... repeat for each service in order
```

---

## Full API Reference

All requests go through the API Gateway at `http://localhost:8080`.
Protected routes require: `Authorization: Bearer <jwt_token>`

### Authentication (no token required)

| Method | URL | Body | Response |
|---|---|---|---|
| `POST` | `/api/v1/users/auth/signup` | `{ "name", "email", "password" }` | `UserDto` + 201 |
| `POST` | `/api/v1/users/auth/login` | `{ "email", "password" }` | JWT string + 200 |

### Connections 🔒

| Method | URL | Description | Response |
|---|---|---|---|
| `GET` | `/api/v1/connections/core/{userId}/first-degree` | Get all first-degree connections of a user | `List<Person>` |
| `POST` | `/api/v1/connections/core/request/{userId}` | Send connection request to userId | 204 |
| `POST` | `/api/v1/connections/core/accept/{userId}` | Accept connection request from userId | 204 |
| `POST` | `/api/v1/connections/core/reject/{userId}` | Reject connection request from userId | 204 |

### Posts 🔒

| Method | URL | Body | Description | Response |
|---|---|---|---|---|
| `POST` | `/api/v1/posts/core` | `multipart: post (JSON) + file (image)` | Create post with image | `PostDto` + 201 |
| `GET` | `/api/v1/posts/core/{postId}` | — | Get post by ID | `PostDto` |
| `GET` | `/api/v1/posts/core/users/{userId}/allPosts` | — | Get all posts of a user | `List<PostDto>` |
| `POST` | `/api/v1/posts/likes/{postId}` | — | Like a post | 204 |
| `DELETE` | `/api/v1/posts/likes/{postId}` | — | Unlike a post | 204 |

### File Upload 🔒

| Method | URL | Body | Description | Response |
|---|---|---|---|---|
| `POST` | `/api/v1/uploads/file` | `multipart: file` | Upload image to GCS | Public URL string |

---

## Project Structure

```
LinkedIn/
├── pom.xml                                  ← Parent POM (multi-module build)
│
├── DiscoverService/                         ← Eureka Service Registry
│   └── src/main/java/.../DiscoverServiceApplication.java
│
├── APIGateway/
│   └── src/main/java/
│       ├── filter/AuthenticationFilter.java ← JWT validation + X-User-Id injection
│       ├── service/JwtService.java
│       └── resources/application.yml        ← Route definitions
│
├── userService/
│   └── src/main/java/
│       ├── controller/UserController.java
│       ├── service/UserService.java          ← Signup, login, Kafka publish
│       ├── service/JwtService.java           ← JWT generation
│       ├── event/UserCreatedEvent.java
│       └── utils/BCrypt.java
│
├── ConnectionsService/
│   └── src/main/java/
│       ├── controller/ConnectionsController.java
│       ├── service/ConnectionsService.java   ← Neo4j graph operations + Kafka
│       ├── repository/PersonRepository.java  ← Cypher queries
│       ├── consumer/UserService.java         ← Kafka consumer: user_created_topic
│       └── entity/Person.java               ← Neo4j node
│
├── postsService/
│   └── src/main/java/
│       ├── controller/PostController.java
│       ├── controller/PostLikesController.java
│       ├── service/PostService.java           ← Feign + Kafka orchestration
│       ├── service/PostLikeService.java
│       ├── client/ConnectionsServiceClient.java  ← Feign client
│       └── client/UploaderServiceClient.java     ← Feign client
│
├── uploader-service/
│   └── src/main/java/
│       ├── controller/UploaderController.java
│       ├── service/UploaderService.java          ← Strategy interface
│       ├── service/GCStorageUploaderService.java ← Active: GCS
│       └── service/CloudinaryUploadService.java  ← Pluggable: Cloudinary
│
├── notification-service/
│   └── src/main/java/
│       ├── consumer/ConnectionsConsumer.java  ← Kafka: connection events
│       ├── consumer/PostsConsumer.java        ← Kafka: post + like events
│       ├── service/NotificationService.java
│       └── entity/Notification.java
│
└── k8s/                                      ← Kubernetes manifests
    ├── api-gateway.yml
    ├── user-service.yml + user-db.yml
    ├── connections-service.yml + connections-db.yml
    ├── posts-service.yml + posts-db.yml
    ├── notification-service.yml + notification-db.yml
    ├── uploader-service.yml
    ├── kafka.yml                              ← StatefulSet, 2 replicas, KRaft
    ├── ingress.yml                            ← GCE Ingress
    └── secrets.yml                            ← DB passwords, JWT secret
```
