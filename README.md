[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/HIOZk3CI)

# Apache Roller

[Apache Roller](http://roller.apache.org) is a Java-based, full-featured, multi-user and group-blog server suitable for blog sites large and small.
Roller is typically run with Apache Tomcat and MySQL.
Roller is made up of the following Maven projects:

* _roller-project_:         Top level project
* _app_:                    Roller Weblogger webapp, JSP pages, Velocity templates
* _assembly-release_:       Used to create official distributions of Roller
* _docs_:                   Roller documentation in ASCII Doc format
* _it-selenium_:            Integrated browser tests for Roller using Selenium

## Documentation

The Roller Install, User and Template Guides are available in ODT format (for OpenOffice or LibraOffice):

* <https://github.com/apache/roller/tree/master/docs>

## Refactoring Project: God Class (Smell 1 - PageServlet)

This branch (`refactor/smell-1-godclass-pageservlet`) addresses the "God Class" design smell identified in `org.apache.roller.weblogger.ui.rendering.servlets.PageServlet`.

### Problem
The original `PageServlet` class was identified as a "God Class" (or "Large Class") because it accumulated too many responsibilities:
-   Parsing and routing various URL patterns (permalinks, categories, dates, tags, custom pages).
-   Preparing data models for each of these disparate views.
-   Managing caching logic often mixed with business logic.
-   Directing the rendering process.

This resulted in a monolithic file with high complexity, making it difficult to maintain, test, and extend.

### Solution
We refactored the code by decomposing `PageServlet` using the **Strategy Pattern** and **Extract Class** techniques.

1.  **Extracted Handlers:** Specific request handling logic was moved to dedicated classes in `org.apache.roller.weblogger.ui.rendering.servlets.handlers`:
    -   `PermalinkHandler`: Manages individual blog entry requests.
    -   `CategoryHandler`: Manages category feed/page requests.
    -   `TagsHandler`: Manages tag aggregation requests.
    -   `DateArchiveHandler`: Manages time-based archive requests.
    -   `CustomPageHandler`: Manages user-created custom pages.
    -   `HomepageHandler`: Manages the main weblog landing page.
    -   `PopupHandler`: Handlers legacy popup comment windows.

2.  **Page Router:** Introduced `PageRouter` to inspect the request and delegate it to the appropriate `PageRequestHandler`.

### Improvements
-   **Reduced Complexity:** `PageServlet` is now significantly smaller (~600 LOC) and focuses solely on initialization, request orchestration, and common concerns (like referrer checking). The specific logic (~1100 LOC) is distributed across 7 cohesive handler classes.
-   **Separation of Concerns:** Each handler focuses on a single type of request, improving readability.
-   **Bug Fixes:** During validation, we identified and fixed a critical bug where model loading was incorrectly using property keys as class names (causing 500 errors).

### Validation
The refactoring was validated through:
-   **Manual Smoke Tests:** Verified successful creation of weblogs, posting of entries, and rendering of all page types.
-   **Error Handling Verification:** Confirmed that invalid URLs (e.g., non-existent entries) properly return 404 errors instead of crashing with 500 Internal Server Errors.

## For more information

Hit the Roller Confluence wiki:

* How to build and run Roller: <https://cwiki.apache.org/confluence/x/EM4>
* How to contribute to Roller: <https://cwiki.apache.org/confluence/x/2hsB>
* How to make a release of Roller: <https://cwiki.apache.org/confluence/x/gycB>
* Other developer resources: <https://cwiki.apache.org/confluence/x/D84>


## Installing Roller 

If you want to run Roller in production, then you should down load the latest official release and install it by following the Installation Guide, which you can find at the documentation link: <https://github.com/apache/roller/tree/master/docs>.


## Quick start: Running via Maven

You probably should not run Roller in production using this technique, but it's a relatively easy way to try Roller for yourself. 
Assuming you've got a UNIX shell, Java, Maven and Git:

Get the code:

    $ git clone https://github.com/apache/roller.git

Compile and build Roller:

    $ cd roller
    $ mvn -DskipTests=true install

Run Roller in Jetty with an embedded Derby database (for testing only):

    $ mvn jetty:run

Once Jetty is up and running browse to <http://localhost:8080/roller> to try to Roller.


## Quick start: running via Docker

Another way to try Roller is to use Docker. 
This is actually easier than running via Maven because you do not need Maven or Java. 
If you've got Docker, here's how you can run Roller for demo purposes.

Get the code:

    $ git clone https://github.com/apache/roller.git

Run Docker Compose to build and launch Roller along with a PostgreSQL database:

    $ cd roller
    $ docker-compose up
    
It will take a while to build and start the Docker image. 
Once it's done browse to <http://localhost:8080/roller> to try Roller.
