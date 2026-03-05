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
import org.apache.roller.weblogger.pojos.StaticThemeTemplate;
import org.apache.roller.weblogger.pojos.TemplateRendition.TemplateLanguage;
import org.apache.roller.weblogger.pojos.ThemeTemplate;
import org.apache.roller.weblogger.ui.rendering.Renderer;
import org.apache.roller.weblogger.ui.rendering.RendererManager;
import org.apache.roller.weblogger.ui.rendering.model.ModelLoader;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;
import org.apache.roller.weblogger.util.cache.CachedContent;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler for popup comments page requests.
 * 
 * Responsible for:
 * - Matching requests with "popup" parameter
 * - Loading the popup comments template (_popupcomments) if available
 * - Falling back to default built-in popup comments template
 * - Building the rendering model
 * - Rendering and returning the content
 */
public class PopupHandler implements PageRequestHandler {

    private static Log log = LogFactory.getLog(PopupHandler.class);

    @Override
    public boolean matches(WeblogPageRequest request) {
        // Note: This handler needs access to servlet request to check for popup param
        // The routing should be done at servlet level before calling handlers
        // This is a marker - actual routing happens in PageServlet
        return false;
    }

    /**
     * Alternative match method that takes servlet request directly.
     * This is used by PageServlet before creating WeblogPageRequest routing.
     */
    public boolean matchesPopup(HttpServletRequest servletRequest) {
        return servletRequest.getParameter("popup") != null;
    }

    @Override
    public CachedContent handle(WeblogPageRequest request, HttpServletRequest servletRequest,
            Map<String, Object> initData) throws Exception {

        log.debug("PopupHandler: handling popup comments request");

        ThemeTemplate page = null;

        // Try to load user's custom _popupcomments template
        try {
            page = request.getWeblog().getTheme().getTemplateByName("_popupcomments");
        } catch (Exception e) {
            log.debug("Custom popupcomments template not found, using default");
        }

        // If no custom template, use the built-in default
        if (page == null) {
            page = new StaticThemeTemplate(
                    "templates/weblog/popupcomments.vm",
                    TemplateLanguage.VELOCITY);
            log.debug("Using default popupcomments template");
        }

        // Build the rendering model
        Map<String, Object> model = new HashMap<>();
        Map<String, Object> pageInitData = new HashMap<>(initData);

        try {
            ModelLoader.loadModels(WebloggerConfig.getProperty("rendering.pageModels"), model, pageInitData, true);
        } catch (WebloggerException ex) {
            log.error("Error loading model objects for popup page", ex);
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

            String contentType = page.getOutputContentType() != null
                    ? page.getOutputContentType() + "; charset=utf-8"
                    : "text/html; charset=utf-8";

            CachedContent rendererOutput = new CachedContent(
                    RollerConstants.TWENTYFOUR_KB_IN_BYTES, contentType);

            renderer.render(model, rendererOutput.getCachedWriter());
            rendererOutput.flush();
            rendererOutput.close();

            return rendererOutput;
        } catch (Exception e) {
            log.error("Error rendering popup comments page", e);
            throw e;
        }
    }

    @Override
    public String getHandlerName() {
        return "PopupHandler";
    }
}
