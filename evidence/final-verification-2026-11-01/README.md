# Final Verification Evidence – 2026-11-01

Everything in this folder was produced by real commands on the project machine
(Windows 11, Java 25.0.4.1, Maven 3.10.0, Docker Desktop engine 29.8.2,
ansible-core 2.21.5 in WSL Ubuntu). Nothing was edited by hand except the `#` header
lines that name the command.

| File / folder | What it shows | Result |
|---|---|---|
| [`maven-test-results.txt`](maven-test-results.txt) | `mvn -B clean verify`, then the Selenium journeys executed against the packaged JAR | 19 unit/integration tests pass; 6 Selenium journeys pass (executed, not skipped) |
| [`surefire-reports/`](surefire-reports/) | Raw Surefire XML/TXT reports for all 4 test classes | 25 tests, 0 failures, 0 errors |
| [`selenium-screenshots/`](selenium-screenshots/) | Final screen of each Selenium journey (Chrome in `selenium/standalone-chrome`) | 6 screenshots |
| [`docker-lifecycle.txt`](docker-lifecycle.txt) | Build, inspect, run, health, logs, non-root user, stop, start, remove | Container `healthy`, `/actuator/health` = `UP` |
| [`ansible-1-deploy.txt`](ansible-1-deploy.txt) | First `deploy.yml` run (new image) | `changed=2 failed=0` |
| [`ansible-2-idempotency.txt`](ansible-2-idempotency.txt) | Same playbook again | `changed=0 failed=0` |
| [`ansible-3-rollback.txt`](ansible-3-rollback.txt) | `rollback-demo.yml`: bad candidate fails health check, previous image restored | `rescued=1 failed=0` |
| [`ansible-4-post-rollback-idempotency.txt`](ansible-4-post-rollback-idempotency.txt) | `deploy.yml` after the rollback | `changed=0 failed=0` |
| [`ui-screenshots/`](ui-screenshots/) | 67 screenshots: every role, desktop 1440 / tablet 834 / mobile 390 px, light and dark | 0 pages scroll sideways (`README.txt` lists the overflow value per page) |

The UI screenshots were taken from the `helpdesk-demo` container just before the final image rebuild.
That rebuild only added `TZ=Asia/Kolkata` to the Dockerfile, so displayed times in these screenshots are UTC.

## Not verified here

| Item | Why |
|---|---|
| Jenkins build | The existing job `Knowledge-Base-Helpdesk-CI` has no pipeline definition and Jenkins requires a login. Setup steps: [docs/Jenkins-Pipeline.md](../../docs/Jenkins-Pipeline.md) |
| Docker Hub push | No `dockerhub` credential exists in Jenkins; publishing is optional and off by default |
| Remote server provisioning | No remote host is configured; Ansible targets the local Docker Desktop only |

## Problems found and fixed during this verification

| Problem | Fix |
|---|---|
| USER could list, count and open **draft** articles (only the sidebar link was hidden) | Drafts filtered on the server for USER in list, counts, dashboard and `/articles/{id}` |
| Session ID appeared in redirect URLs (`;jsessionid=...`) | `server.servlet.session.tracking-modes=cookie` |
| Session ID was not changed at login (session fixation) | `request.changeSessionId()` on successful login |
| Missing or invalid `status` on create/update produced a raw 400 page | Status validated with a field message; title/description/solution length limits added |
| Dockerfile `HEALTHCHECK` used `wget`, which the JRE image does not contain (no built image had ever run it) | Dependency-free bash `/dev/tcp` probe; container now reports `healthy`. Container runs as non-root |
| `ansible/docker-desktop.sh` had CRLF line endings in the Windows checkout, so bash could not run it | `.gitattributes` forces LF for `*.sh`/`*.yml` |
| `deploy.yml` compared tag names, so a rebuilt image was never redeployed | Compares image IDs |
| `rollback-demo.yml` always restored a hard-coded image | Restores the image that was running before the candidate |
| Jenkinsfile ran Selenium twice and ran the test app from `target/*.jar` (locked on Windows) | One test pass per kind; app runs from a copy; Docker health is part of the deployment gate |
| Article list scrolled sideways on phones and the list/table toggle was off-screen | Chip row width capped on small screens |
| A submitted form restored with the browser's Back button kept a stuck "loading" button | Reset on `pageshow` |

Earlier evidence: [`../selenium-2026-10-14/`](../selenium-2026-10-14/), [`../ui-redesign-2026-10-20/`](../ui-redesign-2026-10-20/),
[`../DevOps-Verification.md`](../DevOps-Verification.md) (see the correction note at its top).
