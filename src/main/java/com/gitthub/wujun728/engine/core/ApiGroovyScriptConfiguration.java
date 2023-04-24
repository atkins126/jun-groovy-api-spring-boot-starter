
package com.gitthub.wujun728.engine.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.gitthub.wujun728.engine.core.controller.GroovyScriptController;

import javax.script.ScriptEngineManager;

@Configuration
@ConditionalOnProperty(prefix = "spring.groovy-api", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(GroovyScriptController.class)
public class ApiGroovyScriptConfiguration {
    @Bean
    public ScriptEngineManager scriptEngineManager() {
        return new ScriptEngineManager();
    }
}
