/*
 * Wrapper handler that ensures models required by the homepage are loaded
 * and that a frontpage weblog is set when the incoming request is for the
 * site root (no weblog handle and no path info).
 */
package org.apache.roller.weblogger.ui.rendering.servlets.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * HomepageHandlerWrapper - guarantees model loading and frontpage weblog
 * wiring before delegating to the existing HomepageHandler.
 */
public class HomepageHandlerWrapper implements PageRequestHandler {

    private static final Log log = LogFactory.getLog(HomepageHandlerWrapper.class);

    @Override
    public boolean matches(WeblogPageRequest request) {
        // wrapper is used explicitly by the router for root requests, but
        // we'll also return true as a fallback.
        return true;
    }

    @Override
    public org.apache.roller.weblogger.util.cache.CachedContent handle(WeblogPageRequest request,
                                                                       HttpServletRequest servletRequest,
                                                                       Map<String, Object> initData) throws Exception {

        log.debug("HomepageHandlerWrapper: preparing frontpage weblog");

        // If this request didn't resolve a weblog, try wiring the configured
        // frontpage weblog so the existing HomepageHandler can operate.
        if (request.getWeblog() == null) {
            try {
                String frontHandle = WebloggerRuntimeConfig.getProperty("site.frontpage.weblog.handle");
                if (frontHandle != null && !frontHandle.isBlank()) {
                    Weblog front = WebloggerFactory.getWeblogger().getWeblogManager()
                            .getWeblogByHandle(frontHandle, Boolean.TRUE);
                    if (front != null) {
                        request.setWeblog(front);
                        log.debug("HomepageHandlerWrapper: set frontpage weblog to " + frontHandle);
                    }
                }
            } catch (WebloggerException ex) {
                log.warn("Unable to lookup frontpage weblog", ex);
            }
        }

        // Delegate to the original HomepageHandler for actual rendering
        // (it handles model loading including site-wide models).
        HomepageHandler delegate = new HomepageHandler();
        return delegate.handle(request, servletRequest, initData);
    }

    @Override
    public String getHandlerName() {
        return "HomepageHandlerWrapper";
    }
}
