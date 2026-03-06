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
import org.apache.roller.weblogger.pojos.WeblogEntry;

/**
 * Command pattern implementation: Unstars a weblog entry for a user.
 */
public class UnstarEntryCommand implements StarCommand {

    private final User user;
    private final WeblogEntry entry;
    private final StarFacade starFacade;

    public UnstarEntryCommand(User user, WeblogEntry entry, StarFacade starFacade) {
        this.user = user;
        this.entry = entry;
        this.starFacade = starFacade;
    }

    @Override
    public void execute() throws WebloggerException {
        starFacade.unstarEntry(user, entry);
    }

    @Override
    public String getDescription() {
        return "Unstar entry '" + entry.getTitle() + "' by user '" + user.getUserName() + "'";
    }
}
