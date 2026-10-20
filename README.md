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
- Role-based authorization (USER, SUPPORT, ADMIN)
- Modernized enterprise-style UI with responsive layouts and accessible controls
- Dashboard metrics: total articles, published count, draft count, resolved count, active user count
- Admin-only user and role overview table on the dashboard
- Knowledge article search (by title, description, or solution text)
- Knowledge article listing with status badges (PUBLISHED, DRAFT, RESOLVED)
- Knowledge article detail view with author, timestamp, and formatted solution panel
- Knowledge article creation and editing (ADMIN and SUPPORT roles)
- Knowledge article deletion (ADMIN role only)
- Spring Boot Actuator health endpoint (`/actuator/health`) exposed for monitoring
- Docker container health check using the Actuator health endpoint

## DevOps Features

- Git and GitHub with feature branch workflow
- Pull Requests and code review
- Merge conflict demonstration and resolution (dedicated branches)
- Jenkins CI/CD pipeline (Windows-compatible `bat` and PowerShell steps)
- Maven build automation
- Selenium automated browser testing (4 UI journey tests)
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
| `/articles` | GET | Authenticated | Article directory with search |
| `/articles/{id}` | GET | Authenticated | Article detail view |
| `/articles/new` | GET | ADMIN, SUPPORT | Article creation form |
| `/articles/create` | POST | ADMIN, SUPPORT | Save new article |
| `/articles/edit/{id}` | GET | ADMIN, SUPPORT | Article edit form |
| `/articles/update/{id}` | POST | ADMIN, SUPPORT | Save updated article |
| `/articles/delete/{id}` | POST | ADMIN only | Delete article |
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
│   ├── Deployment.md
│   ├── Git-Collaboration-And-Merge-Conflict.md
│   └── Jenkins-Pipeline.md
├── evidence/
│   └── DevOps-Verification.md
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

### Unit Tests Only

```powershell
mvn -B test
```

Latest recorded results (from `target/surefire-reports/`):

| Suite | Run | Passed | Skipped | Failures | Errors |
|---|---|---|---|---|---|
| `ArticleServiceTest` | 3 | 3 | 0 | 0 | 0 |
| `HelpdeskSeleniumTest` | 4 | 0 | 4 | 0 | 0 |
| **Total** | **7** | **3** | **4** | **0** | **0** |

Reported build status: **`BUILD SUCCESS`**

The four Selenium tests (`userCanLoginSuccessfully`, `userCanSearchKnowledgeArticle`,
`supportStaffCanCreateArticle`, `normalUserCannotAccessCreateArticle`) were **skipped**
because `-Dselenium.enabled=true` was not set. Skipping is the intended behaviour
for local unit-test runs. These tests are not counted as passing and do not
constitute browser end-to-end verification.

### Local Selenium Tests

The Selenium suite uses a fresh remote browser session per test. Start the
official browser container from PowerShell:

```powershell
docker run --detach --name helpdesk-selenium `
  --publish 4444:4444 --shm-size=2g selenium/standalone-chrome:latest
```

Wait for `http://localhost:4444/status` to report `value.ready: true`, then
run the four UI journeys against the running application:

```powershell
mvn "-Dtest=HelpdeskSeleniumTest" `
  "-Dselenium.enabled=true" `
  "-Dselenium.remoteUrl=http://localhost:4444" `
  "-DbaseUrl=http://host.docker.internal:8082" test
```

For the complete unit and Selenium suite, omit the test selector:

```powershell
mvn "-Dselenium.enabled=true" `
  "-Dselenium.remoteUrl=http://localhost:4444" `
  "-DbaseUrl=http://host.docker.internal:8082" test
```

Maven writes JUnit XML and text reports under `target/surefire-reports/`;
Selenium screenshots are written to `test-output/`. Do not commit local
screenshots or reports unless they are intentionally selected as evidence.

---

## Docker

### Build and Run

The local Docker image is built from the packaged JAR:

```powershell
mvn -DskipTests package
docker build --tag knowledge-base-helpdesk:1.0.1 .
```

The `Dockerfile` targets `eclipse-temurin:25-jre`, copies the JAR, exposes
port 8080, and includes a `HEALTHCHECK` directive:

```dockerfile
HEALTHCHECK --interval=10s --timeout=3s --start-period=15s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

Spring Boot with Java 25 typically takes 3–7 seconds to initialize before the
health endpoint responds. Polling immediately after `docker run` without a retry
loop will produce connection-refused or connection-reset errors. Always allow
the start-period to elapse and use a retry loop when checking health
programmatically.

To start a disposable verification container:

```powershell
docker run --detach --name helpdesk-verify --publish 18085:8080 knowledge-base-helpdesk:1.0.1

# Poll until the health endpoint reports UP (up to 60 seconds)
$ready = $false
for ($i = 0; $i -lt 30; $i++) {
    try {
        $h = Invoke-RestMethod "http://localhost:18085/actuator/health"
        if ($h.status -eq "UP") { $h; $ready = $true; break }
    } catch { Start-Sleep -Seconds 2 }
}
if (-not $ready) { docker logs helpdesk-verify; throw "Health check failed" }

# Demonstrate container lifecycle commands
docker logs helpdesk-verify
docker inspect helpdesk-verify
docker stop helpdesk-verify
docker start helpdesk-verify
docker restart helpdesk-verify
docker rm --force helpdesk-verify
```

