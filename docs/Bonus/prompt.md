# LLM Benchmarking Prompt

**Design Smell:** God Class (PageServlet)

---

You are given a Java codebase excerpt from Apache Roller.

Context:
- The system is already in production.
- All existing unit and integration tests must pass.
- No data loss or behavioral change is allowed.
- Refactoring must be at the class level (non-trivial).

Tasks:
1. Identify the design smells present in the given code.
2. Determine whether the following manually identified smell is valid:
   - **God Class (PageServlet)**
3. Refactor the code ONLY to address this smell.
4. Do not introduce new features, persistence changes, or API behavior changes.

Constraints:
- Preserve original behavior exactly.
- Avoid over-engineering.
- Do not refactor unrelated smells.

Output format:
A) Identified design smells (with brief justification)
B) Refactoring rationale
C) Refactored code or class-level diff
D) Assumptions or risks (if any)

---

## Code to Analyze

**File:** `app/src/main/java/org/apache/roller/weblogger/ui/rendering/servlets/PageServlet.java`

**Metrics:**
- Total LOC: 680
- `doGet()` method LOC: 415 (61% of class)
- Cyclomatic Complexity: ~71
- if-statements: 60
- Responsibilities: 12+ (HTTP handling, routing, caching, spam detection, theme management, model building, rendering)
- Imports: 46 classes

**Code Excerpt (Key Methods):**

