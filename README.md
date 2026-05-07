# GuardRail - Core API & Guardrails Microservice

GuardRail is a robust, high-performance Spring Boot 3.x microservice that acts as the central API gateway and guardrail system. It demonstrates advanced concepts like concurrent request handling, distributed state management using Redis, and event-driven scheduling.

## 🎯 Objective

Build a production-ready Spring Boot microservice with:
- Thread-safe concurrent request handling (200 concurrent bot replies tested)
- Distributed Redis-based guardrails and state management
- Event-driven notification batching and scheduling
- Strict mathematical guardrails to prevent AI compute runaway

## 📋 Tech Stack

- **Java 17+** - Modern Java version
- **Spring Boot 3.4.5** - Latest Spring Boot release
- **PostgreSQL 16** - Relational database for content storage
- **Redis 7** - In-memory data store for guardrails and caching
- **Spring Data JPA/Hibernate** - ORM framework
- **Lombok** - Boilerplate reduction
- **Docker & Docker Compose** - Containerization

## ✨ Implemented Features

### Phase 1: Core API & Database Setup ✅

**Database Schema (JPA/Hibernate):**
- User entity: id, username, is_premium, createdAt
- Bot entity: id, name, persona_description
- Post entity: id, authorId, authorType, content, createdAt
- Comment entity: id, postId, authorId, authorType, content, depthLevel, parentCommentId, createdAt
- Notification entity: id, recipientUserId, message, processed, createdAt

**REST Endpoints:**
- `POST /api/users` - Create a new user (for testing)
- `GET /api/users` - List all users
- `GET /api/users/{userId}` - Get user by ID
- `POST /api/bots` - Create a new bot (for testing)
- `GET /api/bots` - List all bots
- `GET /api/bots/{botId}` - Get bot by ID
- `POST /api/posts` - Create a new post
- `GET /api/posts/{postId}` - Get post with virality score
- `POST /api/posts/{postId}/comments` - Add a comment to a post
- `GET /api/posts/{postId}/comments` - Get all comments on a post
- `POST /api/posts/{postId}/like` - Like a post

### Phase 2: Redis Virality Engine & Atomic Locks ✅

**Virality Score (Real-time Calculation):**
- Bot Reply: +1 Point
- Human Like: +20 Points
- Human Comment: +50 Points
- Implementation: `ViralityService` with atomic Redis increments

**Atomic Locks (Concurrency Protection):**

1. **Horizontal Cap** (Max 100 Bot Replies per Post)
   - Redis Key: `post:{id}:bot_count`
   - Uses INCR and range check for atomic operations
   - Returns 429 Too Many Requests when exceeded
   - Decrement on failure ensures counter accuracy

2. **Vertical Cap** (Max 20 Comment Depth)
   - Checked in `CommentService.validateDepth()`
   - Returns error if depth > 20

3. **Cooldown Cap** (10 Minutes Between Bot-Human Interactions)
   - Redis Key: `cooldown:bot_{id}:human_{id}`
   - TTL: 10 minutes
   - Uses SETNX (setIfAbsent) for atomic lock acquisition
   - Returns 429 Too Many Requests if key exists

### Phase 3: Notification Engine (Smart Batching) ✅

**Redis Throttler:**
- When bot interacts with user's post, checks 15-minute cooldown
- Key: `user:{id}:notif_cooldown`
- If YES (within 15 min): Push notification to Redis list `user:{id}:pending_notifications`
- If NO (after 15 min): Log "Push Notification Sent to User" and set 15-minute cooldown

**CRON Sweeper:**
- `@Scheduled` runs every 5 minutes
- Scans `pending_notification_users` set in Redis
- For each user:
  - Pops all pending messages from list
  - Counts them
  - Logs summarized message: "Summarized Push Notification: [Bot X and N others interacted with your posts]"
  - Clears the Redis list

### Phase 4: Corner Cases & Testing Criteria ✅

