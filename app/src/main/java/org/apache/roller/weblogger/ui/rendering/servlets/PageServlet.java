/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 */

package org.apache.roller.weblogger.ui.rendering.servlets;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.roller.util.RollerConstants;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.HitCountQueue;

import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.themes.ThemeManager;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.pojos.ThemeTemplate;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogTheme;
import org.apache.roller.weblogger.ui.core.RollerContext;
import org.apache.roller.weblogger.ui.rendering.servlets.handlers.CategoryHandler;
import org.apache.roller.weblogger.ui.rendering.servlets.handlers.CustomPageHandler;
import org.apache.roller.weblogger.ui.rendering.servlets.handlers.ArchivePageHandler;
import org.apache.roller.weblogger.ui.rendering.servlets.handlers.HomepageHandlerWrapper;
import org.apache.roller.weblogger.ui.rendering.servlets.handlers.PageRequestHandler;
import org.apache.roller.weblogger.ui.rendering.servlets.handlers.PageRouter;
import org.apache.roller.weblogger.ui.rendering.servlets.handlers.BlogEntryHandler;
import org.apache.roller.weblogger.ui.rendering.servlets.handlers.PopupHandler;
import org.apache.roller.weblogger.ui.rendering.servlets.handlers.TagsHandler;
import org.apache.roller.weblogger.ui.rendering.util.InvalidRequestException;
import org.apache.roller.weblogger.ui.rendering.util.ModDateHeaderUtil;
import org.apache.roller.weblogger.ui.rendering.util.WeblogEntryCommentForm;
import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;
import org.apache.roller.weblogger.ui.rendering.util.cache.SiteWideCache;
import org.apache.roller.weblogger.ui.rendering.util.cache.WeblogPageCache;
import org.apache.roller.weblogger.util.BannedwordslistChecker;
import org.apache.roller.weblogger.util.I18nMessages;
import org.apache.roller.weblogger.util.cache.CachedContent;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Provides access to weblog pages.
 */
public class PageServlet extends HttpServlet {

    private static Log log = LogFactory.getLog(PageServlet.class);
    // for referrer processing
    private boolean processReferrers = true;
    private static Pattern robotPattern = null;
    // for caching
    private boolean excludeOwnerPages = false;
    private WeblogPageCache weblogPageCache = null;
    private SiteWideCache siteWideCache = null;

    // Development theme reloading
    Boolean themeReload = false;

    // Page request routing
    private PageRouter pageRouter = null;

