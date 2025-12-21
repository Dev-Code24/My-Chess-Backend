# MyChess - Backend

A production-ready Spring Boot REST API for real-time multiplayer chess, featuring JWT authentication, WebSocket-based game synchronization, and comprehensive chess logic with FEN notation.

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.8-brightgreen?logo=spring)
![Java](https://img.shields.io/badge/Java-24-orange?logo=java)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Latest-blue?logo=postgresql)
![WebSocket](https://img.shields.io/badge/WebSocket-6.2.12-yellow)

<img width="1408" height="3040" alt="Preview of MyChess on Macbook and iPhone" src="https://github.com/user-attachments/assets/d5dc07dc-2d10-475c-a9bc-b0bbdec4380b" />

## ✨ Key Features

- **JWT Authentication**: Secure token-based auth with HTTP-only cookies and BCrypt password hashing
- **Real-time Multiplayer**: WebSocket-based bidirectional communication with STOMP protocol for live game synchronization
- **Complete Chess Engine**: Full move validation, checkmate detection, FEN notation, and special moves (castling, en passant, promotion)
- **RESTful API**: 9 endpoints for auth, room management, and gameplay
- **Database Persistence**: PostgreSQL with JPA/Hibernate for users, rooms, and game moves
- **Layered Architecture**: Clean separation of concerns (Controllers → Services → Repositories)

## 🛠️ Tech Stack

| Technology | Version  | Purpose |
|------------|----------|---------|
| **Spring Boot** | 3.4.8    | Application framework |
| **Java** | 21       | Programming language |
| **Spring Security** | 6.x      | Authentication & authorization |
| **Spring Data JPA** | 3.5.7    | ORM and data access |
| **Spring WebSocket** | 6.2.12   | Real-time communication with STOMP |
| **PostgreSQL** | Latest   | Relational database |
| **JJWT** | 0.11.5   | JWT token management |
| **Lombok** | Latest   | Boilerplate reduction with @Builder |
| **HikariCP** | Embedded | Connection pooling |
| **Maven** | 3.x+     | Build management |

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

### Design Patterns Implemented

- **[Strategy Pattern](https://refactoring.guru/design-patterns/strategy)**: Chess move handling in `MoveUtils.handleMove()` with different strategies (`handlePieceCapture()`, `handleCastling()`, `handlePawnPromotion()`) for each move type

- **Helper/Template Method Pattern**: `RoomServiceHelper` class provides protected static methods for shared game logic, extended by `RoomService` for code reuse

- **[Builder Pattern](https://refactoring.guru/design-patterns/builder)** with **Fluent API**: Lombok `@Builder` + `@Accessors(chain = true)` for entity/DTO construction with readable chained setters

- **State Machine Pattern**: Explicit game state management using `GameStatus` enum (`WAITING`, `IN_PROGRESS`, `WHITE_WON`, `BLACK_WON`, `PAUSED`) and `RoomStatus` enum (`AVAILABLE`, `OCCUPIED`)

- **Immutable Utility Pattern**: Pure functional static methods in `FenUtils`, `MoveUtils`, `CapturedPieceUtil` with no side effects for testability

- **Enhanced Enum Pattern**: `ChessPiece` enum with domain methods `fromFenChar()` and `toFenChar()` for FEN notation conversion

- **Custom Exception Hierarchy**: Domain-specific exceptions (`RoomNotFoundException`, `RoomJoinNotAllowedException`, `MoveNotAllowed`) with semantic error messages

- **Error Message Enumeration**: `ErrorMessage` enum centralizing all error messages (`ALREADY_IN_ROOM`, `WHITES_TURN`, etc.) preventing string duplication

- **[Observer Pattern](https://refactoring.guru/design-patterns/observer)**: `WebSocketEvents` class with `@EventListener` for handling WebSocket lifecycle (connect/disconnect) and automatic game state updates

- **Centralized Constants**: `RoomConstants` class for magic values (`DEFAULT_CHESSBOARD_FEN`, `ROOM_CODE_LENGTH`, etc.)

- **Broadcast Pattern**: `SimpMessagingTemplate` encapsulated in service methods for room-specific message distribution

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
Broadcasts to WebSocket subscribers via SimpMessagingTemplate
   ↓
Player B receives move update → UI updates board
```

**3. WebSocket Architecture**
```
WebSocketConfig (Spring WebSocket with STOMP)
├── Endpoint: /room/ws/{roomCode}
├── Authentication: JWT cookie validation
├── Handler: RoomWebSocketHandler
│   ├── onOpen: Add to subscribers
│   ├── onClose: Remove from subscribers
│   └── onError: Cleanup and logging
└── Broadcasting: ExecutorService for async
```

**4. Chess Moves Validation Design**
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
captured_pieces (custom format), room_status, game_status,
white_player (FK), black_player (FK), last_activity
```

**GameMoves**:
```sql
id (UUID), room_id (FK), moved_by (FK),
move_number, move_notation (algebraic)
```

## 🚀 Quick Start

### Prerequisites
```bash
Java 21+, Maven 3+, PostgreSQL
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
git clone https://github.com/Dev-Code24/My-Chess-Backend
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
- Strategy pattern with separate handlers for each move type
- **Pawn**: Single/double step, diagonal capture, en passant, promotion
- **Rook**: Horizontal/vertical with path checking
- **Knight**: L-shaped movement (jumps over pieces)
- **Bishop**: Diagonal with path checking
- **Queen**: Combined rook + bishop
- **King**: Single square + castling validation with path and safety checks

**Checkmate Detection** (`RoomServiceHelper.isCheckMate()`):
```java
1. Check if king is captured (targetPiece is king)
2. Update GameStatus (WHITE_WON/BLACK_WON)
3. Return boolean for game ending
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

Methods:
- parseFenToPieces(): FEN string → List<Piece>
- piecesToFen(): List<Piece> → FEN string
- getTurn(): Extract current turn from FEN
```

**Captured Pieces** (`CapturedPieceUtil.java`):
- Custom format: `b1n1p2/R2Q1` (black pieces / white pieces)
- Format: `{piece_letter}{count}` (e.g., `p2` = 2 pawns)
- Methods: `recordCapture()`, `parseSection()`, `buildSection()`

## 📊 Performance Optimizations

- **HikariCP**: Maximum pool size of 5 connections with connection test query
- **Async Broadcasting**: ExecutorService for WebSocket messages to prevent blocking
- **JPA Optimization**: `spring.jpa.hibernate.ddl-auto=update` (dev), `validate` (prod)
- **Builder Pattern**: Lombok builders reduce object creation overhead

## 🐛 Troubleshooting

**JWT Invalid**: Check `JWT_KEY` environment variable is set and token hasn't expired
**DB Connection Failed**: Verify `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, ensure PostgreSQL is running
**WebSocket Fails**: Confirm JWT cookie is being sent, backend is running, room code is valid
**CORS Errors**: Update `SecurityConfig` allowed origins for your frontend URL
**Move Validation Fails**: Check `MoveUtils` logs, verify FEN string format

## 📚 Key Configuration

**application.properties**:
```properties
server.port=8080
spring.jpa.hibernate.ddl-auto=update          # Auto-create tables (dev)
spring.datasource.hikari.maximum-pool-size=5  # Connection pool
spring.jpa.show-sql=true                       # Log SQL (dev)
```

**Production Checklist**:
- [ ] Change JWT secret to strong random value (256-bit minimum)
- [ ] Set `spring.jpa.hibernate.ddl-auto=validate`
- [ ] Use Flyway/Liquibase for database migrations
- [ ] Enable HTTPS and set `Secure` flag on cookies
- [ ] Update CORS allowed origins to production domains
- [ ] Configure proper logging (SLF4J/Logback)
- [ ] Set up monitoring (Spring Boot Actuator)

---

**Built with Spring Boot 3.4.8 • Java 21 • PostgreSQL • WebSocket**
