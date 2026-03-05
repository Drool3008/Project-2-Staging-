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
import org.apache.roller.weblogger.ui.core.RollerContext;
import org.apache.roller.weblogger.ui.rendering.Renderer;
import org.apache.roller.weblogger.ui.rendering.RendererManager;
import org.apache.roller.weblogger.ui.rendering.model.ModelLoader;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.ui.rendering.util.InvalidRequestException;
import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;
import org.apache.roller.weblogger.util.cache.CachedContent;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for date-based archive page requests.
 * 
 * Responsible for:
 * - Matching requests that have a date specified
 * - Validating that the date is in valid format (YYYYMMDD or YYYYMM)
 * - Loading the default template for date archive view
 * - Building the rendering model with date-based entry data
 * - Rendering and returning the content
 */
public class DateArchiveHandler implements PageRequestHandler {

    private static Log log = LogFactory.getLog(DateArchiveHandler.class);

    @Override
    public boolean matches(WeblogPageRequest request) {
        // Handle date-based archive requests
        return "date".equals(request.getContext()) &&
                request.getWeblogDate() != null;
    }

    @Override
    public CachedContent handle(WeblogPageRequest request, HttpServletRequest servletRequest,
            Map<String, Object> initData) throws Exception {

        log.debug("DateArchiveHandler: handling date archive request");

        String dateString = request.getWeblogDate();

        // Note: date validation already done in WeblogPageRequest parsing
        // Just ensure it's present
        if (dateString == null || dateString.isEmpty()) {
            throw new InvalidRequestException("Invalid date string");
        }

        log.debug("Date archive validation passed");

        // Load the page template (use default template for date view)
        ThemeTemplate page = request.getWeblog().getTheme().getDefaultTemplate();

        if (page == null) {
            throw new InvalidRequestException("Default template not found");
        }

        // Build the rendering model
        Map<String, Object> model = new HashMap<>();
        Map<String, Object> pageInitData = new HashMap<>(initData);
        pageInitData.put("date", dateString);

        try {
            ModelLoader.loadModels(WebloggerConfig.getProperty("rendering.pageModels"), model, pageInitData, true);

            // Load special models for site-wide blog
            if (WebloggerRuntimeConfig.isSiteWideWeblog(request.getWeblog().getHandle())) {
                String siteModels = WebloggerConfig.getProperty("rendering.siteModels");
                ModelLoader.loadModels(siteModels, model, pageInitData, true);
            }
        } catch (WebloggerException ex) {
            log.error("Error loading model objects for date archive page", ex);
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
            log.error("Error rendering date archive page", e);
            throw e;
        }
    }

    @Override
    public String getHandlerName() {
        return "DateArchiveHandler";
    }
}
