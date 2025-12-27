# SplitWise

SplitWise is a web application designed to help users manage and split expenses among friends and groups. It provides an easy-to-use interface for tracking shared costs, settling debts, and maintaining transparency in financial transactions.

## Features

- User Registration and Authentication (Jwt)
- Expense Tracking
- Group Management
- Debt Settlement
- Notifications and Reminders
- Reporting and Analytics

## Technologies Used

- Backend: Spring Boot, Java
- Database: PostgreSQL

## Getting Started

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/Sanjeev-Kumar78/splitwise-v2)

JDK 21 or higher is required.

1. Clone the repository:
   ```bash
   git clone
   ```
2. Navigate to the project directory:
   ```bash
   cd splitwise-v2
   ```
3. Set up environment variables: in `.env` file
   - `DB_NAME:` Name of the PostgreSQL database (e.g., Splitwisev2)
   - `DB_USERNAME:` Your PostgreSQL username (e.g., postgres)
   - `DB_PASSWORD:` Your PostgreSQL password (e.g., postgres)
   - `DB_URL:` JDBC URL for your PostgreSQL database (e.g., jdbc:postgresql://db:5432/Splitwisev2)
   - `ADMIN_USERNAME:` (Optional) Admin username (default: admin)
   - `ADMIN_PASSWORD:` (Optional) Admin password (default: admin)
   - `FRONTEND_URL:` URL of the frontend application (e.g., http://localhost:3000)

## Using Docker Compose

1. Run the application using Docker Compose:
   - Make sure you have Docker and Docker Compose installed on your machine.
   ```bash
   docker compose up --build
   ```
2. Access the application:
   Open your web browser and navigate to `http://localhost:8080/api/users/ping`.

## Using JDK and Postgres

1. Run Postgres or PgAdmin and create a database with the name specified in the `DB_NAME` environment variable.

2. Run the application using Maven:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
3. Access the application:

   Open your web browser and navigate to `http://localhost:8080/api/users/ping`.

## API Documentation

API documentation is available via Swagger UI at:
`http://localhost:8080/swagger-ui/index.html`

### Routes

#### Authentication (Public)

- `POST /api/auth/signup` - User registration
- `POST /api/auth/login` - User login

#### Users (Protected)

- `POST /api/users` - Create user
- `GET /api/users/ping` - Health check (Public)
- `GET /api/users` - List all users
- `GET /api/users/{id}` - Get user by ID
- `GET /api/users/me` - Get authenticated user profile
- `GET /api/users/search?username=<username>` - Search user by username
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user
- `POST /api/users/set-username` - Set username for current user
- `POST /api/users/set-password` - Change password for current user

#### Events (Protected)

- `POST /api/events` - Create event
- `GET /api/events` - List all events
- `GET /api/events/{id}` - Get event by ID
- `PUT /api/events/{id}` - Update event
- `DELETE /api/events/{id}` - Delete event
- `POST /api/events/{id}/cancel` - Cancel event (soft delete)
- `GET /api/events/{eventId}/debitors` - Get event participants/splits

#### Debitors (Protected)

- `POST /api/events/{eventId}/debitors` - Add debitor to event
- `POST /api/debitors/{eventId}` - Add debitor (alternate path)
- `DELETE /api/debitors/{debitorId}` - Remove debitor from event

#### Payments (Protected)

- `POST /api/payments/pay` - Make payment towards a split
