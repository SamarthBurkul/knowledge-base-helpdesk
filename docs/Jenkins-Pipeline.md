# Jenkins CI Pipeline and Docker Hub Publishing

## 1. Environment (inspected 2026-11-02)

| Item | Value |
|---|---|
| Jenkins | 2.580.1, Windows service running as `LocalSystem`, `http://localhost:8081/` |
| Security | Jenkins user database, "logged-in users can do anything"; anonymous access returns HTTP 403 |
| Jenkins user | `Samarth03` |
| Tools configured in Jenkins | Maven `Maven3.10`, JDK `JDK25` (both referenced by the `Jenkinsfile`) |
| Machine `PATH` (seen by the service) | Java 25, Maven 3.10.0, Git, Docker CLI |
| Plugins present | Pipeline, Git, GitHub, JUnit, Credentials Binding, Timestamper, Docker Pipeline, Pipeline Graph View |
| Job `Knowledge-Base-Helpdesk-CI` | Pipeline script from SCM, branch `*/main` |
| Credentials | **None stored.** There is no `dockerhub` credential |

Build history before the container-based Selenium setup:

| Build | Result | What happened |
|---|---|---|
| #1 | Aborted | The application started on 18082, but the `powershell` step never exited, so the 45-minute timeout aborted the build |
| #2 | Success | Run with `RUN_SELENIUM=false`, so **no Selenium test ran in Jenkins** |

