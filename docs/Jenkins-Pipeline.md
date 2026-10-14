# Jenkins Pipeline

The root `Jenkinsfile` uses Windows `bat` and PowerShell steps for the Windows
Jenkins service. It invokes `mvn`, `java`, and `docker` from the service
account's `PATH`; no Jenkins global tool names were verifiable because the
Jenkins UI requires sign-in. The agent needs Java 25, Maven, Docker CLI with
access to Docker Desktop, PowerShell, and the Jenkins Pipeline and JUnit
plugins.

Selenium runs in the official `selenium/standalone-chrome` container. The
pipeline maps port 4444, allocates 2 GB of shared memory, and waits for
`/status` to report ready. Tests create a new headless `RemoteWebDriver` session
per test, connecting to `http://localhost:24444` (mapped to container port
4444); the browser container reaches the host application at
`http://host.docker.internal:18082`. The non-default host port avoids a
collision with the developer Selenium container on port 4444. No manually
opened Chrome or debugging session is required.

Pipeline stages:

1. Check out the configured SCM revision.
2. Build/package with Maven and run `ArticleServiceTest`.
3. Start the packaged app on port 18082 and verify its Actuator health.
4. Start and await the Selenium Chrome container.
5. Run the four Selenium journeys and then the complete test suite with
   Selenium enabled.
6. Build a local Docker image, deploy a temporary container on port 18083, and
   verify its health.
7. Optionally push the validated image tagged with the Jenkins build number.
   This is disabled by default and requires the Jenkins credential ID
   `dockerhub` plus the `DOCKERHUB_REPOSITORY` build parameter.
8. Stop the test application, remove both temporary containers, and publish
   JUnit reports and available screenshots/logs.

Ports 18082, 18083, and 24444 must be available to the Jenkins service account.
Before enabling image publishing, sign in to Jenkins, add a **Username with
password** credential under **Manage Jenkins > Credentials > System > Global
credentials (unrestricted)** with ID `dockerhub`, and use a Docker Hub access
token as its password. Run the job with `PUBLISH_TO_DOCKERHUB=true` and the
lowercase namespace/repository (without a tag) in `DOCKERHUB_REPOSITORY`.
The stage pushes only the build-number tag, never `latest`.

Remote Ansible provisioning still requires a target host, SSH access, and
validated inventory; see [Deployment.md](Deployment.md). Jenkins configuration
and publishing have not been executed from this checkout.