```java
package org.apache.roller.weblogger.ui.rendering.servlets;

// 46 imports omitted for brevity

/**
 * Provides access to weblog pages.
 */
public class PageServlet extends HttpServlet {
    
    private static Log log = LogFactory.getLog(PageServlet.class);
    private boolean processReferrers = true;
    private static Pattern robotPattern = null;
    private boolean excludeOwnerPages = false;
    private WeblogPageCache weblogPageCache = null;
    private SiteWideCache siteWideCache = null;
    Boolean themeReload = false;

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        log.info("Initializing PageServlet");
        this.excludeOwnerPages = WebloggerConfig.getBooleanProperty("cache.excludeOwnerEditPages");
        this.weblogPageCache = WeblogPageCache.getInstance();
        this.siteWideCache = SiteWideCache.getInstance();
        this.processReferrers = WebloggerConfig.getBooleanProperty("site.bannedwordslist.enable.referrers");
        log.info("Referrer spam check enabled = " + this.processReferrers);
        String robotPatternStr = WebloggerConfig.getProperty("referrer.robotCheck.userAgentPattern");
        if (robotPatternStr != null && robotPatternStr.length() > 0) {
            try {
                robotPattern = Pattern.compile(robotPatternStr);
            } catch (Exception e) {
                log.error("Error parsing referrer.robotCheck.userAgentPattern value '" + robotPatternStr + "'.  Robots will not be filtered. ", e);
            }
        }
        themeReload = WebloggerConfig.getBooleanProperty("themes.reload.mode");
    }

    /**
     * Handle GET requests for weblog pages.
     * 
     * This 415-LOC method performs:
     * 1. Spam/Referrer processing (lines 145-155)
     * 2. Request parsing (lines 161-178)
     * 3. 304 Not Modified handling (lines 188-200)
     * 4. Cache key generation (lines 202-208)
     * 5. Theme reloading (lines 211-233)
     * 6. Cache lookup (lines 236-264)
     * 7. Template resolution (lines 269-351) - massive if-else chain
     * 8. Input validation (lines 356-407)
     * 9. Locale forcing (lines 410-412)
     * 10. Hit counting (lines 415-419)
     * 11. Model building (lines 442-477)
     * 12. Content rendering (lines 489-525)
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        log.debug("Entering");
        
        // 1. Spam/referrer processing
        if (this.processReferrers) {
            boolean spam = this.processReferrer(request);
            if (spam) {
                log.debug("spammer, giving 'em a 403");
                if (!response.isCommitted()) response.reset();
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }
        
        Weblog weblog;
        boolean isSiteWide;
        
        // 2. Request parsing
        WeblogPageRequest pageRequest;
        try {
            pageRequest = new WeblogPageRequest(request);
            weblog = pageRequest.getWeblog();
            if (weblog == null) {
                throw new WebloggerException("unable to lookup weblog: " + pageRequest.getWeblogHandle());
            }
            isSiteWide = WebloggerRuntimeConfig.isSiteWideWeblog(pageRequest.getWeblogHandle());
        } catch (Exception e) {
            log.debug("error creating page request", e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // 3. 304 Not Modified handling
        long lastModified = System.currentTimeMillis();
        if (isSiteWide) {
            lastModified = siteWideCache.getLastModified().getTime();
        } else if (weblog.getLastModified() != null) {
            lastModified = weblog.getLastModified().getTime();
        }
        
        if (!pageRequest.isLoggedIn()) {
            if (ModDateHeaderUtil.respondIfNotModified(request, response, lastModified, pageRequest.getDeviceType())) {
                return;
            } else {
                ModDateHeaderUtil.setLastModifiedHeader(response, lastModified, pageRequest.getDeviceType());
            }
        }
        
        // 4. Cache key generation
        String cacheKey;
        if (isSiteWide) {
            cacheKey = siteWideCache.generateKey(pageRequest);
        } else {
            cacheKey = weblogPageCache.generateKey(pageRequest);
        }
        
        // 5. Theme reloading (dev mode only)
        if (themeReload && !weblog.getEditorTheme().equals(WeblogTheme.CUSTOM) 
                && (pageRequest.getPathInfo() == null || pageRequest.getPathInfo() != null 
                && !pageRequest.getPathInfo().endsWith(".css"))) {
            try {
                ThemeManager manager = WebloggerFactory.getWeblogger().getThemeManager();
                boolean reloaded = manager.reLoadThemeFromDisk(weblog.getEditorTheme());
                if (reloaded) {
                    if (isSiteWide) {
                        siteWideCache.clear();
                    } else {
                        weblogPageCache.clear();
                    }
                    I18nMessages.reloadBundle(weblog.getLocaleInstance());
                }
            } catch (Exception ex) {
                log.error("ERROR - reloading theme " + ex);
            }
        }
        
        // 6. Cache lookup
        if ((!this.excludeOwnerPages || !pageRequest.isLoggedIn()) 
                && request.getAttribute("skipCache") == null 
                && request.getParameter("skipCache") == null) {
            
            CachedContent cachedContent;
            if (isSiteWide) {
                cachedContent = (CachedContent) siteWideCache.get(cacheKey);
            } else {
                cachedContent = (CachedContent) weblogPageCache.get(cacheKey, lastModified);
            }
            
            if (cachedContent != null) {
                log.debug("HIT " + cacheKey);
                if (!isSiteWide && (pageRequest.isWebsitePageHit() || pageRequest.isOtherPageHit())) {
                    this.processHit(weblog);
                }
                response.setContentLength(cachedContent.getContent().length);
                response.setContentType(cachedContent.getContentType());
                response.getOutputStream().write(cachedContent.getContent());
                return;
            } else {
                log.debug("MISS " + cacheKey);
            }
        }
        
        log.debug("Looking for template to use for rendering");
        
        // 7. Template resolution - MASSIVE IF-ELSE CHAIN (83 LOC)
        ThemeTemplate page = null;
        
        if (request.getParameter("popup") != null) {
            try {
                page = weblog.getTheme().getTemplateByName("_popupcomments");
            } catch (Exception e) { }
            
            if (page == null) {
                page = new StaticThemeTemplate("templates/weblog/popupcomments.vm", TemplateLanguage.VELOCITY);
            }
            
        } else if ("page".equals(pageRequest.getContext())) {
            page = pageRequest.getWeblogPage();
            if (page == null) {
                if (!response.isCommitted()) response.reset();
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
        } else if ("tags".equals(pageRequest.getContext()) && pageRequest.getTags() != null) {
            try {
                page = weblog.getTheme().getTemplateByAction(ComponentType.TAGSINDEX);
            } catch (Exception e) {
                log.error("Error getting weblog page for action 'tagsIndex'", e);
            }
            
            if (page == null) {
                if (!response.isCommitted()) response.reset();
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            
        } else if (pageRequest.getWeblogAnchor() != null) {
            try {
                page = weblog.getTheme().getTemplateByAction(ComponentType.PERMALINK);
            } catch (Exception e) {
                log.error("Error getting weblog page for action 'permalink'", e);
            }
        }
        
        if (page == null) {
            try {
                page = weblog.getTheme().getDefaultTemplate();
            } catch (Exception e) {
                log.error("Error getting default page for weblog = " + weblog.getHandle(), e);
            }
        }
        
        if (page == null) {
            if (!response.isCommitted()) response.reset();
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        log.debug("page found, dealing with it");
        
        // 8. Input validation - COMPLEX NESTED VALIDATION (52 LOC)
        boolean invalid = false;
        if (pageRequest.getWeblogPageName() != null && page.isHidden()) {
            invalid = true;
        }
        if (pageRequest.getLocale() != null && !pageRequest.getWeblog().isEnableMultiLang()) {
            invalid = true;
        }
        if (pageRequest.getWeblogAnchor() != null) {
            WeblogEntry entry = pageRequest.getWeblogEntry();
            if (entry == null) {
                invalid = true;
            } else if (pageRequest.getLocale() != null && !entry.getLocale().startsWith(pageRequest.getLocale())) {
                invalid = true;
            } else if (!entry.isPublished()) {
                invalid = true;
            } else if (new Date().before(entry.getPubTime())) {
                invalid = true;
            }
        } else if (pageRequest.getWeblogCategoryName() != null) {
            if (pageRequest.getWeblogCategory() == null) {
                invalid = true;
            }
        } else if (pageRequest.getTags() != null && !pageRequest.getTags().isEmpty()) {
            try {
                WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();
                invalid = !wmgr.getTagComboExists(pageRequest.getTags(), (isSiteWide) ? null : weblog);
            } catch (WebloggerException ex) {
                invalid = true;
            }
        }
        
        if (invalid) {
            log.debug("page failed validation, bailing out");
            if (!response.isCommitted()) response.reset();
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // 9. Locale forcing
        if (pageRequest.getLocale() == null && !weblog.isShowAllLangs()) {
            pageRequest.setLocale(weblog.getLocale());
        }
        
        // 10. Hit counting
        if (!isSiteWide && (pageRequest.isWebsitePageHit() || pageRequest.isOtherPageHit())) {
            this.processHit(weblog);
        }
        
        // 11. Content type determination
        String contentType;
        if (StringUtils.isNotEmpty(page.getOutputContentType())) {
            contentType = page.getOutputContentType() + "; charset=utf-8";
        } else {
            final String defaultContentType = "text/html; charset=utf-8";
            if (page.getLink() == null) {
                contentType = defaultContentType;
            } else {
                String mimeType = RollerContext.getServletContext().getMimeType(page.getLink());
                if (mimeType != null) {
                    contentType = mimeType + "; charset=utf-8";
                } else {
                    contentType = defaultContentType;
                }
            }
        }
        
        // 12. Model building
        HashMap<String, Object> model = new HashMap<>();
        try {
            PageContext pageContext = JspFactory.getDefaultFactory()
                    .getPageContext(this, request, response, "", false, RollerConstants.EIGHT_KB_IN_BYTES, true);
            
            request.setAttribute("pageRequest", pageRequest);
            
            Map<String, Object> initData = new HashMap<>();
            initData.put("requestParameters", request.getParameterMap());
            initData.put("parsedRequest", pageRequest);
            initData.put("pageContext", pageContext);
            initData.put("urlStrategy", WebloggerFactory.getWeblogger().getUrlStrategy());
            
            WeblogEntryCommentForm commentForm = (WeblogEntryCommentForm) request.getAttribute("commentForm");
            if (commentForm != null) {
                initData.put("commentForm", commentForm);
            }
            
            String pageModels = WebloggerConfig.getProperty("rendering.pageModels");
            ModelLoader.loadModels(pageModels, model, initData, true);
            
            if (WebloggerRuntimeConfig.isSiteWideWeblog(weblog.getHandle())) {
                String siteModels = WebloggerConfig.getProperty("rendering.siteModels");
                ModelLoader.loadModels(siteModels, model, initData, true);
            }
            
        } catch (WebloggerException ex) {
            log.error("Error loading model objects for page", ex);
            if (!response.isCommitted()) response.reset();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        
        // 13. Rendering
        Renderer renderer;
        try {
            log.debug("Looking up renderer");
            renderer = RendererManager.getRenderer(page, pageRequest.getDeviceType());
        } catch (Exception e) {
            log.error("Couldn't find renderer for page " + page.getId(), e);
            if (!response.isCommitted()) response.reset();
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        CachedContent rendererOutput = new CachedContent(RollerConstants.TWENTYFOUR_KB_IN_BYTES, contentType);
        try {
            log.debug("Doing rendering");
            renderer.render(model, rendererOutput.getCachedWriter());
            rendererOutput.flush();
            rendererOutput.close();
        } catch (Exception e) {
            log.error("Error during rendering for page " + page.getId(), e);
            if (!response.isCommitted()) response.reset();
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        // Post rendering process
        log.debug("Flushing response output");
        response.setContentType(contentType);
        response.setContentLength(rendererOutput.getContent().length);
        response.getOutputStream().write(rendererOutput.getContent());
        
        // Cache rendered content
        if ((!this.excludeOwnerPages || !pageRequest.isLoggedIn()) && request.getAttribute("skipCache") == null) {
            log.debug("PUT " + cacheKey);
            if (isSiteWide) {
                siteWideCache.put(cacheKey, rendererOutput);
            } else {
                weblogPageCache.put(cacheKey, rendererOutput);
            }
        } else {
            log.debug("SKIPPED " + cacheKey);
        }
        
        log.debug("Exiting");
    }
    
    private void processHit(Weblog weblog) {
        HitCountQueue counter = HitCountQueue.getInstance();
        counter.processHit(weblog);
    }
    
    private boolean processReferrer(HttpServletRequest request) {
        // 100 LOC of referrer spam detection logic
        // (omitted for brevity)
        return false;
    }
}
```

---

**Expected Test Suite:**
- 158 existing tests must pass without modification
- No changes to URL mappings: `/entry/`, `/page/`, `/category/`, `/tags/`, archive, homepage, popup
