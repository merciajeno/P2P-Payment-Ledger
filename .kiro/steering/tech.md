# Tech Stack

## Backend

| Component | Detail |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.2 |
| Web | Spring MVC (`spring-boot-starter-webmvc`) |
| Persistence | Spring Data JPA (`spring-boot-starter-data-jpa`) |
| Security | Spring Security — stateless JWT (no server-side session) |
| Database | PostgreSQL (default DB name: `ledger`) |
| JWT library | `io.jsonwebtoken` jjwt 0.12.6 (api / impl / jackson) |
| Boilerplate | Lombok |
| Validation | `spring-boot-starter-validation` (Bean Validation) |
| Build tool | Maven Wrapper (`mvnw` / `mvnw.cmd`) |
| Coverage | JaCoCo 0.8.13 |

## Frontend

| Component | Detail |
|---|---|
| Framework | React 18 |
| Build tool | Vite 6 with `@vitejs/plugin-react` |
| HTTP client | Axios 1.21 |
| Routing | Custom — `window.history.pushState` + `popstate` (no router library) |
| Styling | Plain CSS (`src/styles.css`) — no CSS framework |

## Environment Variables

### Backend (required)
- `SPRING_DATASOURCE_URL` — JDBC URL, e.g. `jdbc:postgresql://localhost:5432/ledger`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `SECRET_KEY` — HMAC-SHA signing key, at least 32 bytes; must be stable between restarts

### Frontend (optional)
- `VITE_API_PROXY_TARGET` — overrides the default backend proxy target (`http://localhost:8080`)
- `VITE_API_BASE_URL` — overrides the Axios `baseURL` (defaults to `""`)

## Common Commands

### Backend (run from repository root)

```powershell
# Start the application
.\mvnw.cmd spring-boot:run

# Run tests
.\mvnw.cmd test

# Package (produces JAR)
.\mvnw.cmd package
```

### Frontend (run from `frontend/` directory)

```sh
# Install dependencies (use ci for reproducible installs)
npm ci

# Start dev server (http://localhost:5173, proxies API to :8080)
npm run dev

# Production build (output to frontend/dist/)
npm run build

# Preview production build locally
npm run preview
```

### Database setup

```sql
CREATE DATABASE ledger;
```

Hibernate updates the schema automatically on startup.

## Key Architectural Constraints

- CSRF is disabled; auth is stateless JWT only — do not add session-based auth
- The JWT token is sent via cookie (`withCredentials: true` on the Axios instance)
- All frontend API calls go through the single Axios instance in `frontend/src/api.js`
- The Vite dev server proxies all known API path prefixes to the backend; when adding new endpoints, update `vite.config.js` if the path prefix is new
