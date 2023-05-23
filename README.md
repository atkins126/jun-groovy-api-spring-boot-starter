# jun-groovy-api-spring-boot-starter


jun-groovy-api-spring-boot-starter是低代码开发平台开发基座stater，基于SpringBoot项目都可以嵌入支持。项目基于SpringBoot+Groovy+SQL动态生成API并动态发布，且发布后可动态执行groovy脚本
及SQL脚本的Stater。提供在线执行动态程序脚热加载本及动态生成API并执行的功能。支持动态注册Mapping，动态生成类及源码并动态编译生成Bean，可动态生成HTTP接口。支持在线编辑写好SQL或者
Java源码、Groovy源码、Python源码（TODO），JavaScript源码（TODO）后即可快速生成Rest接口对外提供服务，同时支持服务在线热加载在线编辑替换逻辑，还将提供了一键生成CRUD通用接口方法，减少
通用接口的SQL编写，让开发人员专注更复杂的业务逻辑实现。支持有JDBC驱动的的数据源(Java支持的都可以支持)。
后续将集成微服务注册中心、网关支持接口转发、黑白名单、权限认证、限流、缓存、监控等提供一站式API服务功能。

说明：本项目仅是一个Stater，无法独立运行（通用模块），需要嵌入到jun_springboot_api_service中jun_springboot_groovy_api独立模块（定制模块）才能运行。

The project is based on SpringBoot+Groovy to dynamically generate APIs and publish them, and can dynamically execute Groovy scripts and SQL scripts' Staters after publication. Provide online execution of dynamic scripts class and hot loader and dynamic API generation and execution functions.



# 使用教程

- 在自己的maven项目中引入maven坐标（已发布中央仓库）
```xml
<dependency>
    <groupId>io.github.wujun728</groupId>
    <artifactId>jun-groovy-api-spring-boot-starter</artifactId>
    <version>1.0.2-RELEASE</version>
</dependency>
```

- 核心api - Step1-加载源码生成类及SpringBean
```
Class clazz = groovyClassLoader.parseClass(groovyInfo.getGroovyContent());
BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
BeanDefinition beanDefinition = builder.getBeanDefinition();
registry.registerBeanDefinition(groovyInfo.getBeanName(), beanDefinition);
```

- 核心api - Step2-动态生成SpringMapping映射HTTP服务及接口（无需手写Mapping接口）
```
// 取消历史注册
requestMappingHandlerMapping.unregisterMapping(mappingInfo);
// 重新注册mapping
requestMappingHandlerMapping.registerMapping(mappingInfo, mappingFactory, targetMethod);
}
```


- 核心api - Step3-执行分N种实现，目前实现三种；SQL脚本模式、Java源码模式、Groovy源码
Python源码（TODO），NodeJS源码（TODO），Shell(TODO)、PHP(TODO)、PowerShell(TODO)等类型脚本(JVM支持的都可以支持), 具体见注入mapping的method方法；
```
// 源码解析并执行
switch (config.getScriptType()) {
case "SQL":
	data = doSQLProcess(config, request, response);
	break;
case "Class":
	data = doGroovyProcess(config, request, response);
	break;
case "Groovy": 
	data = doGroovyProcess(config, request, response);
	break;
case "Jython": // TODO
	data = doPythonProcess(config, request, response);
	break; // TODO
case "Javascript": // TODO
	data = doNodeJSProcess(config, request, response);
case "Jruby":// TODO
	data = doRubyProcess(config, request, response);
	break;
default:
	break;
}

// SQL源码执行（SQL脚本支持mybatis写法及原生写法）
SqlMeta sqlMeta = JdbcUtil.getEngine().parse(apiSql.getSqlText(), sqlParam); 
Object data = JdbcUtil.executeSql(connection, sqlMeta.getSql(), sqlMeta.getJdbcParamValues());
dataList.add(data);

// Java源码执行（实现IExecutor接口，支持定制不同的接口，一般情况下也够用了）
String beanName = GroovyInnerCache.getByPath(config.getPath());//请求Path映射Bean名称
Map<String, Object> params = getParams(request, config);//获取Request参数转Map
IExecutor bean = SpringUtil.getBean(beanName);//调用Bean
return bean.execute(params);//返回Bean结果集

//其他源码执行（使用ScriptEngineManager来执行），举例JS如下，其他的还没来得及整,TODO中
ScriptEngineManager manager = new ScriptEngineManager();
ScriptEngine engine = manager.getEngineByName("javascript");
//向engine中存值
engine.put("str", "jsEnginePutTest");
engine.eval("var output ='' ;for (i = 0; i <= str.length; i++) {  output = str.charAt(i) + output }");     

```

- 
- 示例
```
Step1-启动的时候会读取api_config表中的记录并注册

开始解析groovy脚本...
2023-05-23 10:50:20.862  INFO 18900 --- [           main] c.g.w.e.g.core.bean.GroovyDynamicLoader  :  --- info --- 
2023-05-23 10:50:20.862  WARN 18900 --- [           main] c.g.w.e.g.core.bean.GroovyDynamicLoader  :  --- warn --- 
2023-05-23 10:50:20.862 ERROR 18900 --- [           main] c.g.w.e.g.core.bean.GroovyDynamicLoader  :  --- error --- 
2023-05-23 10:50:21.746  INFO 18900 --- [           main] c.g.w.e.g.core.bean.GroovyDynamicLoader  : 当前groovyInfo加载完成,className-testdataresult,path-/api/test/testdataresult,beanName-testdataresult,BeanType-Class：
2023-05-23 10:50:21.799  INFO 18900 --- [           main] c.g.w.e.g.core.bean.GroovyDynamicLoader  : 当前groovyInfo加载完成,className-testjsonobject,path-/api/test/testjsonobject,beanName-testjsonobject,BeanType-Class：
2023-05-23 10:50:21.881  INFO 18900 --- [           main] c.g.w.e.g.core.bean.GroovyDynamicLoader  : 当前groovyInfo加载完成,className-uploadBean,path-/api/test/upload,beanName-uploadBean,BeanType-Class：
2023-05-23 10:50:21.936  INFO 18900 --- [           main] c.g.w.e.g.core.bean.GroovyDynamicLoader  : 当前groovyInfo加载完成,className-downloadBean,path-/api/test/download,beanName-downloadBean,BeanType-Class：
2023-05-23 10:50:21.980  INFO 18900 --- [           main] c.g.w.e.g.core.bean.GroovyDynamicLoader  : 当前groovyInfo加载完成,className-fileListBean,path-/api/test/fileList,beanName-fileListBean,BeanType-Class：
2023-05-23 10:50:22.189  INFO 18900 --- [           main] c.g.w.e.g.core.bean.GroovyDynamicLoader  : 当前groovyInfo加载完成,className-test23Bean,path-/api/test23,beanName-test23Bean,BeanType-Class：
2023-05-23 10:50:22.213  INFO 18900 --- [           main] c.g.w.e.g.core.bean.GroovyDynamicLoader  : 当前groovyInfo加载完成,className-test11Bean,path-/mobile/api/test11,beanName-test11Bean,BeanType-Class：
2023-05-23 10:50:22.247  INFO 18900 --- [           main] c.g.w.e.g.core.bean.GroovyDynamicLoader  : 当前groovyInfo加载完成,className-test22Bean1,path-/mobile/api/test22,beanName-test22Bean1,BeanType-Class：
结束解析groovy脚本...，耗时：1398

```

- Step2，择取上面启动的两个示例：SQL示例
```

select * from user where name in  ( ? , ? ) 
tom
jerry
```