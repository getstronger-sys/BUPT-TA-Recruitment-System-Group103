# User Manual - BUPT TA Recruitment System

## Setup

### Prerequisites

- **JDK 11+** with `JAVA_HOME` set correctly
- Maven Wrapper included (`mvnw.cmd` on Windows)

### Method 1: One-click run with AI env (Windows, recommended)

1. Copy `ai.env.example` to `ai.env` and optionally set `TA_AI_API_KEY`.
2. Run `run-with-ai.ps1` or `run.cmd`.
3. Open **http://localhost:8080/ta-recruitment/**

### Method 2: Build & Deploy to Tomcat

1. **Build**: `mvnw.cmd clean package`
2. Deploy `target/ta-recruitment.war` to Tomcat `webapps/`
3. Start Tomcat and open **http://localhost:8080/ta-recruitment/**

### Method 3: Embedded Tomcat (Cargo)

1. Ensure `JAVA_HOME` points to JDK 11+.
2. Run: `mvnw.cmd package cargo:run`
3. Open **http://localhost:8080/ta-recruitment/**

### Demo accounts

| Role  | Username | Password |
| ----- | -------- | -------- |
| TA    | ta1      | ta123    |
| TA    | ta5      | ta123    |
| TA    | ta6      | ta123    |
| MO    | mo1      | mo123    |
| Admin | admin    | admin123 |

`ta5` / `ta6` are pre-seeded for workload-cap demos (see README).

---

## Login Page

- **Screenshot**: [Login screen - index.jsp]
- Enter username and password, or **Register** for a new TA/MO account.
- Optional **Remember me** keeps you signed in on this device.

## TA - My Profile

- **Screenshot**: [TA Profile screen - ta/profile.jsp]
- Complete profile fields and upload CV (PDF, DOC, DOCX, TXT, max 5MB).
- Optional LLM-assisted profile hints when AI is configured.

## TA - Find Jobs & Apply

- **Screenshot**: [Job search - ta/jobs.jsp]
- Search by keyword, module code, or skill.
- Apply to a posting directly; all planned TA recruits share the same role.
- **Apply confirm** page shows match score and optional streaming match insight.

## TA - My Applications

- **Screenshot**: [Applications list - ta/applications.jsp]
- Statuses include PENDING, INTERVIEW, WAITLIST, SELECTED, REJECTED, WITHDRAWN, AUTO_CLOSED.
- View timeline events; withdraw while pending when allowed.

## MO - Post Job

- **Screenshot**: [Post job form - mo/post-job.jsp]
- Choose module from **Admin-assigned** list only.
- Structured fields: responsibilities, work arrangements, exam timeline, interview schedule, payment, skills, planned recruits.

## MO - My Jobs (applicant pipeline)

- **Screenshot**: [MO jobs - mo/jobs.jsp]
- Open a posting to manage applicants by stage: **Pending → Interview → Waitlist → Outcome**.
- **Interview**: send notice; record **interview evaluation** before select.
- **Select** requires decision reason; blocked if applicant is at **admin workload cap** or job slots are full.
- **Generate AI summary** button fetches a 5–8 line summary on demand (not at page load).

## MO - Interview calendar

- **Screenshot**: [Interview calendar - mo/interview-calendar.jsp]
- Calendar view of scheduled interviews across postings.

## Admin - Summary & workload

- **Screenshot**: [Admin dashboard - admin/dashboard.jsp]
- Set **max selected jobs per TA** and/or **max estimated hours per TA**.
- **Workload** (`admin/workload-v2.jsp`): sortable table, overload flags, CSV export.
- Legacy `admin/workload.jsp` is unused; the servlet serves **workload-v2**.

## Admin - Monitoring

- **Screenshot**: [Monitoring - admin/monitoring.jsp]
- Limit alerts (e.g. at cap with pending applications), stale jobs, and other checks.

## Admin - MO module assignment

- **Screenshot**: [MO detail - admin/mo-detail.jsp]
- Open an MO user; edit **Assigned modules** (`EBU6304 | Software Engineering` per line).
- MO cannot post jobs for unassigned module codes.

## Admin - AI API & email

- **AI API** (`admin/ai-api-settings.jsp`): DeepSeek URL, model, API key for team demos.
- **Email** (`admin/email-settings.jsp`): SMTP for notifications (optional).

---

## Deployment Flowchart

![Deployment Flowchart](images/Deployment%20flowchart.png)

The diagram illustrates build-and-deploy vs Maven Wrapper + Cargo embedded Tomcat.
