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

import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;
import org.apache.roller.weblogger.util.cache.CachedContent;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Strategy interface for handling different types of weblog page requests.
 * 
 * Implementations of this interface are responsible for:
 * 1. Determining if they can handle a particular request (matches method)
 * 2. Validating the request
 * 3. Building the model for rendering
 * 4. Selecting the appropriate template
 * 5. Rendering the content and returning it as CachedContent
 * 
 * This interface helps break down the God Class anti-pattern by delegating
 * page-type-specific logic to focused handler implementations.
 */
public interface PageRequestHandler {

    /**
     * Determines if this handler can process the given request.
     * 
     * @param request the parsed weblog page request
     * @return true if this handler should handle this request, false otherwise
     */
    boolean matches(WeblogPageRequest request);

    /**
     * Handle the request and produce rendered content.
     * 
     * @param request the parsed weblog page request
     * @param servletRequest the raw HTTP servlet request
     * @param initData initial model data (requestParameters, parsedRequest, pageContext, etc.)
     * @return CachedContent containing the rendered output, or null if unable to render
     * @throws Exception if handler encounters an error (e.g., NotFoundException, WebloggerException)
     */
    CachedContent handle(WeblogPageRequest request, HttpServletRequest servletRequest,
                        Map<String, Object> initData) throws Exception;

    /**
     * Returns a human-readable name for this handler (useful for logging/debugging).
     * 
     * @return handler name (e.g., "PermalinkHandler", "TagsHandler")
     */
    String getHandlerName();
}
