package com.gitthub.wujun728.engine.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cn.hutool.core.util.ArrayUtil;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * 代码生成器 工具类
 *
 * @author wujun
 * @version V1.0
 * @date 2020年3月18日
 */
@Slf4j
public class GenUtils {

	public static final String PROJECT_PATH = System.getProperty("user.dir");// 项目在硬盘上的基础路径，项目路径
	public static final String JAVA_PATH = "/src/main/java"; // java文件路径
	public static final String RESOURCES_PATH = "/src/main/resources";// 资源文件路径
	public static final String TEMPLATE_FILE_PATH = PROJECT_PATH + "/src/main/resources/templates";// 模板位置
	public static String PACKAGE = "com.bjc.lcp.app";// 资源文件路径
	public static String CONFIG = "config.properties";// 资源文件路径
	public static Boolean isDefaultTemplate = true;// 资源文件路径

	public static Properties props = new Properties(); // 配置文件
	static {
		try {
			props = getProperties(CONFIG);
			Class.forName(props.getProperty("driver"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static {
		PACKAGE = GenUtils.props.getProperty("basePackage");
	}

	public static Properties getProperties(String fileName) {
		try {
			String outpath = System.getProperty("user.dir") + File.separator + RESOURCES_PATH + File.separator;// 先读取config目录的，没有再加载classpath的
			System.out.println(outpath);
			Properties properties = new Properties();
			InputStream in = new FileInputStream(new File(outpath + fileName));
			properties.load(in);
			return properties;
		} catch (IOException e) {
			System.out.println(e.getMessage());
			try {
				Properties properties = new Properties();
				InputStream in = GenUtils.class.getClassLoader().getResourceAsStream(fileName);// 默认加载classpath的
				properties.load(in);
				return properties;
			} catch (IOException es) {
				System.out.println(es.getMessage());
				return null;
			}
		}
	}

	public static void genTables(String[] tables) throws Exception {
		List<ClassInfo> classInfos = GenUtils.getClassInfo(tables);
		classInfos.forEach(classInfo -> {
			Map<String, Object> datas = new HashMap<String, Object>();
			datas.put("classInfo", classInfo);
//			datas.putAll(GenUtils.getPackages());
			datas.put("authorName", "wujun");
			datas.put("isLombok", true);
			datas.put("isAutoImport", true);
			datas.put("isWithPackage", true);
			datas.put("isSwagger", true);
			datas.put("isComment", true);
			datas.put("packageName", GenUtils.PACKAGE);
			Map<String, String> result = new HashMap<String, String>();
			try {
				// GenUtils.processTemplatesStringWriter(datas, result);
				GenUtils.processTemplates(classInfo, datas, getTemplates());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (TemplateException e) {
				e.printStackTrace();
			}
			// 计算,生成代码行数
			int lineNum = 0;
			for (Map.Entry<String, String> item : result.entrySet()) {
				if (item.getValue() != null) {
					lineNum += StringUtils.countMatches(item.getValue(), "\n");
				}
			}
			log.info("生成代码行数：{}", lineNum);
		});

	}

	public static List<String> getFilePaths(ClassInfo classInfo) {
		List<String> filePaths = new ArrayList<>();
		filePaths.add(PROJECT_PATH + JAVA_PATH + package2Path(PACKAGE + ".controller") + classInfo.getClassName()
				+ "Controller.java");
		filePaths.add(PROJECT_PATH + JAVA_PATH + package2Path(PACKAGE + ".entity") + classInfo.getClassName()
				+ "Entity.java");
		filePaths.add(PROJECT_PATH + JAVA_PATH + package2Path(PACKAGE + ".mapper") + classInfo.getClassName()
				+ "Mapper.java");
		filePaths.add(PROJECT_PATH + JAVA_PATH + package2Path(PACKAGE + ".service") + classInfo.getClassName()
				+ "Service.java");
		filePaths
				.add(PROJECT_PATH + JAVA_PATH + package2Path(PACKAGE + ".dto") + classInfo.getClassName() + "DTO.java");
		filePaths.add(PROJECT_PATH + JAVA_PATH + package2Path(PACKAGE + ".vo") + classInfo.getClassName() + "VO.java");
		filePaths.add(PROJECT_PATH + JAVA_PATH + package2Path(PACKAGE + ".service.impl") + classInfo.getClassName()
				+ "ServiceImpl.java");
		return filePaths;
	}

	public static List<String> getTemplates() {
		List<String> templates = Lists.newArrayList();
		templates.add("code-generator/mybatis-plus-v2/plus-controller.ftl");
		templates.add("code-generator/mybatis-plus-v2/plus-entity.ftl");
		templates.add("code-generator/mybatis-plus-v2/plus-mapper.ftl");
		templates.add("code-generator/mybatis-plus-v2/plus-service.ftl");
		templates.add("code-generator/mybatis-plus-v2/plus-dto.ftl");
		templates.add("code-generator/mybatis-plus-v2/plus-vo.ftl");
		templates.add("code-generator/mybatis-plus-v2/plus-serviceimpl.ftl");
		return templates;
	}

	private static String package2Path(String packageName) {
		return String.format("/%s/", packageName.contains(".") ? packageName.replaceAll("\\.", "/") : packageName);
	}

	public static Boolean skipTables(String str) {
		str = str.toLowerCase();
		for (String x : props.getProperty("skipTable").split(",")) {
			if (str.contains(x.toLowerCase())) {
				return true;
			}
		}
		return true;
	}

	public static Boolean includeTabbles(String str) {
		str = str.toLowerCase();
		if (props.getProperty("inclueTables").equals("*")) {
			return false;
		}
		for (String x : props.getProperty("inclueTables").split(",")) {
			if (str.contains(x.toLowerCase())) {
				return false;
			}
		}
		return true;
	}

	public static void getFile(String path, List<Map<String, Object>> list) {
		File file = new File(path);
		File[] array = file.listFiles();
		for (int i = 0; i < array.length; i++) {
			if (array[i].isFile()) {
				Map<String, Object> map = new HashMap<String, Object>();
				// only take file name
				// System.out.println("^^^^^" + array[i].getName());
				// take file path and name
				// System.out.println("*****" + array[i].getPath());
				map.put(array[i].getName(), array[i].getPath());
				list.add(map);
			} else if (array[i].isDirectory()) {
				getFile(array[i].getPath(), list);
			}
		}
	}

	public static String replace_(String str) {
		// 根据下划线分割
		String[] split = str.split("_");
		// 循环组装
		String result = split[0];
		if (split.length > 1) {
			for (int i = 1; i < split.length; i++) {
				result += firstUpper(split[i]);
			}
		}
		return result;
	}

	public static String firstUpper(String str) {
		// log.info("str:"+str+",str.length="+str.length());
		if (!org.springframework.util.StringUtils.isEmpty(str)) {
			return str.substring(0, 1).toUpperCase() + str.substring(1);
		} else {
			return str;
		}
	}

	public static String firstLower(String str) {
		return str.substring(0, 1).toLowerCase() + str.substring(1);
	}

	public static String replaceTabblePreStr(String str) {
		return str.replaceFirst("tab_", "").replaceFirst("tb_", "").replaceFirst("t_", "").replaceFirst("T_", "");
	}

	public static String replaceRow(String str) {
		str = str.toLowerCase().replaceFirst("tab_", "").replaceFirst("tb_", "").replaceFirst("t_", "").replaceFirst("T_", "");
		for (String x : props.getProperty("rowRemovePrefixes").split(",")) {
			str = str.replaceFirst(x.toLowerCase(), "");
		}
		return str;
	}

	public static String simpleNameLowerFirst(String type) {
		// 去掉前缀
		type = simpleName(type);
		// 将第一个字母转成小写
		return GenUtils.firstLower(type);
	}

	public static String simpleName(String type) {
		return type.replace("java.lang.", "").replaceFirst("java.util.", "");
	}

	public static String upperCaseFirstWord(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	public static String getType(int value) {
		switch (value) {
		case -6:
			return "java.lang.Integer";
		case 5:
			return "java.lang.Integer";
		case 4:
			return "java.lang.Integer";
		case -5:
			return "java.lang.Long";
		case 6:
			return "java.lang.Float";
		case 8:
			return "java.lang.Double";
		case 1:
			return "java.lang.String";
		case 12:
			return "java.lang.String";
		case -1:
			return "java.lang.String";
		case 91:
			return "java.util.Date";
		case 92:
			return "java.util.Date";
		case 93:
			return "java.util.Date";
		case 16:
			return "java.lang.Boolean";
		default:
			return "java.lang.String";
		}
	}

	// ****************************************************************************************************

	public static void processTemplates(ClassInfo classInfo, Map<String, Object> datas, List<String> templates)
			throws IOException, TemplateException {
		// List<String> templates = CodeGeneratorUtils.getTemplates();
		for (int i = 0; i < templates.size(); i++) {
			GenUtils.processFile(templates.get(i), datas, GenUtils.getFilePaths(classInfo).get(i));
		}
	}

	public static void processFile(String templateName, Map<String, Object> data, String filePath)
			throws IOException, TemplateException {
		Template template = getConfiguration().getTemplate(templateName);
		File file = new File(filePath);
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		template.process(data, new FileWriter(file));
		System.out.println(filePath + " 生成成功");
	}

	/***
	 * 模板构建，StringWriter 返回构建后的文本，不生成文件
	 */
	public static String processString(String templateName, Map<String, Object> params)
			throws IOException, TemplateException {
		Template template = getConfiguration().getTemplate(templateName);
		StringWriter result = new StringWriter();
		template.process(params, result);
		String htmlText = result.toString();
		return htmlText;
	}

	private static freemarker.template.Configuration getConfiguration() throws IOException {
		return getConfiguration(isDefaultTemplate);
	}

	private static freemarker.template.Configuration getConfiguration(Boolean isDefaultTemplate) throws IOException {
		freemarker.template.Configuration cfg = new freemarker.template.Configuration(
				freemarker.template.Configuration.VERSION_2_3_23);
		if (!isDefaultTemplate) {
			cfg.setDirectoryForTemplateLoading(new File(GenUtils.TEMPLATE_FILE_PATH)); // 此行配置是初始化外部模板路径的
			// configuration.setDirectoryForTemplateLoading(file); 此行配置不要了
			// https://blog.csdn.net/cg_Amaz1ng/article/details/100126456
		} else {
			cfg.setClassForTemplateLoading(GenUtils.class.getClass(), "/templates"); // 此行配置是初始化默认模板路径的
			cfg.setTemplateLoader(new ClassTemplateLoader(GenUtils.class.getClass(), "/templates"));
		}
		cfg.setDefaultEncoding("UTF-8");
		cfg.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
		return cfg;
	}

	/***
	 * 模板构建，输出源码字符串
	 */
	public static void processTemplatesStringWriter(Map<String, Object> datas, Map<String, String> result)
			throws IOException, TemplateException {
		result.put("controller_code", GenUtils.processString("code-generator/controller.ftl", datas));
		result.put("service_code", GenUtils.processString("code-generator/service.ftl", datas));
		result.put("service_impl_code", GenUtils.processString("code-generator/service_impl.ftl", datas));
		result.put("dao_code", GenUtils.processString("code-generator/dao.ftl", datas));
		result.put("mybatis_code", GenUtils.processString("code-generator/mybatis.ftl", datas));
		result.put("model_code", GenUtils.processString("code-generator/model.ftl", datas));
		System.out.println(result);
	}

	public static List<ClassInfo> getClassInfo(String[] tables) {
		return getClassInfo(tables, null);
	}

	public static List<ClassInfo> getClassInfo(String[] tables, Connection conn) {
		List<ClassInfo> list = new ArrayList<ClassInfo>();
		try {
			if (conn == null) {
				conn = DriverManager.getConnection(GenUtils.props.getProperty("url"),
						GenUtils.props.getProperty("uname"), GenUtils.props.getProperty("pwd"));
			}
			DatabaseMetaData metaData = conn.getMetaData();
			String databaseType = metaData.getDatabaseProductName(); // 获取数据库类型：MySQL
			// 针对MySQL数据库进行相关生成操作
			if (databaseType.equals("MySQL")) {
				ResultSet tableResultSet = metaData.getTables(conn.getCatalog(), conn.getSchema() /* "%" */, "%",
						new String[] { "TABLE" }); // 获取所有表结构
				String database = conn.getCatalog(); // 获取数据库名字
				while (tableResultSet.next()) { // 循环所有表信息
					String tableName = tableResultSet.getString("TABLE_NAME"); // 获取表名
					if (tables == null || ArrayUtil.containsIgnoreCase(tables, tableName)) {
						List<Map<String, String>> pkList = getPrimaryKeysInfo(metaData, tableName);
						String table = GenUtils.replace_(GenUtils.replaceTabblePreStr(tableName)); // 名字操作,去掉tab_,tb_，去掉_并转驼峰
						String Table = GenUtils.firstUpper(table); // 获取表名,首字母大写
						String tableComment = tableResultSet.getString("REMARKS"); // 获取表备注
						String className = GenUtils.replace_(GenUtils.replaceTabblePreStr(tableName)); // 名字操作,去掉tab_,tb_，去掉_并转驼峰
						String classNameFirstUpper = GenUtils.firstUpper(className); // 大写对象
//						showTableInfo(tableResultSet); 
						log.info("当前表名：" + tableName);
						Set<String> typeSet = new HashSet<String>(); // 所有需要导包的类型
						ResultSet cloumnsSet = metaData.getColumns(database, GenUtils.props.getProperty("uname"),
								tableName, null); // 获取表所有的列
						ResultSet keySet = metaData.getPrimaryKeys(database, GenUtils.props.getProperty("uname"),
								tableName); // 获取主键
						String key = "", keyType = "";
						while (keySet.next()) {
							key = keySet.getString(4);
						}
						// V1 初始化数据及对象 模板V1 field List
						List<FieldInfo> fieldList = new ArrayList<FieldInfo>();
						while (cloumnsSet.next()) {
							String remarks = cloumnsSet.getString("REMARKS");// 列的描述
							String columnName = cloumnsSet.getString("COLUMN_NAME"); // 获取列名
							String javaType = GenUtils.getType(cloumnsSet.getInt("DATA_TYPE"));// 获取类型，并转成JavaType
							int COLUMN_SIZE = cloumnsSet.getInt("COLUMN_SIZE");// 获取
							String TABLE_SCHEM = cloumnsSet.getString("TABLE_SCHEM");// 获取
							String COLUMN_DEF = cloumnsSet.getString("COLUMN_DEF");// 获取
							int NULLABLE = cloumnsSet.getInt("NULLABLE");// 获取
							//int DATA_TYPE = cloumnsSet.getInt("DATA_TYPE");// 获取
							// showColumnInfo(cloumnsSet);
							String propertyName = GenUtils.replace_(GenUtils.replaceRow(columnName));// 处理列名，驼峰
							typeSet.add(javaType);// 需要导包的类型
							Boolean isPk = false;
							if (columnName.equals(key)) {
								keyType = GenUtils.simpleName(javaType);// 主键类型,单主键支持
								isPk = true;
							}
							// V1 初始化数据及对象
							FieldInfo fieldInfo = new FieldInfo();
							fieldInfo.setColumnName(columnName);
							fieldInfo.setFieldName(propertyName);
							fieldInfo.setFieldClass(GenUtils.simpleName(javaType));
							fieldInfo.setFieldComment(remarks);
							fieldInfo.setColumnSize(COLUMN_SIZE);
							fieldInfo.setNullable(NULLABLE == 0);
							fieldInfo.setFieldType(javaType);
							fieldInfo.setColumnType(javaType);
							fieldInfo.setIsPrimaryKey(isPk);
							fieldList.add(fieldInfo);
						}
						// ************************************************************************
						if (fieldList != null && fieldList.size() > 0) {
							ClassInfo classInfo = new ClassInfo();
							classInfo.setTableName(tableName);
							classInfo.setClassName(classNameFirstUpper);
							classInfo.setClassComment(tableComment);
							classInfo.setFieldList(fieldList);
							classInfo.setPkSize(pkList.size());
							list.add(classInfo);
						}
						// ************************************************************************
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	// 获取表主键信息
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static List getPrimaryKeysInfo(DatabaseMetaData dbmd, String tablename) {
		List pkList = Lists.newArrayList();
		ResultSet rs = null;
		try {
			rs = dbmd.getPrimaryKeys(null, null, tablename);
			while (rs.next()) {
				String tableCat = rs.getString("TABLE_CAT"); // 表类别(可为null)
				String tableSchemaName = rs.getString("TABLE_SCHEM");// 表模式（可能为空）,在oracle中获取的是命名空间,其它数据库未知
				String tableName = rs.getString("TABLE_NAME"); // 表名
				String columnName = rs.getString("COLUMN_NAME");// 列名
				short keySeq = rs.getShort("KEY_SEQ");// 序列号(主键内值1表示第一列的主键，值2代表主键内的第二列)
				String pkName = rs.getString("PK_NAME"); // 主键名称
				Map m = Maps.newHashMap();
				m.put("COLUMN_NAME", columnName);
				m.put("KEY_SEQ", keySeq);
				m.put("PK_NAME", pkName);
				pkList.add(m);
				System.out.println(tableCat + " - " + tableSchemaName + " - " + tableName + " - " + columnName + " - "
						+ keySeq + " - " + pkName);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return pkList;
	}

	// ****************************************************************************************************

	/***
	 * 构建 Java文件，遍历文件夹下所有的模板，然后生成对应的文件（需要配置模板的package及path）
	 */
	public static void batchBuilderByDirectory1111(Map<String, Object> modelMap) {
		List<Map<String, Object>> srcFiles = new ArrayList<Map<String, Object>>();
		String TEMPLATE_PATH = GenUtils.class.getClassLoader().getResource("").getPath().replace("/target/classes/", "")
				+ "/src/main/resources/" + props.getProperty("template_path");
		getFile(TEMPLATE_PATH, srcFiles);
		for (int i = 0; i < srcFiles.size(); i++) {
			HashMap<String, Object> m = (HashMap<String, Object>) srcFiles.get(i);
			Set<String> set = m.keySet();
			Iterator<String> it = set.iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				if ((boolean) modelMap.get("Swagger") == true) {
					if (!key.contains(".json")) {
						continue;
					}
				}
				String templateFileName = key;
				String templateFileNameSuffix = key.substring(key.lastIndexOf("."), key.length());
				String templateFileNamePrefix = key.substring(0, key.lastIndexOf("."));
				String templateFilePathAndName = String.valueOf(m.get(key));
				String templateFilePath = templateFilePathAndName.replace("\\" + templateFileName, "");
				String templateFilePathMiddle = "";
				if (!templateFilePath.endsWith(props.getProperty("template_path").replace("/", "\\"))) {
					templateFilePathMiddle = templateFilePath
							.substring(templateFilePath.lastIndexOf("\\"), templateFilePath.length()).replace("\\", "");
				}
				if (key.contains(".json")) {
					// logger.info("templateFilePath=" + templateFilePath);
					continue;
				}
				try {
					String path = null;
					if (templateFileNameSuffix.equalsIgnoreCase(".java")) {
						// 创建文件夹
						path = GenUtils.PROJECT_PATH + "/" + props.getProperty("basePackage").replace(".", "/") + "/"
								+ templateFileNamePrefix.toLowerCase();
					}
					if (templateFileNameSuffix.equalsIgnoreCase(".ftl")) {
						path = GenUtils.PROJECT_PATH + "/" + props.getProperty("basePackage").replace(".", "/") + "/"
								+ templateFilePathMiddle + "/";
					}
					String fileNameNew = templateFileNamePrefix
							.replace("${className}", String.valueOf(modelMap.get("Table")))
							.replace("${classNameLower}", String.valueOf(modelMap.get("Table")).toLowerCase());
					// 创建文件
//					GeneratorUtils.writer(template, modelMap, path + "/" + fileNameNew);
					GenUtils.processFile(templateFileName, modelMap, path + "/" + fileNameNew);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
	}

	public static void showTableInfo1111(ResultSet tableResultSet) throws SQLException {
		System.out.println(tableResultSet.getString("TABLE_CAT"));
		System.out.println(tableResultSet.getString("TABLE_SCHEM"));
		System.out.println(tableResultSet.getString("TABLE_NAME"));
		System.out.println(tableResultSet.getString("TABLE_TYPE"));
		System.out.println(tableResultSet.getString("REMARKS"));
	}

	public static void showColumnInfo1111(ResultSet cloumnsSet) throws SQLException {
		System.out.println("TABLE_CAT is :" + cloumnsSet.getString("TABLE_CAT"));
		System.out.println("TABLE_SCHEM is :" + cloumnsSet.getString("TABLE_SCHEM"));
		System.out.println("TABLE_NAME is :" + cloumnsSet.getString("TABLE_NAME"));
		System.out.println("COLUMN_NAME is :" + cloumnsSet.getString("COLUMN_NAME"));
		System.out.println("DATA_TYPE is :" + cloumnsSet.getInt("DATA_TYPE"));
		System.out.println("TYPE_NAME is :" + cloumnsSet.getString("TYPE_NAME"));
		System.out.println("COLUMN_SIZE is :" + cloumnsSet.getInt("COLUMN_SIZE"));
		System.out.println("BUFFER_LENGTH is :" + cloumnsSet.getInt("BUFFER_LENGTH"));
		System.out.println("DECIMAL_DIGITS is :" + cloumnsSet.getInt("DECIMAL_DIGITS"));
		System.out.println("NUM_PREC_RADIX is :" + cloumnsSet.getInt("NUM_PREC_RADIX"));
		System.out.println("NULLABLE is :" + cloumnsSet.getInt("NULLABLE"));
		System.out.println("REMARKS is :" + cloumnsSet.getString("REMARKS"));
		System.out.println("COLUMN_DEF is :" + cloumnsSet.getString("COLUMN_DEF"));
		System.out.println("SQL_DATA_TYPE is :" + cloumnsSet.getInt("SQL_DATA_TYPE"));
		System.out.println("SQL_DATETIME_SUB is :" + cloumnsSet.getInt("SQL_DATETIME_SUB"));
		System.out.println("CHAR_OCTET_LENGTH is :" + cloumnsSet.getInt("CHAR_OCTET_LENGTH"));
		System.out.println("ORDINAL_POSITION is :" + cloumnsSet.getInt("ORDINAL_POSITION"));
		System.out.println("IS_NULLABLE is :" + cloumnsSet.getString("IS_NULLABLE"));
	}

}