**Race Condition Testing (200 Concurrent Requests):**
- Tested with Redis atomic operations
- Horizontal cap stops at exactly 100 bot comments
- No database inconsistencies (101+ comments)
- Uses Redis INCR with range check pattern

**Statelessness:**
- All counters stored in Redis, never in Java memory
- No HashMap or static variables
- Service remains completely stateless

**Data Integrity:**
- PostgreSQL = source of truth for content
- Redis = gatekeeper for all access control
- Database transactions only committed if Redis guardrails pass

## 🏗️ Architecture

### Database Layer
- **JPA/Hibernate** for ORM
- **Spring Data JPA** repositories for data access
- **PostgreSQL** for persistent storage

### Redis Layer
- **RedisTemplate** for low-level Redis operations
- **Atomic Operations** for thread-safe guardrails
- **TTL Keys** for automatic expiration

### Service Layer
- **RedisGuardrailService** - Manages bot reply count and cooldown checks
- **ViralityService** - Tracks virality scores
- **NotificationService** - Handles notification batching
- **CommentService** - Comment creation with guardrail enforcement
- **PostService** - Post creation and virality updates
- **UserService** - User management
- **BotService** - Bot management

### Controller Layer
- **PostController** - Post CRUD operations
- **CommentController** - Comment CRUD operations
- **UserController** - User management endpoints
- **BotController** - Bot management endpoints

### Scheduling
- **NotificationScheduler** - Runs every 5 minutes to process pending notifications

## 🧵 Thread Safety & Concurrency

### How Redis Atomic Operations Guarantee Thread Safety

**1. Horizontal Cap (100 Bot Replies)**

```
RedisGuardrailService.incrementBotReplyCount():
1. INCR post:{id}:bot_count atomically
2. Get the incremented value
3. If value > 100:
   - DECR post:{id}:bot_count (rollback)
   - Return false
4. Return true
```

**Why it's thread-safe:**
- Redis INCR is atomic - executed on Redis server as single operation
- No race condition between read/check/write
- All 200 concurrent requests see consistent counter value
- Exactly 100 succeed, 100 fail

**2. Cooldown Check (10 Minutes)**

```
RedisGuardrailService.checkCooldown():
1. SETNX cooldown:bot_{id}:human_{id} "ACTIVE" EX 600
2. Return true if key was newly created
3. Return false if key already existed
```

**Why it's thread-safe:**
- Redis SETNX (SET if Not eXists) is atomic
- First request wins, all others fail
- TTL expires after 10 minutes automatically
- No concurrent access possible for same pair

**3. Notification Throttle (15 Minutes)**

```
NotificationService.handleBotNotification():
1. SETNX user:{id}:notif_cooldown "ACTIVE" EX 900
2. If true: send push notification
3. If false: queue to Redis list
```

**Why it's thread-safe:**
- SETNX ensures exactly one "real" notification per 15 minutes
- Other notifications safely queued
- Batch processing in scheduler is non-blocking

### Key Design Principles

1. **Atomic Operations Only** - Use Redis INCR, SETNX, not read-then-write
2. **TTL Expiration** - Keys automatically expire (no manual cleanup needed)
3. **Fail-Fast** - Guardrail failures return 429 immediately
4. **Database Transactions** - Only committed after Redis guardrails pass
5. **No Java Memory** - All state in Redis, no HashMap or static variables

## 🚀 Quick Start Guide

### Prerequisites
- Docker and Docker Compose installed
- Java 17+ installed
- Maven installed (or use ./mvnw.cmd)

### 1. Start PostgreSQL and Redis

```powershell
# Windows PowerShell
Set-Location "D:\GuardRail\guardrail"
docker compose up -d

# Verify containers are running
docker ps
```

Expected output shows:
- `guardrail-postgres` container running on port 5432
- `guardrail-redis` container running on port 6379

### 2. Run the Application

