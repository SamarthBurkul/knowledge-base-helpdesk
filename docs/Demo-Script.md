# Faculty Demonstration Script (about 8–10 minutes)

## Before the demo (10 minutes earlier)

```powershell
# 1. Docker Desktop running? Both containers healthy?
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
#    helpdesk-demo     knowledge-base-helpdesk:1.0.2  Up ... (healthy)  8082->8080
#    helpdesk-ansible  knowledge-base-helpdesk:1.0.2  Up ... (healthy)  18084->8080

# If helpdesk-demo is missing:
docker run -d --name helpdesk-demo --restart unless-stopped -p 8082:8080 knowledge-base-helpdesk:1.0.2

# 2. Fresh demo data (articles are in memory; a restart resets them to the 3 samples)
docker restart helpdesk-demo
```

Open these browser tabs:

1. `http://localhost:8082/login` (the app)
2. The GitHub repository: branches, pull requests, tag `v1.0.0`
3. `http://localhost:8081/` (Jenkins, signed in), only if the job was set up and has a green build
4. `evidence/final-verification-2026-11-01/README.md`

Open a PowerShell window in the project folder and an Ubuntu (WSL) window.

## The script

| # | Time | Say / do |
|---|---|---|
| 1 | 0:30 | **Problem.** Support teams answer the same questions repeatedly, because solutions are hard to find. This app is one searchable knowledge base: employees search, support staff write, admins manage. |
| 2 | 1:00 | **Architecture.** Spring Boot 4 with Thymeleaf pages. Controllers check the role on every request, and services hold the data in memory. Maven builds and tests, Docker packages, Ansible deploys, and Jenkins automates it all. The code lives on GitHub with feature branches and pull requests. |
| 3 | 0:45 | **ADMIN.** Sign in by clicking the *Administrator* demo card. Show the dashboard figures, the status bar, the team panel and **Users & roles**. |
| 4 | 1:00 | **SUPPORT creates.** Sign out, then sign in as *Support Staff*. Click **New article**, then **Save Article** with an empty form to show the validation. Fill in title `DEMO – VPN not connecting`, a problem, three solution steps, status **Draft**. Saving shows a toast and the article page. |
| 5 | 0:30 | **Edit.** Click **Edit Article**, change status to **Published**, then **Save changes**. |
| 6 | 0:30 | **Search and filter.** Open the Knowledge base and type `vpn`. Results update while typing and the match is highlighted. Click the **Published** chip, then switch list and table view. |
| 7 | 0:45 | **USER finds the solution.** Sign in as *Regular User*, search `wifi` and open *Wi-Fi Troubleshooting*. The user sees no Drafts, no New article and no Edit or Delete. |
| 8 | 0:45 | **Role restrictions are real.** As USER, type `localhost:8082/articles/new` and then `/admin/users` in the address bar. Both are refused with a message. Say: "Hiding buttons is not security. The server checks the role, and an integration test sends forbidden POST requests directly to prove it." |
| 9 | 0:30 | **ADMIN deletes.** Sign in as Admin, open `DEMO – VPN not connecting`, click **Delete article**, then confirm in the dialog. A toast appears and the article is gone. |
| 10 | 1:00 | **Maven tests.** Show `evidence/final-verification-2026-11-01/maven-test-results.txt`, or run `mvn -B test`, which takes about 1 minute. There are 25 tests: 8 unit, 11 HTTP integration and 6 Selenium. Skipped Selenium tests are not counted as passed. Show `selenium-screenshots/`. |
| 11 | 0:45 | **Docker.** Run `docker ps` to show "(healthy)", `docker image ls knowledge-base-helpdesk` to show the versioned tags, and `curl http://localhost:8082/actuator/health`. Mention the health check and the non-root user. |
| 12 | 0:30 | **Jenkins.** Show the stage view of a green build, if one exists. Otherwise show the `Jenkinsfile` stages and say honestly that the job is configured but was not run before the demo. |
| 13 | 1:15 | **Ansible.** In WSL, run the deploy twice to show `changed=0` the second time, which is idempotency. Then run `rollback-demo.yml`: the bad image fails its health check and the old version comes back with `rescued=1`. Commands are below. |
| 14 | 0:45 | **GitHub.** Show the `feature/*` branches, PR #1 and PR #2, the `develop` and `main` branches, the `v1.0.0` tag, and the merge-conflict demo commit `9555e3b`. |

