# User and Role Management Subsystem: Detailed Design Recovery

## 1. Subsystem Overview
The **User and Role Management Subsystem** manages the identity lifecycle, authentication protocols, and granular authorization policies of Apache Roller. It is implemented primarily in the `org.apache.roller.weblogger.business` (Service) and `org.apache.roller.weblogger.ui.core.security` (Security) packages.

## 2. Structural Analysis (Class Decomposition)

### A. Core Identity & Domain (POJOs)

#### Class: `org.apache.roller.weblogger.pojos.User`
**Stereotype**: `<<Domain>>`
*   **Implements**: `java.io.Serializable`
*   **Key Fields**:
    *   `id`: `String` (UUID generated via `UUIDGenerator.generateUUID()`).
    *   `userName`: `String` (Unique identifier).
    *   `password`: `String` (Encrypted via `PasswordEncoder` in `RollerContext`).
    *   `openIdUrl`: `String` (Optional, for OpenID authentication).
    *   `screenName`, `fullName`, `emailAddress`: `String` (Profile content).
    *   `locale`: `String` (e.g., "en_US").
    *   `timeZone`: `String` (e.g., "America/Los_Angeles").
    *   `enabled`: `Boolean` (Controls login access).
    *   `activationCode`: `String` (Used in `Register.activate()` flow).
*   **Key Methods**:
    *   `hasGlobalPermission(action: String)`: Convenience delegator to `UserManager.checkPermission()`.
    *   `resetPassword(newPassword: String)`: Hashes password using Spring Security `PasswordEncoder`.
*   **Code Evidence**: Defined in `User.java`. Used as `UserDetails` conversion source in `RollerUserDetailsService`.

#### Class: `org.apache.roller.weblogger.pojos.UserRole`
**Stereotype**: `<<Domain>>`
*   **Implements**: `java.io.Serializable`
*   **Key Fields**:
    *   `userName`: `String` (Foreign Key logical link to `User`).
    *   `role`: `String` (e.g., "admin", "editor").
*   **Code Evidence**: Defined in `UserRole.java`. Persisted via `JPAUserManagerImpl` to table `roller_user_role`.

### B. Authorization & Permissions
Roller extends Java's `java.security.Permission` model.

#### Abstract Class: `org.apache.roller.weblogger.pojos.RollerPermission`
*   **Extends**: `java.security.Permission`
*   **Responsibility**: Base class handling action string parsing (comma-separated).
*   **Key Methods**:
    *   `implies(Permission)`: Abstract method implementing hierarchy check.
    *   `getActionsAsList()`: Helper using `Utilities.stringToStringList`.

#### Class: `org.apache.roller.weblogger.pojos.GlobalPermission`
*   **Extends**: `RollerPermission`
*   **Scope**: Application-wide (System Admin vs User).
*   **Actions (Constants)**:
    *   `ADMIN` ("admin"): Superuser.
    *   `WEBLOG` ("weblog"): Can create blogs.
    *   `LOGIN` ("login"): Basic access.
*   **Logic**: `implies()` implementation enforces `ADMIN > WEBLOG > LOGIN`.
*   **Code Evidence**: `UserAdmin.java` checks `GlobalPermission.ADMIN`.

#### Class: `org.apache.roller.weblogger.pojos.WeblogPermission`
*   **Extends**: `ObjectPermission` (adds `objectId` for Weblog Handle).
*   **Scope**: Specific to a single Weblog instance.
*   **Actions (Constants)**:
    *   `ADMIN` ("admin"): Blog owner settings.
    *   `POST` ("post"): Can publish entries.
    *   `EDIT_DRAFT` ("edit_draft"): Can save drafts.
*   **Logic**: `implies()` enforces `ADMIN > POST > EDIT_DRAFT`.

### C. Business Logic Services (Managers)

#### Interface: `org.apache.roller.weblogger.business.UserManager`
**Stereotype**: `<<Service>>`
*   **Responsibilities**:
    1.  **Identity Management**:
        *   `addUser(User)`: Persists new user.
        *   `saveUser(User)`: Updates profile/password.
        *   `getUserByUserName(String)`: Primary lookup for login.
        *   `getUserByActivationCode(String)`: Used by Registration flow.
    2.  **Authorization**:
        *   `checkPermission(RollerPermission, User)`: Central authorization point.
        *   `getRoles(User)`: Retrives string roles.
    3.  **Weblog Access**:
        *   `grantWeblogPermission(...)`, `revokeWeblogPermission(...)`.

