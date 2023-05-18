package com.gitthub.wujun728.engine.controller;

//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gitthub.wujun728.engine.common.DataResult;
import com.gitthub.wujun728.engine.groovy.core.bean.GroovyDynamicLoader;

import javax.annotation.Resource;
import javax.script.*;
import java.util.HashMap;

@RestController
//@Api(tags = "Groovy脚本执行接口")
@RequestMapping(path = "/groovy")
public class GroovyScriptController {
    private ScriptEngineManager scriptEngineManager;
    private ApplicationContext applicationContext;
    
    @Resource
	private GroovyDynamicLoader groovyDynamicLoader;
	
	@RequestMapping("/test")
	public int test(int number1, int number2) {
		return number1 + number2;
		
	}

	@RequestMapping("/refresh")
	public DataResult refresh() {
		try {
			groovyDynamicLoader.refreshNew();
		} catch (Exception e) {
			DataResult.success("緩存刷新失败！" + e.getMessage());
			e.printStackTrace();
		}
		return DataResult.success("緩存刷新成功");
	}

    @Autowired
    public GroovyScriptController(ScriptEngineManager scriptEngineManager, ApplicationContext applicationContext) {
        Assert.notNull(scriptEngineManager, "scriptEngineManager is not allowed null.");
        Assert.notNull(applicationContext, "applicationContext is not allowed null.");
        this.scriptEngineManager = scriptEngineManager;
        this.applicationContext = applicationContext;
    }

    @PostMapping
    //@ApiOperation(notes = "执行Groovy脚本", value = "执行groovy脚本")
    public Object execute(String script) throws ScriptException {
        ScriptEngine engine = scriptEngineManager.getEngineByName("groovy");
        ScriptContext context = new SimpleScriptContext();
        context.setBindings(new SimpleBindings(new HashMap<String, Object>(1) {{
            put("spring", applicationContext);
        }}), ScriptContext.ENGINE_SCOPE);
        return engine.eval(script, context).toString();
    }
}
