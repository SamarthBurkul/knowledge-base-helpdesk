# Jenkins CI/CD Pipeline & Docker Hub Publication Guide

## 1. Overview and Environment

The Jenkins pipeline is defined in the root [`Jenkinsfile`](../Jenkinsfile). It is built for a Windows Jenkins installation (Jenkins 2.580.1 running at `http://localhost:8081/`) executing against Docker Desktop and Temurin Java 25.

### Pipeline Prerequisites on Windows
- Jenkins running as a Windows service or local agent
- Installed tools on the Jenkins service account's `PATH`:
  - `mvn` (Maven 3.10.0+)
  - `java` (Java 25)
  - `docker` (Docker Engine CLI connected to Docker Desktop)
  - `powershell` (PowerShell 5.1 / 7+)
- Jenkins Plugins:
  - Pipeline plugin
  - JUnit plugin
  - Credentials Binding plugin

---

## 2. Pipeline Execution Stages

| Stage | Action | Success Criteria | Cleanup Behavior |
|---|---|---|---|
| **Checkout** | Clones/checks out the git revision | Code available in `$WORKSPACE` | N/A |
| **Maven build** | `mvn -B -DskipTests package` | Artifact `target/knowledge-base-helpdesk-1.0.0.jar` generated | Handled in post |
| **Unit tests** | `mvn -B -Dtest=ArticleServiceTest test` | 3/3 tests pass (0 failures) | XML archived in JUnit |
| **Start application for Selenium** | Starts app on port 18082 in background; records PID | `http://localhost:18082/actuator/health` returns `UP` | Killed in `post { always }` |
| **Selenium tests** | Starts `selenium/standalone-chrome` container on port 24444; runs 4 browser journeys | 4/4 UI journeys pass; screenshots captured | Selenium container removed in `post` |
| **Full test suite** | `mvn -B -Dselenium.enabled=true test` | All unit & Selenium tests pass | Test reports archived |
| **Docker image build** | `docker build --tag "%APP_IMAGE%" .` | Image built with tag `knowledge-base-helpdesk:${BUILD_NUMBER}` | Image retained locally |
| **Temporary-container deployment** | Runs temporary container on port 18083 | Container starts in detached mode | Container removed in `post` |
| **Health check** | Polls `http://localhost:18083/actuator/health` with retry loop | Returns HTTP 200 with `{"status":"UP"}` | Diagnostic logs dumped on failure |
| **Docker Hub publish (opt-in)** | Conditional stage: checks `params.PUBLISH_TO_DOCKERHUB` | Logs in using `dockerhub` credential, tags and pushes image | `docker logout` in finally block |

---

## 3. Docker Hub Publishing Configuration (Manual Setup Required)

> [!IMPORTANT]
> **Docker Hub Publishing is strictly opt-in and disabled by default (`PUBLISH_TO_DOCKERHUB = false`).**
> Because you forgot your previous Docker Hub access token, you must generate a new Personal Access Token (PAT) from Docker Hub and configure it in Jenkins following the steps below.

### Step 3.1: Generate a Docker Hub Personal Access Token (PAT)
1. Sign in to your Docker Hub account at [https://hub.docker.com](https://hub.docker.com).
2. Click on your profile username in the upper-right corner and select **Account Settings**.
3. In the left navigation menu, navigate to **Security** > **Personal access tokens**.
4. Click **New Access Token**.
5. Fill in the details:
   - **Access Token Description**: `jenkins-ci-knowledge-base-helpdesk`
   - **Access permissions**: Select **Read & Write** (or Read, Write, Delete if managing tags).
6. Click **Generate**.
7. **Copy the generated token immediately**. Docker Hub will never show it again. Store it securely in your password manager.

### Step 3.2: Configure the Credential in Jenkins
1. Open Jenkins in your browser at `http://localhost:8081/`.
2. Sign in with your Jenkins administrative account.
3. In the left dashboard, click **Manage Jenkins** > **Credentials**.
4. Under **Stores scoped to Jenkins**, click on **System**, then click **Global credentials (unrestricted)**.
5. In the left sidebar, click **Add Credentials**.
6. Fill in the form fields:
   - **Kind**: Select `Username with password`.
   - **Scope**: `Global (Jenkins, nodes, items, all child items, etc.)`.
   - **Username**: Your exact Docker Hub username (e.g., `samarthburkul`).
   - **Password**: Paste the **Personal Access Token** copied in Step 3.1.
   - **ID**: `dockerhub` *(must be exact lowercase: `dockerhub`, as referenced in `Jenkinsfile`)*.
   - **Description**: `Docker Hub Service Account for Knowledge Base Helpdesk CI`.
7. Click **Create**.

---

## 4. Running the Job with or without Publishing

### Scenario A: Standard CI Validation (Publishing Disabled — Default)
1. In Jenkins, open the job **`Knowledge-Base-Helpdesk-CI`**.
2. Click **Build with Parameters**.
3. Leave **PUBLISH_TO_DOCKERHUB** unchecked (`false`).
4. Leave **DOCKERHUB_REPOSITORY** empty.
5. Click **Build**.
6. The pipeline will build, run unit tests, run Selenium tests, build the local Docker image, verify the container health, and skip the publishing stage.

### Scenario B: Release Publication to Docker Hub
1. Open **`Knowledge-Base-Helpdesk-CI`** in Jenkins.
2. Click **Build with Parameters**.
3. Check the box **PUBLISH_TO_DOCKERHUB** (`true`).
4. Enter your lowercase repository name in **DOCKERHUB_REPOSITORY** (e.g., `samarthburkul/knowledge-base-helpdesk`).
5. Click **Build**.
6. Upon passing all unit, Selenium, and container health checks, the image will be tagged and pushed:
   - Pushed tag format: `<DOCKERHUB_REPOSITORY>:<BUILD_NUMBER>` (e.g., `samarthburkul/knowledge-base-helpdesk:2`).
   - **Note**: The pipeline never pushes `latest` directly, preserving strict build provenance.

---

## 5. Verifying the Published Image on Docker Hub

After a successful publication build:
1. Log in to [https://hub.docker.com](https://hub.docker.com).
2. Navigate to **Repositories** > `<your-username>/knowledge-base-helpdesk`.
3. Under the **Tags** tab, verify that tag `<BUILD_NUMBER>` appears with the recent timestamp and matching digest.
4. Locally verify pulling the published image:
   ```powershell
   docker pull <your-username>/knowledge-base-helpdesk:<BUILD_NUMBER>
   ```

---

## 6. Post-Build Artifacts and Diagnostics
Regardless of build success or failure, the pipeline always:
- Collects JUnit XML reports from `target/surefire-reports/TEST-*.xml`
- Archives test failure screenshots from `test-output/`
- Archives application logs: `app.log` and `app-error.log`
- Guarantees termination of background JVM processes via `app.pid`
- Force-removes temporary CI containers (`selenium-chrome-ci-${BUILD_NUMBER}` and `helpdesk-app-ci-${BUILD_NUMBER}`)
