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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.WeblogManager;
import org.apache.roller.weblogger.business.plugins.PluginManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.plugins.entry.WeblogEntryPlugin;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.pojos.GlobalRole;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;
import org.apache.roller.weblogger.util.Blacklist;
import org.apache.roller.weblogger.util.cache.CacheManager;
import org.apache.struts2.interceptor.validation.SkipValidation;


/**
 * Action for modifying weblog configuration.
 */
public class WeblogConfig extends UIAction {
    
    private static Log log = LogFactory.getLog(WeblogConfig.class);

    private WeblogEntryManager weblogEntryManager;

    public void setWeblogEntryManager(WeblogEntryManager weblogEntryManager) {
        this.weblogEntryManager = weblogEntryManager;
    }

    private WeblogManager weblogManager;

    public void setWeblogManager(WeblogManager weblogManager) {
        this.weblogManager = weblogManager;
    }

    private PluginManager pluginManager;

    public void setPluginManager(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    // bean for managing submitted data
    private Weblog bean = new Weblog();
    
    // categories list
    private List weblogCategories = Collections.emptyList();
    
    // list of available plugins
    private List pluginsList = Collections.emptyList();

    // list of available editors
    private static List<Pair<String, String>> editorsList = new ArrayList<>(2);

    static {
        editorsList.add(Pair.of("editor-text.jsp", "editor.text.name"));
        editorsList.add(Pair.of("editor-xinha.jsp", "editor.xinha.name"));
    }

    public WeblogConfig() {
        this.actionName = "weblogConfig";
        this.desiredMenu = "editor";
        this.pageTitle = "websiteSettings.title";
    }
    
    
    @Override
    public GlobalRole requiredGlobalRole() {
        return GlobalRole.BLOGGER;
    }

    public void prepare() {
        
        try {
            // set categories list
            setWeblogCategories(weblogManager.getWeblogCategories(getActionWeblog()));
            
            // set plugins list
            Map<String, WeblogEntryPlugin> pluginsMap = pluginManager.getWeblogEntryPlugins(getActionWeblog());
            List<WeblogEntryPlugin> plugins = new ArrayList<>();
            for (WeblogEntryPlugin entryPlugin : pluginsMap.values()) {
                plugins.add(entryPlugin);
            }

            // sort
            setPluginsList(plugins);

        } catch (Exception ex) {
            log.error("Error preparing weblog config action", ex);
        }
    }
    
    
    @SkipValidation
    public String execute() {
        
        // load bean with data from weblog
        Weblog currentWeblog = getActionWeblog();
        bean.setHandle(currentWeblog.getHandle());
        bean.setName(currentWeblog.getName());
        bean.setTagline(currentWeblog.getTagline());
        bean.setEditorPage(currentWeblog.getEditorPage());
        bean.setBlacklist(currentWeblog.getBlacklist());
        bean.setAllowComments(currentWeblog.getAllowComments());
        bean.setDefaultAllowComments(currentWeblog.getDefaultAllowComments());
        bean.setDefaultCommentDays(currentWeblog.getDefaultCommentDays());
        bean.setModerateComments(currentWeblog.getModerateComments());
        bean.setEmailComments(currentWeblog.getEmailComments());
        bean.setEmailAddress(currentWeblog.getEmailAddress());
        bean.setLocale(currentWeblog.getLocale());
        bean.setTimeZone(currentWeblog.getTimeZone());
        bean.setDefaultPlugins(currentWeblog.getDefaultPlugins());
        bean.setEntryDisplayCount(currentWeblog.getEntryDisplayCount());
        bean.setActive(currentWeblog.isActive());
        bean.setCommentModerationRequired(currentWeblog.getCommentModerationRequired());
        bean.setAnalyticsCode(currentWeblog.getAnalyticsCode());
        bean.setIconPath(currentWeblog.getIconPath());
        bean.setAbout(currentWeblog.getAbout());
        return INPUT;
    }
    
    
    /**
     * Save weblog configuration.
     */
    public String save() {
        
        // run validation
        myValidate();
        
        if(!hasActionErrors()) {
            try {
                Weblog weblog = getActionWeblog();

                if (bean.getAnalyticsCode() != null) {
                    bean.setAnalyticsCode(bean.getAnalyticsCode().trim());
                }

                weblog.setName(bean.getName());
                weblog.setTagline(bean.getTagline());
                weblog.setEditorPage(bean.getEditorPage());
                weblog.setBlacklist(bean.getBlacklist());
                weblog.setAllowComments(bean.getAllowComments());
                weblog.setDefaultAllowComments(bean.getDefaultAllowComments());
                weblog.setModerateComments(bean.getModerateComments());
                weblog.setEmailComments(bean.getEmailComments());
                weblog.setEmailAddress(bean.getEmailAddress());
                weblog.setLocale(bean.getLocale());
                weblog.setTimeZone(bean.getTimeZone());
                weblog.setDefaultPlugins(bean.getDefaultPlugins());
                weblog.setEntryDisplayCount(bean.getEntryDisplayCount());
                weblog.setActive(bean.isActive());
                weblog.setCommentModerationRequired(bean.getCommentModerationRequired());
                weblog.setIconPath(bean.getIconPath());
                weblog.setAbout(bean.getAbout());
                weblog.setAnalyticsCode(bean.getAnalyticsCode());
                weblog.setDefaultCommentDays(bean.getDefaultCommentDays());

                // ROL-485: comments not allowed on inactive weblogs
                if(!weblog.isActive()) {
                    weblog.setAllowComments(Boolean.FALSE);
                    addMessage("websiteSettings.commentsOffForInactiveWeblog");
                }

                // save config
                weblogManager.saveWeblog(weblog);

                // ROL-1050: apply comment defaults to existing entries
                if(bean.isApplyCommentDefaults()) {
                    weblogEntryManager.applyCommentDefaultsToEntries(weblog);
                }

                // flush
                WebloggerFactory.flush();

                addMessage("websiteSettings.savedChanges");

                // Clear cache entries associated with website
                CacheManager.invalidate(weblog);

            } catch (Exception ex) {
                log.error("Error updating weblog config", ex);
                addError("Error updating configuration");
            }
        }
        
        return  INPUT;
    }
    
    
    private void myValidate() {
        if (StringUtils.isEmpty(bean.getName())) {
            addError("WeblogConfig.error.nameNull");
        }

        // make sure user didn't enter an invalid entry display count
        int maxEntries = WebloggerRuntimeConfig.getIntProperty("site.pages.maxEntries");
        if(bean.getEntryDisplayCount() > maxEntries) {
            addError("websiteSettings.error.entryDisplayCount");
        }
        
        // check blacklist
        List<Pattern> regexRules = new ArrayList<>();
        List<String> stringRules = new ArrayList<>();
        try {
            // just for testing/counting, this does not persist rules in any way
            Blacklist.populateSpamRules(bean.getBlacklist(), null, stringRules, regexRules);
            addMessage("websiteSettings.acceptedBlacklist",
                    Arrays.asList(new String[] {""+stringRules.size(), ""+regexRules.size()}));
        } catch (Exception e) {
            addError("websiteSettings.error.processingBlacklist", e.getMessage());
        }
    }
    
    
    public Weblog getBean() {
        return bean;
    }

    public void setBean(Weblog bean) {
        this.bean = bean;
    }

    public List getWeblogCategories() {
        return weblogCategories;
    }

    public void setWeblogCategories(List weblogCategories) {
        this.weblogCategories = weblogCategories;
    }

    public List getEditorsList() {
        return editorsList;
    }
    
    public List getPluginsList() {
        return pluginsList;
    }

    public void setPluginsList(List pluginsList) {
        this.pluginsList = pluginsList;
    }
    
}
