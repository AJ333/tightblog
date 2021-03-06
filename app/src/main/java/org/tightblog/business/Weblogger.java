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
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 * Source file modified from the original ASF source; all changes made
 * are also under Apache License.
 */
package org.tightblog.business;

import org.tightblog.business.themes.ThemeManager;

/**
 * Helper methods to obtain managers of the Weblogger business tier.
 * Meant for cases where Spring DI is not easily available or inefficient to use.
 */
public class Weblogger {

    // managers
    private final ThemeManager themeManager;
    private final WeblogManager weblogManager;
    private final WeblogEntryManager weblogEntryManager;

    // url strategy
    private final URLStrategy urlStrategy;

    /**
     * Single constructor.
     */
    protected Weblogger(
            ThemeManager themeManager,
            WeblogManager weblogManager,
            WeblogEntryManager weblogEntryManager,
            URLStrategy urlStrategy) {

        this.themeManager = themeManager;
        this.weblogManager = weblogManager;
        this.weblogEntryManager = weblogEntryManager;
        this.urlStrategy = urlStrategy;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    public WeblogEntryManager getWeblogEntryManager() {
        return weblogEntryManager;
    }

    public WeblogManager getWeblogManager() {
        return weblogManager;
    }

    public URLStrategy getUrlStrategy() {
        return urlStrategy;
    }

}