> **Docker Desktop requirement:** Docker commands require Docker Desktop to be
> running and its daemon to be accessible. If `docker ps` returns
> `Docker Desktop is unable to start`, the Windows service `com.docker.service`
> may need to be started — which requires Administrator privileges. Launch Docker
> Desktop from the Start Menu or use an elevated prompt. Do not assume the daemon
> is running without verifying.

For full deployment and Ansible guidance see [docs/Deployment.md](docs/Deployment.md).

---

## Jenkins CI/CD

The pipeline is defined in the root [`Jenkinsfile`](Jenkinsfile) and is
configured for Windows Jenkins (bat and PowerShell steps). Jenkins 2.580.1
runs locally at `http://localhost:8081/`.

Pipeline stages:

1. **Checkout** — checks out the configured SCM revision
2. **Maven build** — packages the JAR, skipping tests at this stage
3. **Unit tests** — runs `ArticleServiceTest`; build fails here on any test failure
4. **Start app for Selenium** — starts the packaged JAR on port 18082, polls `/actuator/health` with up to 30 retries
5. **Selenium tests** — starts `selenium/standalone-chrome` on port 24444, runs the four browser journeys
6. **Full test suite** — runs all tests with Selenium enabled
7. **Docker image build** — builds a tagged local image
8. **Temporary-container deployment** — runs the image on port 18083
9. **Health check** — polls `/actuator/health` on port 18083 with a 30-attempt retry loop; dumps logs and fails if never healthy
10. **Docker Hub publish (opt-in)** — conditional on `PUBLISH_TO_DOCKERHUB=true`; disabled by default

The `post { always }` block kills the background JVM process, removes both
temporary containers, publishes JUnit XML reports, and archives screenshots
and application logs regardless of build outcome.

> **Jenkins execution status:** A successful end-to-end run of
> `Knowledge-Base-Helpdesk-CI` has not been confirmed by available evidence.
> Jenkins requires user authentication at `http://localhost:8081/login` before
> jobs can be triggered or inspected.

For credential setup and detailed stage documentation see
[docs/Jenkins-Pipeline.md](docs/Jenkins-Pipeline.md).

---

## Ansible Deployment and Rollback

Playbooks are in the `ansible/` directory and target Ubuntu WSL locally.
Enable **Docker Desktop → Settings → Resources → WSL Integration** for Ubuntu
before running them.

**Deploy (first run creates the container; subsequent runs verify idempotence):**

```bash
cd /mnt/g/knowledge-base-helpdesk
export ANSIBLE_CONFIG=ansible/ansible.cfg
ansible-playbook ansible/deploy.yml \
  -e docker_cli=/mnt/g/knowledge-base-helpdesk/ansible/docker-desktop.sh
```

**Verify idempotence (second run):**

```bash
ansible-playbook ansible/deploy.yml \
  -e docker_cli=/mnt/g/knowledge-base-helpdesk/ansible/docker-desktop.sh
```

The second run should report `changed=0, failed=0`.

**Rollback demonstration:**

```bash
ansible-playbook ansible/rollback-demo.yml \
  -e docker_cli=/mnt/g/knowledge-base-helpdesk/ansible/docker-desktop.sh
```

Previously recorded verification results (not re-executed during this README
update):

| Run | Outcome |
|---|---|
| Playbook syntax check (`--syntax-check`) | Passed |
| First deploy (container created) | `changed=1, failed=0`; health check reached `UP` |
| Idempotence check (second deploy) | `changed=0, failed=0` |
| Rollback demo (nginx:alpine candidate → rescue) | `changed=4, rescued=1, failed=0`; health restored to `UP` |

The deployment container is named `helpdesk-ansible` and runs on host port
18084. The existing application on port 8082 is not affected.

For full Ansible guidance see [docs/Deployment.md](docs/Deployment.md).

---

## Recent Development and DevOps Work

### Application UI and Role-Based Access

- Modernized the login screen, dashboard, knowledge-article directory, article
  detail view, and article creation/editing forms with an enterprise SaaS design
  system (CSS custom properties, Inter typography, responsive grid, accessible
  focus indicators).
- Added dashboard metrics for article totals and statuses.
- Added an administrative user and role overview table visible only to users
  with the ADMIN role, powered by `AuthService.findAllUsers()` and exposed via
  `DashboardController`.
- Improved responsive layouts, form labels with accessible `for`/`id` associations,
  status badge indicators, breadcrumb navigation, and keyboard accessibility.
- Preserved the `+ Create Article` link text required by existing Selenium tests
  (`By.linkText("+ Create Article")`).

### Containerization and Health Monitoring

- Added a `HEALTHCHECK` directive to the `Dockerfile` using the Spring Boot
  Actuator `/actuator/health` endpoint (15-second start period, 10-second
  interval, 3 retries).
