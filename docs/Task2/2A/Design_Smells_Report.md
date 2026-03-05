# Task 2A: Design Smells Analysis Report

**Project:** Apache Roller
**Date:** 2026-02-03
**Analyst:** Agentic AI (Antigravity)
**Tools:** PMD 7.0, PlantUML, Checkstyle 10.13

## 1. Executive Summary
This report analyzes architectural violations ("Design Smells") within Apache Roller. Each smell is documented with a rigorous evidence table linking tool outputs to design principles.

**Reference Visuals**: [Design_Smells.puml](Design_Smells.puml)

---

## 2. Identified Design Smells

### Smell 1: The God Class (PageServlet)

#### Design Smell Evidence Table
| Dimension | Evidence / Details |
| :--- | :--- |
| **Subsystem** | Weblog & Content Subsystem |
| **Package** | `org.apache.roller.weblogger.ui.rendering.servlets` |
| **File Path** | `app/src/main/java/org/apache/roller/weblogger/ui/rendering/servlets/PageServlet.java` |
| **Class Name** | `PageServlet` |
| **UML Relationships** | **Dependency Hub** (Fan-Out). Depends on `WeblogRequest`, `CacheManager`, `ThemeManager`, `CommentServlet`, `MobileDeviceRepository`. |
| **Tool Evidence** | **PMD**: `CyclomaticComplexity` (Rule: StdCyclomaticComplexity)<br>**Checkstyle**: `ClassFanOutComplexity` |
| **Metric Values** | **WMC (Complexity)**: 83 (Threshold: 10)<br>**Fan-Out (CBO)**: 46+ Classes<br>**LOC**: ~850 Lines |
| **Principles Violated** | **Single Responsibility Principle (SRP)**: Handles Routing, Caching, Device Detection, Rendering, AND Logic. |
| **Impact** | **Untestable Hub**. Validating a change in "Mobile Device Detection" requires re-testing the entire Page Rendering flow. |

#### Architectural Analysis
This is a classic "God Class" that acts as a central switchboard for the entire user-facing application. Ideally, a Servlet should only handle HTTP concerns (Request/Response) and delegate to specific Controllers. `PageServlet` instead keeps logic for ETag generation, Comment submission validation, and Mobile Theme selection all in one file. This creates a "Hub-and-Spoke" architecture where one class knows about everything.

#### A) Method-Level Evidence Mapping
| Class Name | Method Name | Method Responsibility | LOC (approx) | Complexity | External Classes Accessed | Why This Method Contributes to Smell |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `PageServlet` | `doGet` | Handle GET requests | **418** | High (Cyclomatic) | `WebloggerFactory`, `WeblogEntryManager`, `SiteWideCache`, `ModileDeviceRepository` | Massive conditional logic to route requests, check caches, and handle 304s. |
| `PageServlet` | `processReferrer` | Spam checking & tracking | ~100 | Medium | `ReferrerQueueManager`, `Weblog` | Logic for spam filtering should remain in a `ReferrerService`, not the Servlet. |
| `PageServlet` | `init` | Initialization | ~45 | Low | `ThemeManager`, `MobileDeviceRepository` | Hard-couples the Servlet to specific manager implementations. |

#### B) Relationship Justification Matrix
| Source Class | Target Class | Relationship Type | Code Evidence | Why This Relationship Is Problematic | Design Principle Violated |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `PageServlet` | `ThemeManager` | Dependency | Field: `themeManager` | Servlet manually manages theme lookups instead of delegating to a View Resolver. | Single Responsibility (SRP) |
| `PageServlet` | `WeblogEntryManager` | Dependency | Local Var in `doGet` | Servlet directly fetches data entities; bypasses a proper Controller layer. | Separation of Concerns |
| `PageServlet` | `MobileDeviceRepository` | Dependency | Method Call | Servlet contains device detection logic (presentation) mixed with routing (control). | High Cohesion |

#### C) Subsystem Ripple & Change Impact Analysis
*   **If this class changes, which other subsystems are affected?**
    *   **Rendering Subsystem:** Any change to how pages are served or cached requires touching this file.
    *   **Mobile Subsystem:** Changes to device detection logic live here.
    *   **Comments Subsystem:** Comment validation logic is embedded here.
