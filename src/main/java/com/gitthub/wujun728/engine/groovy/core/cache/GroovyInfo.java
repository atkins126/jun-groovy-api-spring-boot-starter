package com.gitthub.wujun728.engine.groovy.core.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class GroovyInfo {

	/**
	 * 类名
	 */
	private String className;

	/**
	 * 脚本内容
	 */
	@JsonIgnore
	private String groovyContent;

	/**
	 * 接口ID
	 */
	private String interfaceId;

	/**
	 * BeanName
	 */
	private String beanName;
	
	/**
	 * BeanType
	 */
	private String BeanType;
}
