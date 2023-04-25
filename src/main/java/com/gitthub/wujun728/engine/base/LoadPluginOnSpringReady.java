package com.gitthub.wujun728.engine.base;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.gitthub.wujun728.engine.plugin.PluginManager;

@Component
public class LoadPluginOnSpringReady {
    @EventListener
    public void loadPlugins(ApplicationReadyEvent event){
        PluginManager.loadPlugins();
    }
}