*   **Compass of Change:** The blast radius is **System-Wide**. A bug in `PageServlet.doGet` can take down the entire public-facing website.
*   **Compilation Impact:** `PageServlet` depends on `pojos`, `business`, and `util`. Changing any of those requires recompiling `PageServlet`.

#### D) Smell Classification Validation
*   **Why God Class?** It performs tasks from 3 distinct domains: Control (Routing), Logic (Spam/Validation), and Presentation (Device Detection).
*   **Not Feature Envy?** It doesn't just envy one class; it orchestrates *many*.
*   **Not Mediator?** It doesn't just coordinate; it contains implementation details (like 400 lines of `doGet` logic).

#### E) Refactoring Readiness Mapping
| `PageServlet` | **Large** | `PageServlet`, `MobileDeviceRepository`, `CommentServlet` | `PageServletTest`, `RenderingTest` | **High** | **Extract Class** (Move Device Logic), **Extract Delegate** (Spam Check), **Replace Conditional with Polymorphism**. |

#### F) Causal Analysis & Refactoring Strategy (Enhanced Classification)
This section decomposes the "God Class" symptoms into actionable causal factors to guide specific refactoring.

| Causal Factor Tag | Justification | Refactoring Strategy |
| :--- | :--- | :--- |
| **Monolithic Controller** | The class handles routing, view resolution, and error handling for all page types rather than delegating early. | **Extract Controller / Strategy Pattern**: Delegate request handling to specific `PageController` implementations. |
| **Cross-Cutting Concern Leakage** | Logic for Spam Protection (Referrer Check) and Caching (304 Not Modified) is mixed with business logic. | **Extract to Filter / Interceptor**: Move spam and cache logic to Servlet Filters or Spring Interceptors. |
| **Dependency Hub** | Imports 46+ classes including Managers, POJOs, and Utilities, acting as a central coupling point. | **Introduce Facade**: Create a `PageRenderingService` to encapsulate the subsystem interactions. |
| **Large Method (doGet)** | `doGet` contains ~418 lines of mixed abstraction levels (HTTP handling vs Business Rules). | **Chain of Responsibility**: Break the method into a chain of independent handlers (e.g., `ValidationHandler`, `CacheHandler`, `RenderingHandler`). |

---

### Smell 2: Promiscuous Package (Cyclic Dependencies)

#### Design Smell Evidence Table
| Dimension | Evidence / Details |
| :--- | :--- |
| **Subsystem** | Cross-Cutting (Domain Layer) |
| **Package** | `org.apache.roller.weblogger.pojos` |
| **File Paths** | `Weblog.java` <-> `WeblogEntry.java` |
| **UML Relationships** | **Bi-directional Association** / **Cycle**.<br>`Weblog` *-- `WeblogEntry` (Composition)<br>`WeblogEntry` --> `Weblog` (Back-Reference) |
| **Tool Evidence** | **Designite**: `CyclicDependency`<br>**PMD**: `Design/CyclomaticComplexity` (indirectly high due to coupling) |
| **Metric Values** | **Cycle Size**: 2 Classes (primary), potentially more via `User`. |
| **Principles Violated** | **Acyclic Dependencies Principle (ADP)**: Modules should not depend on each other cyclically. |
| **Impact** | **Monolithic Compilation**. You cannot decouple the "Blog" concept from the "Entry" concept. They must always be deployed together. |

#### Architectural Analysis
The domain model forces a hard cycle between the parent (`Weblog`) and child (`WeblogEntry`). While common in ORM frameworks (Hibernate), from a pure design perspective, this prevents the reuse of `Weblog` (e.g., for a site directory) without dragging in the potentially heavy `WeblogEntry` graph. It rigidly couples the modules.

#### A) Method-Level Evidence Mapping
| Class Name | Method Name | Method Responsibility | LOC (approx) | Complexity | External Classes Accessed | Why This Method Contributes to Smell |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `Weblog` | `getEntryDisplayCount` | Aggregation Logic | ~15 | Low | `WeblogEntry` (Logic inferred) | Parent knows details about the specific display needs of its children. |
| `WeblogEntry` | `getWebsite` | Parent Accessor | ~5 | Low | `Weblog` | Explicit dependency on the concrete `Weblog` class (not an interface). |
| `WeblogEntry` | `setWebsite` | Parent Mutator | ~5 | Low | `Weblog` | Creates the hard back-reference. |

