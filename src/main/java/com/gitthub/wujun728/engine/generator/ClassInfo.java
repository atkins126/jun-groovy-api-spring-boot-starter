package com.gitthub.wujun728.engine.generator;

import java.util.List;

import lombok.Data;

/**
 * class info
 */
@Data
public class ClassInfo {

    private String tableName;
    private String className;
	private String classComment;
	private int pkSize;

	private List<FieldInfo> fieldList;
	private List<FieldInfo> pkfieldList;


}