/*
 * CommentPageHandler wrapper. Comment rendering is historically handled by
 * popup/permaling logic; this wrapper delegates to PopupHandler when
 * appropriate, otherwise falls back to PermalinkHandler. This keeps the
 * comment-related behavior identical while providing a dedicated handler.
 */
package org.apache.roller.weblogger.ui.rendering.servlets.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.util.cache.CachedContent;
import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class CommentPageHandler implements PageRequestHandler {

    private static Log log = LogFactory.getLog(CommentPageHandler.class);
    private final PopupHandler popupDelegate = new PopupHandler();
    private final PermalinkHandler permalinkDelegate = new PermalinkHandler();

    @Override
    public boolean matches(WeblogPageRequest request) {
        // Try to detect comment-specific requests. Historically comments
        // can be rendered inline (permalink) or via popup. We check the
        // popup handler first.
        boolean m = popupDelegate.matches(request) || permalinkDelegate.matches(request);
        log.debug("CommentPageHandler.matches -> " + m);
        return m;
    }

    @Override
    public CachedContent handle(WeblogPageRequest request, HttpServletRequest servletRequest,
                                Map<String, Object> initData) throws Exception {
        log.debug("CommentPageHandler: delegating to popup or permalink handler");
        // Prefer popup if it matches (legacy behavior in PageServlet handles popup specially)
        if (popupDelegate.matches(request)) {
            return popupDelegate.handle(request, servletRequest, initData);
        }
        return permalinkDelegate.handle(request, servletRequest, initData);
    }

    @Override
    public String getHandlerName() {
        return "CommentPageHandler(delegate=PopupHandler|PermalinkHandler)";
    }
}