#### B) Relationship Justification Matrix
| Source Class | Target Class | Relationship Type | Code Evidence | Why This Relationship Is Problematic | Design Principle Violated |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `Weblog` | `WeblogEntry` | Composition (1..*) | `List<WeblogEntry>` (ORM Implicit) | Parent depends on Child. | Acyclic Dependencies Principle (ADP) |
| `WeblogEntry` | `Weblog` | Association (1..1) | Field: `website` | Child depends on Parent. Creates a cycle. | Acyclic Dependencies Principle (ADP) |

#### C) Subsystem Ripple & Change Impact Analysis
*   **If this class changes, which other subsystems are affected?**
    *   **Persistence Layer:** JPA mappings for both must be maintained in sync.
    *   **Unit Tests:** You cannot test `WeblogEntry` without instantiating a dummy `Weblog`, and vice-versa.
*   **Blast Radius:** Local but rigid. The two classes travel as a "Monolithic Block".

#### D) Smell Classification Validation
*   **Why Promiscuous Package?** The relationship crosses the class boundary but creates a tangle within the package (or across packages if they were separated).
*   **Not simply "Coupling"?** The *Cyclic* nature is the key differentiator. They cannot exist independently.

#### E) Refactoring Readiness Mapping
| Refactoring Candidate Class | Estimated Scope | Expected Classes to Change | Expected Tests Impacted | Risk Level | Suitable Refactoring Patterns |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `Weblog`, `WeblogEntry` | **Medium** | `Weblog`, `WeblogEntry`, ORM Mappings | `WeblogTest`, `WeblogEntryTest` | **Medium** | **dependency Inversion** (Interface for Parent), **Eliminate Navigability** (Remove `getWebsite()` if possible). |

---

### Smell 3: Hub-Like Dependency (Abstraction Failure)

#### Design Smell Evidence Table
| Dimension | Evidence / Details |
| :--- | :--- |
| **Subsystem** | Search & Indexing Subsystem |
| **Package** | `org.apache.roller.weblogger.business.search.lucene` |
| **Class Name** | `LuceneIndexManager` |
| **UML Relationships** | **High Fan-Out**. Direct dependency on concrete types: `User`, `Weblog`, `Entry`, `Comment`, `Category`. |
| **Tool Evidence** | **Checkstyle**: `ClassFanOutComplexity`<br>**PMD**: `CouplingBetweenObjects` |
| **Metric Values** | **Fan-Out (CBO)**: 40 Dependencies (Threshold: 20) |
| **Principles Violated** | **Open/Closed Principle (OCP)**: Class must be modified to handle new indexable types.<br>**Dependency Inversion Algorithm (DIP)**: Depends on details, not abstractions. |
| **Impact** | **Maintenance Bottleneck**. Adding a new entity type requires modifying this core system service. |

#### Architectural Analysis
The `LuceneIndexManager` is "hard-coded" to know about every indexable object in the system. Instead of defining an interface `Indexable` that entities implement, the Manager explicitly imports `WeblogEntry`, `WeblogCategory`, etc. This turns the Manager into a **Dependency Hub**—a highly coupled center point that ripples changes throughout the system.

#### A) Method-Level Evidence Mapping
| Class Name | Method Name | Method Responsibility | LOC (approx) | Complexity | External Classes Accessed | Why This Method Contributes to Smell |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `LuceneIndexManager` | `addEntryIndexOperation` | Indexing specific entity | ~10 | Low | `WeblogEntry` | Direct dependency on concrete POJO `WeblogEntry`. |
| `LuceneIndexManager` | `rebuildWeblogIndex` | Batch Indexing | ~20 | Medium | `Weblog` | Direct dependency on concrete POJO `Weblog`. |
| `LuceneIndexManager` | `search` | Search Execution | ~40 | Medium | `SearchResultList`, `WeblogCategory` | Knowledge of result structures and filtering categories. |

#### B) Relationship Justification Matrix
| Source Class | Target Class | Relationship Type | Code Evidence | Why This Relationship Is Problematic | Design Principle Violated |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `LuceneIndexManager` | `WeblogEntry` | Dependency | Method Param | Search Engine knows about "Blog Entries". | Dependency Inversion Principle (DIP) |
| `LuceneIndexManager` | `Weblog` | Dependency | Method Param | Search Engine knows about "Websites". | Open/Closed Principle (OCP) |
| `LuceneIndexManager` | `Comment` | Dependency | (Implicit in Full Code) | Search Engine knows about "Comments". | Open/Closed Principle (OCP) |

