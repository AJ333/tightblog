/*
   Copyright 2017 Glen Mazza

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.tightblog.rendering.thymeleaf;

import org.attoparser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.spel.SpelEvaluationException;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.tightblog.pojos.Template;
import org.tightblog.rendering.Renderer;
import org.tightblog.util.WebloggerException;

import java.io.Writer;
import java.util.Map;

public class ThymeleafRenderer implements Renderer {

    private static Logger logger = LoggerFactory.getLogger(ThemeTemplateResolver.class);

    private SpringTemplateEngine templateEngine;

    public void setTemplateEngine(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public void render(Template template, Map<String, Object> model, Writer writer) throws WebloggerException {
        try {
            Context ctx = new Context();
            ctx.setVariables(model);
            templateEngine.process(template.getId(), ctx, writer);
        } catch (TemplateInputException e) {
            // Provide end-user friendly error messages for at least two common errors:
            // unknown method call and unknown property name
            Throwable firstCause = e.getCause();
            if (firstCause instanceof ParseException) {
                Context ctx = new Context();
                ctx.setVariable("templateName", template.getName());
                // output commonly "Exception evaluating Spring EL expression..."
                ctx.setVariable("firstMessage", firstCause.getMessage());
                Throwable secondCause = firstCause.getCause();
                if (secondCause instanceof TemplateProcessingException) {
                    Throwable thirdCause = secondCause.getCause();
                    if (thirdCause instanceof SpelEvaluationException) {
                        // output commonly "unknown [method|property] XXX..."
                        ctx.setVariable("secondMessage", thirdCause.getMessage());
                    }
                }
                templateEngine.process("error-page", ctx, writer);
            } else {
                throw e;
            }
        }
    }
}