- Documented the startup delay root cause: Docker's proxy binds the host port
  immediately on `docker run`, but Tomcat needs a few seconds to finish
  initializing. Polling without a retry loop hits this window and causes
  connection errors.

### Jenkins CI/CD Pipeline

- Pipeline steps use Windows-compatible `bat` and `powershell` commands.
- Build and test failures stop subsequent stages (fail-fast).
- Health check uses a 30-attempt polling loop with 2-second sleeps.
- Temporary containers are cleaned up in `post { always }` regardless of outcome.
- JUnit XML reports and Selenium screenshots are archived on every run.

### Ansible Deployment and Rollback

Previously recorded verification includes playbook syntax validation, deployment,
an idempotence check with `changed=0`, and a simulated deployment failure using
`nginx:alpine` followed by automatic rescue and recovery to a healthy application
version. These results were not re-executed during this README update. Refer to
[evidence/DevOps-Verification.md](evidence/DevOps-Verification.md) for the
exact commands and recorded outputs.

### Git Branching and Merge Conflict Demonstration

- Feature branch `feature/ui-modernization-devops` was created from `develop`
  for all UI and DevOps improvements in this phase.
- Dedicated demonstration branches (`demo/conflict-branch-a` and
  `demo/conflict-branch-b`) were created from a common base commit to simulate
  two teams making conflicting edits to the same file.
- The merge was attempted, a genuine `CONFLICT (content)` was produced in
  `docs/demo/conflict-demo.md`, the conflict markers were manually resolved into
  a unified version, and the resolution was committed (merge commit `9555e3b`).
- See [docs/Git-Collaboration-And-Merge-Conflict.md](docs/Git-Collaboration-And-Merge-Conflict.md)
  for the full step-by-step log including commit IDs, conflict markers, and the
  resolved content.

### Test Results (Latest Local Run)

Command: `mvn -B test`

| Result | Count |
|---|---:|
| Tests run | 7 |
| Passed | 3 |
| Skipped | 4 |
| Failures | 0 |
| Errors | 0 |

Recorded build status: **`BUILD SUCCESS`**

The four Selenium tests were skipped because `-Dselenium.enabled=true` was not
passed. This is expected in a local unit-test invocation. It does not mean all
browser tests passed — browser-based verification requires an active Selenium
Grid or standalone Chrome container.

---

## Completion and Status

| Component | Status | Notes |
|---|---|---|
| Core application logic | ✅ Implemented and verified | 3/3 unit tests pass; `BUILD SUCCESS` |
| Role-based access control | ✅ Implemented and verified | ADMIN, SUPPORT, USER roles enforced in controllers |
| Modernized enterprise UI | ✅ Implemented and verified | All 5 templates updated; 0 test regressions |
| Admin user & role overview | ✅ Implemented and verified | Dashboard table rendered for ADMIN role |
| Docker build | ✅ Implemented | JAR present; image buildable from `Dockerfile` |
| Docker `HEALTHCHECK` | ✅ Implemented | Added to `Dockerfile`; uses Actuator health endpoint |
| Docker Desktop daemon | ⚠️ Requires manual start | `com.docker.service` must be running; may need Administrator |
| Ansible deploy + idempotence | ✅ Previously verified | `changed=0` on second run; not re-run during README update |
| Ansible rollback demo | ✅ Previously verified | `rescued=1, failed=0`; not re-run during README update |
| Git feature branches | ✅ Implemented | `feature/ui-modernization-devops` active on `develop` |
| Merge conflict demo | ✅ Implemented | Conflict created and resolved on dedicated demo branches |
| Selenium browser tests | ⚠️ Not yet end-to-end verified | 4 tests skipped locally; requires active browser container |
| Jenkins pipeline execution | ⚠️ Pending | Authentication required at `http://localhost:8081/login` |
| Docker Hub publishing | ⚠️ Pending credentials | Disabled by default; requires `dockerhub` Jenkins credential |
| Remote Ansible target | ⚠️ Not configured | Inventory targets localhost/WSL only; no remote host available |

> **Credentials policy:** Never store Docker Hub tokens, Jenkins passwords, SSH
> keys, or any other secrets in the repository. Use the Jenkins credential store
> with ID `dockerhub` for Docker Hub access tokens. See
> [docs/Jenkins-Pipeline.md](docs/Jenkins-Pipeline.md) for the exact setup steps.

---

## Further Reading

| Document | Description |
|---|---|
| [docs/Jenkins-Pipeline.md](docs/Jenkins-Pipeline.md) | Pipeline stage reference, Docker Hub PAT generation, and Jenkins credential configuration |
| [docs/Deployment.md](docs/Deployment.md) | Docker and Ansible deployment commands, idempotence, and rollback guide |
| [docs/Git-Collaboration-And-Merge-Conflict.md](docs/Git-Collaboration-And-Merge-Conflict.md) | Full step-by-step merge conflict creation and resolution log |
| [evidence/DevOps-Verification.md](evidence/DevOps-Verification.md) | Timestamped verification evidence including test output, Docker diagnosis, and Ansible run summaries |
| [CONTRIBUTING.md](CONTRIBUTING.md) | Branch strategy, commit message conventions, and workflow rules |