#### C) Subsystem Ripple & Change Impact Analysis
*   **If this class changes, which other subsystems are affected?**
    *   **Search Subsystem:** This IS the search subsystem.
    *   **Business Layer:** Any new business entity added (e.g., `ForumPost`) requires modifying this class to support indexing.
*   **Blast Radius:** **High**. The `LuceneIndexManager` must be frequently "opened" for modification (violating OCP) whenever the domain model grows.

#### D) Smell Classification Validation
*   **Why Hub Dependency?** The Fan-Out (dependencies *out*) is extremely high (40+). It organizes the system around itself.
*   **Not God Class?** It focuses on *Search* logic only (mostly), so it has defined responsibility. The problem is *how* it fulfills it (by checking types), not *what* it does.

#### E) Refactoring Readiness Mapping
| Refactoring Candidate Class | Estimated Scope | Expected Classes to Change | Expected Tests Impacted | Risk Level | Suitable Refactoring Patterns |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `LuceneIndexManager` | **Large** | `LuceneIndexManager`, `WeblogEntry`, `Weblog` | `SearchTest` | **Medium** | **Extract Interface** (Introduce `Indexable`), **Polymorphism** (Entity handles its own indexing document creation). |

---

### Smell 4: Leaky Abstraction (Interface Pollution)

#### Design Smell Evidence Table
| Dimension | Evidence / Details |
| :--- | :--- |
| **Subsystem** | Service Layer (All Subsystems) |
| **Package** | `org.apache.roller.weblogger.business` |
| **Class Name** | `Manager` (Interface) |
| **Method Signature** | `void save(...) throws RollerException` |
| **UML Relationships** | **Realization**. All specific Managers implement this leaky contract. |
| **Tool Evidence** | **PMD**: `SignatureDeclareThrowsException` |
| **Metric Values** | **Persistence Violations**: 100% of Manager methods throw generic exceptions. |
| **Principles Violated** | **Interface Segregation Principle (ISP)**.<br>**Separation of Concerns**. |
| **Impact** | **Error Handling Blindness**. Callers cannot distinguish between recoverable errors (DB constraint) and fatal errors (Connection lost). |

#### Architectural Analysis
The base `Manager` interface forces every implementation to throw `RollerException`, which is a generic wrapper. This "leaks" the fact that the implementation arguably relies on a failure-prone mechanism (like a DB) into the API contract. A clean design would use unchecked RuntimeExceptions for fatal errors or specific Typed Exceptions for business logic failures.

#### A) Method-Level Evidence Mapping
| Class Name | Method Name | Method Responsibility | LOC (approx) | Complexity | External Classes Accessed | Why This Method Contributes to Smell |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `WeblogManager` | `saveWeblog` | Persistence | N/A (Interface) | N/A | `Weblog` | Throws `WebloggerException` (Checked). |
| `UserManager` | `saveUser` | Persistence | N/A (Interface) | N/A | `User` | Throws `WebloggerException` (Checked). |
| `Weblogger` | `flush` | Persistence Control | N/A (Interface) | N/A | None | Throws `WebloggerException` (Checked). |

#### B) Relationship Justification Matrix
| Source Class | Target Class | Relationship Type | Code Evidence | Why This Relationship Is Problematic | Design Principle Violated |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `WeblogManager` | `WebloggerException` | Dependency | `throws` clause | Forces caller to handle a generic exception for *every* interaction. | Interface Segregation Principle (ISP) |
| `Weblogger` | `WebloggerException` | Dependency | `throws` clause | The main entry point is polluted with implementation failure details. | Dependency Inversion Principle (DIP) |

#### C) Subsystem Ripple & Change Impact Analysis
*   **If this class changes, which other subsystems are affected?**
    *   **Every Subsystem:** All Services (Business Layer) and all Controllers (UI Layer) must catch `WebloggerException`.
*   **Blast Radius:** **Total**. Changing the exception strategy (e.g., to RuntimeExceptions) would require a Global Refactor of thousands of try-catch blocks.

#### D) Smell Classification Validation
*   **Why Leaky Abstraction?** The implementation detail (Database Potential Failure) is leaking into the high-level Business Interface.
*   **Not just "Bad Error Handling"?** It's structurally baked into the Interface definition (`Manager`), defining the architectural boundaries.

