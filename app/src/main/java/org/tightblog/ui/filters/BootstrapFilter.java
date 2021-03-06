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
package org.tightblog.ui.filters;

import org.tightblog.business.WebloggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Redirects clients to install page when app is not bootstrapped, otherwise does nothing.
 */
public class BootstrapFilter implements Filter {

    private static Logger log = LoggerFactory.getLogger(BootstrapFilter.class);

    private ServletContext context = null;

    public void init(FilterConfig filterConfig) throws ServletException {
        context = filterConfig.getServletContext();
    }

    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;

        if (!WebloggerContext.isBootstrapped() && !isInstallUrl(request.getRequestURI())) {
            log.debug("Forwarding to install page");
            // install page will check database connectivity & schema status and bootstrap if all OK.
            RequestDispatcher rd = context.getRequestDispatcher("/tb-ui/install/install");
            rd.forward(req, res);
        } else {
            chain.doFilter(request, res);
        }
    }

    private boolean isInstallUrl(String uri) {
        return (uri != null && (
                uri.endsWith("/install/bootstrap") || uri.endsWith("/install/create") ||
                        uri.endsWith(".js") || uri.endsWith(".css")));
    }

    public void destroy() {
    }
}