    /**
     * Init method for this servlet
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {

        super.init(servletConfig);

        log.info("Initializing PageServlet");

        this.excludeOwnerPages = WebloggerConfig
                .getBooleanProperty("cache.excludeOwnerEditPages");

        // get a reference to the weblog page cache
        this.weblogPageCache = WeblogPageCache.getInstance();

        // get a reference to the site wide cache
        this.siteWideCache = SiteWideCache.getInstance();

        // see if built-in referrer spam check is enabled
        this.processReferrers = WebloggerConfig
                .getBooleanProperty("site.bannedwordslist.enable.referrers");

        log.info("Referrer spam check enabled = " + this.processReferrers);

        // check for possible robot pattern
        String robotPatternStr = WebloggerConfig
                .getProperty("referrer.robotCheck.userAgentPattern");
        if (robotPatternStr != null && robotPatternStr.length() > 0) {
            // Parse the pattern, and store the compiled form.
            try {
                robotPattern = Pattern.compile(robotPatternStr);
            } catch (Exception e) {
                // Most likely a PatternSyntaxException; log and continue as if
                // it is not set.
                log.error(
                        "Error parsing referrer.robotCheck.userAgentPattern value '"
                                + robotPatternStr
                                + "'.  Robots will not be filtered. ",
                        e);
            }
        }

        // Development theme reloading
        themeReload = WebloggerConfig.getBooleanProperty("themes.reload.mode");

        // Initialize page request router with handlers in priority order
        // More specific handlers first, default handler last
        // Build handler chain (wrappers used where we introduced clearer names).
        // Order is preserved: more specific handlers first, generic last.
        java.util.List<PageRequestHandler> handlers = new java.util.ArrayList<>();
        handlers.add(new BlogEntryHandler()); // /entry/<anchor> paths (wraps PermalinkHandler)
        handlers.add(new TagsHandler()); // tags query/path params (existing)
        handlers.add(new CustomPageHandler()); // /page/<pagename> paths (existing)
        handlers.add(new CategoryHandler()); // /category/<cat> paths (existing)
        handlers.add(new ArchivePageHandler()); // archive/date pages (wraps DateArchiveHandler)
        handlers.add(new HomepageHandlerWrapper()); // fallback default handler (wrapper)
        this.pageRouter = new PageRouter(handlers);
    }

    /**
     * Handle GET requests for weblog pages.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        log.debug("Entering doGet");

        // do referrer processing, if it's enabled
        // NOTE: this *must* be done first because it triggers a hibernate flush
        // which will close the active session and cause lazy init exceptions
        // otherwise
        if (this.processReferrers) {
            boolean spam = this.processReferrer(request);
            if (spam) {
                log.debug("spammer, giving 'em a 403");
                if (!response.isCommitted()) {
                    response.reset();
                }
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        Weblog weblog;
        boolean isSiteWide;

        WeblogPageRequest pageRequest;
        try {
            pageRequest = new WeblogPageRequest(request);

            weblog = pageRequest.getWeblog();
            if (weblog == null) {
                throw new WebloggerException("unable to lookup weblog: "
                        + pageRequest.getWeblogHandle());
            }

            // is this the site-wide weblog?
            isSiteWide = WebloggerRuntimeConfig.isSiteWideWeblog(pageRequest
                    .getWeblogHandle());
        } catch (Exception e) {
            // some kind of error parsing the request or looking up weblog
            log.debug("error creating page request", e);
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // determine the lastModified date for this content
        long lastModified = System.currentTimeMillis();
        if (isSiteWide) {
            lastModified = siteWideCache.getLastModified().getTime();
        } else if (weblog.getLastModified() != null) {
            lastModified = weblog.getLastModified().getTime();
        }

        // 304 Not Modified handling.
        // We skip this for logged in users to avoid the scenerio where a user
        // views their weblog, logs in, then gets a 304 without the 'edit' links
        if (!pageRequest.isLoggedIn()) {
            if (ModDateHeaderUtil.respondIfNotModified(request, response,
                    lastModified, pageRequest.getDeviceType())) {
                return;
            } else {
                // set last-modified date
                ModDateHeaderUtil.setLastModifiedHeader(response, lastModified,
                        pageRequest.getDeviceType());
            }
        }

        // generate cache key
        String cacheKey;
        if (isSiteWide) {
            cacheKey = siteWideCache.generateKey(pageRequest);
        } else {
            cacheKey = weblogPageCache.generateKey(pageRequest);
        }

        // Development only. Reload if theme has been modified
        if (themeReload
                && !weblog.getEditorTheme().equals(WeblogTheme.CUSTOM)
                && (pageRequest.getPathInfo() == null || pageRequest
                        .getPathInfo() != null
                        && !pageRequest.getPathInfo().endsWith(".css"))) {
            try {
                ThemeManager manager = WebloggerFactory.getWeblogger()
                        .getThemeManager();
                boolean reloaded = manager.reLoadThemeFromDisk(weblog
                        .getEditorTheme());
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

        // cached content checking
        if ((!this.excludeOwnerPages || !pageRequest.isLoggedIn())
                && request.getAttribute("skipCache") == null
                && request.getParameter("skipCache") == null) {

            CachedContent cachedContent;
            if (isSiteWide) {
                cachedContent = (CachedContent) siteWideCache.get(cacheKey);
            } else {
                cachedContent = (CachedContent) weblogPageCache.get(cacheKey,
                        lastModified);
            }

            if (cachedContent != null) {
                log.debug("HIT " + cacheKey);

                // allow for hit counting
                if (!isSiteWide
                        && (pageRequest.isWebsitePageHit() || pageRequest
                                .isOtherPageHit())) {
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

        // validation: locale view allowed only if weblog has enabled it
        if (pageRequest.getLocale() != null
                && !pageRequest.getWeblog().isEnableMultiLang()) {
            log.debug("locale view not allowed, bailing out");
            if (!response.isCommitted()) {
                response.reset();
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // do we need to force a specific locale for the request?
        if (pageRequest.getLocale() == null && !weblog.isShowAllLangs()) {
            pageRequest.setLocale(weblog.getLocale());
        }

        log.debug("Looking for handler to process request");

        // Handle popup requests specially (popup parameter doesn't fit routing pattern)
        if (request.getParameter("popup") != null) {
            handlePopupRequest(request, response, pageRequest, cacheKey, isSiteWide, weblog);
            return;
        }

        // Prepare initial model data for handlers
        Map<String, Object> initData = new HashMap<>();
        initData.put("requestParameters", request.getParameterMap());
        initData.put("parsedRequest", pageRequest);
        initData.put("isSiteWide", isSiteWide);
        // Pass CSRF salt to templates so they can include it in AJAX POSTs
        Object saltAttr = request.getAttribute("salt");
        if (saltAttr != null) {
            initData.put("salt", saltAttr.toString());
        }
        // Pass context path so templates can build correct URLs
        initData.put("contextPath", request.getContextPath());

        PageContext pageContext = null;
        try {
            pageContext = JspFactory.getDefaultFactory()
                    .getPageContext(this, request, response, "", false,
                            RollerConstants.EIGHT_KB_IN_BYTES, true);
            initData.put("pageContext", pageContext);

            // special hack for menu tag
            request.setAttribute("pageRequest", pageRequest);

            // define url strategy
            initData.put("urlStrategy", WebloggerFactory.getWeblogger()
                    .getUrlStrategy());

            // if this was a comment posting, check for comment form
            WeblogEntryCommentForm commentForm = (WeblogEntryCommentForm) request
                    .getAttribute("commentForm");
            if (commentForm != null) {
                initData.put("commentForm", commentForm);
            }

        } catch (Exception ex) {
            log.error("Error preparing page context for rendering", ex);
            if (!response.isCommitted()) {
                response.reset();
            }
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // Route request to appropriate handler
        PageRequestHandler handler = pageRouter.route(pageRequest);
        if (handler == null) {
            log.debug("No handler found for request");
            if (!response.isCommitted()) {
                response.reset();
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        log.debug("Request routed to handler: " + handler.getHandlerName());

        // Handle the request with the selected handler
        CachedContent renderedContent;
        try {
            renderedContent = handler.handle(pageRequest, request, initData);

            if (renderedContent == null) {
                log.debug("Handler returned null content");
                if (!response.isCommitted()) {
                    response.reset();
                }
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

        } catch (InvalidRequestException e) {
            log.debug("Handler threw InvalidRequestException: " + e.getMessage());
            if (!response.isCommitted()) {
                response.reset();
            }
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        } catch (Exception e) {
            log.error("Error handling request", e);
            if (!response.isCommitted()) {
                response.reset();
            }
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        // allow for hit counting
        if (!isSiteWide
                && (pageRequest.isWebsitePageHit() || pageRequest
                        .isOtherPageHit())) {
            this.processHit(weblog);
        }

        // post rendering process
        // flush rendered content to response
        log.debug("Flushing response output");
        response.setContentType(renderedContent.getContentType());
        response.setContentLength(renderedContent.getContent().length);
        response.getOutputStream().write(renderedContent.getContent());

        // cache rendered content. only cache if user is not logged in?
        if ((!this.excludeOwnerPages || !pageRequest.isLoggedIn())
                && request.getAttribute("skipCache") == null) {
            log.debug("PUT " + cacheKey);

            // put it in the right cache
            if (isSiteWide) {
                siteWideCache.put(cacheKey, renderedContent);
            } else {
                weblogPageCache.put(cacheKey, renderedContent);
            }
        } else {
            log.debug("SKIPPED " + cacheKey);
        }

        log.debug("Exiting doGet");
    }

    /**
     * Handle popup comments request (special case before routing).
     * Popup is a legacy feature where we render a special popup comments template.
     */
    private void handlePopupRequest(HttpServletRequest request, HttpServletResponse response,
            WeblogPageRequest pageRequest, String cacheKey,
            boolean isSiteWide, Weblog weblog) {
        log.debug("Handling popup comments request");

        try {
            // Prepare initial model data for popup handler
            Map<String, Object> initData = new HashMap<>();
            initData.put("requestParameters", request.getParameterMap());
            initData.put("parsedRequest", pageRequest);

            PageContext pageContext = JspFactory.getDefaultFactory()
                    .getPageContext(this, request, response, "", false,
                            RollerConstants.EIGHT_KB_IN_BYTES, true);
            initData.put("pageContext", pageContext);

            request.setAttribute("pageRequest", pageRequest);
            initData.put("urlStrategy", WebloggerFactory.getWeblogger()
                    .getUrlStrategy());

            WeblogEntryCommentForm commentForm = (WeblogEntryCommentForm) request
                    .getAttribute("commentForm");
            if (commentForm != null) {
                initData.put("commentForm", commentForm);
            }

            // Use popup handler
            PopupHandler popupHandler = new PopupHandler();
            CachedContent renderedContent = popupHandler.handle(pageRequest, request, initData);

            if (renderedContent == null) {
                if (!response.isCommitted()) {
                    response.reset();
                }
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Write response
            response.setContentType(renderedContent.getContentType());
            response.setContentLength(renderedContent.getContent().length);
            response.getOutputStream().write(renderedContent.getContent());

        } catch (Exception e) {
            log.error("Error handling popup request", e);
            try {
                if (!response.isCommitted()) {
                    response.reset();
                }
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception ee) {
                log.error("Error sending error response", ee);
            }
        }
    }

    /**
     * Handle POST requests.
     * 
     * We have this here because the comment servlet actually forwards some of
     * its requests on to us to render some pages with cusom messaging. We may
     * want to revisit this approach in the future and see if we can do this in
     * a different way, but for now this is the easy way.
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // make sure caching is disabled
        request.setAttribute("skipCache", "true");

        // handle just like a GET request
        this.doGet(request, response);
    }

    /**
     * Notify the hit tracker that it has an incoming page hit.
     */
    private void processHit(Weblog weblog) {

        HitCountQueue counter = HitCountQueue.getInstance();
        counter.processHit(weblog);
    }

    /**
     * Process the incoming request to extract referrer info and pass it on to
     * the referrer processing queue for tracking.
     * 
     * @return true if referrer was spam, false otherwise
     */
    private boolean processReferrer(HttpServletRequest request) {

        log.debug("processing referrer for " + request.getRequestURI());

        // bleh! because ref processing does a flush it will close
        // our hibernate session and cause lazy init exceptions on
        // objects we have fetched, so we need to use a separate
        // page request object for this
        WeblogPageRequest pageRequest;
        try {
            pageRequest = new WeblogPageRequest(request);
        } catch (InvalidRequestException ex) {
            return false;
        }

        // if this came from site-wide frontpage then skip it
        if (WebloggerRuntimeConfig.isSiteWideWeblog(pageRequest
                .getWeblogHandle())) {
            return false;
        }

        // if this came from a robot then don't process it
        if (robotPattern != null) {
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 0
                    && robotPattern.matcher(userAgent).matches()) {
                log.debug("skipping referrer from robot");
                return false;
            }
        }

        String referrerUrl = null;
        String[] schemes = { "http", "https" };
        UrlValidator urlValidator = new UrlValidator(schemes);
        if (urlValidator.isValid(request.getHeader("Referer"))) {
            referrerUrl = request.getHeader("Referer");
        }
        log.debug("referrer = " + referrerUrl);

        StringBuffer reqsb = request.getRequestURL();
        if (request.getQueryString() != null) {
            reqsb.append("?");
            reqsb.append(request.getQueryString());
        }
        String requestUrl = reqsb.toString();

        // if this came from persons own blog then don't process it
        String selfSiteFragment = "/" + pageRequest.getWeblogHandle();
        if (referrerUrl != null && referrerUrl.contains(selfSiteFragment)) {
            log.debug("skipping referrer from own blog");
            return false;
        }

        // validate the referrer
        if (pageRequest.getWeblogHandle() != null) {

            // Base page URLs, with and without www.
            String basePageUrlWWW = WebloggerRuntimeConfig
                    .getAbsoluteContextURL()
                    + "/"
                    + pageRequest.getWeblogHandle();
            String basePageUrl = basePageUrlWWW;
            if (basePageUrlWWW.startsWith("http://www.")) {
                // chop off the http://www.
                basePageUrl = "http://" + basePageUrlWWW.substring(11);
            }

            // ignore referrers coming from users own blog
            if (referrerUrl == null
                    || (!referrerUrl.startsWith(basePageUrl) && !referrerUrl
                            .startsWith(basePageUrlWWW))) {

                // validate the referrer
                if (referrerUrl != null) {
                    // treat editor referral as direct
                    int lastSlash = requestUrl.indexOf('/', 8);
                    if (lastSlash == -1) {
                        lastSlash = requestUrl.length();
                    }
                    String requestSite = requestUrl.substring(0, lastSlash);

                    return !(referrerUrl.startsWith(requestSite)
                            && referrerUrl.indexOf(".rol") >= requestSite.length())
                            && BannedwordslistChecker.checkReferrer(pageRequest.getWeblog(), referrerUrl);
                }
            } else {
                log.debug("Ignoring referer = " + referrerUrl);
                return false;
            }
        }

        return false;
    }
}
