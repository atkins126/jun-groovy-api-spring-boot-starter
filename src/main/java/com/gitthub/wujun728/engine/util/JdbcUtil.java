package com.gitthub.wujun728.engine.util;

import com.alibaba.druid.DbType;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.freakchick.orange.engine.DynamicSqlEngine;
import com.gitthub.wujun728.engine.common.DataResult;
import com.gitthub.wujun728.engine.common.ApiDataSource;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class JdbcUtil {
	
	static DynamicSqlEngine engine = new DynamicSqlEngine();

	public static DynamicSqlEngine getEngine() {
		return engine;
	}

	public static ResultSet query(String sql, Connection connection) throws SQLException {
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		ResultSet resultSet = preparedStatement.executeQuery();
		return resultSet;
	}

	public static Connection getConnection(ApiDataSource ds) throws Exception {
		try {
			Class.forName(ds.getDriver());
			String password = ds.getPassword();
			// String password = ds.isEdit_password() ? ds.getPassword() :
			// DESUtils.decrypt(ds.getPassword());
			Connection connection = DriverManager.getConnection(ds.getUrl(), ds.getUsername(), password);
			log.info("successfully connected");
			return connection;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(
					"Please check whether the jdbc driver jar is missing, if missed copy the jdbc jar file to lib dir. "
							+ e.getMessage());
		}
	}

	/**
	 * 查询库中所有表
	 *
	 * @param conn
	 * @param sql
	 * @return
	 */
	public static List<String> getAllTables(Connection conn, String sql) {
		List<String> list = new ArrayList<>();
		PreparedStatement pst = null;
		try {
			pst = conn.prepareStatement(sql);
			ResultSet resultSet = pst.executeQuery();

			while (resultSet.next()) {
				String s = resultSet.getString(1);
				list.add(s);
			}
			return list;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		} finally {
			try {
				if (pst != null)
					pst.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 查询表所有字段
	 *
	 * @param conn
	 * @param type
	 * @param table
	 * @return
	 */
	public static List<JSONObject> getRDBMSColumnProperties(Connection conn, String type, String table) {
		List<JSONObject> list = new ArrayList<>();
		PreparedStatement pst = null;
		try {
			String sql;
			switch (type) {
			case "POSTGRESQL":
				sql = "select * from \"" + table + "\" where 1=2";
				break;
			default:
				sql = "select * from " + table + " where 1=2";
			}
			pst = conn.prepareStatement(sql);
			ResultSetMetaData rsd = pst.executeQuery().getMetaData();

			for (int i = 0; i < rsd.getColumnCount(); i++) {
				JSONObject jsonObject = new JSONObject();

				String columnTypeName = rsd.getColumnTypeName(i + 1);
				jsonObject.put("fieldTypeName", columnTypeName);// 数据库字段类型名
				jsonObject.put("TypeName", columnTypeName);
				jsonObject.put("fieldJavaTypeName", rsd.getColumnClassName(i + 1));// 映射到java的类型名
				String columnName = rsd.getColumnName(i + 1);
				if (columnName.contains("."))
					columnName = columnName.split("\\.")[1];
				jsonObject.put("label", columnName);// 表字段
				list.add(jsonObject);
			}
			return list;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		} finally {
			try {
				if (pst != null)
					pst.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 没有关闭连接，需要在调用方关闭
	 *
	 * @param connection
	 * @param sql
	 * @param jdbcParamValues
	 * @return
	 */
	public static Object executeSql(Connection connection, String sql, List<Object> jdbcParamValues)
			throws SQLException {
		log.debug(sql);
		log.debug(JSON.toJSONString(jdbcParamValues));
		PreparedStatement statement = connection.prepareStatement(sql);
		// 参数注入
		for (int i = 1; i <= jdbcParamValues.size(); i++) {
			statement.setObject(i, jdbcParamValues.get(i - 1));
		}
		boolean hasResultSet = statement.execute();

		if (hasResultSet) {
			ResultSet rs = statement.getResultSet();
			int columnCount = rs.getMetaData().getColumnCount();

			List<String> columns = new ArrayList<>();
			for (int i = 1; i <= columnCount; i++) {
				String columnName = rs.getMetaData().getColumnLabel(i);
				columns.add(columnName);
			}
			List<JSONObject> list = new ArrayList<>();
			while (rs.next()) {
				JSONObject jo = new JSONObject();
				columns.stream().forEach(t -> {
					try {
						Object value = rs.getObject(t);
						jo.put(t, value);
					} catch (SQLException throwables) {
						throwables.printStackTrace();
					}
				});
				list.add(jo);
			}
			return list;
		} else {
			int updateCount = statement.getUpdateCount();
			return updateCount + " rows affected";
		}

	}

	/**
	 * 表结构解析
	 */
	public static String getByPattern(String sql, String pattern, int group) {
		Pattern compile = Pattern.compile(pattern);
		Matcher matcher = compile.matcher(sql);
		while (matcher.find()) {
			return matcher.group(group);
		}
		return null;
	}

	public static List<String> getColumnSqls(String sql) {
		List<String> lines = new ArrayList<>();
		Scanner scanner = new Scanner(sql);
		boolean start = false;
		while (scanner.hasNextLine()) {
			String nextLine = scanner.nextLine();
			if (nextLine.indexOf("CREATE TABLE") != -1) {
				start = true;
				continue;
			}
			if (nextLine.indexOf("KEY") != -1 || nextLine.indexOf("ENGINE=") != -1) {
				start = false;
				continue;
			}
			if (start) {
				lines.add(nextLine);
			}
		}
		return lines;
	}
	
	

    public static Connection getConnectionByDBType(ApiDataSource ds) throws SQLException, ClassNotFoundException {
        String url = ds.getUrl();
        switch (ds.getType()) {
            case "mysql":
                Class.forName("com.mysql.jdbc.Driver");
                break;
            case "postgresql":
                Class.forName("org.postgresql.Driver");
                break;
            case "hive":
                Class.forName("org.apache.hive.jdbc.HiveDriver");
                break;
            case "sqlserver":
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                break;
            default:
                break;
        }

        Connection connection = DriverManager.getConnection(url, ds.getUsername(), ds.getPassword());
        log.info("获取连接成功");
        return connection;
    }

    public static DataResult executeSql(int isSelect, ApiDataSource datasource, String sql, List<Object> jdbcParamValues) {
        log.info("sql:\n" + sql);
        DruidPooledConnection connection = null;
        try {

            connection = PoolManager.getPooledConnection(datasource);
            PreparedStatement statement = connection.prepareStatement(sql);
            //参数注入
            for (int i = 1; i <= jdbcParamValues.size(); i++) {
                statement.setObject(i, jdbcParamValues.get(i - 1));
            }

            if (isSelect == 1) {
                ResultSet rs = statement.executeQuery();

                int columnCount = rs.getMetaData().getColumnCount();

                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rs.getMetaData().getColumnLabel(i);
                    columns.add(columnName);
                }
                List<JSONObject> list = new ArrayList<>();
                while (rs.next()) {
                    JSONObject jo = new JSONObject();
                    columns.stream().forEach(t -> {
                        try {
                            Object value = rs.getObject(t);
                            jo.put(t, value);
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    });
                    list.add(jo);
                }
                return DataResult.success(list);
            } else {
                int rs = statement.executeUpdate();
                return DataResult.success("sql修改数据行数： " + rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return DataResult.fail(e.getMessage());
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

}