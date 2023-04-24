package com.gitthub.wujun728.engine.core.base;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.gitthub.wujun728.engine.core.plugin.PluginManager;

@Component
public class LoadPluginOnSpringReady {
    @EventListener
    public void loadPlugins(ApplicationReadyEvent event){
        PluginManager.loadPlugins();
    }
}