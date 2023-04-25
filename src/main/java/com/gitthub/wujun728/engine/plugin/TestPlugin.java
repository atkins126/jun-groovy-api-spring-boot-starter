package com.gitthub.wujun728.engine.plugin;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitthub.wujun728.engine.entity.ApiConfig;

import javax.servlet.http.HttpServletRequest;

public abstract class TestPlugin implements BasePlugin {

    public Logger logger = LoggerFactory.getLogger(TestPlugin.class);

    /**
     * 告警逻辑
     * @param e 异常
     * @param config API元数据
     * @param request 请求
     * @param pluginParam 告警插件局部参数
     */
    public abstract void alarm(Exception e, ApiConfig config, HttpServletRequest request, String pluginParam);

}