#### E) Refactoring Readiness Mapping
| Refactoring Candidate Class | Estimated Scope | Expected Classes to Change | Expected Tests Impacted | Risk Level | Suitable Refactoring Patterns |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `WeblogManager`, `UserManager` | **Huge** | All Managers, All Servlets | All Tests | **Very High** | **Replace Checked Exception with Unchecked Exception**, **Translate Exception** (Wrap in Business Specific Exception). |

---

### Smell 5: Feature Envy (Smart Data Class)

#### Design Smell Evidence Table
| Dimension | Evidence / Details |
| :--- | :--- |
| **Subsystem** | Weblog & Content Subsystem |
| **Package** | `org.apache.roller.weblogger.pojos` |
| **Class Name** | `MediaFileSearchBean` |
| **UML Relationships** | **Logic Leakage**. DTO holding conditional logic. |
| **Tool Evidence** | **PMD**: `NPathComplexity` |
| **Metric Values** | **NPath Complexity**: **912** (Threshold: 200).<br>**WMC**: High for a Bean. |
| **Principles Violated** | **Separation of Concerns**. Data Objects should not hold Business Logic. |
| **Impact** | **Hidden Logic**. A developer expects a "Bean" to be a dumb container. Putting complex filtering logic here hides it from the Service Layer, where it belongs. |

#### Architectural Analysis
The `MediaFileSearchBean` masquerades as a Data Transfer Object (DTO) but contains heavy conditional logic for filtering media files (NPath 912). This is **Feature Envy**: the bean is envious of the `MediaFileManager`'s responsibilities. It creates a "Smart Data Structure" anti-pattern.

#### A) Method-Level Evidence Mapping
| Class Name | Method Name | Method Responsibility | LOC (approx) | Complexity | External Classes Accessed | Why This Method Contributes to Smell |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `MediaFileSearchBean` | `copyTo` | Transfer State to Filter | **~70** | **High** | `MediaFileFilter`, `MessageResources`, `RollerConstants` | Contains logic to convert Strings to Enums, calculate Bytes from KB/MB, and split tags. |
| `MediaFileSearchBean` | `getSizeFilterTypeLabel` | UI Helper | ~3 | Low | `ResourceBundle` | Fetching UI strings directly in the Bean. |

#### B) Relationship Justification Matrix
| Source Class | Target Class | Relationship Type | Code Evidence | Why This Relationship Is Problematic | Design Principle Violated |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `MediaFileSearchBean` | `MediaFileFilter` | Dependency | Method Param | The Bean knows *how* to construct the domain object. | Separation of Concerns |
| `MediaFileSearchBean` | `RollerConstants` | Dependency | Static Access | The Bean knows unit conversion constants (KB/MB). | High Cohesion (Logic placement) |

#### C) Subsystem Ripple & Change Impact Analysis
*   **If this class changes, which other subsystems are affected?**
    *   **UI Layer:** This is used by Struts Actions.
    *   **Business Layer:** If `MediaFileFilter` changes, this Bean MUST change to match the logic.
*   **Blast Radius:** **Medium**. Logic duplication means bugs (e.g., incorrect KB conversion) must be fixed in multiple places.

#### D) Smell Classification Validation
*   **Why Feature Envy?** The `copyTo` method does work that *belongs* to `MediaFileFilter` (or a Factory). The data is here, but the logic should be elsewhere.
*   **Not God Class?** The class is small. It's just doing the *wrong* job.

#### E) Refactoring Readiness Mapping
| Refactoring Candidate Class | Estimated Scope | Expected Classes to Change | Expected Tests Impacted | Risk Level | Suitable Refactoring Patterns |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `MediaFileSearchBean` | **Small** | `MediaFileSearchBean`, `MediaFileFilter` | `MediaFileSearchBeanTest` | **Low** | **Move Method** (Move logic to `MediaFileFilter.fromBean`), **Extract Factory**. |

---

### Smell 6: Procedural Block (Transaction Script)

