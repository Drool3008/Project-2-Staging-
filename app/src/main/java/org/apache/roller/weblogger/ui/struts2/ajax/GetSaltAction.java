/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
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
 * limitations under the License.
 */
package org.apache.roller.weblogger.ui.struts2.ajax;

import com.opensymphony.xwork2.ActionSupport;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.roller.weblogger.ui.core.RollerSession;
import org.apache.roller.weblogger.ui.rendering.util.cache.SaltCache;
import org.apache.struts2.interceptor.ServletRequestAware;

/**
 * Returns a fresh CSRF salt token as JSON, for use after an AJAX POST
 * consumes the current page-level salt.
 */
public class GetSaltAction extends ActionSupport implements ServletRequestAware {

    private static final long serialVersionUID = 1L;

    private HttpServletRequest servletRequest;
    private Map<String, Object> result = new HashMap<>();

    @Override
    public void setServletRequest(HttpServletRequest request) {
        this.servletRequest = request;
    }

    @Override
    public String execute() {
        RollerSession rs = RollerSession.getRollerSession(servletRequest);
        String userId = (rs != null && rs.getAuthenticatedUser() != null)
                ? rs.getAuthenticatedUser().getId() : "";
        String salt = RandomStringUtils.random(20, 0, 0, true, true, null, new SecureRandom());
        SaltCache.getInstance().put(salt, userId);
        result.put("salt", salt);
        return SUCCESS;
    }

    public Map<String, Object> getResult() {
        return result;
    }
}
