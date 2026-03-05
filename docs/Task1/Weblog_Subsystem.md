# Weblog and Content Subsystem: Detailed Design Recovery

## 1. Subsystem Overview
The **Weblog and Content Subsystem** is responsible for managing the core blogging entities (Blogs, Entries, Comments, Categories) and orchestrating the rendering of public blog pages. It sits at the intersection of persistence (Managers) and presentation (Servlets/Rendering).

## 2. Structural Analysis (Class Decomposition)

### A. Core Domain Entities (POJOs)

#### `org.apache.roller.weblogger.pojos.Weblog`
**Role**: Aggregate root representing a website/blog.
*   **Key Fields**:
    *   **Identity**: `id` (UUID), `handle` (URL slug, unique), `name`, `tagline`.
    *   **Configuration**: `locale`, `timeZone`, `editorTheme`, `enableBloggerApi`, `editorPage`, `bannedwordslist`.
    *   **Access Control**: `active` (globally enabled), `visible` (publicly listed).
    *   **Policies**: `allowComments` (global switch), `defaultAllowComments` (per-entry default), `defaultCommentDays` (auto-close window), `moderateComments`, `emailComments`, `emailAddress`.
    *   **Metadata**: `dateCreated`, `lastModified`, `creator` (username), `analyticsCode`, `iconPath`, `about`.
    *   **I18n**: `enableMultiLang`, `showAllLangs`.
*   **Relationships**:
    *   **Aggregation**: `weblogCategories` (List<WeblogCategory>), `bookmarkFolders` (List<WeblogBookmarkFolder>).
    *   **Association**: `bloggerCategory` (default category for API), `creator` (User).
*   **Behavior**:
    *   `hasUserPermissions(User, List<String>)`: Delegated permission check via `UserManager`.
    *   `getLocaleInstance()` / `getTimeZoneInstance()`: Converters for raw string properties.
    *   `getRecentWeblogEntries(...)`, `getRecentComments(...)`, `getPopularTags(...)`: **Active Record** style fetchers delegating to `WeblogEntryManager`.
    *   `getTheme()`: Delegates to `ThemeManager` via `WebloggerFactory`.

#### `org.apache.roller.weblogger.pojos.WeblogEntry`
**Role**: Represents a single blog post.
*   **Key Fields**:
    *   **Identity**: `id`, `anchor` (permalink slug), `link` (for link-blogs).
    *   **Content**: `title`, `text` (body), `summary` (excerpt), `contentType`, `contentSrc`.
    *   **State**: `status` (Enum: `DRAFT`, `PUBLISHED`, `PENDING`, `SCHEDULED`), `pubTime`, `updateTime`.
    *   **Settings**: `allowComments`, `commentDays`, `rightToLeft`, `pinnedToMain`.
    *   **Metadata**: `creatorUserName`, `locale`, `searchDescription`.
*   **Relationships**:
    *   **Composition**: `attSet` (Set<WeblogEntryAttribute>), `tagSet` (Set<WeblogEntryTag>), `addedTags` / `removedTags` (dirty tracking).
    *   **Association**: `website` (Weblog), `category` (WeblogCategory).
*   **Behavior**:
    *   `addTag(String)`: Normalizes tag name (lowercase, locale-aware) and manages dirty sets.
    *   `getCommentsStillAllowed()`: Complex logic checking site config + blog config + entry config + expiration date vs. current date.
    *   `getPermalink()`: Delegates URL generation to `UrlStrategy`.

#### `org.apache.roller.weblogger.pojos.WeblogEntryComment`
**Role**: A user comment on an entry.
*   **Key Fields**:
    *   **Identity**: `id`.
    *   **Author identity**: `name`, `email`, `url`, `remoteHost`.
    *   **Content**: `content`, `contentType`, `postTime`.
    *   **Status**: `status` (Enum: `APPROVED`, `DISAPPROVED`, `SPAM`, `PENDING`).
    *   **Tracking**: `referrer`, `userAgent`, `plugins`, `notify` (subscribe to thread).
*   **Relationships**: `weblogEntry` (Parent).

### B. Business Logic Services (Managers)