```powershell
# Windows PowerShell
Set-Location "D:\GuardRail\guardrail"
.\mvnw.cmd spring-boot:run

# Or build and run JAR
.\mvnw.cmd clean package
java -jar target/guardrail-0.0.1-SNAPSHOT.jar
```

Expected output:
```
Guardrail Application Started
Spring Boot server started on port 8080
```

### 3. Test the API

Use the included Postman collection or curl:

```powershell
# Create a user
$user = Invoke-WebRequest -Uri "http://localhost:8080/api/users" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"username":"alice","isPremium":true}'

# Create a bot
$bot = Invoke-WebRequest -Uri "http://localhost:8080/api/bots" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"name":"BotX","personaDescription":"Helpful AI assistant"}'

# Create a post
$post = Invoke-WebRequest -Uri "http://localhost:8080/api/posts" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"authorId":1,"authorType":"USER","content":"Hello World"}'

# Add a comment
Invoke-WebRequest -Uri "http://localhost:8080/api/posts/1/comments" `
  -Method POST `
  -Headers @{"Content-Type"="application/json"} `
  -Body '{"authorId":1,"authorType":"BOT","content":"Great post"}'
```

## 📊 API Endpoints Reference

### User Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create a new user |
| GET | `/api/users` | List all users |
| GET | `/api/users/{userId}` | Get user by ID |

### Bot Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/bots` | Create a new bot |
| GET | `/api/bots` | List all bots |
| GET | `/api/bots/{botId}` | Get bot by ID |

### Post Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/posts` | Create a new post |
| GET | `/api/posts/{postId}` | Get post with virality score |
| POST | `/api/posts/{postId}/like` | Like a post (updates virality) |

### Comment Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/posts/{postId}/comments` | Add comment to post |
| GET | `/api/posts/{postId}/comments` | Get all comments on post |

## 🔑 Redis Keys Reference

| Key Pattern | TTL | Purpose |
|------------|-----|---------|
| `post:{id}:virality_score` | None | Tracks post virality score |
| `post:{id}:bot_count` | None | Counts bot replies per post |
| `cooldown:bot_{id}:human_{id}` | 10 min | Prevents bot spam to human |
| `user:{id}:notif_cooldown` | 15 min | Throttles notifications |
| `user:{id}:pending_notifications` | None | Queue of pending notifications |
| `pending_notification_users` | None | Set of users with pending notifs |

## 📝 Example Workflow

### Test Bot Reply Cap (100 Maximum)

```powershell
# 1. Create a user and bot
# User ID: 1, Bot ID: 1

# 2. Create a post by user
# POST /api/posts
# Body: {"authorId": 1, "authorType": "USER", "content": "Test"}
# Response: PostId: 1

# 3. Simulate 200 concurrent bot comments
# Send 200 parallel requests:
# POST /api/posts/1/comments
# Body: {"authorId": 1, "authorType": "BOT", "content": "Reply"}

# Expected Result:
# - First 100 succeed with 201 Created
# - Next 100 fail with 429 Too Many Requests
# - Database shows exactly 100 bot comments (no race condition)
# - Redis key: post:1:bot_count = 100
```

### Test Notification Batching

```powershell
# 1. Create user (ID: 2) and bot (ID: 2)

# 2. Create user's post
# POST /api/posts
# Body: {"authorId": 2, "authorType": "USER", "content": "My post"}
# Response: PostId: 2

# 3. Bot replies (first time within 15 min window)
# POST /api/posts/2/comments
# Body: {"authorId": 2, "authorType": "BOT", "content": "Comment 1"}
# Console Output: "Push Notification Sent to User: ..."

# 4. Bot replies again (within 15 min window)
# POST /api/posts/2/comments
# Body: {"authorId": 2, "authorType": "BOT", "content": "Comment 2"}
# Console Output: "QUEUE NOTIFICATION CALLED"
# Redis: user:2:pending_notifications contains messages

# 5. Wait for scheduler (5 minutes)
# Scheduler logs: "Summarized Push Notification: Bot 2 replied to your post and [1] others..."
# Notification saved to PostgreSQL
# Redis list cleared
```

