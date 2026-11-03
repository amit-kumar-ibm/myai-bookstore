# Book Worm: E-Bookstore Backend

Spring Boot 4 / Java 26 REST API for the capstone e-bookstore use case: login, landing page with
recommendations, catalogue browsing (category, brand, product, related reads), basket, delivery
address, payment with gift points, purchase confirmation, order history with "Buy In Again", and
cancellation within 48 hours.

The OpenAPI spec is in [`docs/openapi.yaml`](docs/openapi.yaml).

## Run locally

**Quick start (in-memory H2, no database needed):**

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

**PostgreSQL:**

```powershell
# one-time: create the database and user in psql (CREATE USER bookstore ...; CREATE DATABASE bookstore OWNER bookstore;)
# then set the credentials for this terminal (they are prompted, never written to a file):
$env:SPRING_DATASOURCE_PASSWORD = Read-Host "Database credential"
$env:APP_JWT_SECRET = Read-Host "JWT signing key (32+ characters)"
.\mvnw.cmd spring-boot:run
```

| Variable                     | Default                                      | Purpose                                             |
|------------------------------|----------------------------------------------|-----------------------------------------------------|
| `DB_URL`                     | `jdbc:postgresql://localhost:5432/bookstore` | JDBC URL                                            |
| `DB_USER`                    | `bookstore`                                  | DB user                                             |
| `SPRING_DATASOURCE_PASSWORD` | (empty)                                      | DB credential. Never commit it                      |
| `APP_JWT_SECRET`             | random per start                             | HS256 signing key (>= 32 bytes). Set it in real use |
| `SERVER_PORT`                | `8080`                                       | Port. The service binds to `127.0.0.1` only        |

On first start the nine books and 23 categories from the wireframes are seeded. No users are
seeded; call `POST /api/auth/register`.

## Test

```powershell
.\mvnw.cmd test
```

`BookstoreJourneyTests` drives the whole journey (browse, basket, address, pay, gift points,
history, cancel) and checks per-user data isolation, input validation and auth.

## Design notes

- Stateless JWT bearer auth (BCrypt passwords). Guests may browse; basket, orders and addresses require login.
- Resources owned by another user return 404, not 403, so ids cannot be probed.
- Checkout row-locks the books to prevent overselling. Prices are snapshotted on the order.
- Payment is **simulated**. No card data is accepted or stored. A real gateway must tokenise cards client-side.
- Gift points: 1 point = INR 1 at redemption; 1 point earned per INR 100 paid; reversed on cancel.
- Tax is a flat 12% (`app.tax-rate`).

## Git check-in (Step 9)

```powershell
git init
git checkout -b feature/api-implementation
git add .
git commit -m "Implement Spring Boot API for e-commerce"
git remote add origin https://github.com/<your-user>/<your-repo>.git
git push origin feature/api-implementation
# then open a Pull Request and add your manager as a collaborator
```
