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
 *
 * Source file modified from the original ASF source; all changes made
 * are also under Apache License.
 */

package org.apache.roller.weblogger.ui.struts2.editor;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WeblogManager;
import org.apache.roller.weblogger.pojos.GlobalRole;
import org.apache.roller.weblogger.pojos.WeblogCategory;
import org.apache.roller.weblogger.pojos.WeblogRole;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;

/**
 * Manage weblog categories.
 */
public class Categories extends UIAction {

	private static Log log = LogFactory.getLog(Categories.class);

    private WeblogManager weblogManager;

    public void setWeblogManager(WeblogManager weblogManager) {
        this.weblogManager = weblogManager;
    }

    // all categories from the action weblog
	private List<WeblogCategory> allCategories;

	public Categories() {
		this.actionName = "categories";
		this.desiredMenu = "editor";
		this.pageTitle = "categoriesForm.rootTitle";
	}

    @Override
    public GlobalRole requiredGlobalRole() {
        return GlobalRole.BLOGGER;
    }

    @Override
    public WeblogRole requiredWeblogRole() {
        return WeblogRole.POST;
    }

	public String execute() {
		try {
			allCategories = weblogManager.getWeblogCategories(getActionWeblog());
		} catch (WebloggerException ex) {
			log.error("Error building categories list", ex);
			addError("Error building categories list");
		}

		return LIST;
	}

	public List<WeblogCategory> getAllCategories() {
		return allCategories;
	}

	public void setAllCategories(List<WeblogCategory> allCategories) {
		this.allCategories = allCategories;
	}
}
