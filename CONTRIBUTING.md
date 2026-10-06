# Contributing and Branching Rules

## Branch Strategy

The project uses the following branches:

### main

Stable and release-ready code only.

### develop

Integration branch for completed feature work.

### feature/*

Used for new functionality.

Examples:

- feature/login
- feature/article-search
- feature/create-article
- feature/admin-dashboard

### bugfix/*

Used for fixing defects.

Examples:

- bugfix/login-validation
- bugfix/article-search

### hotfix/*

Used for urgent fixes to the stable release.

Example:

- hotfix/security-fix

## Workflow

1. Create a feature or bugfix branch from `develop`.
2. Implement the change.
3. Commit with a meaningful commit message.
4. Push the branch to GitHub.
5. Create a Pull Request into `develop`.
6. Review the changes.
7. Merge only after review and successful CI checks.
8. Release-ready changes are merged from `develop` into `main`.

## Commit Message Examples

- `Add user authentication`
- `Implement article search`
- `Fix article validation`
- `Add Selenium login test`
- `Configure Jenkins pipeline`

## Rules

- Do not directly develop features on `main`.
- Do not commit passwords, tokens or other secrets.
- Keep commits focused on one logical change.
- Pull Requests should describe the change and testing performed.
