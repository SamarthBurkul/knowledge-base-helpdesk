# Knowledge Base Helpdesk - DevOps Workflow

## DevOps Lifecycle

The Knowledge Base Helpdesk follows the following DevOps lifecycle:

```text
PLAN
  |
  v
Problem Definition
User Stories
Product Backlog
  |
  v
DEVELOP
  |
  v
Feature Branch
Code Development
Git Commit
  |
  v
CODE REVIEW
  |
  v
Pull Request
Review
Merge
  |
  v
CONTINUOUS INTEGRATION
  |
  v
Jenkins
  |
  +----> Maven Build
  |
  +----> Unit Tests
  |
  +----> Selenium Tests
  |
  v
PACKAGE
  |
  v
Docker Image Build
Docker Image Versioning
  |
  v
CONTINUOUS DELIVERY / DEPLOYMENT
  |
  v
Docker Hub
  |
  v
Ansible Provisioning
  |
  v
Target Server
  |
  v
APPLICATION RUNNING
  |
  v
MONITOR / VALIDATE
  |
  +----> Health Check
  |
  +----> Deployment Validation
  |
  +----> Idempotency Check
  |
  +----> Rollback / Recovery
