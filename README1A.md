# Task 1A — Star Feature & Facade Pattern Refactoring
## Apache Roller | Branch: P2-1

---

## Overview

Task 1A adds a **Star (Favourite) feature** to Apache Roller. Logged-in users
can star/unstar any Weblog or WeblogEntry via AJAX. Their starred content
appears on the home page and on a dedicated Starred Entries page.

The codebase was then refactored to apply the **Facade design pattern**,
consolidating all star-related UI-to-business-layer calls behind a single
`StarFacade` class.

---

## Feature: What It Does

| Action | How |
|--------|-----|
| Star a blog | User clicks ★ on any weblog — AJAX POST to `/roller-ui/starWeblog` |
| Unstar a blog | User clicks ★ again — AJAX POST to `/roller-ui/unstarWeblog` |
| Star a post | User clicks ★ on any entry — AJAX POST to `/roller-ui/starEntry` |
| Unstar a post | User clicks ★ again — AJAX POST to `/roller-ui/unstarEntry` |
| View starred blogs | Home page (MainMenu) shows starred weblogs sorted by most recent post |
| View starred posts | Dedicated `/roller-ui/starredEntries` page with pagination (5 per page) |

---

## Files Created

| File | Purpose |
|------|---------|
| `ui/struts2/ajax/StarAction.java` | Struts2 AJAX action — handles all 4 star/unstar HTTP requests |
| `ui/struts2/ajax/GetSaltAction.java` | Returns a fresh CSRF token after each AJAX call |
| `ui/struts2/core/StarredEntries.java` | Action for the starred entries page (with pagination) |
| `ui/struts2/core/StarredEntries.jsp` | JSP view for the starred entries page |
| `pojos/StarredWeblogEntry.java` | DTO — carries a `Weblog` + its latest post `Date` together |
| `business/StarFacade.java` | **Facade** — single entry point for all star subsystem operations |

---

## Files Modified

| File | What Changed |
|------|-------------|
| `pojos/User.java` | Added `starredWeblogs` and `starredEntries` `@ManyToMany` collections |
| `pojos/Weblog.java` | Added `starredByUsers` — reverse side of the weblog star relationship |
| `pojos/WeblogEntry.java` | Added `starredByUsers` — reverse side of the entry star relationship |
| `business/WeblogManager.java` | Added 4 interface methods: `starWeblog`, `unstarWeblog`, `isWeblogStarredByUser`, `getStarredWeblogsSortedByRecency` |
| `business/jpa/JPAWeblogManagerImpl.java` | Implemented the 4 weblog star methods with JPQL named queries |
| `business/WeblogEntryManager.java` | Added 6 interface methods: `starEntry`, `unstarEntry`, `isEntryStarredByUser`, `getStarredEntriesForUser` (×2), `countStarredEntriesForUser`, `getTrendingEntries` |
| `business/jpa/JPAWeblogEntryManagerImpl.java` | Implemented all 6 entry star methods |
| `ui/struts2/core/MainMenu.java` | `execute()` now loads starred weblogs for the home page |
| `ui/rendering/model/PageModel.java` | Added `isWeblogStarred()`, `isEntryStarred()` (×2 overloads), `getSalt()`, `getContextPath()` |
| `struts.xml` | Added action mappings for star, unstar, getSalt, starredEntries |
| `sql/createdb.sql` | Added `CREATE TABLE` for join tables (`user_starred_weblog`, `user_starred_entry`) |
| `sql/droptables.sql` | Added `DROP TABLE` for join tables |
| `persistence.xml` | Added `hbm2ddl.auto=update` for test DB schema auto-creation |
| `weblog.vm` (macro) | Added `#showWeblogStarControls` and `#showEntryStarControls` macros |
| All 4 theme templates | Added macro calls so every theme shows the star button |
| `MainMenu.jsp` | Added starred blogs list and link to starred entries page |
| `tiles.xml` | Added `StarredEntries` tile definition |

---

## DB Schema

Two new join tables are created automatically:

```sql
-- Users ↔ Weblogs (many-to-many)
CREATE TABLE user_starred_weblog (
    user_id    VARCHAR(48) NOT NULL REFERENCES rolleruser(id),
    weblog_id  VARCHAR(48) NOT NULL REFERENCES weblog(id),
    PRIMARY KEY (user_id, weblog_id)
);

-- Users ↔ WeblogEntries (many-to-many)
CREATE TABLE user_starred_entry (
    user_id   VARCHAR(48) NOT NULL REFERENCES rolleruser(id),
    entry_id  VARCHAR(48) NOT NULL REFERENCES weblogentry(id),
    PRIMARY KEY (user_id, entry_id)
);
```

## Design Pattern: Facade
The Problem (Actual Code — Before Refactoring)
Before StarFacade was introduced, 4 separate UI classes all reached
directly into the business layer, knowing which manager owns which operation:

#### StarAction.java — 8 direct manager calls:

``` java
// starWeblog()
WebloggerFactory.getWeblogger().getWeblogManager().getWeblog(weblogId);
WebloggerFactory.getWeblogger().getWeblogManager().starWeblog(user, weblog);

// starEntry()
WebloggerFactory.getWeblogger().getWeblogEntryManager().getWeblogEntry(entryId);
WebloggerFactory.getWeblogger().getWeblogEntryManager().starEntry(user, entry);

// unstarWeblog(), unstarEntry() — same pattern, 4 more calls
```

#### MainMenu.java — Line 67–69:

``` java
starredWeblogs = WebloggerFactory.getWeblogger()
        .getWeblogManager()
        .getStarredWeblogsSortedByRecency(getAuthenticatedUser());
```
#### StarredEntries.java — Line 67–69:

``` java
allStarredWeblogs = WebloggerFactory.getWeblogger()
        .getWeblogManager()
        .getStarredWeblogsSortedByRecency(getAuthenticatedUser());
```
#### PageModel.java — Lines 366–410:

``` java
// isWeblogStarred()
WebloggerFactory.getWeblogger().getWeblogManager()
        .isWeblogStarredByUser(pageRequest.getUser(), weblog);

// isEntryStarred()
WebloggerFactory.getWeblogger().getWeblogEntryManager()
        .isEntryStarredByUser(pageRequest.getUser(), entry);

// isEntryStarred(WeblogEntryWrapper) — 2 more calls
WebloggerFactory.getWeblogger().getWeblogEntryManager().getWeblogEntry(...);
WebloggerFactory.getWeblogger().getWeblogEntryManager().isEntryStarredByUser(...);
```

Total: 13 direct manager call sites across 4 UI classes.

## Coupling Map (Before Facade)

#### StarAction     ──── WeblogManager      (getWeblog, starWeblog, unstarWeblog)
#### StarAction     ──── WeblogEntryManager (getWeblogEntry, starEntry, unstarEntry)
#### MainMenu       ──── WeblogManager      (getStarredWeblogsSortedByRecency)
#### StarredEntries ──── WeblogManager      (getStarredWeblogsSortedByRecency)
#### PageModel      ──── WeblogManager      (isWeblogStarredByUser)
#### PageModel      ──── WeblogEntryManager (isEntryStarredByUser ×2, getWeblogEntry)

---

#### Every UI class knows the internal structure of the business layer.

---

#### The Solution: StarFacade.java
`StarFacade` is a single class that wraps both `WeblogManager` and
`WeblogEntryManager`. UI classes only talk to the facade — they no longer
know which manager handles which operation.

``` java
public class StarFacade {

    private final WeblogManager weblogManager;
    private final WeblogEntryManager entryManager;

    public StarFacade() throws WebloggerException {
        this.weblogManager  = WebloggerFactory.getWeblogger().getWeblogManager();
        this.entryManager   = WebloggerFactory.getWeblogger().getWeblogEntryManager();
    }

    // --- Weblog star operations ---
    public void starWeblog(User user, Weblog weblog)   throws WebloggerException { weblogManager.starWeblog(user, weblog); }
    public void unstarWeblog(User user, Weblog weblog) throws WebloggerException { weblogManager.unstarWeblog(user, weblog); }
    public boolean isWeblogStarred(User user, Weblog weblog) throws WebloggerException {
        return weblogManager.isWeblogStarredByUser(user, weblog);
    }
    public List<StarredWeblogEntry> getStarredWeblogsSortedByRecency(User user) throws WebloggerException {
        return weblogManager.getStarredWeblogsSortedByRecency(user);
    }

    // --- Entry star operations ---
    public void starEntry(User user, WeblogEntry entry)   throws WebloggerException { entryManager.starEntry(user, entry); }
    public void unstarEntry(User user, WeblogEntry entry) throws WebloggerException { entryManager.unstarEntry(user, entry); }
    public boolean isEntryStarred(User user, WeblogEntry entry) throws WebloggerException {
        return entryManager.isEntryStarredByUser(user, entry);
    }
    public List<WeblogEntry> getStarredEntriesForUser(User user) throws WebloggerException {
        return entryManager.getStarredEntriesForUser(user);
    }
}
```

Rule: StarFacade only delegates — zero business logic inside it.
All JPQL, DB access, and business rules stay in the manager implementations.

---

### Caller Changes After Facade

#### StarAction.java:

``` java
// Before (×8 manager calls)
WebloggerFactory.getWeblogger().getWeblogManager().starWeblog(user, weblog);
WebloggerFactory.getWeblogger().getWeblogEntryManager().starEntry(user, entry);

// After (×1 facade call each)
StarFacade starFacade = new StarFacade();
starFacade.starWeblog(user, weblog);
starFacade.starEntry(user, entry);
```

#### MainMenu.java + StarredEntries.java:

``` java
// Before
WebloggerFactory.getWeblogger().getWeblogManager()
        .getStarredWeblogsSortedByRecency(user);

// After
StarFacade starFacade = new StarFacade();
starFacade.getStarredWeblogsSortedByRecency(user);

```

#### PageModel.java:

``` java
// Before
WebloggerFactory.getWeblogger().getWeblogManager()
        .isWeblogStarredByUser(pageRequest.getUser(), weblog);

// After
StarFacade starFacade = new StarFacade();
starFacade.isWeblogStarred(pageRequest.getUser(), weblog);
```
---

### Without the Facade — What Breaks

Adding a new star operation (e.g. starring a comment in Task 2) means
updating StarAction, MainMenu, StarredEntries, and PageModel — a
minimum of 4 files every time instead of only StarFacade.

A bug in manager acquisition (e.g. WebloggerFactory.getWeblogger()
throwing in a specific context) must be fixed in 13 call sites instead of
one constructor.

Unit testing is harder — mocking StarAction requires setting up
2 separate manager mocks; with StarFacade, only 1 mock is needed.

---


| Attribute       | Impact                                                                                       |
| --------------- | -------------------------------------------------------------------------------------------- |
| Maintainability | Star subsystem internals can change without touching any UI class                            |
| Modularity      | UI layer has zero knowledge of whether an operation uses WeblogManager or WeblogEntryManager |
| Extensibility   | New star targets (comments, tags) only require adding methods to StarFacade                  |
| Testability     | UI classes can be tested by injecting a single mock StarFacade instead of two manager mocks  |

---
