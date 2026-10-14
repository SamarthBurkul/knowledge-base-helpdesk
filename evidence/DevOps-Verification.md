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

## Ansible local deployment and rollback

- Both `ansible/deploy.yml` and `ansible/rollback-demo.yml` passed
  `ansible-playbook --syntax-check` in Ubuntu WSL.
- The Docker Desktop Windows CLI was reachable from WSL using
  `ansible/docker-desktop.sh`; the playbooks accept that executable through
  `docker_cli`.
- The first `ansible/deploy.yml` run created `helpdesk-ansible` on host port
  18084 and its health endpoint eventually returned `UP`: recap `changed=1`,
  `failed=0`. The first health request retries were expected during startup.
- A second `ansible/deploy.yml` run found the desired container already
  running with the correct image and port: recap `changed=0`, `failed=0`.
- The successful `ansible/rollback-demo.yml` run started `nginx:alpine` on
  port 18084, observed the candidate health probe fail with connection reset,
  removed only `helpdesk-ansible`, restored
  `knowledge-base-helpdesk:1.0.1`, and confirmed health `UP`: recap
  `changed=4`, `rescued=1`, `failed=0`. A preceding attempt encountered a
  transient WSL/Docker CLI timeout while starting the candidate; its rescue
  also restored the known-good image. No Docker/WSL reset or data removal was
  attempted.
- The deployed local container remains `helpdesk-ansible` on port 18084,
  using `knowledge-base-helpdesk:1.0.1`; the existing app on port 8082 was
  not replaced.

## Blocked / not run

- GitHub PR: no PR was found for the feature branch and the compare page
  requires GitHub sign-in. Manual action: sign in at the compare URL recorded
  in the task report, select base `develop` and compare
  `feature/core-helpdesk`, retain the repository PR template, then create the
  PR.
- Jenkins pipeline: the local Jenkins URL presents a sign-in page. No Jenkins
  credentials were supplied, so job configuration and build console could
  not be inspected or executed.
- Docker Hub push: the local Docker CLI configuration has a credential-store
  helper but no Docker Hub registry entry was discoverable from its metadata.
  No image push was attempted because the Docker Hub namespace and Jenkins
  credential were not verified.
- Remote Ansible deployment: no remote Ubuntu target, SSH account, or key was
  available. The verified playbooks target only local Ubuntu WSL.
- A second feature branch, conflict-resolution evidence, and release tag were
  not created because the baseline PR has not been reviewed or merged.

No Jenkins run, Docker Hub publication, remote deployment, or production
health/rollback check is claimed as completed.
