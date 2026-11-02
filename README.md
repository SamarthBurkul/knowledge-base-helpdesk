# Knowledge Base Helpdesk

A web-based Knowledge Base Helpdesk that provides a centralized platform
for users to search and view solutions to common problems.

Authorized Support Staff can create and update knowledge articles, while
Administrators can manage users, roles and knowledge articles.

## Problem

Organizations often receive repeated support requests for common technical
and operational problems. Existing solutions can be difficult to find,
causing repeated work for support teams.

The Knowledge Base Helpdesk provides a centralized and searchable repository
of solutions.

## Target Users

- User / Employee
- Support Staff
- Administrator

## Main Features

- Session-based user authentication with BCrypt password hashing
- Enterprise-style responsive UI with light/dark themes (see [User Interface](#user-interface))
- Role-based authorization (USER, SUPPORT, ADMIN)
- Dashboard metrics: total, published, resolved and (for ADMIN/SUPPORT) draft counts
- Admin-only Users & roles page (`/admin/users`) and team panel on the dashboard
- Draft articles are visible only to ADMIN and SUPPORT (enforced on the server)
- Server-side validation (required fields, length limits, valid status) with field messages
- Knowledge article search (by title, description, or solution text)
- Knowledge article listing with status badges (PUBLISHED, DRAFT, RESOLVED)
- Knowledge article detail view with author, timestamp, and formatted solution panel
- Knowledge article creation and editing (ADMIN and SUPPORT roles)
- Knowledge article deletion (ADMIN role only)
- Spring Boot Actuator health endpoint (`/actuator/health`) exposed for monitoring
- Docker container health check using the Actuator health endpoint

## User Interface

The UI is server-rendered with Thymeleaf and a small, dependency-free
progressive-enhancement script. Every page still works with JavaScript disabled.

- **Design system:** `src/main/resources/static/css/style.css` (tokens for colour, type, spacing,
  radii, shadows; light and dark themes; responsive breakpoints at 1180/1024/760 px).
- **Application shell:** `templates/fragments/layout.html` (head, SVG icon sprite, collapsible
  sidebar / mobile drawer, top bar with global search, profile menu, toasts, confirm dialog).
- **Behaviour:** `src/main/resources/static/js/app.js` (theme toggle and collapsed sidebar
  remembered in `localStorage`, debounced live search against `/articles`, inline validation,
  unsaved-changes guard, delete confirmation, `/` keyboard shortcut to search).
- **Error pages:** `templates/error.html` renders 400/403/404/405/500 without exposing exception details.
- Navigation and actions follow the signed-in role; authorization is still enforced in the controllers.

Screenshots of the redesign (desktop 1440 px, laptop 1024 px, tablet 820 px, mobile 390 px,
plus dark theme) are in [`evidence/ui-redesign-2026-10-20/`](evidence/ui-redesign-2026-10-20/).
The final verification set (every role, desktop/tablet/mobile, light/dark, with a horizontal-overflow
check per page) is in [`evidence/final-verification-2026-11-01/ui-screenshots/`](evidence/final-verification-2026-11-01/ui-screenshots/).

## DevOps Features

- Git and GitHub with feature branch workflow
- Pull Requests and code review
- Merge conflict demonstration and resolution (dedicated branches)
- Jenkins CI/CD pipeline (Windows-compatible `bat` and PowerShell steps)
- Maven build automation
- HTTP integration tests for role enforcement and page rendering (11 tests)
- Selenium automated browser testing (6 UI journeys, Chrome in Docker)
- Docker containerization with `HEALTHCHECK` directive
- Docker image versioning
- Docker Hub publishing (opt-in, requires credential configuration)
- Ansible provisioning, idempotence verification, and rollback demonstration
- Spring Boot Actuator health checks

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 (WebMVC, Thymeleaf, Validation, Actuator) |
| Build | Maven 3.10.0 |
| Password hashing | spring-security-crypto (BCrypt) |
| Browser testing | Selenium 4.50.0 |
| CI/CD | Jenkins 2.580.1 |
| Containerization | Docker / Docker Desktop |
| Provisioning | Ansible (Ubuntu WSL) |
| Version control | Git / GitHub |

## Application Routes and Permissions

| Route | Method | Roles | Description |
|---|---|---|---|
| `/login` | GET, POST | All (unauthenticated) | Login page |
| `/logout` | GET | Authenticated | Session invalidation |
| `/dashboard` | GET | Authenticated | Dashboard with metrics |
| `/articles` | GET | Authenticated | Article directory with search. Optional query parameters: `keyword`, `status` (`DRAFT`/`PUBLISHED`/`RESOLVED`), `sort` (`updated`, `title`; default is creation order), `view` (`list`, `table`) |
| `/articles/{id}` | GET | Authenticated | Article detail view |
| `/articles/new` | GET | ADMIN, SUPPORT | Article creation form |
| `/articles/create` | POST | ADMIN, SUPPORT | Save new article |
| `/articles/edit/{id}` | GET | ADMIN, SUPPORT | Article edit form |
| `/articles/update/{id}` | POST | ADMIN, SUPPORT | Save updated article |
| `/articles/delete/{id}` | POST | ADMIN only | Delete article |
| `/admin/users` | GET | ADMIN only | Read-only user and role directory (others are redirected to the dashboard) |
| `/actuator/health` | GET | All | Spring Boot health endpoint |

## Project Structure

```text
knowledge-base-helpdesk/
├── .github/
├── ansible/
│   ├── ansible.cfg
│   ├── deploy.yml
│   ├── docker-desktop.sh
│   ├── inventory.ini
│   └── rollback-demo.yml
├── docs/
│   ├── Backlog.md, User-Stories.md, SRS.md, Scope.md, Definition-of-Done.md, ...
│   ├── Demo-Script.md
│   ├── Deployment.md
│   ├── Git-Collaboration-And-Merge-Conflict.md
│   └── Jenkins-Pipeline.md
├── evidence/
│   ├── final-verification-2026-11-01/   (latest genuine results)
│   ├── ui-redesign-2026-10-20/
│   ├── selenium-2026-10-14/
│   └── DevOps-Verification.md
├── jenkins/
│   └── Knowledge-Base-Helpdesk-CI.config.xml
├── src/
│   ├── main/
│   │   ├── java/com/knowledgebase/helpdesk/
│   │   │   ├── controller/
│   │   │   ├── model/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── static/css/style.css
│   │       ├── templates/
│   │       └── application.properties
│   └── test/
├── Dockerfile
├── Jenkinsfile
├── pom.xml
├── README.md
└── CONTRIBUTING.md
```

> **Note:** `tests/` appears in the original project structure listing but test sources
> live under `src/test/` as standard Maven layout. No separate top-level `tests/`
> directory is present.

---

## Local Setup and Running the Application

### Prerequisites

- **Java 25** (Temurin/Eclipse Temurin recommended)
- **Maven 3.10.0** or compatible
- No database required — the application uses in-memory storage (`ConcurrentHashMap`).
  All data is reset on restart. This is intentional for a demonstration environment.

### Build and Run

**Package the application (skip tests):**

```powershell
mvn -DskipTests package
```

This produces `target/knowledge-base-helpdesk-1.0.0.jar`. The JAR was confirmed present after the last successful `mvn -B -DskipTests package` run.

**Run locally:**

```powershell
java -jar target/knowledge-base-helpdesk-1.0.0.jar
```

The application starts on port **8080** by default (`server.port=8080` in `application.properties`).
To use a different port:

```powershell
java -jar target/knowledge-base-helpdesk-1.0.0.jar --server.port=8082
```

**Access in your browser:**

- Login page: `http://localhost:8080/login`
- Health endpoint: `http://localhost:8080/actuator/health`

### Local Demonstration Accounts

> ⚠️ **These are local demonstration accounts only.** Do not use these credentials
> in any production or shared environment. Do not commit any passwords or tokens
> to the repository.

| Username | Password | Role | Permissions |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | Full access: create, edit, delete, user overview |
| `support` | `support123` | SUPPORT | Authoring: create and edit articles |
| `user` | `user123` | USER | Read-only: search and view articles |

---

## Running Tests

The suite has 25 tests:

| Class | Type | Tests | What it proves |
|---|---|---:|---|
| `ArticleServiceTest` | Unit | 6 | Create, find, search, status filter, sort, delete, author counts |
| `ArticleLinksTest` | Unit | 2 | Filter/view URLs are built and encoded correctly |
| `AccessControlIntegrationTest` | HTTP integration (real Tomcat + Thymeleaf on a random port) | 11 | Every page renders for every role; USER/SUPPORT cannot create/edit/delete/manage users **even with direct POST requests**; drafts are hidden from USER on the server; validation; session ID changes on login; friendly 404 |
| `HelpdeskSeleniumTest` | Selenium browser journeys | 6 | Login, search, SUPPORT creates and edits an article, USER is blocked from creating, ADMIN deletes after the confirmation dialog |

### Unit and integration tests (no browser needed)

```powershell
mvn -B clean verify
```

Expected: `Tests run: 25, Failures: 0, Errors: 0, Skipped: 6` and `BUILD SUCCESS`.
The 6 skipped tests are the Selenium journeys. **Skipped is not passed**: they only
run when `-Dselenium.enabled=true` is given (next section).

### Selenium journeys (Chrome in Docker, no local Chrome driver needed)

Local Chrome start-up is unreliable on this Windows machine, so the journeys run in
the `selenium/standalone-chrome` container and reach the app through
`host.docker.internal`:

```powershell
# 1. Start the app from a copy of the JAR (Windows locks a running JAR)
mvn -B -DskipTests package
New-Item -ItemType Directory -Force ci-app | Out-Null
Copy-Item target\knowledge-base-helpdesk-1.0.0.jar ci-app\app-under-test.jar -Force
Start-Process java -ArgumentList '-jar','ci-app\app-under-test.jar','--server.port=18082'

# 2. Start Chrome (Selenium Grid) in Docker
docker run -d --name selenium-chrome-local -p 24444:4444 --shm-size=2g selenium/standalone-chrome:latest

# 3. Run the journeys (wait until http://localhost:24444/status shows "ready": true)
mvn -B "-Dselenium.enabled=true" "-Dselenium.remoteUrl=http://localhost:24444" "-DbaseUrl=http://host.docker.internal:18082" -Dtest=HelpdeskSeleniumTest test

# 4. Clean up
docker rm -f selenium-chrome-local
Get-NetTCPConnection -LocalPort 18082 -State Listen | ForEach-Object { Stop-Process -Id $_.OwningProcess -Force }
```

Expected: `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`. Screenshots are written to `test-output/`.

Latest verified results (2026-11-01) are in
[`evidence/final-verification-2026-11-01/`](evidence/final-verification-2026-11-01/README.md).

---

## Docker

```powershell
mvn -B clean verify                                   # builds target/knowledge-base-helpdesk-1.0.0.jar
docker build --tag knowledge-base-helpdesk:1.0.2 .
docker run -d --name helpdesk-demo --restart unless-stopped -p 8082:8080 knowledge-base-helpdesk:1.0.2
docker ps --filter name=helpdesk-demo                 # STATUS shows "(healthy)" after ~20 s
curl http://localhost:8082/actuator/health            # {"status":"UP",...}
docker logs --tail 20 helpdesk-demo
docker stop helpdesk-demo; docker start helpdesk-demo # lifecycle demo
docker rm -f helpdesk-demo                            # remove when finished
```

Image details: `eclipse-temurin:25-jre` base, runs as the unprivileged `helpdesk` user,
exposes 8080, `TZ=Asia/Kolkata` (override with `-e TZ=...`). The `HEALTHCHECK` uses bash's
built-in `/dev/tcp` because the JRE image has no `curl`/`wget`; it is healthy only when
`/actuator/health` answers HTTP 200.

Ports used on this machine: 8082 demo container, 18084 Ansible-managed container,
18082/18083/24444 CI test run, 8081 Jenkins, 8080 is taken by another program.

---

## Jenkins CI/CD

The [`Jenkinsfile`](Jenkinsfile) stages: Checkout → Tool check → Compile → Unit and
integration tests → Package JAR → Docker image build (`knowledge-base-helpdesk:ci-<build>`)
→ Temporary test container → Selenium tests (Chrome container) → Temporary-container
deployment → Deployment health gate (HTTP `UP` **and** Docker `healthy`) → optional
Docker Hub publish. A failing test fails its stage, so no image is deployed or published.
JUnit reports, the JAR, Selenium screenshots and logs are published on every run.

**Status:** the job `Knowledge-Base-Helpdesk-CI` on `http://localhost:8081/` builds this
repository from `*/main`. No Jenkins build with Selenium enabled has passed yet.
Steps: [docs/Jenkins-Pipeline.md](docs/Jenkins-Pipeline.md).

---

## Ansible Deployment, Idempotency and Rollback

Ansible runs in WSL Ubuntu and controls Docker Desktop through
`ansible/docker-desktop.sh`. The target is **this machine only** (`localhost`); no remote
server is provisioned.

```bash
# From Ubuntu WSL
cd /mnt/g/knowledge-base-helpdesk
export ANSIBLE_CONFIG=ansible/ansible.cfg      # needed: Ansible ignores a config in a world-writable folder
D="-e docker_cli=/mnt/g/knowledge-base-helpdesk/ansible/docker-desktop.sh"

ansible-playbook -i ansible/inventory.ini ansible/deploy.yml $D         # deploy 1.0.2 to :18084
ansible-playbook -i ansible/inventory.ini ansible/deploy.yml $D         # run again: changed=0 (idempotent)
ansible-playbook -i ansible/inventory.ini ansible/rollback-demo.yml $D  # bad candidate fails -> previous image restored
curl http://localhost:18084/actuator/health
```

Verified on 2026-11-01: deploy `changed=2`, re-run `changed=0`, rollback `rescued=1 failed=0`,
re-run after rollback `changed=0`. Full output: [`evidence/final-verification-2026-11-01/`](evidence/final-verification-2026-11-01/README.md).
Details: [docs/Deployment.md](docs/Deployment.md).

---

## Git Workflow

- `main` ← `develop` ← `feature/*` branches, merged through GitHub pull requests
  (#1 `feature/core-helpdesk`, #2 `feature/ui-modernization-devops`).
- Release tag `v1.0.0` (on GitHub) marks the first release.
- Merge-conflict demonstration on `demo/conflict-*` branches (merge commit `9555e3b`); see
  [docs/Git-Collaboration-And-Merge-Conflict.md](docs/Git-Collaboration-And-Merge-Conflict.md).
  These demo branches exist in the local repository only.
- Current work: `feature/enterprise-ui-redesign` (UI redesign, security fixes, tests, DevOps fixes).

---

## Completion and Status (verified 2026-11-01)

| Component | Status | Evidence |
|---|---|---|
| Login/logout, dashboard, CRUD, search, status filter | ✅ Verified | Integration + Selenium tests |
| Role-based authorization enforced on the server | ✅ Verified | `AccessControlIntegrationTest` (direct POSTs rejected) |
| Drafts hidden from USER (list, counts, dashboard, direct URL) | ✅ Fixed and verified | Integration test |
| Validation and friendly error pages | ✅ Verified | Integration test, screenshots |
| Responsive UI, light/dark themes | ✅ Verified | 67 screenshots, 0 pages with horizontal scrolling |
| Maven build, 19 unit/integration tests | ✅ Pass | `maven-test-results.txt` |
| 6 Selenium journeys (Chrome in Docker) | ✅ Pass (executed, not skipped) | `surefire-reports/`, `selenium-screenshots/` |
| Docker image, health check, run/stop/start/remove | ✅ Verified | `docker-lifecycle.txt` |
| Ansible deploy, idempotency, rollback (local target) | ✅ Verified | `ansible-*.txt` |
| Jenkins pipeline run | ⏳ Selenium build pending; earlier build passed with Selenium off | [docs/Jenkins-Pipeline.md](docs/Jenkins-Pipeline.md) |
| Docker Hub publish | ⏳ Optional; no `dockerhub` credential exists in Jenkins | [docs/Jenkins-Pipeline.md](docs/Jenkins-Pipeline.md) |
| Remote server provisioning | ❌ Not configured | Inventory targets localhost only |

### Known limitations

- Data is in memory: articles reset when the app restarts; users are fixed in code (no add/edit users UI).
- Logins are session-based without CSRF tokens; the session cookie is `SameSite=Lax` and `HttpOnly`, which blocks cross-site POSTs in modern browsers.
- Ansible targets only the local Docker Desktop; there is no remote host.
- `mvn verify` cannot repackage the JAR while a process is running that same JAR (Windows file lock); run a copy as shown above.

> **Credentials policy:** never commit Docker Hub tokens, Jenkins passwords, SSH keys or
> other secrets. Use the Jenkins credential store (ID `dockerhub`).

---

## Further Reading

| Document | Description |
|---|---|
| [docs/Demo-Script.md](docs/Demo-Script.md) | 5–10 minute faculty demonstration script and DevOps concept explanations |
| [docs/Jenkins-Pipeline.md](docs/Jenkins-Pipeline.md) | Pipeline stages, one-time job setup, Docker Hub credential |
| [docs/Deployment.md](docs/Deployment.md) | Docker and Ansible deployment, idempotency, rollback |
| [docs/Git-Collaboration-And-Merge-Conflict.md](docs/Git-Collaboration-And-Merge-Conflict.md) | Merge conflict creation and resolution log |
| [evidence/final-verification-2026-11-01/README.md](evidence/final-verification-2026-11-01/README.md) | Latest genuine test, Docker, Ansible and UI evidence |
| [docs/Backlog.md](docs/Backlog.md), [docs/User-Stories.md](docs/User-Stories.md), [docs/SRS.md](docs/SRS.md), [docs/Definition-of-Done.md](docs/Definition-of-Done.md) | Planning and scope |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Branch strategy and commit conventions |
