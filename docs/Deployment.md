# Local Docker and Ansible Deployment

## Scope and target

The checked-in inventory targets only `localhost` in Ubuntu WSL. No remote
Ubuntu VM, address, SSH account, or private key was available to configure or
test. This is an isolated local assignment target; it is not production
provisioning. The deployment container uses host port 18084 and the name
`helpdesk-ansible`, so the existing application on port 8082 is not replaced.

Enable Ubuntu under Docker Desktop **Settings > Resources > WSL Integration**
before running the playbooks. Ansible and Docker CLI are present in the Ubuntu
WSL distribution; the playbooks fail at their Docker preflight if that
distribution cannot reach the Docker Desktop engine. Keep the existing Docker
images and WSL data intact.

## Deploy and check idempotency

From Ubuntu WSL:

```bash
cd /mnt/g/knowledge-base-helpdesk
ANSIBLE_CONFIG=ansible/ansible.cfg ansible-playbook ansible/deploy.yml
ANSIBLE_CONFIG=ansible/ansible.cfg ansible-playbook ansible/deploy.yml
```

The playbook requires the local `knowledge-base-helpdesk:1.0.1` image, verifies
the Docker engine, ensures the named container uses the requested image and
port, and waits for `/actuator/health` to report `UP`. The second run should
report `changed=0`; if image or port settings differ, it replaces only the
named local Ansible test container.

To use another locally available application tag or port:

```bash
ANSIBLE_CONFIG=ansible/ansible.cfg ansible-playbook ansible/deploy.yml \
  -e app_image=knowledge-base-helpdesk:1.0.1 \
  -e app_port=18084
```

## Safe rollback demonstration

The rollback demonstration operates on the same isolated container and port.
It deliberately starts the locally available `nginx:alpine` image with the
Helpdesk container port mapping, causing application health validation to
fail. The rescue tasks remove that candidate, restore
`knowledge-base-helpdesk:1.0.1`, and verify the restored health endpoint:

```bash
ANSIBLE_CONFIG=ansible/ansible.cfg ansible-playbook ansible/rollback-demo.yml
```

The existing app on port 8082 is not modified. Do not treat this local
demonstration as a production rollback test.

## Remote deployment requirements

Before targeting a remote Ubuntu host, add an explicitly scoped host/group to
the inventory, configure SSH user/key through Jenkins credentials or an
external secret store, and test reachability and privilege escalation. Do not
commit private keys or passwords. Docker Hub publishing likewise requires a
configured Jenkins username/password credential and a confirmed repository
namespace/tag policy; neither can be inferred from this project.
