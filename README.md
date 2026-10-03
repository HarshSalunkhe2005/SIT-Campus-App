# SIT Campus App

A campus issue-reporting system for SIT. Students report problems (with a photo), the right department picks them up on a kanban board, and admins oversee everything.

- **Students** sign up with their `@sitpune.edu.in` email (OTP verified), report issues, browse and upvote the campus feed, and track their own reports.
- **Departments** (Electrical, IT, Cleaning, Plumbing, Hostel, Civil, Furniture, General) get a board of their own issues, move them New → In Progress → Resolved, attach proof photos, and the student is emailed when an issue is resolved.
- **Admins** see all reports and statistics, enable/disable or delete students, and manage departments and their logins.

## Stack

| Part | Tech |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA, PostgreSQL |
| Frontend | Plain HTML, CSS and JavaScript (no build step), served by any static file server |
| Tests | JUnit 5, MockMvc, H2 (in-memory) |

## Run it

### Quick try (no database or email needed)

Requires Java 21 and Python 3.

```
start.bat dev
```

or manually:

```
cd campusbackend
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
# in another terminal
cd src/main/resources
python -m http.server 5500
```

Open http://localhost:5500/templates/auth/login.html. The `dev` profile uses an in-memory database, prints emails (including OTP codes) in the backend console, and creates demo accounts on every start:

| Role | Login | Password |
|---|---|---|
| Admin | `admin@sitpune.edu.in` | `Admin@12345` |
| Department | `it@sitpune.edu.in` (also `electrical@`, `cleaning@`, `plumbing@`, `hostel@`, `civil@`, `furniture@`, `general@`) | `Dept@12345` |
| Student | sign up from the login page; read the OTP in the backend console | your choice |

These demo credentials exist only in `application-dev.properties`. Never use that profile for real data.

### Real deployment

1. Copy `.env.example` to `.env` (git-ignored) and fill it in, or set the same names as environment variables. Required: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `JWT_SECRET` (32+ random characters).
2. Optional first-run setup: set `BOOTSTRAP_ADMIN_EMAIL` and `BOOTSTRAP_ADMIN_PASSWORD` to create the admin, and `BOOTSTRAP_DEPT_PASSWORD` to create the default departments. Existing accounts are never changed. Remove these afterwards.
3. `start.bat`, or `cd campusbackend && ./mvnw spring-boot:run`, and serve `src/main/resources` as static files. The frontend talks to `http://localhost:8080` by default; set `window.API_BASE_URL` before `api.js` loads to point it elsewhere, and set `CORS_ORIGINS` on the backend to the frontend's origin.

No secrets live in the repository. The app refuses to start without `JWT_SECRET` and rejects short secrets.

## Tests

```
cd campusbackend
./mvnw test
```

76 tests cover the sign-up flow (OTP expiry, attempt limits, resend throttling, account takeover attempts), login (rate limiting, disabled accounts, token tampering), role separation, department isolation, complaint routing, photo validation, upvotes, admin operations, response shapes and query counts.

## How it works

```
campusbackend/src/main/java/com/sit/campusbackend/
  auth/        sign-up (OTP), login, JWT filter, security config
  complaint/   complaints, departments, admin operations, photo storage, error handling
  config/      app wiring, first-run bootstrap, dev mail printer
  common/      mail service
src/main/resources/
  templates/   HTML pages (auth, student, dept, admin)
  static/      css and js
```

- **Sign-up:** `POST /auth/register` emails a 6-digit code (valid 10 minutes, 5 attempts, 30 s between resends) → `POST /auth/verify-otp` → `POST /auth/set-password`, which only works right after a successful verification and only for accounts that have no password yet.
- **Login:** `POST /auth/login` returns a JWT (24 h). Failed attempts are rate limited. Disabling or deleting a user takes effect immediately, not when their token expires.
- **Roles:** `/student/**`, `/dept/**` and `/admin/**` are restricted to their role. A department can only see and change its own complaints.
- **Reporting:** `POST /student/report` (multipart: `complaint` JSON + `image`). The category the student picks (or keywords in the description, matched as whole words) decides the department; with no matching department it goes to General.
- **Photos:** JPEG, PNG or WebP up to 5 MB, checked by content, stored under `campusbackend/uploads/` with random names and served at `/uploads/...`.

## Security notes

- Passwords are BCrypt hashed; hashes are never returned by the API.
- All user-provided text is HTML-escaped before it is displayed.
- The login token is kept in the browser's `localStorage`, so any future XSS bug would expose it. Keeping all rendering escaped (see `escapeHtml` in `static/js/shared/api.js`) is therefore important.
- Behind a reverse proxy, forward the real client address so login rate limiting works per client.