### Commands for steps 10–13

```powershell
mvn -B test
docker ps
docker image ls knowledge-base-helpdesk
curl.exe http://localhost:8082/actuator/health
```

```bash
cd /mnt/g/knowledge-base-helpdesk
export ANSIBLE_CONFIG=ansible/ansible.cfg
D="-e docker_cli=/mnt/g/knowledge-base-helpdesk/ansible/docker-desktop.sh"
ansible-playbook -i ansible/inventory.ini ansible/deploy.yml $D
ansible-playbook -i ansible/inventory.ini ansible/deploy.yml $D          # changed=0
ansible-playbook -i ansible/inventory.ini ansible/rollback-demo.yml $D   # rescued=1
```

```powershell
git log --oneline --graph --all -25
git tag -n
git branch -a
```

## Likely questions

- **Where is the database?** It is in memory, so the MVP stays simple and every run starts from known data. A database such as PostgreSQL could replace the service layer without changing the pages.
- **How are passwords stored?** As BCrypt hashes. The session ID is changed at login, and the session cookie is HttpOnly and SameSite=Lax.
- **Why Chrome in Docker for Selenium?** Local Chrome start-up was unreliable on this machine. The container gives the same browser everywhere, Jenkins included.
- **Is remote provisioning done?** No. Ansible deploys to this machine's Docker. Adding a server means adding a host to the inventory and an SSH key; the playbooks stay the same.

## DevOps concepts in one sentence each

| Concept | Simple explanation | Where in this project |
|---|---|---|
| Version control (Git) | Every change is saved as a commit, so you can see who changed what and go back. | Commit history, `git log` |
| Branching and pull requests | New work happens on a separate branch and is reviewed before it joins the main code. | `feature/*` branches, PR #1 and #2 |
| Merge conflict | Two branches change the same lines, and a person has to decide the final version. | `demo/conflict-*`, commit `9555e3b` |
| Release tag | A permanent name for the exact code that was released. | `v1.0.0` |
| Build automation (Maven) | One command downloads libraries, compiles, tests and packages the app. | `pom.xml`, `mvn clean verify` |
| Unit test | Checks one small piece of code in isolation. | `ArticleServiceTest` |
| Integration test | Starts the real app and checks that the pieces work together over HTTP. | `AccessControlIntegrationTest` |
| UI test (Selenium) | A robot browser clicks through the app like a user. | `HelpdeskSeleniumTest` |
| Continuous Integration (Jenkins) | Every change is automatically built and tested, so problems are found early. | `Jenkinsfile` |
| Fail fast / quality gate | If a test fails, the pipeline stops before anything is deployed. | Jenkins stages |
| Containerization (Docker) | The app and its Java runtime are packed into an image that runs the same everywhere. | `Dockerfile` |
| Health check | An automatic "are you OK?" request; Docker marks the container healthy or unhealthy. | `HEALTHCHECK`, `/actuator/health` |
| Image versioning | Each build gets its own tag, so you know exactly what is running and can go back. | `1.0.1`, `1.0.2`, `ci-<build>` |
| Configuration management (Ansible) | You describe the desired state, and Ansible makes the machine match it. | `ansible/deploy.yml` |
| Idempotency | Running the same deployment again changes nothing if everything is already correct. | Second run shows `changed=0` |
| Rollback | When a new version is broken, return automatically to the last working version. | `ansible/rollback-demo.yml` |
| Infrastructure as Code | Pipeline, image and deployment are text files in Git, reviewed like code. | `Jenkinsfile`, `Dockerfile`, `ansible/` |
| Secrets management | Passwords and tokens stay in Jenkins credentials, never in Git. | Credential ID `dockerhub` |