## 🧪 Testing with Postman

1. Import `postman/GuardRail.postman_collection.json` into Postman
2. Set the `baseUrl` variable to `http://localhost:8080` (default)
3. Run requests in order:
   - Create User
   - Create Bot
   - Create Post
   - Add Comment
   - Like Post
   - Get Post (see virality score)
   - Get Comments

## 📊 Monitoring & Debugging

### View Redis Keys

```powershell
# Connect to Redis container
docker exec -it guardrail-redis redis-cli

# View all keys
KEYS *

# Check virality score
GET post:1:virality_score

# Check bot count
GET post:1:bot_count

# Check pending notifications
LRANGE user:1:pending_notifications 0 -1
```

### View Database

```powershell
# Connect to PostgreSQL
docker exec -it guardrail-postgres psql -U postgres -d guardrail_db

# List tables
\dt

# View posts
SELECT * FROM posts;

# View comments
SELECT * FROM comments;

# View notifications
SELECT * FROM notifications;
```

### View Application Logs

```powershell
# Follow logs
docker logs -f guardrail-postgres &
docker logs guardrail-redis
# Or from running app output
```

## 🛑 Error Handling

| Status | Scenario | Fix |
|--------|----------|-----|
| 429 | Bot reply cap exceeded | Max 100 bot replies per post |
| 429 | Cooldown not expired | Wait 10 minutes for cooldown |
| 400 | Max depth exceeded | Max 20 levels nesting |
| 404 | Post/User/Bot not found | Create entity first |
| 500 | Database error | Check PostgreSQL connection |

## 📚 Project Structure

```
guardrail/
├── src/main/java/com/project/guardrail/
│   ├── GuardrailApplication.java          # Main entry point
│   ├── config/
│   │   └── RedisConfig.java               # Redis template beans
│   ├── controller/
│   │   ├── PostController.java
│   │   ├── CommentController.java
│   │   ├── UserController.java
│   │   └── BotController.java
│   ├── dto/
│   │   ├── request/                       # Request DTOs
│   │   └── response/                      # Response DTOs
│   ├── entity/                            # JPA entities
│   ├── repository/                        # Spring Data JPA repos
│   ├── service/
│   │   ├── PostService.java
│   │   ├── CommentService.java
│   │   ├── NotificationService.java
│   │   ├── RedisGuardrailService.java
│   │   ├── ViralityService.java
│   │   ├── UserService.java
│   │   └── BotService.java
│   └── scheduler/
│       └── NotificationScheduler.java     # 5-minute batch processor
├── src/main/resources/
│   └── application.properties             # Config
├── docker-compose.yml                     # PostgreSQL + Redis
├── pom.xml                                # Maven dependencies
├── postman/
│   └── GuardRail.postman_collection.json  # API test collection
└── README.md                              # This file
```

## ⚙️ Configuration

### application.properties

```properties
# PostgreSQL Database
spring.datasource.url=jdbc:postgresql://localhost:5432/guardrail_db
spring.datasource.username=postgres
spring.datasource.password=postgres

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Server
server.port=8080
```

## 🔒 Security & Best Practices

1. **Atomic Operations** - All concurrent operations use Redis atomic operations (INCR, SETNX)
2. **TTL Expiration** - Keys automatically expire (no stale data)
3. **Transactional Safety** - Database transactions only after guardrails pass
4. **No Shared Memory** - All state in Redis, completely stateless
5. **Input Validation** - Jakarta Validation annotations on all DTOs
6. **Error Handling** - Proper HTTP status codes (429 for limits, 404 for not found, etc.)

## 📈 Performance Considerations

- **Throughput**: Tested with 200 concurrent requests
- **Latency**: Redis operations < 1ms, DB operations ~5-50ms
- **Scalability**: Stateless design allows horizontal scaling
- **Memory**: Redis stores only O(P) keys where P = number of posts

---

**Last Updated:** May 8, 2026