#### Class: `org.apache.roller.weblogger.business.jpa.JPAUserManagerImpl`
**Stereotype**: `<<Service Impl>>`
*   **Implements**: `UserManager`.
*   **Persistence**: Uses `JPAPersistenceStrategy` to manage entities.
*   **Cache**: Maintains `userNameToIdMap` caching for performance.
*   **Key Logic**:
    *   `addUser`: Checks for existing users to potentially grant `admin` role to first user.
    *   `checkPermission`: Implements hybrid check: first looks for specific `WeblogPermission`, then falls back to checking `GlobalPermission`.

#### Class: `org.apache.roller.weblogger.ui.core.security.RollerUserDetailsService`
**Stereotype**: `<<SpringSecurity>>`
*   **Implements**: `org.springframework.security.core.userdetails.UserDetailsService`.
*   **Logic**:
    1.  Intercepts Spring Login.
    2.  Calls `WebloggerFactory.getWeblogger().getUserManager()`.
    3.  Calls `getUserByUserName()`.
    4.  Converts Roller `User` to Spring `org.springframework.security.core.userdetails.User`.
    5.  Maps Roller Roles to `SimpleGrantedAuthority`.
*   **Code Evidence**: `loadUserByUsername` method explicitly handles OpenID vs Database logic.

#### Class: `org.apache.roller.weblogger.ui.core.security.CustomUserRegistry`
**Stereotype**: `<<SecurityUtil>>`
*   **Role**: Utility to extract Role and Profile attributes from SSO/LDAP requests when using external authentication.
*   **Usage**: Called by `Register` action to pre-fill user forms.

### D. UI & Action Layer

#### Class: `org.apache.roller.weblogger.ui.struts2.core.Register`
**Stereotype**: `<<Controller>>`
*   **Extends**: `UIAction`.
*   **Context**: Publicly accessible (overrides `isUserRequired()` -> `false`).
*   **Methods**:
    *   `execute()`: Prepares the form. Handles SSO attribute pre-filling via `CustomUserRegistry`.
    *   `save()`: The main registration logic.
        *   Validates: `myValidate()` checks username safety (`CharSetUtils`), duplicates (`checkUsername`), and password match.
        *   Persists: Creates `User` POJO, sets `ActivationCode` (UUID), calls `UserManager.addUser()`.
        *   Notifies: `sendActivationMailIfNeeded()`.
    *   `activate()`: Processes email link clicking via `activationCode`.
*   **Dependencies**: Directly depends on `UserManager` for all persistence checks.

#### Class: `org.apache.roller.weblogger.ui.struts2.core.Login`
**Stereotype**: `<<Controller>>`
*   **Extends**: `UIAction`.
*   **Role**: Handles the login form view and initial error processing (actual auth handled by Spring Security).

#### Class: `org.apache.roller.weblogger.ui.struts2.core.Profile`
**Stereotype**: `<<Controller>>`
*   **Extends**: `UIAction`.
*   **Role**: Allows authenticated users to edit their own profile fields.
*   **Logic**: Uses `getAuthenticatedUser()` (from base) to ensure users can only edit themselves.

## 3. Relationship Explanations (Justification)

*   **Inheritance**: `GlobalPermission` and `WeblogPermission` extend `RollerPermission` (via `ObjectPermission`) to share the `actions` string parsing logic and integration with Java's Security API (`implies` method).
*   **Realization**: `JPAUserManagerImpl` implements `UserManager`, providing the concrete JPA-based logic for the interface contract.
*   **Association (User <-> UserRole)**: The `User` entity does not contain a list of roles directly in its POJO. Instead, `UserManager` acts as the mediator, retrieving `UserRole` lists when needed. This is a decoupled association to keep the `User` object lightweight.
*   **Dependency (RollerUserDetailsService -> UserManager)**: The Security Service does not own data; it serves as an adapter. It *depends* on `UserManager` to fetch the raw data it needs to construct the Spring Security principal.
*   **Dependency (Register -> UserManager)**: The UI Action is a transient controller. It *uses* the `UserManager` to check for duplicates and save the new user, but it does not hold stateful references to it beyond the scope of the request (retrieved via Factory).
*   **Dependency (Register -> CustomUserRegistry)**: `Register` relies on this static utility to extract potential user attributes from the `HttpServletRequest` during SSO/LDAP flows.
*   **Composition (WeblogPermission)**: A `WeblogPermission` is composed of a `User`, a `Weblog` (ID), and a set of actions. It cannot exist meaningfully without these referents.

## 4. Observations
*   **Strength**: The use of `java.security.Permission` API (`implies`) allows for very standard and testable authorization logic.
*   **Code Evidence**: `UserAdmin.java` strictly enforces `GlobalPermission.ADMIN` via `requiredGlobalPermissionActions()`, proving the permission system is deeply integrated into the Struts2 Action lifecycle.
*   **JPA Strategy**: The `JPAUserManagerImpl` demonstrates a clear Strategy pattern usage via `JPAPersistenceStrategy`, decoupling the specific ORM calls from the business logic.
