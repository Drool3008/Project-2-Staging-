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
import org.apache.roller.util.RollerConstants;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.pojos.ThemeTemplate;
import org.apache.roller.weblogger.pojos.ThemeTemplate.ComponentType;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.ui.core.RollerContext;
import org.apache.roller.weblogger.ui.rendering.Renderer;
import org.apache.roller.weblogger.ui.rendering.RendererManager;
import org.apache.roller.weblogger.ui.rendering.model.ModelLoader;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.ui.rendering.util.InvalidRequestException;
import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;
import org.apache.roller.weblogger.util.cache.CachedContent;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for blog entry (permalink) page requests.
 * 
 * Responsible for:
 * - Matching requests that have a weblog anchor (entry permalink)
 * - Validating that the entry exists, is published, and locale matches
 * - Loading the entry-specific template (PERMALINK component type)
 * - Building the rendering model with the entry data
 * - Rendering and returning the content
 */
public class PermalinkHandler implements PageRequestHandler {

    private static Log log = LogFactory.getLog(PermalinkHandler.class);

    @Override
    public boolean matches(WeblogPageRequest request) {
        // Handle requests with a weblog anchor (blog entry permalink)
        return request.getWeblogAnchor() != null;
    }

    @Override
    public CachedContent handle(WeblogPageRequest request, HttpServletRequest servletRequest,
            Map<String, Object> initData) throws Exception {

        log.debug("PermalinkHandler: handling entry permalink");

        // Load the entry (already lazy-loaded by WeblogPageRequest)
        WeblogEntry entry = request.getWeblogEntry();

        // Validate entry exists
        if (entry == null) {
            throw new InvalidRequestException("Entry not found: " + request.getWeblogAnchor());
        }

        // Validate entry is published
        if (!entry.isPublished()) {
            throw new InvalidRequestException("Entry not published: " + request.getWeblogAnchor());
        }

        // Validate entry publish date is not in future
        if (new Date().before(entry.getPubTime())) {
            throw new InvalidRequestException("Entry not yet published: " + request.getWeblogAnchor());
        }

        // Validate locale if specified
        if (request.getLocale() != null &&
                !entry.getLocale().startsWith(request.getLocale())) {
            throw new InvalidRequestException("Entry locale mismatch");
        }

        log.debug("Entry validation passed");

        // Load the PERMALINK template
        ThemeTemplate page = request.getWeblog().getTheme().getTemplateByAction(
                ComponentType.PERMALINK);

        if (page == null) {
            throw new InvalidRequestException("Permalink template not found");
        }

        // Build the rendering model
        Map<String, Object> model = new HashMap<>();
        Map<String, Object> pageInitData = new HashMap<>(initData);
        pageInitData.put("entry", entry);

        try {
            ModelLoader.loadModels(WebloggerConfig.getProperty("rendering.pageModels"), model, pageInitData, true);

            // Load special models for site-wide blog
            if (WebloggerRuntimeConfig.isSiteWideWeblog(request.getWeblog().getHandle())) {
                String siteModels = WebloggerConfig.getProperty("rendering.siteModels");
                ModelLoader.loadModels(siteModels, model, pageInitData, true);
            }
        } catch (WebloggerException ex) {
            log.error("Error loading model objects for permalink", ex);
            throw ex;
        }

        // Render content
        return renderContent(page, model, request);
    }

    /**
     * Helper method to render content using the specified template.
     */
    private CachedContent renderContent(ThemeTemplate page, Map<String, Object> model,
            WeblogPageRequest request) throws Exception {
        try {
            Renderer renderer = RendererManager.getRenderer(page, request.getDeviceType());

            // Determine content type (matching master logic)
            String contentType;
            if (page.getOutputContentType() != null
                    && !page.getOutputContentType().isEmpty()) {
                contentType = page.getOutputContentType() + "; charset=utf-8";
            } else {
                final String defaultContentType = "text/html; charset=utf-8";
                if (page.getLink() == null) {
                    contentType = defaultContentType;
                } else {
                    String mimeType = RollerContext.getServletContext()
                            .getMimeType(page.getLink());
                    if (mimeType != null) {
                        contentType = mimeType + "; charset=utf-8";
                    } else {
                        contentType = defaultContentType;
                    }
                }
            }

            CachedContent rendererOutput = new CachedContent(
                    RollerConstants.TWENTYFOUR_KB_IN_BYTES, contentType);

            renderer.render(model, rendererOutput.getCachedWriter());
            rendererOutput.flush();
            rendererOutput.close();

            return rendererOutput;
        } catch (Exception e) {
            log.error("Error rendering permalink page", e);
            throw e;
        }
    }

    @Override
    public String getHandlerName() {
        return "PermalinkHandler";
    }
}
