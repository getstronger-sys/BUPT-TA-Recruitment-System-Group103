# BUPT International School - Teaching Assistant Recruitment System

EBU6304 Software Engineering Group Project - A lightweight Java Servlet/JSP web application for TA recruitment.

**Canonical repository:** [BUPT-TA-Recruitment-System-Group103](https://github.com/getstronger-sys/BUPT-TA-Recruitment-System-Group103)

## Team Members

| Student ID | Name          |
| ---------- | ------------- |
| 231223771  | Weiyi Li      |
| 231223520  | Qingwei Zhang |
| 231223324  | Jialin Ma     |
| 231223531  | Tongxin Liu   |
| 231223553  | Yue Hu        |
| 231223298  | Erwei Hou     |

<!-- Other members: add a row in your own pull request. -->

## Requirements Met

- **Technology**: Java Servlet + JSP (no Spring Boot, no database)
- **Data Storage**: JSON files under `data/` (users, profiles, jobs, applications, settings, MO module assignments, etc.)
- **AI Features**:
  - **Rule-based matching**: Job–applicant match score (0–100%), matched/missing skills
  - **LLM (optional)**: DeepSeek via `ai.env` or Admin **AI API** settings — CV profile extraction, match insights (streaming), MO on-demand applicant summary (5–8 lines)
- **Governance**:
  - **Admin assigns modules to MO** — MO may only post jobs for assigned module codes
  - **Workload caps** — per-TA selected-job count and/or estimated hours; Admin monitoring and auto-close of pending applications
- **Core Features**:
  - **TA**: Profile, CV upload, job search, apply with role preference, applications timeline, interview booking, match insights
  - **MO**: Post job (assigned modules only), applicant pipeline (pending → interview → waitlist → selected/rejected), interview evaluation, calendar, on-demand AI summary
  - **Admin**: Dashboard, workload v2, monitoring, users, MO detail (module assignment), email & AI API settings

## Default Accounts

| Role  | Username | Password |
| ----- | -------- | -------- |
| TA    | ta1      | ta123    |
| TA    | ta2      | ta123    |
| TA    | ta5      | ta123    |
| TA    | ta6      | ta123    |
| MO    | mo1      | mo123    |
| Admin | admin    | admin123 |

### Workload conflict demo accounts

Seeded automatically on first run (see `DataStorage` demo data). Default admin cap: **2 selected posts per TA** (`settings.json`).

- `ta5 / ta123`: **2 SELECTED** + **1 PENDING** — at cap with a pending conflict for Admin monitoring
- `ta6 / ta123`: **3 SELECTED** — over cap; highlighted on Admin workload / monitoring

Use **Admin → Monitoring** or **Workload** to demo policy conflicts. MO **Select** is blocked when the applicant is already at the admin cap.

## Build & Run

### Prerequisites

- **JDK 11+** (`JAVA_HOME` must point to JDK 11 or newer; the project targets Java 11)
- Maven 3.6+ **or** use the included Maven Wrapper (`mvnw.cmd` / `./mvnw`)
- Apache Tomcat 9+ (or Cargo embedded Tomcat via Maven)

### Quick start (Windows, recommended)

```powershell
cd ta-recruitment-system
copy ai.env.example ai.env
# Edit ai.env and set TA_AI_API_KEY if you want LLM features
.\run-with-ai.ps1
```

Or double-click / run `run.cmd` (loads `ai.env` then builds and starts Cargo).

Open: **http://localhost:8080/ta-recruitment/**

### Build

```bash
cd ta-recruitment-system
mvn clean package
# Or: ./mvnw.cmd clean package   (Windows)
```

This produces `target/ta-recruitment.war`.

### Deploy to Tomcat

1. Copy `target/ta-recruitment.war` to Tomcat's `webapps/` directory.
2. Start Tomcat.
3. Open: `http://localhost:8080/ta-recruitment/`

### Run with Cargo (Embedded Tomcat)

`cargo:run` requires a packaged WAR first:

```bash
mvn package cargo:run
# Or with Maven wrapper:
# Windows: mvnw.cmd package cargo:run
# Unix: ./mvnw package cargo:run
```

### Run tests

```bash
mvnw.cmd test
```

Requires JDK 11+ configured as `JAVA_HOME`.

### AI API configuration (team-friendly)

1. Keep `ai.env.example` in Git as a template.
2. Copy to `ai.env` locally (never commit `ai.env`).
3. Set `TA_AI_API_KEY` and related variables, **or** configure keys in **Admin → AI API** after first login.

PowerShell (manual, without `run-with-ai.ps1`):

```powershell
$env:TA_AI_ENABLED="true"
$env:TA_AI_PROVIDER="deepseek"
$env:TA_AI_BASE_URL="https://api.deepseek.com"
$env:TA_AI_MODEL="deepseek-chat"
$env:TA_AI_API_KEY="YOUR_REAL_KEY"
mvnw.cmd package cargo:run
```

Security note: never commit real API keys. Commit template files only.

## Project Structure

```
ta-recruitment-system/
├── pom.xml
├── README.md
├── USER_MANUAL.md
├── ai.env.example
├── run.cmd
├── run-with-ai.ps1
├── src/main/
│   ├── java/bupt/ta/
│   │   ├── model/         # User, TAProfile, Job, Application, AssignedModule, ...
│   │   ├── storage/       # DataStorage (JSON file I/O)
│   │   ├── servlet/       # Login, Register, TA, MO, Admin servlets
│   │   ├── service/       # Admin, email, timeline, notifications
│   │   ├── llm/           # DeepSeek client, summary & insight services
│   │   └── filter/        # Auth, Remember Me
│   └── webapp/
│       ├── WEB-INF/web.xml
│       ├── css/style.css
│       ├── ta/            # TA portal pages
│       ├── mo/            # MO portal pages
│       └── admin/         # Admin portal (workload-v2, monitoring, users, ...)
└── data/                  # Created at runtime (gitignored except mail.properties.example)
```

## Data Files

For local development, data is stored in the project root `data/` directory so accounts, jobs, applications, and uploads survive restarts.
Override with `-Dta.data.dir=<path>` or environment variable `TA_DATA_DIR`.

- `users.json` - User accounts
- `profiles.json` - TA profiles (skills, CV path, etc.)
- `jobs.json` - Job postings
- `applications.json` - Job applications
- `mo-module-assignments.json` - Admin-assigned modules per MO
- `settings.json` - Admin workload caps and policies
- `uploads/` - Uploaded CV files

## Optional Email Notifications

Configure via **Admin → Email** or environment variables:

- `TA_MAIL_HOST` - SMTP host
- `TA_MAIL_PORT` - SMTP port (default `587`)
- `TA_MAIL_USERNAME` - SMTP username
- `TA_MAIL_PASSWORD` - SMTP password
- `TA_MAIL_FROM` - sender address
- `TA_MAIL_ENABLED` - optional `true` / `false` (default `true`)
- `TA_MAIL_AUTH` - `true` / `false` (defaults to `true` when a username is set)
- `TA_MAIL_STARTTLS` - `true` / `false` (default `true`)
- `TA_MAIL_SSL` - `true` / `false` (default `false`)
- `TA_MAIL_APP_BASE_URL` - optional base URL for email links, e.g. `http://localhost:8080/ta-recruitment`

## User Manual (summary)

See **[USER_MANUAL.md](USER_MANUAL.md)** for step-by-step flows and screenshot placeholders.

### TA Workflow

1. Login (e.g. `ta1` / `ta123`)
2. **My Profile** — skills, CV upload
3. **Find Jobs** — search and apply (role preference)
4. **My Applications** — status, timeline, withdraw pending

### MO Workflow

1. Login (`mo1` / `mo123`)
2. **Post Job** — only for **Admin-assigned** module codes
3. **My Jobs** — pipeline: pending → interview → waitlist → select/reject (evaluation + decision reason required for select)
4. **Generate AI summary** — on demand per applicant (no load-time LLM calls)

### Admin Workflow

1. Login (`admin` / `admin123`)
2. **Summary** — workload caps (jobs and/or hours)
3. **Workload** — TA workload table with export
4. **Monitoring** — limit alerts and operational issues
5. **Users / MO detail** — assign modules to MO (`CODE | Name` per line)
6. **AI API** — team DeepSeek settings (optional)

## License

Educational project - BUPT International School.
