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
package org.apache.roller.weblogger.business.star;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.StarFacade;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.Weblog;

/**
 * Command pattern implementation: Stars a weblog for a user.
 */
public class StarWeblogCommand implements StarCommand {

    private final User user;
    private final Weblog weblog;
    private final StarFacade starFacade;

    public StarWeblogCommand(User user, Weblog weblog, StarFacade starFacade) {
        this.user = user;
        this.weblog = weblog;
        this.starFacade = starFacade;
    }

    @Override
    public void execute() throws WebloggerException {
        starFacade.starWeblog(user, weblog);
    }

    @Override
    public String getDescription() {
        return "Star weblog '" + weblog.getHandle() + "' by user '" + user.getUserName() + "'";
    }
}