No Jenkins build with the current `Jenkinsfile` is claimed here. Its step bodies were
run in order outside Jenkins; see [section 8](#8-local-verification-2026-11-02).

## 2. Pipeline stages ([`Jenkinsfile`](../Jenkinsfile))

| Stage | Command | Stops the pipeline when |
|---|---|---|
| Checkout | `checkout scm` | repository cannot be cloned |
| Tool check | `java -version`, `mvn -v`, `docker version`; removes containers left by a killed build | a tool is missing or Docker is not reachable by the service account |
| Compile | `mvn -B clean compile` | compilation fails |
| Unit and integration tests | `mvn -B test` (19 tests; Selenium skipped here) | any test fails |
| Package JAR | `mvn -B -DskipTests package`, JAR archived and fingerprinted | packaging fails |
| Docker image build | `docker build --tag knowledge-base-helpdesk:ci-<BUILD_NUMBER> .` | build fails |
| Start test application container | runs the image as `helpdesk-app-under-test-<BUILD_NUMBER>` on 18082 | container cannot start |
| Wait for test application health | waits for HTTP `UP` **and** Docker health `healthy` | not healthy within 120 s, or the container stops |
| Start Selenium Chrome container | starts `selenium/standalone-chrome` on 24444, waits for Grid ready, checks that Chrome's container can reach the app | Grid not ready within ~120 s, or the app is unreachable from the container |
| Selenium browser tests | runs `HelpdeskSeleniumTest`, then checks the report | any journey fails, or fewer than 6 journeys ran, or any was skipped |
| Temporary-container deployment | runs the image as `helpdesk-deploy-ci-<BUILD_NUMBER>` on 18083 | container cannot start |
| Deployment health check | waits for HTTP `UP` **and** Docker health `healthy` | not healthy within 120 s, or the container stops |
| Docker Hub publish (opt-in) | tag and push `<repo>:<BUILD_NUMBER>` | login or push fails |

The four test-container stages run only when `RUN_SELENIUM` is ticked. A failed stage
makes declarative Pipeline skip all later stages, so a failed Selenium test means the
image is never deployed or published.

Stages that wait on containers have their own 5 to 15 minute timeouts, so a hang fails
quickly instead of using the whole 45-minute build timeout.

Parameters: `RUN_SELENIUM` (default on), `PUBLISH_TO_DOCKERHUB` (default off),
`DOCKERHUB_REPOSITORY`.

`disableConcurrentBuilds()` stays enabled because ports 18082, 18083 and 24444 are fixed.

## 3. Container-based Selenium setup

### Why a container

The pipeline used to start the application with `Start-Process java` inside a
`powershell` step. The application came up, but the step could not finish while the Java
process it had started was alive, so the build hung until the timeout. Nothing in the
pipeline starts a background process on the agent any more: `docker run --detach`
returns immediately and Docker keeps the application running.

This also means Selenium tests the same image that is deployed, not a separately
started JAR.

### How the pieces connect

| From | To | Address |
|---|---|---|
| Jenkins health check | test application | `http://localhost:18082/actuator/health` |
| Jenkins (Maven, `RemoteWebDriver`) | Selenium Grid | `http://localhost:24444` |
| Chrome, inside the Selenium container | test application | `http://host.docker.internal:18082` |
| Jenkins health check | temporary deployment | `http://localhost:18083/actuator/health` |

The application listens on 8080 inside each container; Docker publishes it on the host
port. `localhost` inside the Selenium container would mean that container, which is why
Chrome uses `host.docker.internal`, provided by Docker Desktop.

### Health checks

[`jenkins/Wait-ContainerHealthy.ps1`](../jenkins/Wait-ContainerHealthy.ps1) is used for
both the test application and the deployment. It succeeds only when Docker reports
`healthy` and the HTTP endpoint answers `UP`, and it logs each change of state. If the
container stops, it fails straight away and prints the last 50 log lines, with the same
output when the 120 seconds run out.

### Test count check

Surefire treats skipped tests as a success. After Maven finishes, the pipeline reads
`TEST-...HelpdeskSeleniumTest.xml` and fails unless it shows 6 tests, 0 failures,
0 errors and 0 skipped. **When you add a journey, update `EXPECTED_SELENIUM_TESTS` in
the `Jenkinsfile`.**

### Cleanup

`post { always }` runs after a success, failure, timeout or manual abort. For each of
the three containers it saves the logs and then runs `docker rm --force`. It prints
`WARNING: not removed:` if a CI container is still present afterwards.

The containers also carry the label `com.knowledgebase.helpdesk.ci=true`. If Jenkins
itself is killed, `post` cannot run, so **Tool check** removes anything with that label
at the start of the next build. `helpdesk-demo` and `helpdesk-ansible` do not have the
label and are never touched.

Build artifacts: `app-under-test.log`, `selenium-container.log`, `deploy-container.log`,
screenshots in `test-output/`, and the JUnit reports.

## 4. One-time setup of the existing job (needs your Jenkins login)

The pipeline builds what is on GitHub, so push the branch you want to build first.

1. Open `http://localhost:8081/` and sign in as `Samarth03`.
2. Open **Knowledge-Base-Helpdesk-CI** → **Configure**. Do not create a new job.
3. Under **Pipeline**, set:
   - **Definition**: `Pipeline script from SCM`
   - **SCM**: `Git`
   - **Repository URL**: `https://github.com/SamarthBurkul/knowledge-base-helpdesk.git`
     (public repository, no credentials needed)
   - **Branch Specifier**: `*/main` (or a feature branch to build it before merging)
   - **Script Path**: `Jenkinsfile`
4. **Save**, then **Build with Parameters** → leave `PUBLISH_TO_DOCKERHUB` unticked → **Build**.
5. Open the build → **Console Output**. Success ends with `Finished: SUCCESS`.
   The **Tests** tab shows the JUnit results.

The same job definition is in [`jenkins/Knowledge-Base-Helpdesk-CI.config.xml`](../jenkins/Knowledge-Base-Helpdesk-CI.config.xml).
With an API token (user menu → **Security** → **API Token**) it can be applied without the UI:

```powershell
curl.exe -u Samarth03:<API_TOKEN> -X POST -H "Content-Type: application/xml" `
  --data-binary "@jenkins/Knowledge-Base-Helpdesk-CI.config.xml" `
  http://localhost:8081/job/Knowledge-Base-Helpdesk-CI/config.xml
```

## 5. Running a build with Selenium

1. Make sure Docker Desktop is running. Keep the machine awake during the build.
2. **Knowledge-Base-Helpdesk-CI** → **Build with Parameters**.
3. Keep `RUN_SELENIUM` ticked and `PUBLISH_TO_DOCKERHUB` unticked → **Build**.
4. In **Console Output**, expect these lines in order:
   - `helpdesk-app-under-test-<build> is healthy (HTTP UP, Docker healthy).`
   - `Selenium Grid is ready`
   - `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`
   - `All Selenium journeys ran and passed: tests=6, failures=0, errors=0, skipped=0`
   - `helpdesk-deploy-ci-<build> is healthy (HTTP UP, Docker healthy).`
   - `Finished: SUCCESS`

## 6. Troubleshooting

| Console message | Cause and fix |
|---|---|
| `Tool type "maven" does not have an install of "Maven3.10"` | Manage Jenkins → Tools: keep the names `Maven3.10` and `JDK25` |
| `ERROR: Docker engine is not reachable` | Docker Desktop is not running. If it is, the `LocalSystem` service may be unable to reach it: in `services.msc`, set the Jenkins service **Log On** to your Windows account (member of `docker-users`), restart Jenkins |
| `ERROR: Could not start ... Is port ... already in use?` | Find the owner with `Get-NetTCPConnection -LocalPort <port> -State Listen`. Ports 18082, 18083 and 24444 must be free |
| `Container ... stopped during start-up` | The application crashed. Read the log lines printed above the message |
| `Container ... did not become healthy within 120 s` | Read the printed logs and the `last seen` states in the message |
| `Selenium container did not become ready` | The Chrome container is slow to start or Docker is short of memory; see `selenium-container.log` |
| `ERROR: The Chrome container cannot reach ...` | `host.docker.internal` is not resolving or the app is not published on 18082 |
| `Expected 6 Selenium tests to pass, but got: ...` | Journeys failed or were skipped, or a journey was added without updating `EXPECTED_SELENIUM_TESTS` |

The first build downloads Maven dependencies for the service account, so it is slower.

## 7. Docker Hub publishing (optional)

Local images are enough for the assignment; publishing is off by default. To enable it:

1. Docker Hub → **Account settings** → **Personal access tokens** → **Generate new token**
   (description `jenkins-knowledge-base-helpdesk`, permission **Read & Write**). Copy it once.
2. Jenkins → **Manage Jenkins** → **Credentials** → **System** → **Global credentials** → **Add Credentials**:
   - Kind: `Username with password`
   - Username: your Docker Hub username
   - Password: the access token
   - ID: `dockerhub` (exactly, it is referenced by the `Jenkinsfile`)
3. **Build with Parameters**: tick `PUBLISH_TO_DOCKERHUB`, set `DOCKERHUB_REPOSITORY` to
   `<your-username>/knowledge-base-helpdesk`.

The token is read through `withCredentials` and piped to `docker login --password-stdin`,
so it is masked in the console and never written to the repository.
The pushed tag is `<repository>:<BUILD_NUMBER>`; `latest` is never pushed.

## 8. Local verification (2026-11-02)

The `bat` and `powershell` step bodies were extracted from the `Jenkinsfile` and run in
stage order on the Jenkins machine, under the developer account rather than the Jenkins
service. This is **not a Jenkins build**.

| Scenario | Result |
|---|---|
| All stages | 25 tests with 6 skipped in the test stage, then all 6 Selenium journeys passed, both health gates passed, all containers removed |
| Failing Selenium test | Stopped at Selenium; deployment did not run; containers removed |
| All journeys skipped | Maven passed, the test count check failed the stage; containers removed |
| Port 18082 in use | Clear error; containers removed |
| Container crash, never healthy, container missing | Health script failed with a clear message |
| Leftover labelled container | Removed by Tool check |

Not exercised: a manual abort or timeout, which depends on Jenkins running `post`, and
the Docker Hub stage, which is unchanged.
