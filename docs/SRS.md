# Software Requirements Specification (SRS)

# Knowledge Base Helpdesk

## 1. Introduction

The Knowledge Base Helpdesk is a web-based application designed to provide
a centralized repository of solutions to common technical and operational
problems.

The system allows users to search and view knowledge articles while
authorized support staff can create and update articles. Administrators can
manage users, roles and knowledge articles.

## 2. Problem Statement

Organizations frequently receive repeated requests for common problems.
Existing solutions may be difficult to locate and support staff may need to
solve the same problems repeatedly.

The Knowledge Base Helpdesk provides a centralized and searchable platform
for storing and retrieving these solutions.

## 3. Objectives

- Provide a centralized knowledge repository.
- Allow users to search for solutions quickly.
- Reduce repeated support requests.
- Allow support staff to maintain knowledge articles.
- Provide role-based access control.
- Demonstrate an automated DevOps lifecycle using Git, Jenkins, Docker and Ansible.

## 4. Scope

### In Scope

- Authentication
- Role-based authorization
- User management
- Knowledge article management
- Create article
- View article
- Search article
- Update article
- Delete article
- Administrator dashboard
- Automated testing
- Jenkins CI/CD
- Docker containerization
- Ansible provisioning
- Health checking
- Rollback and recovery

### Out of Scope

- Mobile application
- AI chatbot
- Payment functionality
- Email notification system
- Kubernetes
- Advanced analytics
- Large-scale cloud infrastructure

## 5. User Types

### User / Employee

Can:
- Login
- Search articles
- View articles

### Support Staff

Can:
- Login
- Search articles
- View articles
- Create articles
- Update articles

### Administrator

Can:
- Login
- Search and view articles
- Create, update and delete articles
- Manage users and roles
- View dashboard

## 6. Functional Requirements

### FR-01 Authentication

The system shall authenticate users using valid credentials.

### FR-02 Authorization

The system shall restrict functionality according to the user's role.

### FR-03 Article Creation

Authorized staff shall be able to create knowledge articles.

### FR-04 Article Viewing

Users shall be able to view available knowledge articles.

### FR-05 Article Search

Users shall be able to search articles using keywords.

### FR-06 Article Update

Authorized staff shall be able to update existing articles.

### FR-07 Article Deletion

Authorized administrators shall be able to delete obsolete articles.

### FR-08 User Management

Administrators shall be able to manage users and roles.

### FR-09 Dashboard

Administrators shall be able to view relevant system information.

### FR-10 Automated Testing

The project shall contain automated unit and UI tests.

### FR-11 CI/CD

Jenkins shall build and test the application automatically.

### FR-12 Containerization

The application shall be packaged and executed as a Docker container.

### FR-13 Provisioning

Ansible shall automate deployment and target environment configuration.

### FR-14 Health Validation

The deployed application shall be validated using a health check.

### FR-15 Recovery

The previous stable release shall be recoverable after deployment failure.

## 7. Non-Functional Requirements

### Performance

Common pages and searches should respond within a reasonable time in the
local deployment environment.

### Security

- Passwords must not be stored in plain text.
- Role-based authorization must be enforced.
- Administrative functions must be protected.

### Reliability

The application should remain available after successful deployment and
should support rollback to a known stable version.

### Maintainability

The application should use a structured project architecture and clear
documentation.

### Testability

The system should support automated unit and Selenium testing.

### Deployability

The application should be buildable and deployable through Jenkins,
Docker and Ansible.

## 8. Technology Stack

- Java
- Maven
- Git
- GitHub
- Jenkins
- Selenium
- Docker
- Docker Hub
- Ansible
- Linux/Ubuntu target environment

## 9. MVP

The MVP contains the 15 tasks defined in the project backlog.

## 10. Acceptance

The project is accepted when the functional requirements are implemented,
automated tests pass, the Jenkins pipeline executes successfully, the
application can run in Docker, Ansible can provision the target environment,
health validation succeeds, and rollback/recovery can be demonstrated.
