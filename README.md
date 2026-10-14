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

- User authentication
- Role-based authorization
- Knowledge article creation
- Knowledge article viewing
- Knowledge article search
- Knowledge article updating
- Knowledge article deletion
- Administrator dashboard
- Automated testing

## DevOps Features

- Git and GitHub
- Feature branch workflow
- Pull Requests and code review
- Jenkins CI/CD
- Maven build automation
- Selenium automated testing
- Docker containerization
- Docker image versioning
- Docker Hub
- Ansible provisioning
- Health checks
- Rollback and recovery

## Technology Stack

- Java
- Maven
- Git
- GitHub
- Jenkins
- Selenium
- Docker
- Docker Hub
- Ansible
- Ubuntu

## Project Structure

```text
knowledge-base-helpdesk/
├── .github/
├── docs/
├── src/
├── tests/
├── ansible/
├── evidence/
├── README.md
├── CONTRIBUTING.md
└── .gitignore
```

## Local Selenium Tests

The Selenium suite uses a fresh remote browser session per test. Start the
official browser container from PowerShell:

```powershell
docker run --detach --name helpdesk-selenium `
  --publish 4444:4444 --shm-size=2g selenium/standalone-chrome:latest
```

Wait for `http://localhost:4444/status` to report `value.ready: true`, then
run the four UI journeys against the application container:

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
Selenium screenshots are written to `test-output/`. See
[docs/Jenkins-Pipeline.md](docs/Jenkins-Pipeline.md) for the Windows Jenkins
pipeline and [docs/Deployment.md](docs/Deployment.md) for Docker and Ansible
deployment guidance. Do not commit local screenshots or reports unless they
are intentionally selected as assignment evidence.

The local Docker image is built from the packaged JAR:

```powershell
mvn -DskipTests package
docker build --tag knowledge-base-helpdesk:1.0.1 .
```

The Ansible inventory currently targets Ubuntu WSL locally. Jenkins Docker Hub
publishing and remote-host provisioning require credentials and target details
to be configured outside the repository before those deployment steps can be
enabled.
