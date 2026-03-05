/*
 * Wrapper BlogEntryHandler that delegates to the existing PermalinkHandler.
 *
 * Purpose: provide a clearer handler name (BlogEntryHandler) for the
 * refactor while preserving the exact behavior of the original
 * PermalinkHandler. This keeps routing and rendering identical.
 */
package org.apache.roller.weblogger.ui.rendering.servlets.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.util.cache.CachedContent;
import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class BlogEntryHandler implements PageRequestHandler {

    private static Log log = LogFactory.getLog(BlogEntryHandler.class);
    // delegate to existing implementation to preserve behavior
    private final PermalinkHandler delegate = new PermalinkHandler();

    @Override
    public boolean matches(WeblogPageRequest request) {
        // delegate matching to the existing permalink handler
        boolean m = delegate.matches(request);
        log.debug("BlogEntryHandler.matches -> " + m);
        return m;
    }

    @Override
    public CachedContent handle(WeblogPageRequest request, HttpServletRequest servletRequest,
                                Map<String, Object> initData) throws Exception {
        log.debug("BlogEntryHandler: delegating handle to PermalinkHandler");
        return delegate.handle(request, servletRequest, initData);
    }

    @Override
    public String getHandlerName() {
        return "BlogEntryHandler(delegate=PermalinkHandler)";
    }
}
