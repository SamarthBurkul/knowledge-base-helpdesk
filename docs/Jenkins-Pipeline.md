# Jenkins CI Pipeline and Docker Hub Publishing

## 1. Environment (inspected 2026-11-01)

| Item | Value |
|---|---|
| Jenkins | 2.580.1, Windows service running as `LocalSystem`, `http://localhost:8081/` |
| Security | Jenkins user database, "logged-in users can do anything"; anonymous access returns HTTP 403 |
| Jenkins user | `Samarth03` |
| Tools configured in Jenkins | Maven `Maven3.10`, JDK `JDK25` (both referenced by the `Jenkinsfile`) |
| Machine `PATH` (seen by the service) | Java 25, Maven 3.10.0, Git, Docker CLI |
| Plugins present | Pipeline, Git, GitHub, JUnit, Credentials Binding, Timestamper, Docker Pipeline, Pipeline Graph View |
| Job `Knowledge-Base-Helpdesk-CI` | **Exists but has no pipeline definition** (neither inline script nor SCM) and **has never been built** |
| Credentials | **None stored.** There is no `dockerhub` credential |

Because the job is empty and Jenkins requires a login, no Jenkins build has been run
and none is claimed. Everything the pipeline does was verified outside Jenkins with the
same commands (see [`evidence/final-verification-2026-11-01/`](../evidence/final-verification-2026-11-01/README.md)).

## 2. Pipeline stages ([`Jenkinsfile`](../Jenkinsfile))

| Stage | Command | Stops the pipeline when |
|---|---|---|
| Checkout | `checkout scm` | repository cannot be cloned |
| Tool check | `java -version`, `mvn -v`, `docker version` | a tool is missing or Docker is not reachable by the service account |
| Compile | `mvn -B clean compile` | compilation fails |
| Unit and integration tests | `mvn -B test` (19 tests; Selenium skipped here) | any test fails |
| Package JAR | `mvn -B -DskipTests package`, JAR archived and fingerprinted | packaging fails |
| Start application for Selenium | copies the JAR to `ci-app\app-under-test.jar`, starts it on 18082, waits for `UP` | app does not become healthy |
| Selenium tests | starts `selenium/standalone-chrome` on 24444, runs the 6 journeys | any journey fails |
| Docker image build | `docker build --tag knowledge-base-helpdesk:ci-<BUILD_NUMBER> .` | build fails |
| Temporary-container deployment | runs the image on 18083 | container cannot start |
| Deployment health check | waits for HTTP `UP` **and** Docker health `healthy` | not healthy within ~90 s |
| Docker Hub publish (opt-in) | tag and push `<repo>:<BUILD_NUMBER>` | login or push fails |

Failed tests stop the pipeline: the `bat` step exits non-zero, the stage fails, and the
image build and deployment stages never run. `post { always }` stops the test JVM,
removes the CI containers, publishes `target/surefire-reports/TEST-*.xml` with the JUnit
plugin, and archives screenshots (`test-output/`) and logs.

Parameters: `RUN_SELENIUM` (default on), `PUBLISH_TO_DOCKERHUB` (default off),
`DOCKERHUB_REPOSITORY`.

## 3. One-time setup of the existing job (needs your Jenkins login)

The pipeline builds what is on GitHub, so push the branch you want to build first.

1. Open `http://localhost:8081/` and sign in as `Samarth03`.
2. Open **Knowledge-Base-Helpdesk-CI** → **Configure**. Do not create a new job.
3. Under **Pipeline**, set:
   - **Definition**: `Pipeline script from SCM`
   - **SCM**: `Git`
   - **Repository URL**: `https://github.com/SamarthBurkul/knowledge-base-helpdesk.git`
     (public repository, no credentials needed)
   - **Branch Specifier**: `*/main` (or `*/feature/enterprise-ui-redesign` to build the branch before merging)
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

### If the first build fails

| Console message | Cause and fix |
|---|---|
| `Tool type "maven" does not have an install of "Maven3.10"` | Manage Jenkins → Tools: keep the names `Maven3.10` and `JDK25` |
| `docker: error during connect` / `Access is denied` in **Tool check** | The `LocalSystem` service cannot reach Docker Desktop. In `services.msc`, set the Jenkins service **Log On** to your Windows account (member of `docker-users`), restart Jenkins |
| Selenium stage times out | Docker Desktop not running, or port 24444 in use: `docker ps` |
| Health check fails | Port 18083 in use, or see `deploy-container.log` in the build artifacts |

The first build downloads Maven dependencies for the service account, so it is slower.

## 4. Docker Hub publishing (optional)

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
