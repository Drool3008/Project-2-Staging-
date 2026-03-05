/*
 * ArchivePageHandler wrapper that delegates to existing DateArchiveHandler
 * (and preserves existing archive-related behavior). This wrapper exists to
 * provide a clearer name in the refactor while not changing existing logic.
 */
package org.apache.roller.weblogger.ui.rendering.servlets.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.util.cache.CachedContent;
import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class ArchivePageHandler implements PageRequestHandler {

    private static Log log = LogFactory.getLog(ArchivePageHandler.class);
    private final DateArchiveHandler delegate = new DateArchiveHandler();

    @Override
    public boolean matches(WeblogPageRequest request) {
        boolean m = delegate.matches(request);
        log.debug("ArchivePageHandler.matches -> " + m);
        return m;
    }

    @Override
    public CachedContent handle(WeblogPageRequest request, HttpServletRequest servletRequest,
                                Map<String, Object> initData) throws Exception {
        log.debug("ArchivePageHandler: delegating handle to DateArchiveHandler");
        return delegate.handle(request, servletRequest, initData);
    }

    @Override
    public String getHandlerName() {
        return "ArchivePageHandler(delegate=DateArchiveHandler)";
    }
}
