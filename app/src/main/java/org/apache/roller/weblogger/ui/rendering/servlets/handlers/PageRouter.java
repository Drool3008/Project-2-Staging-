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

package org.apache.roller.weblogger.ui.rendering.servlets.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes weblog page requests to the appropriate handler implementation.
 * 
 * Uses a chain-of-responsibility pattern with an ordered list of handlers.
 * The first handler that matches the request (via matches()) is used.
 * 
 * This class is responsible for:
 * - Maintaining a registry of available page request handlers
 * - Finding the right handler for a given request
 * - Delegating request processing to that handler
 */
public class PageRouter {

    private static Log log = LogFactory.getLog(PageRouter.class);

    private final List<PageRequestHandler> handlers;

    /**
     * Create a router with the specified handlers.
     * 
     * Handlers are checked in the order they are provided, so order matters.
     * More specific handlers should come before more general ones.
     * 
     * @param handlers list of handlers to check in order
     */
    public PageRouter(List<PageRequestHandler> handlers) {
        this.handlers = new ArrayList<>(handlers);
        log.info("PageRouter initialized with " + handlers.size() + " handlers");
        for (PageRequestHandler h : handlers) {
            log.debug("  - " + h.getHandlerName());
        }
    }

    /**
     * Find the appropriate handler for the given request.
     * 
     * @param request the parsed weblog page request
     * @return the first handler that matches, or null if no handler matches
     */
    public PageRequestHandler route(WeblogPageRequest request) {
        // Special-case site root requests (no weblog handle and no extra path)
        try {
            if (request.getWeblog() == null && request.getPathInfo() == null) {
                log.debug("Routing site root request to HomepageHandlerWrapper");
                return new HomepageHandlerWrapper();
            }
        } catch (Exception e) {
            // ignore and fall through to normal routing
        }
        for (PageRequestHandler handler : handlers) {
            if (handler.matches(request)) {
                log.debug("Routed to handler: " + handler.getHandlerName());
                return handler;
            }
        }
        log.debug("No handler matched the request");
        return null;
    }

    /**
     * Register an additional handler at the end of the handler chain.
     * 
     * @param handler the handler to add
     */
    public void registerHandler(PageRequestHandler handler) {
        handlers.add(handler);
        log.info("Registered handler: " + handler.getHandlerName());
    }

    /**
     * Get the count of registered handlers.
     * 
     * @return number of handlers
     */
    public int getHandlerCount() {
        return handlers.size();
    }
}
