# ProfitAs Backend

Spring Boot 4 / Java 17 / PostgreSQL / JWT backend for a fractional real-estate platform with two products:

- **Instant Exit** – sell an asset stake immediately at a 3% haircut to its current valuation.
- **Overdraft Loan** – borrow against an asset at up to 70% LTV, 11% p.a. (daily compounding).

## Run locally

```bash
# 1. PostgreSQL
createdb profitas_db            # or any DB; set DB_URL / DB_USERNAME / DB_PASSWORD

# 2. Configuration (see profitas-backend/.env.example)
export JWT_SECRET="$(openssl rand -base64 48)"   # REQUIRED, >= 32 chars
export ADMIN_EMAIL=admin@example.com             # optional bootstrap admin
export ADMIN_PASSWORD='choose-a-strong-one'

# 3. Start
cd profitas-backend && ./mvnw spring-boot:run
```

Shortcut for development only: `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` (uses a throwaway JWT secret and `admin@profitas.local` / `ChangeMe123!`).

Tables are created automatically (`DDL_AUTO=update`). For production use `validate` plus Flyway/Liquibase.

Tests run on in-memory H2, no database needed: `./mvnw test`.

## API

All routes except `/auth/**` need `Authorization: Bearer <token>`. Errors are JSON `{timestamp,status,error,message,details?}`.

| Method | Path | Who | Purpose |
|---|---|---|---|
| POST | `/api/v1/auth/register` | public | Create a USER account (role cannot be chosen) |
| POST | `/api/v1/auth/login` | public | Get a JWT |
| GET | `/api/v1/assets`, `/assets/{id}` | owner | My assets |
| POST | `/api/v1/loans/apply` | owner | `{assetId, requestedAmount}` |
| GET | `/api/v1/loans`, `/loans/{id}` | owner | My loans (interest accrued up to now) |
| POST | `/api/v1/loans/{id}/repay` | owner | `{amount}`; full repayment sets `REPAID` |
| POST | `/api/v1/exit/request` | owner | `{assetId}`; valuation is read from the asset |
| GET | `/api/v1/exit` | owner | My exit requests |
| GET | `/api/v1/admin/users` | admin | Find user ids |
| POST | `/api/v1/admin/assets` | admin | `{ownerId, assetName, currentValuation, monthlyRentalIncome}` |
| GET | `/api/v1/admin/assets` | admin | All assets |
| PUT | `/api/v1/admin/assets/{id}/valuation` | admin | `{currentValuation}` |
| GET | `/api/v1/admin/loans?status=` | admin | All loans |
| POST | `/api/v1/admin/loans/{id}/default` | admin | Mark an ACTIVE loan `DEFAULTED` |
| GET | `/api/v1/admin/exits?status=` | admin | All exit requests |
| POST | `/api/v1/admin/exits/{id}/approve` `/reject` `/settle` | admin | Manual exit workflow |

## Business rules

- **Assets are created by admins only.** Otherwise a user could invent a valuation and borrow against it.
- **Loan limit** is 70% of current valuation across *all* active loans on the asset (outstanding balance incl. interest counts).
- A loan is refused if the asset is exited or has an exit in progress. An exit is refused while the asset has an active loan, is already exited, or already has a pending exit.
- **Exit payout** = valuation × (1 − 3%), rounded to cents. With `EXIT_AUTO_SETTLE=true` (default) it settles immediately and the asset becomes `EXITED`. With `false`, it stays `PENDING` until an admin approves and settles it.
- **Interest** accrues for whole elapsed days, compounded daily at rate/365, applied whenever a loan is read or repaid.
- Loan/exit operations lock the asset row so concurrent requests can't both pass the limit checks.

Rates and limits are configurable in `application.properties` (`app.loan.*`, `app.exit.*`).
