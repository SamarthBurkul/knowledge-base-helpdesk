# Knowledge Base Helpdesk - User Stories

## US-01 - User Login

As a User, I want to log in to the Knowledge Base Helpdesk so that I can access the system securely.

### Acceptance Criteria
- Valid username and password allow successful login.
- Invalid credentials are rejected.
- Successful login redirects the user to the dashboard.
- The user gets access according to their assigned role.

## US-02 - Search Knowledge Articles

As a User, I want to search knowledge articles so that I can quickly find a solution to my problem.

### Acceptance Criteria
- User can enter a keyword.
- Matching articles are displayed.
- Search is case-insensitive.
- A suitable message is displayed when no article matches.

## US-03 - View Knowledge Article

As a User, I want to view a knowledge article so that I can understand the solution to my problem.

### Acceptance Criteria
- Article title is displayed.
- Article description is displayed.
- Solution steps are displayed.
- Article information is readable and complete.

## US-04 - Create Knowledge Article

As Support Staff, I want to create a knowledge article so that new solutions can be shared with users.

### Acceptance Criteria
- Authorized staff can open the create article form.
- Required fields are validated.
- Article is saved successfully.
- Newly created article can be searched and viewed.

## US-05 - Update Knowledge Article

As Support Staff, I want to update a knowledge article so that outdated or incorrect information can be corrected.

### Acceptance Criteria
- Authorized staff can edit an article.
- Updated fields are validated.
- Changes are saved successfully.
- Updated content is displayed when the article is viewed.

## US-06 - Delete Knowledge Article

As an Administrator, I want to delete obsolete articles so that the knowledge base remains accurate.

### Acceptance Criteria
- Only authorized administrators can delete articles.
- A confirmation is requested before deletion.
- Deleted articles are no longer available to users.

## US-07 - User and Role Management

As an Administrator, I want to manage users and roles so that access to the system can be controlled.

### Acceptance Criteria
- Administrator can view users.
- Administrator can assign or change roles.
- Unauthorized users cannot access administrative functions.

## US-08 - Dashboard

As an Administrator, I want to view a dashboard so that I can understand the current state of the knowledge base.

### Acceptance Criteria
- Dashboard loads successfully.
- Article count is displayed.
- User information or relevant system statistics are displayed.
