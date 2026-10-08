# Entity Diagram (Phase 6: Microservices)

Four databases, one per service. Ten tables in total.

| Service | Database | Tables |
|---|---|---|
| user-service | `userdb` | `users`, `refresh_tokens` |
| cinema-service | `cinemadb` | `movies`, `shows`, `seats` |
| booking-service | `bookingdb` | `bookings`, `booking_seats` |
| payment-service | `paymentdb` | `payments` |

notification-service has no table yet.

Rule: a foreign key exists only between tables in the same database.
When a table points to another service's table, it stores a plain id, and the service must ask the owner through an API.

## ER diagram (Mermaid)

Solid line = real foreign key inside one database.
Dashed line = plain id across services, no foreign key.

```mermaid
erDiagram
    USER ||--o{ REFRESH_TOKEN : "owns"

    MOVIE ||--o{ SHOW : "has"
    SHOW ||--o{ SEAT : "has"

    BOOKING ||--o{ BOOKING_SEAT : "contains"

    USER ||..o{ BOOKING : "user_id (plain id)"
    SHOW ||..o{ BOOKING : "show_id (plain id)"
    SEAT ||..o{ BOOKING_SEAT : "seat_id (plain id)"
    BOOKING ||..o{ PAYMENT : "booking_id (plain id)"

    USER {
        bigint id PK
        varchar name
        varchar email "unique"
        varchar password_hash
        varchar role "USER, ORGANIZER or ADMIN"
    }

    REFRESH_TOKEN {
        bigint id PK
        bigint user_id FK
        varchar token_hash "unique, SHA-256"
        timestamp expires_at
        boolean revoked
    }

    MOVIE {
        bigint id PK
        varchar title
        varchar language
        int duration_minutes
    }

    SHOW {
        bigint id PK
        bigint movie_id FK
        timestamp start_time
        numeric price
    }

    SEAT {
        bigint id PK
        bigint show_id FK
        varchar seat_number
        varchar status "AVAILABLE or BOOKED"
        bigint version "optimistic lock"
    }

    BOOKING {
        bigint id PK
        bigint show_id "plain id, no FK"
        bigint user_id "plain id, no FK"
        numeric total_amount
        varchar status "CONFIRMED or CANCELLED"
        timestamp created_at
    }

    BOOKING_SEAT {
        bigint id PK
        bigint booking_id FK
        bigint seat_id "plain id, no FK"
    }

    PAYMENT {
        bigint id PK
        bigint booking_id "plain id, no FK"
        numeric amount
        varchar status "PENDING, SUCCESS or FAILED"
    }
```

## Same diagram as plain text

```
user-service (userdb)
  USER (1) ──────< REFRESH_TOKEN (many)

cinema-service (cinemadb)
  MOVIE (1) ──────< SHOW (many) ──────< SEAT (many)

booking-service (bookingdb)
  BOOKING (1) ──────< BOOKING_SEAT (many)

payment-service (paymentdb)
  PAYMENT (many)

Across services (plain ids only, no foreign keys):
  BOOKING.user_id         ····> USER.id          (user-service)
  BOOKING.show_id         ····> SHOW.id          (cinema-service)
  BOOKING_SEAT.seat_id    ····> SEAT.id          (cinema-service)
  PAYMENT.booking_id      ····> BOOKING.id       (booking-service)
```

Read `A (1) ──< B (many)` as: one A has many B.
Read `A ····> B` as: A stores B's id, but the database does not enforce it.

## All relations

| From | To | Type | Link | Enforced by |
|---|---|---|---|---|
| User | RefreshToken | one to many | `refresh_tokens.user_id` | foreign key |
| Movie | Show | one to many | `shows.movie_id` | foreign key |
| Show | Seat | one to many | `seats.show_id` | foreign key |
| Booking | BookingSeat | one to many | `booking_seats.booking_id` | foreign key |
| User | Booking | one to many | `bookings.user_id` | plain id (was a foreign key) |
| Show | Booking | one to many | `bookings.show_id` | plain id (was a foreign key) |
| Seat | BookingSeat | one to many | `booking_seats.seat_id` | plain id (was a foreign key) |
| Booking | Payment | one to many | `payments.booking_id` | plain id (new) |
| Booking | Seat | many to many (through BookingSeat) | none | plain id |

## What changed from Phase 1 to 5

- Three foreign keys were removed: `bookings.show_id`, `bookings.user_id` and `booking_seats.seat_id`. Their columns stay, but the database no longer checks them.
- In Java, `Booking.show` becomes `Long showId`, and `BookingSeat.seat` becomes `Long seatId`.
- One new table, `payments`, in its own database.
- `seats` gains `version` (Phase 4, optimistic locking), and `users` and `refresh_tokens` (Phase 5) are now part of the diagram.

## What we lose without foreign keys

- The database no longer stops a booking from pointing to a show that does not exist.
- So Booking must call Cinema to check the show before it saves anything.
- Deleting a show or user would not warn the booking tables, so we never delete them (the plan already leaves delete for later).
- You cannot join `bookings` with `shows` in one SQL query. To show "booking with movie title", Booking asks Cinema through its API.

## Each entity in simple statements

**User** (user-service)
- One user has many refresh tokens, because each login creates a new one.
- A user does not know about bookings. Booking stores the user's id only.

**RefreshToken** (user-service)
- Belongs to exactly one user.
- Stores a hash of the token, never the token itself.

**Movie** (cinema-service)
- One movie has many shows.
- A movie has no seats and no bookings of its own. It reaches seats through its shows.

**Show** (cinema-service)
- One show belongs to exactly one movie.
- One show has many seats (every show gets its own full set, like A1, A2, ...).
- A show does not know about bookings. Booking stores the show's id only.

**Seat** (cinema-service)
- One seat belongs to exactly one show.
- Cinema owns the seat status. Booking asks Cinema to reserve or release a seat.
- At any moment, a seat is in at most one active booking.

**Booking** (booking-service)
- One booking is for one show and one user, stored as plain ids.
- One booking contains many seats, through `booking_seats`.
- To know the movie title or price, Booking asks Cinema.

**BookingSeat** (booking-service)
- The link between a booking and a seat.
- One row means: this seat id is part of this booking.
- It has a real foreign key to its booking, and a plain id for the seat.

**Payment** (payment-service)
- One payment is for one booking, stored as a plain id.
- A booking can have many payment rows over time, for example a failed try and a later success.

## One example in words

"Inception" (Movie, in cinema-service) has a 7 PM show with seats A1, A2, A3. Rahul (User, in user-service) books A1 and A2.
Booking asks Cinema for the show's price and to reserve seats A1 and A2. Then Booking saves one Booking row in its own database, holding `show_id` and `user_id` as plain numbers, and two BookingSeat rows holding the seat ids.
Later, Payment saves one Payment row holding the booking's id as a plain number.
No database holds a foreign key to another service's tables. The services are linked only through API calls.