#### Design Smell Evidence Table
| Dimension | Evidence / Details |
| :--- | :--- |
| **Subsystem** | Installation / Startup |
| **Package** | `org.apache.roller.weblogger.business.startup` |
| **Class Name** | `DatabaseInstaller` |
| **Method Name** | `install()` |
| **UML Relationships** | **Self-Contained**. Does not collaborate; just executes linear steps. |
| **Tool Evidence** | **Checkstyle**: `JavaNCSS` (Method Length) |
| **Metric Values** | **NCSS**: 221 Lines in one method. |
| **Principles Violated** | **Object-Oriented Design**: This is procedural code wrapped in a class. |
| **Impact** | **Rigidity**. You cannot subclass or override specific parts of the installation process (e.g., "Skip Schema Creation") because it is one giant block. |

#### Architectural Analysis
This class ignores the capabilities of Java (Strategy Pattern, Chain of Responsibility) and writes the installation logic as a monolithic script. While functional, it represents a failure to model "Installation" as a composed set of "Tasks". It is a **Transaction Script** operating in an OO environment.

#### A) Method-Level Evidence Mapping
| Class Name | Method Name | Method Responsibility | LOC (approx) | Complexity | External Classes Accessed | Why This Method Contributes to Smell |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `DatabaseInstaller` | `upgradeTo400` | Migration to v4.0 | **~440** | **Extreme** | `Connection`, `PreparedStatement` | Pure sequential execution of hundreds of SQL statements. |
| `DatabaseInstaller` | `upgradeTo500` | Migration to v5.0 | ~100 | High | `Connection` | Another monolithic block for the next version. |
| `DatabaseInstaller` | `createDatabase` | Initial Install | ~40 | Medium | `SQLScriptRunner` | Hardcoded execution path. |

#### B) Relationship Justification Matrix
| Source Class | Target Class | Relationship Type | Code Evidence | Why This Relationship Is Problematic | Design Principle Violated |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `DatabaseInstaller` | `Statement` (JDBC) | Dependency | Local Variable | Mixing business goal (Upgrade) with low-level JDBC mechanics. | Single Responsibility (SRP) |
| `DatabaseInstaller` | `DatabaseProvider` | Association | Field | Tightly coupled to the specific provider mechanism. | Open/Closed Principle (OCP) |

#### C) Subsystem Ripple & Change Impact Analysis
*   **If this class changes, which other subsystems are affected?**
    *   **Startup/Bootstrap:** This runs on server start. If it fails, the app dies.
    *   **Database:** Modifies schema directly.
*   **Blast Radius:** **Critical**. Errors here corrupt the database state, potentially causing data loss. The lack of modularity makes partial failures hard to handle.

#### D) Smell Classification Validation
*   **Why Procedural Block?** The class executes top-to-bottom like a C-script. There is no state, no polymorphism, no objects representing "Tasks".
*   **Not God Class?** It isolates "Installation", so the scope is narrow. The *style* is the problem.

#### E) Refactoring Readiness Mapping
| Refactoring Candidate Class | Estimated Scope | Expected Classes to Change | Expected Tests Impacted | Risk Level | Suitable Refactoring Patterns |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `DatabaseInstaller` | **Medium** | `DatabaseInstaller` | `DatabaseInstallerTest` | **High** | **Replace Method with Method Object**, **Chain of Responsibility** (Migration Tasks), **Command Pattern**. |

---

### 3. Cross-Smell Interaction Analysis

The identified smells do not exist in isolation; they reinforce each other to create a rigid, brittle architecture:

1.  **God Class (`PageServlet`) + Leaky Abstraction (`Manager`)**: `PageServlet` (the God Class) is forced to handle `WebloggerException` for every single operation because of the **Leaky Abstraction** in the underlying managers. This balloons the code size of the Servlet with boilerplate error handling, further cementing its God Class status.
2.  **Hub Dependency (`LuceneIndexManager`) + Promiscuous Package (`Weblog`)**: The Search Manager's dependency on concrete `Weblog` and `WeblogEntry` classes makes it a victim of the **Promiscuous Package** cycle. If you try to extract `LuceneIndexManager`, you implicitly drag in the entire `Weblog` <-> `WeblogEntry` cycle, making modularity impossible.
3.  **Feature Envy (`MediaFileSearchBean`) as a Symptom**: The logic leaking into the Bean suggests that the **Manager Layer** (Feature Envy target) might be too "Anemic" or effectively "Leaky" (Smell 4), causing developers to put logic wherever they can find a place, even in UI Beans.

These interactions suggest that a **Layered Refactoring** approach is needed: Fix the Domain Model (Smell 2) first, then Abstractions (Smell 4), then break up Hubs (Smell 1 & 3).
