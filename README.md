# MyChess - Backend

A production-ready Spring Boot REST API for real-time multiplayer chess, featuring JWT authentication, WebSocket-based game synchronization, and comprehensive chess logic with FEN notation.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.8-brightgreen?logo=spring)
![Java](https://img.shields.io/badge/Java-24-orange?logo=java)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Latest-blue?logo=postgresql)
![WebSocket](https://img.shields.io/badge/WebSocket-6.2.12-yellow)

## ✨ Key Features

- **JWT Authentication**: Secure token-based auth with HTTP-only cookies and BCrypt password hashing
- **Real-time Multiplayer**: WebSocket-based bidirectional communication for live game synchronization
- **Complete Chess Engine**: Full move validation, checkmate detection, FEN notation, and special moves (castling, en passant, promotion)
- **RESTful API**: 9 endpoints for auth, room management, and gameplay
- **Database Persistence**: PostgreSQL with JPA/Hibernate for users, rooms, and game moves
- **Layered Architecture**: Clean separation of concerns (Controllers → Services → Repositories)

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Spring Boot** | 3.4.8 | Application framework |
| **Java** | 24 | Programming language |
| **Spring Security** | 6.x | Authentication & authorization |
| **Spring Data JPA** | 3.5.7 | ORM and data access |
| **Spring WebSocket** | 6.2.12 | Real-time communication |
| **PostgreSQL** | Latest | Relational database |
| **JJWT** | 0.11.5 | JWT token management |
| **Lombok** | Latest | Boilerplate reduction |
| **HikariCP** | Embedded | Connection pooling |
| **Maven** | 3.x+ | Build management |

## 🏗️ Architecture & System Design

### Layered Architecture

```
┌─────────────────────────────────────────┐
│         REST Controllers                │  ← HTTP/WebSocket endpoints
├─────────────────────────────────────────┤
│         Service Layer                   │  ← Business logic
├─────────────────────────────────────────┤
│         Repository Layer                │  ← Data access (JPA)
├─────────────────────────────────────────┤
│         Database (PostgreSQL)           │  ← Persistence
└─────────────────────────────────────────┘
```

### System Design Highlights

**1. Authentication Flow**
```
Client → POST /auth/login → AuthService validates credentials
   ↓
JWTService generates token → Set HTTP-only cookie → Response
   ↓
Subsequent requests → JwtAuthenticationFilter extracts cookie → Validates → Sets SecurityContext
```

**2. Real-time Game Synchronization**
```
Player A submits move (POST /room/move/{code})
   ↓
RoomService validates move → Updates FEN → Saves to DB
   ↓
Broadcasts to WebSocket subscribers (ConcurrentHashMap)
   ↓
Player B receives move update → UI updates board
```

**3. WebSocket Architecture**
```
WebSocketConfig (Spring WebSocket)
├── Endpoint: /room/ws/{roomCode}
├── Authentication: JWT cookie validation
├── Handler: RoomWebSocketHandler
│   ├── onOpen: Add to subscribers
│   ├── onClose: Remove from subscribers
│   └── onError: Cleanup and logging
└── Broadcasting: ExecutorService for async
```

**4. Chess Engine Design**
```
Move Validation Pipeline:
├── FenUtils.parseFen() → Convert FEN to 8x8 board
├── MoveUtils.validateMove() → Check piece-specific rules
│   ├── Path clearance (sliding pieces)
│   ├── Attack square detection
│   └── Special moves (castling, en passant)
├── CapturedPieceUtil.trackCapture()
├── Check for checkmate → isSquareAttacked()
└── FenUtils.generateFen() → Update board state
```

**5. Security Architecture**
```
SecurityConfig
├── CSRF: Disabled (REST API)
├── Session: Stateless
├── CORS: Configured origins with credentials
├── Public endpoints: /auth/**, /hello-world
├── Protected endpoints: All others
└── Filter chain:
    ├── JwtAuthenticationFilter (custom)
    └── Spring Security filters
```

**6. Database Schema Design**

**Users**:
```sql
id (UUID), email (unique), username (unique), password (BCrypt),
auth_provider, two_factor_enabled, is_active, in_game
```

**Rooms**:
```sql
id (UUID), code (unique 6-char), fen (board state),
captured_pieces, room_status, game_status,
white_player (FK), black_player (FK), last_activity
```

**GameMoves**:
```sql
id (UUID), room_id (FK), moved_by (FK),
move_number, move_notation (algebraic)
```

### Project Structure

```
src/main/java/com/mychess/my_chess_backend/
├── configs/
│   ├── SecurityConfig.java              # JWT + CORS + stateless sessions
│   ├── WebSocketConfig.java             # WebSocket endpoints
│   └── filters/JwtAuthenticationFilter  # Token validation
├── controllers/
│   ├── auth/AuthController              # Signup, login
│   ├── user/UserController              # Get current user
│   └── room/RoomController              # CRUD + move submission
├── models/                              # JPA entities (User, Room, GameMove)
├── repositories/                        # Spring Data JPA
├── services/
│   ├── auth/                            # AuthService, JWTService
│   ├── room/                            # RoomService (game logic)
│   └── user/                            # UserService
├── dtos/                                # Request/Response DTOs
├── utils/
│   ├── FenUtils.java                    # FEN parsing/generation
│   ├── MoveUtils.java                   # Chess validation
│   └── CapturedPieceUtil.java           # Capture tracking
├── exceptions/
│   └── GlobalExceptionHandler.java      # Centralized error handling
└── websocket/
    └── RoomWebSocketHandler.java        # WebSocket logic
```

## 🚀 Quick Start

### Prerequisites
```bash
Java 24+, Maven 3+, PostgreSQL
```

### Environment Variables
```bash
export JWT_KEY="your-base64-secret-key"
export JWT_EXPIRATION="PT24H"
export DB_URL="jdbc:postgresql://host:5432/mychess"
export DB_USERNAME="your-db-username"
export DB_PASSWORD="your-db-password"
```

### Installation & Run
```bash
# Clone repository
git clone <repo-url>
cd My-Chess-Backend

# Install dependencies
mvn clean install

# Run application
mvn spring-boot:run

# Access API
# Base URL: http://localhost:8080
```

## 📚 API Endpoints

### Authentication
```http
POST /auth/signup          # User registration
POST /auth/login           # User login (sets JWT cookie)
```

### User
```http
GET  /user/me              # Get current authenticated user
```

### Room Management
```http
POST /room/create          # Create new game room
POST /room/join            # Join existing room (body: { "code": "ABC123" })
GET  /room/{code}          # Get room details
POST /room/move/{code}     # Submit chess move
```

### WebSocket
```
ws://localhost:8080/room/ws/{roomCode}

Message Types:
- MOVE: { type: 'MOVE', move: {...}, fen: '...' }
- GAME_END: { type: 'GAME_END', winner: 'white'|'black', roomDetails: {...} }
```

### Example: Create Room
```bash
curl -X POST http://localhost:8080/room/create \
  -b cookies.txt

Response:
{
  "status": "success",
  "statusCode": 200,
  "data": { "code": "ABC123" },
  "selfLink": "/room/create"
}
```

## 🔒 Security Features

**JWT Configuration**:
- Algorithm: HS256
- Storage: HTTP-only cookies (prevents XSS)
- Expiration: 24 hours (configurable)
- Claims: email (subject), username (custom)

**Password Security**:
- Hashing: BCrypt with automatic salt
- Strength: 10 rounds

**CORS**:
- Allowed origins: `http://localhost:4200`
- Credentials: true (allows cookies)
- Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS

**Stateless Sessions**:
- No server-side session storage
- Horizontal scaling ready

## ♟️ Chess Engine Highlights

**Move Validation** (`MoveUtils.java`):
- **Pawn**: Single/double step, diagonal capture, en passant, promotion
- **Rook**: Horizontal/vertical with path checking
- **Knight**: L-shaped movement (jumps over pieces)
- **Bishop**: Diagonal with path checking
- **Queen**: Combined rook + bishop
- **King**: Single square + castling validation

**Checkmate Detection**:
```java
1. Check if king is in check
2. Try all possible moves for all pieces
3. If no legal move removes check → Checkmate
4. Update GameStatus (WHITE_WON/BLACK_WON)
```

**FEN Notation** (`FenUtils.java`):
```
Format: rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1
        ├─────────────────┬──────────────────┘ │ │    │ │ │
        Piece placement (8 ranks)              │ │    │ │ └─ Fullmove
                                               │ │    │ └─ Halfmove
                                               │ │    └─ En passant
                                               │ └─ Castling rights
                                               └─ Active color
```

**Captured Pieces** (`CapturedPieceUtil.java`):
- Format: `b{pieces}/w{pieces}` (e.g., `bPN/wRQ`)
- Tracked in Room entity

## 🏗️ Build & Deploy

### Production Build
```bash
# Build JAR
mvn clean package -DskipTests

# Output: target/my-chess-backend-0.0.1-SNAPSHOT.jar

# Run
java -jar target/my-chess-backend-0.0.1-SNAPSHOT.jar
```

### Docker Deployment
```dockerfile
FROM eclipse-temurin:24-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# Build and run
docker build -t mychess-backend .
docker run -p 8080:8080 \
  -e JWT_KEY="secret" \
  -e DB_URL="jdbc:postgresql://host:5432/mychess" \
  mychess-backend
```

### Docker Compose (Full Stack)
```yaml
version: '3.8'
services:
  postgres:
    image: postgres:latest
    environment:
      POSTGRES_DB: mychess
      POSTGRES_USER: user
      POSTGRES_PASSWORD: pass
    ports: ["5432:5432"]
  backend:
    build: .
    ports: ["8080:8080"]
    environment:
      JWT_KEY: "secret"
      DB_URL: "jdbc:postgresql://postgres:5432/mychess"
    depends_on: [postgres]
```

## 🧪 Testing

```bash
mvn test                    # Run unit tests
mvn verify                  # Run integration tests
mvn test jacoco:report      # Generate coverage report
```

## 📊 Performance Optimizations

- **HikariCP**: Maximum pool size of 5 connections
- **Async Broadcasting**: ExecutorService for WebSocket messages
- **JPA Optimization**: `spring.jpa.hibernate.ddl-auto=update` (dev), `validate` (prod)
- **Connection Pooling**: Jedis with 50 total, 10 idle, 5 min

## 🐛 Troubleshooting

**JWT Invalid**: Check `JWT_KEY` environment variable and token expiration
**DB Connection Failed**: Verify `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, ensure PostgreSQL is running
**WebSocket Fails**: Confirm JWT cookie sent, backend running, room code valid
**CORS Errors**: Update `SecurityConfig` allowed origins for your frontend URL

## 📚 Key Configuration

**application.properties**:
```properties
server.port=8080
spring.jpa.hibernate.ddl-auto=update          # Auto-create tables
spring.datasource.hikari.maximum-pool-size=5  # Connection pool
spring.jpa.show-sql=true                       # Log SQL
```

**Production Checklist**:
- [ ] Change JWT secret to strong random value
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Use Flyway/Liquibase for migrations
- [ ] Enable HTTPS and set `Secure` flag on cookies
- [ ] Update CORS allowed origins
- [ ] Configure proper logging

## 📄 License

[Add your license]

---

**Built with Spring Boot 3.4.8 • Java 24 • PostgreSQL • WebSocket**
