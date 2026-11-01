# Local Docker and Ansible Deployment

## Scope and target

The inventory targets only `localhost` (Ubuntu in WSL). No remote Ubuntu server, SSH
account or key is available, so **no remote provisioning has been done**. Ansible
controls Docker Desktop on this Windows machine through `ansible/docker-desktop.sh`,
which calls Docker Desktop's `docker.exe` from WSL. Docker Desktop's own WSL
integration is not enabled for Ubuntu, which is why the bridge is used.

| Container | Port | Managed by |
|---|---|---|
| `helpdesk-ansible` | 18084 | `ansible/deploy.yml` and `ansible/rollback-demo.yml` (only this container is ever replaced) |
| `helpdesk-demo` | 8082 | started manually for the demonstration |

## Prerequisites (WSL Ubuntu)

- `ansible-playbook` (verified: ansible-core 2.21.5)
- Docker Desktop running on Windows
- The image to deploy exists locally: `docker image ls knowledge-base-helpdesk`

Two Windows-specific details:

- **Line endings.** `.gitattributes` keeps `*.sh` and `*.yml` as LF. With CRLF endings
  the bridge fails with `/usr/bin/env: 'bash\r': No such file or directory`.
- **Config file.** Ansible ignores `ansible.cfg` in a world-writable directory (all of
  `/mnt/g` is), so set `ANSIBLE_CONFIG` and pass `-i ansible/inventory.ini`.

## Deploy and prove idempotency

```bash
cd /mnt/g/knowledge-base-helpdesk
export ANSIBLE_CONFIG=ansible/ansible.cfg
D="-e docker_cli=/mnt/g/knowledge-base-helpdesk/ansible/docker-desktop.sh"

ansible-playbook -i ansible/inventory.ini ansible/deploy.yml $D   # 1st run: creates/replaces the container
ansible-playbook -i ansible/inventory.ini ansible/deploy.yml $D   # 2nd run: changed=0
```

`deploy.yml` checks the Docker engine and the image, then compares the running
container's **image ID**, running state and port with the desired state. Only if they
differ does it replace `helpdesk-ansible`. It then waits for `/actuator/health` to report
`UP`. Because the comparison uses the image ID, rebuilding the same tag triggers a
redeploy, and an unchanged system reports `changed=0`.

Other image or port: `-e app_image=knowledge-base-helpdesk:1.0.1 -e app_port=18084`.

## Rollback demonstration

```bash
ansible-playbook -i ansible/inventory.ini ansible/rollback-demo.yml $D
```

1. Records the image `helpdesk-ansible` is currently running (the known-good version).
2. Replaces it with a deliberately unsuitable candidate (`nginx:alpine`, which has no
   `/actuator/health`).
3. The health check fails. The `rescue` block removes the candidate, starts the recorded
   known-good image again, and asserts it reports `UP`.

Expected recap: `failed=0 rescued=1`. A following `deploy.yml` run reports `changed=0`.

## Verified results (2026-11-01)

| Run | Recap |
|---|---|
| deploy (new 1.0.2 image) | `ok=10 changed=2 failed=0` |
| deploy again | `ok=8 changed=0 failed=0` |
| rollback demo | `ok=11 changed=4 failed=0 rescued=1` |
| deploy after rollback | `ok=8 changed=0 failed=0` |

Full output: [`evidence/final-verification-2026-11-01/`](../evidence/final-verification-2026-11-01/README.md).

## Adding a remote server later

Add a host group to the inventory (`ansible_host`, `ansible_user`), keep the SSH key in
Jenkins credentials or `ssh-agent` (never in Git), install Docker on the host, copy or
pull the image there (for example from Docker Hub), and run the same playbooks with
`docker_cli=docker`.
