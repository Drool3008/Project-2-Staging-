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
 * Handler for homepage/default page requests.
 * 
 * This is the fallback/default handler that matches any request.
 * It should be registered last in the handler chain so it catches all
 * requests that don't match more specific handlers.
 * 
 * Responsible for:
 * - Matching all requests (default fallback)
 * - Loading the default template for the weblog
 * - Building the rendering model with default/homepage data
 * - Rendering and returning the content
 */
public class HomepageHandler implements PageRequestHandler {

    private static Log log = LogFactory.getLog(HomepageHandler.class);

    @Override
    public boolean matches(WeblogPageRequest request) {
        // This is the fallback handler, always matches
        return true;
    }

    @Override
    public CachedContent handle(WeblogPageRequest request, HttpServletRequest servletRequest,
            Map<String, Object> initData) throws Exception {

        log.debug("HomepageHandler: handling homepage/default request");

        // Load the default template for the weblog
        ThemeTemplate page = request.getWeblog().getTheme().getDefaultTemplate();

        if (page == null) {
            throw new InvalidRequestException("Default template not found for weblog");
        }

        log.debug("Default template loaded");

        // Build the rendering model
        Map<String, Object> model = new HashMap<>();
        Map<String, Object> pageInitData = new HashMap<>(initData);

        try {
            // Load page models
            ModelLoader.loadModels(WebloggerConfig.getProperty("rendering.pageModels"), model, pageInitData, true);

            // Load special models for site-wide blog
            if (WebloggerRuntimeConfig.isSiteWideWeblog(request.getWeblog().getHandle())) {
                String siteModels = WebloggerConfig.getProperty("rendering.siteModels");
                ModelLoader.loadModels(siteModels, model, pageInitData, true);
            }
        } catch (WebloggerException ex) {
            log.error("Error loading model objects for homepage", ex);
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
            log.error("Error rendering homepage", e);
            throw e;
        }
    }

    @Override
    public String getHandlerName() {
        return "HomepageHandler";
    }
}
