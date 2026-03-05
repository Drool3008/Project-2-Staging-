# Smell 6 Refactoring Report: Procedural Block in DatabaseInstaller

## 1. Smell Description

The `DatabaseInstaller` class contained a method called `upgradeTo400()` that was around 440 lines long. This method handled database migrations from older Roller versions to version 4.0.0. It did too many things in one place - running SQL scripts, updating feed URLs, merging planet groups, populating parent IDs, and more. All this logic was written as one long sequence of steps instead of being split into focused, reusable pieces.

**Location:** `app/src/main/java/org/apache/roller/weblogger/business/startup/DatabaseInstaller.java`

**Problem:** When a method grows this large, it becomes hard to read, test, and maintain. If something breaks, you have to search through hundreds of lines to find the issue. Adding new migration steps means editing this already bloated method.

---

## 2. Refactoring Approach

We used the **Command Pattern** (also known as Task-based decomposition) to break the monolithic method into smaller, independent tasks.

### What we did:

1. Created a `MigrationTask` interface that defines what every migration step must do
2. Extracted each logical operation from `upgradeTo400()` into its own task class
3. Each task handles one specific migration responsibility

### New Files Created:

| File                                | Purpose                                      |
| ----------------------------------- | -------------------------------------------- |
| `MigrationTask.java`                | Interface that all migration tasks implement |
| `RunMigrationScriptTask.java`       | Runs SQL migration scripts                   |
| `UpdateLocalFeedUrlsTask.java`      | Updates local feed URLs in the database      |
| `MergePlanetGroupsTask.java`        | Merges planet groups during upgrade          |
| `PopulateParentidsTask.java`        | Populates parent ID fields                   |
| `PopulatePathsTask.java`            | Populates path information                   |
| `UpgradeCommentPropertiesTask.java` | Upgrades comment plugin properties           |

---

## 3. Code Changes

### 3.1 MigrationTask Interface

```java
package org.apache.roller.weblogger.business.startup;

import java.sql.Connection;
import java.util.List;

public interface MigrationTask {

    void execute(Connection con) throws StartupException;

    String getName();

    List<String> getMessages();
}
```

This interface sets the contract for all migration tasks:

- `execute()` - does the actual work
- `getName()` - returns a readable name for logging
- `getMessages()` - returns any messages generated during execution

### 3.2 Example Task Implementation (RunMigrationScriptTask)

```java
public class RunMigrationScriptTask implements MigrationTask {

    private final DatabaseScriptProvider scripts;
    private final String scriptPath;
    private final boolean runScripts;
    private final List<String> messages = new ArrayList<>();

    public RunMigrationScriptTask(DatabaseScriptProvider scripts,
                                   String scriptPath,
                                   boolean runScripts) {
        this.scripts = scripts;
        this.scriptPath = scriptPath;
        this.runScripts = runScripts;
    }

    @Override
    public void execute(Connection con) throws StartupException {
        SQLScriptRunner runner = null;
        try {
            if (runScripts) {
                successMessage("Running database upgrade script: " + scriptPath);
                runner = new SQLScriptRunner(scripts.getDatabaseScript(scriptPath));
                runner.runScript(con, true);
                messages.addAll(runner.getMessages());
            }
        } catch(Exception ex) {
            log.error("ERROR running database upgrade script: " + scriptPath, ex);
            if (runner != null) {
                messages.addAll(runner.getMessages());
            }
            errorMessage("Problem running migration script: " + scriptPath, ex);
            throw new StartupException("Problem running migration script: " + scriptPath, ex);
        }
    }

    @Override
    public String getName() {
        return "Run migration script: " + scriptPath;
    }

    @Override
    public List<String> getMessages() {
        return messages;
    }
}
```

Each task class follows this same pattern - it takes its dependencies through the constructor, does one job in `execute()`, and reports what it did through `getMessages()`.

---

## 4. Before vs After

| Aspect                | Before                             | After                                |
| --------------------- | ---------------------------------- | ------------------------------------ |
| Method size           | ~440 lines in one method           | 6 task classes, each under 100 lines |
| Testing               | Hard to test individual operations | Each task can be unit tested alone   |
| Adding new migrations | Edit the large method              | Add a new task class                 |
| Error localization    | Search through 440 lines           | Error points to specific task        |
| Code reuse            | None                               | Tasks can be reused or composed      |

---

## 5. Testing Results

### Unit Tests

```
Tests run: 158, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
```

All existing tests pass after the refactoring.

### Manual Testing on Localhost

Tested the following on `http://localhost:8080/roller/`:

1. Fresh database installation - tables created successfully
2. Application startup - no errors
3. User registration and login - works
4. Creating blogs and posts - works
5. Comments and categories - work

The refactored code does not change any observable behavior. The migration tasks only run when upgrading from an older Roller version, which is why the logs show "Creating Roller Weblogger database tables" for a fresh install rather than the individual task executions.

---

## 6. Files Changed

### New Files (7 total):

- `app/src/main/java/org/apache/roller/weblogger/business/startup/MigrationTask.java`
- `app/src/main/java/org/apache/roller/weblogger/business/startup/tasks/RunMigrationScriptTask.java`
- `app/src/main/java/org/apache/roller/weblogger/business/startup/tasks/UpdateLocalFeedUrlsTask.java`
- `app/src/main/java/org/apache/roller/weblogger/business/startup/tasks/MergePlanetGroupsTask.java`
- `app/src/main/java/org/apache/roller/weblogger/business/startup/tasks/PopulateParentidsTask.java`
- `app/src/main/java/org/apache/roller/weblogger/business/startup/tasks/PopulatePathsTask.java`
- `app/src/main/java/org/apache/roller/weblogger/business/startup/tasks/UpgradeCommentPropertiesTask.java`

### Modified Files:

- `app/src/main/java/org/apache/roller/weblogger/business/startup/DatabaseInstaller.java` - now uses the task classes instead of inline code

---

## 7. Summary

The refactoring breaks down a 440-line procedural method into focused task classes. Each task does one thing and can be tested independently. The code is now easier to understand, maintain, and extend. If a new migration step is needed in the future, we can add it as a new task class without touching the existing ones.

This follows the Single Responsibility Principle - each class has one reason to change - and the Open/Closed Principle - we can add new migration behavior without modifying existing code.
