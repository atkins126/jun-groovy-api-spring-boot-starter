package com.gitthub.wujun728.engine.generator;

import lombok.Data;

/**
 * field info
 */
@Data
public class FieldInfo {

    private String columnName;
    private String columnType;
    private String fieldName;
    private String fieldClass;
    private String fieldType;
    private String fieldComment;
    private Boolean isPrimaryKey;
    private int columnSize;
    private Boolean comment;
    /**
     * 自增标识
     */
    private boolean isAutoIncrement;
    /**
     * 是否为空
     */
    private Boolean notNull;

    /**
     * 默认值
     */
    private String defaultValue;
    /**
     * 精度
     */
    private int precision;
    /**
     * 小数位数
     */
    private int scale;
    
}