#### `org.apache.roller.weblogger.business.WeblogManager`
**Role**: Management of Blog containers and Templates.
*   **Operations**:
    *   **Method**: `addWeblog(Weblog)`, `saveWeblog(Weblog)`, `removeWeblog(Weblog)`.
    *   **Lookup**: `getWeblog(id)`, `getWeblogByHandle(handle, enabled)`, `getWeblogs(...)` (criteria-based list), `getUserWeblogs(User)`.
    *   **Stats**: `getMostCommentedWeblogs(...)`, `getWeblogHandleLetterMap()` (A-Z directory).
    *   **Template Mgmt**: `saveTemplate`, `removeTemplate`, `getTemplate`, `getTemplateByAction`, `getTemplateByLink` (custom pages).

#### `org.apache.roller.weblogger.business.WeblogEntryManager`
**Role**: Management of Content (Entries, Comments, Categories, Tags).
*   **Operations**:
    *   **Entry CRUD**: `saveWeblogEntry`, `removeWeblogEntry`, `getWeblogEntry`.
    *   **Entry Query**: `getWeblogEntries(WeblogEntrySearchCriteria)` (The primary search API), `getWeblogEntriesPinnedToMain`, `getNextEntry`, `getPreviousEntry`.
    *   **Comment Mgmt**: `saveComment`, `removeComment`, `getComments(CommentSearchCriteria)`, `removeMatchingComments`.
    *   **Category Mgmt**: `saveWeblogCategory`, `removeWeblogCategory`, `moveWeblogCategoryContents`.
    *   **Tag Mgmt**: `getPopularTags`, `getTagComboExists`.
    *   **Hit Counting**: `incrementHitCount`, `getHitCountByWeblog`, `getHotWeblogs`.

### C. Request Handling & Rendering

#### `org.apache.roller.weblogger.ui.rendering.servlets.PageServlet`
**Role**: Main entry point for rendering blog content.
*   **Dependencies**:
    *   `WeblogPageCache` (Singleton Access): Caches rendered pages for logged-out / clean requests.
    *   `SiteWideCache` (Singleton Access): Caches site-wide (planet) content.
    *   `WebloggerFactory`: Access to Managers.
    *   `RendererManager`: Selects rendering engine (Velocity/JSP).
*   **Flow (`doGet`)**:
    1.  **Referrer Check**: Calls `processReferrer()` to detect spam refs or robots.
    2.  **Request Parsing**: Instantiates `WeblogPageRequest` to parse URL segments.
    3.  **Last-Modified Check**: Handles `304 Not Modified` for standard hits.
    4.  **Cache Lookup**: Checks `WeblogPageCache` using generated key. Returns immediately on HIT.
    5.  **Template Resolution**:
        *   Checks request context (`page`, `tags`, `entry`, `search`).
        *   Lookups appropriate `ThemeTemplate` from `Weblog.getTheme()`.
    6.  **Validation**: Verifies `isActive`, `isPublished`, `locale` match, etc. 404s if invalid.
    7.  **Model Loading**: Populates a `Map<String, Object> model` using `ModelLoader`.
    8.  **Rendering**: Uses `Renderer.render(model, writer)`.
    9.  **Cache Put**: Stores result in `WeblogPageCache`.

#### `org.apache.roller.weblogger.ui.rendering.util.WeblogRequest` -> `WeblogPageRequest`
**Role**: Encapsulates URL parsing logic.
*   **`WeblogRequest` (Base)**:
    *   Parses: `/<handle>[/locale][/extra]`
    *   Logic: Extracts `weblogHandle` and optional `locale` (e.g. `en_US`).
    *   Lookup: Fetches `Weblog` object via `WeblogManager`.
*   **`WeblogPageRequest` (Concrete)**:
    *   Parses: `/entry/<anchor>`, `/date/<YYYYMMDD>`, `/category/<name>`, `/tags/<tag>`, `/page/<custom>`.
    *   Logic: Decodes URL-encoded parameters, handles query params (`?tags=...`), validating date formats.
    *   Lookup: Lazy-loads `WeblogEntry`, `WeblogCategory`, `ThemeTemplate` only when requested via getters (avoiding database hits if valid cache exists).

## 3. Key Interactions
*   **The "Active Record" Shortcut**: `Weblog` and `WeblogEntry` objects are not simple POJOs; they contain business logic that calls back into the `WeblogEntryManager`. For example, calling `weblog.getRecentWeblogEntries()` triggers a DB query via `WebloggerFactory`.
*   **Cache-First Rendering**: The `PageServlet` is aggressively optimized. It attempts to serve from memory (`WeblogPageCache`) before even resolving templates or data models.
*   **Theme/Template Abstraction**: Content is never rendered directly by the Servlet. It is always mediated through a `ThemeTemplate` (which could be file-based or database-stored) and a `Renderer`.
