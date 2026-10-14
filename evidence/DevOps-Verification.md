# DevOps Verification

Date: 2026-10-14

## Maven and Selenium

All commands were run from the repository root on Windows with Temurin Java
25.0.4.1 and Maven 3.10.0. Property arguments are quoted for PowerShell.

| Command | Result |
|---|---|
| `mvn test-compile` | `BUILD SUCCESS` |
| `mvn "-Dtest=HelpdeskSeleniumTest#userCanLoginSuccessfully" "-Dselenium.enabled=true" "-Dselenium.remoteUrl=http://localhost:4444" "-DbaseUrl=http://host.docker.internal:8082" test` | 1 test, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS` |
| `mvn "-Dtest=HelpdeskSeleniumTest" "-Dselenium.enabled=true" "-Dselenium.remoteUrl=http://localhost:4444" "-DbaseUrl=http://host.docker.internal:8082" test` | 4 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS` |
| `mvn "-Dselenium.enabled=true" "-Dselenium.remoteUrl=http://localhost:4444" "-DbaseUrl=http://host.docker.internal:8082" test` | 7 tests, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS` |

JUnit XML/text reports and the four Selenium screenshots are retained in
[`selenium-2026-10-14/`](selenium-2026-10-14/).

## Docker build and local health check

- `mvn -DskipTests package`: `BUILD SUCCESS`.
- `docker build --tag knowledge-base-helpdesk:1.0.1 .`: completed successfully.
  BuildKit manifest-list digest: `sha256:28a8e5b035f5425ee0cb82c91d89e30fe3d3f51f1f942000ba4dcaaec4703395`.
- Started the image as temporary container `helpdesk-app-verify-20261014` on
  host port 18083. `GET /actuator/health` returned `"status":"UP"`. Removed
  that specifically named temporary container after the check; the image was
  retained locally.

## Not run

- Jenkins pipeline: the local Jenkins URL presents a sign-in page. No Jenkins
  credentials were supplied, so the job configuration and build console could
  not be inspected or executed.
- Ansible playbooks: Ubuntu WSL has Ansible and Docker CLI installed, but a
  subsequent WSL invocation timed out while creating the WSL instance. No
  Docker/WSL reset or data removal was attempted. Playbook YAML was parsed
  separately with PyYAML; this does not substitute for `ansible-playbook
  --syntax-check` or execution.
- Docker Hub push: the local Docker CLI configuration has a credential-store
  helper but no Docker Hub registry entry was discoverable from its metadata.
  No image push was attempted because the Docker Hub namespace and Jenkins
  credential were not verified.

No Jenkins, Ansible deployment, Docker Hub publication, remote health check,
idempotency run, or rollback run is claimed as completed.
