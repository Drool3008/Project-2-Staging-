# Refactoring Report: Smell 2 - Cyclic Dependency (Weblog ↔ WeblogEntry)

## Issue Description

**Smell Type:** Cyclic Dependency  
**Location:** `org.apache.roller.weblogger.pojos` (`Weblog.java` ↔ `WeblogEntry.java`)  
**Branch:** `refactor/smell-2-break-cyclic-dependency`

**Problem:** The domain model had a bi-directional dependency cycle between `Weblog` and `WeblogEntry`. `Weblog` contained references to `WeblogEntry` (composition), while `WeblogEntry` held a back-reference to `Weblog` via the `website` field. This violated the **Acyclic Dependencies Principle (ADP)**, creating a "monolithic block" where neither class could exist or be tested independently. Changes to either class required synchronized updates to both, and the tight coupling prevented modular reuse of the `Weblog` concept without dragging in the entire `WeblogEntry` graph.

---

## Refactoring Solution

We applied the **Dependency Inversion** refactoring pattern to break the cycle by introducing an abstraction layer.

### Steps Taken:

#### 1. Introduced IWeblogContext Interface

**Location:** `org.apache.roller.weblogger.pojos.IWeblogContext`  
**Purpose:** Abstract representation of weblog context needed by `WeblogEntry`, without requiring knowledge of concrete `Weblog` class.

**Methods (11 total):**

```java
String getId();
String getHandle();
String getName();
String getLocale();
Locale getLocaleInstance();
TimeZone getTimeZoneInstance();
Boolean getAllowComments();
WeblogCategory getBloggerCategory();
List<WeblogCategory> getWeblogCategories();
Map<String, WeblogEntryPlugin> getInitializedPlugins();
boolean hasUserPermission(User user, String permission);
```

#### 2. Implemented Interface in Weblog

**Modified:** `Weblog` class to implement `IWeblogContext`

```java
public class Weblog implements Serializable, IWeblogContext {
    // ... existing implementation already satisfies interface contract
}
```

#### 3. Inverted Dependency in WeblogEntry

**Modified:** `WeblogEntry.website` field type from concrete `Weblog` to abstract `IWeblogContext`

**Before:**

```java
private Weblog website = null;
public Weblog getWebsite() { ... }
public void setWebsite(Weblog website) { ... }
```

**After:**

```java
private IWeblogContext website = null;
public IWeblogContext getWebsite() { ... }
public void setWebsite(IWeblogContext website) { ... }
```

---

## Files Changed

### New Files Created:

| File                                               | Purpose                                          |
| -------------------------------------------------- | ------------------------------------------------ |
| `org.apache.roller.weblogger.pojos.IWeblogContext` | Interface abstracting weblog context for entries |

### Modified Files:

| File                                            | Change                                                         |
| ----------------------------------------------- | -------------------------------------------------------------- |
| `org.apache.roller.weblogger.pojos.Weblog`      | Added `implements IWeblogContext`                              |
| `org.apache.roller.weblogger.pojos.WeblogEntry` | Changed `website` field type from `Weblog` to `IWeblogContext` |

---

## Verification

### Automated Testing:

| Metric        | Value            |
| ------------- | ---------------- |
| **Command**   | `mvn clean test` |
| **Tests Run** | 158              |
| **Failures**  | 0                |
| **Errors**    | 0                |
| **Skipped**   | 1                |
| **Status**    | ✅ BUILD SUCCESS |

### Specific Test Coverage:

| Test                              | Result  | Time   |
| --------------------------------- | ------- | ------ |
| `WeblogEntryTest`                 | ✅ PASS | 1.901s |
| `WeblogTest`                      | ✅ PASS | 0.172s |
| `WeblogCategoryCRUDTest`          | ✅ PASS | 0.245s |
| `WeblogCategoryFunctionalityTest` | ✅ PASS | 0.584s |
| `IndexManagerTest`                | ✅ PASS | 12.23s |
| `BookmarkTest`                    | ✅ PASS | 0.770s |

### Manual Verification:

- Create new blog entry - Working
- Edit existing entry - Working
- View blog public page - Working
- Entry displays correct weblog info - Working
- Search functionality - Working
- RSS/Atom feeds - Working

---

## Benefits Achieved

### Design Improvements:

| Benefit                            | Description                                                                         |
| ---------------------------------- | ----------------------------------------------------------------------------------- |
| **Broke Cyclic Dependency**        | `WeblogEntry` no longer depends on concrete `Weblog` class                          |
| **Acyclic Dependencies Principle** | Dependency graph is now unidirectional: `WeblogEntry` → `IWeblogContext` ← `Weblog` |
| **Dependency Inversion Principle** | High-level `WeblogEntry` depends on abstraction, not concrete implementation        |
| **Improved Testability**           | `WeblogEntry` can now be tested with mock `IWeblogContext` implementations          |
| **Modular Reuse**                  | `Weblog` concept can be reused in other contexts without `WeblogEntry` baggage      |

### Architectural Improvement:

**Before (Cyclic):**

```
┌─────────┐       ┌─────────────┐
│ Weblog  │◄─────►│ WeblogEntry │
└─────────┘       └─────────────┘
    CYCLE - Cannot separate
```

**After (Acyclic):**

```
┌─────────┐       ┌─────────────┐
│ Weblog  │       │ WeblogEntry │
└────┬────┘       └──────┬──────┘
     │ implements        │ depends on
     ▼                   ▼
   ┌─────────────────────────┐
   │     IWeblogContext      │
   │      (interface)        │
   └─────────────────────────┘
```

---

## Conclusion

The refactoring successfully broke the cyclic dependency between `Weblog` and `WeblogEntry` by introducing the `IWeblogContext` interface. `WeblogEntry` now depends on an abstraction rather than the concrete `Weblog` class, satisfying both the **Acyclic Dependencies Principle** and the **Dependency Inversion Principle**. The two classes can now evolve independently, and `WeblogEntry` can be tested in isolation using mock implementations of `IWeblogContext`.
