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

StarAction.java — 8 direct manager calls:

``` java
// starWeblog()
WebloggerFactory.getWeblogger().getWeblogManager().getWeblog(weblogId);
WebloggerFactory.getWeblogger().getWeblogManager().starWeblog(user, weblog);

// starEntry()
WebloggerFactory.getWeblogger().getWeblogEntryManager().getWeblogEntry(entryId);
WebloggerFactory.getWeblogger().getWeblogEntryManager().starEntry(user, entry);

// unstarWeblog(), unstarEntry() — same pattern, 4 more calls
```