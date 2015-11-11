package com.scorpio.bean_create_util.bean_create_util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShowValueByDataBase {
	
	public String basePackageName = "cn.com.enorth.govopen";
	
	public String voPackageName = basePackageName + ".vo;";
	
	public String voChildPackageName = basePackageName + ".vo;";
	
	public String xmlBaseNamespace = "cn.com.enorth.govopen.mapper.";
	
	public boolean isHaveParentVo = true;
	
	public boolean isChangeVoPackageName = false;
	
	public String voFolder = "vo";
	public String mapperFolder = "mapper";
	public String serviceFolder = "service";
	public String xmlFolder = "xml";
	
	public String baseFilePath = "D:" + File.separator + "maven_workspace" + File.separator + "bean-create-util" + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator + "com" + File.separator + "scorpio";
	
	public void init() throws SQLException, IOException {
		Connection conn = C3P0Utils.getConnection();
		String sql = "show tables";
		ResultSet rs = C3P0Utils.query(conn, sql);
		while (rs.next()) {
			String tableName = rs.getString(1);
//			System.out.println("tableName:【" + tableName + "】start:-----------------------------------");
			initColumn(tableName, conn);
//			System.out.println("tableName:【" + tableName + "】end:-----------------------------------");
		}
		C3P0Utils.closeConnection(conn);
	} 
	
	public void initColumn(String tableName, Connection conn) throws SQLException, IOException {
		String sql = "show create table " + tableName;
		ResultSet rs = C3P0Utils.query(conn, sql);
		String regex = "\\s*`([^`]*)`\\s*(\\w+[^ ]*)\\s*(NOT\\s+NULL\\s*)?(DEFAULT\\s*([^ ]*|NULL|'0'|''|CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)\\s*)?(COMMENT\\s*'([^']*)')?\\s*,\\s*";
		Pattern p = Pattern.compile(regex);
		
		if (isHaveParentVo) {
			StringBuffer packageBuf = initClassHead(tableName, voChildPackageName);
			
//			String className = initNameByTable(tableName, true);
			String baseName = initMapperNameByTable(tableName, true);
			StringBuffer childClazzBuf = new StringBuffer();
			if (!isChangeVoPackageName) {
				voPackageName = voPackageName.substring(0, voPackageName.length() - 1) + ".base;";
				voFolder = "vo" + File.separator + "base";
				isChangeVoPackageName = true;
			}
			childClazzBuf.append("import java.io.Serializable;\r");
			childClazzBuf.append("public class " + baseName + "Vo extends " + voPackageName.substring(0, voPackageName.length() - 1) + "." + baseName + "Vo implements \r\t\tSerializable {\r\r");
			childClazzBuf.append("\t/**\r\t *\r\t */\r\tprivate static final long serialVersionUID = 1L;\r");
			childClazzBuf.append("}");
			packageBuf.append(childClazzBuf);
			writeFile(baseName, "vo", packageBuf, "Vo.java");
		}
		StringBuffer packageBuf = initClassHead(tableName, voPackageName);
		
//		String className = initNameByTable(tableName, true);
		String baseName = initMapperNameByTable(tableName, true);
		StringBuffer clazzBuf = new StringBuffer();
		clazzBuf.append("public class " + baseName + "Vo {\r\r");
		
		StringBuffer importBuf = new StringBuffer();
		StringBuffer fieldBuf = new StringBuffer();
		StringBuffer methodBuf = new StringBuffer();
		
		StringBuffer xmlBuf = initXmlHead(tableName);
		
		List<String> columns = new ArrayList<String>();
		
		while (rs.next()) {
			String sqlColumn = rs.getString(2);
			String lines[]  = sqlColumn.split("\n");
			for (String line : lines) {
				line = delLeftSpace(line);
				if (lineIsColumn(line)) {
					Matcher m = p.matcher(line);
					while (m.find()) {
						/*for (int i = 1; i <= 7; i++) {
							System.out.print(i + ":" + m.group(i) + "|");
						}*/
						/*StringBuffer columnName = new StringBuffer(m.group(1) + ":");
						if (m.group(4) != null) {
							columnName.append("4:" + m.group(4) + "|");
						}
						if (m.group(5) != null) {
							columnName.append("5:" + m.group(5) + "|");
						}*/
						dealCodeByMatcher(m, importBuf, fieldBuf, methodBuf);
						
						// 将字段存入list中，用于xml中的内容做准备
						columns.add(m.group(1));
					}
				}
			}
		}
//		clazzBuf.append(methodBuf.toString());
		appendOtherBuffer(packageBuf, clazzBuf, importBuf, fieldBuf, methodBuf);
		
		dealXmlByColumns(xmlBuf, columns, tableName);
		
		StringBuffer serviceBuf = new StringBuffer();
		String serviceFolder = initMapperNameByTable(tableName, false);
		String servicePackageName = basePackageName + ".service." + serviceFolder + ";";
		serviceBuf = initClassHead(tableName, servicePackageName);
		dealServiceByTableName(serviceBuf, tableName);
		StringBuffer mapperBuf = new StringBuffer();
		mapperBuf = dealMapperByTableName(mapperBuf, tableName);
		
		writeFile(baseName, voFolder, packageBuf, "Vo.java");
		
		writeFile(baseName, xmlFolder, xmlBuf, "Mapper.xml");
		
		writeFile(baseName, this.serviceFolder + File.separator + serviceFolder, serviceBuf, "Service.java");
		
		writeFile(baseName, mapperFolder, mapperBuf, "Mapper.java");
	}
	
	/**
	 * 将class文件的代码写入文件中
	 * @param className
	 * @param packageBuf
	 * @throws IOException
	 */
	public void writeFile(String className, String folderName, StringBuffer packageBuf, String fileSuffix) throws IOException {
		File classFile = new File(baseFilePath + File.separator + folderName);
		if (!classFile.exists()) {
			classFile.mkdirs();
		}
		classFile = new File(baseFilePath + File.separator + folderName + File.separator + className + fileSuffix);
		if (classFile.exists()) {
			classFile.delete();
			classFile.createNewFile();
		}
		
		FileWriter fw = new FileWriter(classFile);
		fw.write(packageBuf.toString());
		fw.flush();
		fw.close();
	}
	
	/**
	 * 删除左空格
	 * @param line
	 * @return
	 */
	public String delLeftSpace(String line) {
		char[] charArray = line.toCharArray();
		int size = 0;
		for (char c : charArray) {
			if (c != ' ') {
				return line.substring(size, line.length());
			}
			size++;
		}
		return "";
	}
	
	/**
	 * 判断当前sql语句的内容是否为列的数据
	 * @param line
	 * @return
	 */
	public boolean lineIsColumn(String line) {
		if (line.startsWith("KEY")) {
			return false;
		}
		if (line.startsWith("PRIMARY KEY")) {
			return false;
		}
		if (line.startsWith("CONSTRAINT")) {
			return false;
		}
		if (line.startsWith("UNIQUE KEY")) {
			return false;
		}
		return true;
	}
	
	/**
	 * 初始化类的头代码
	 * @param tableName
	 * @return
	 */
	public StringBuffer initClassHead(String tableName, String packageName) {
		StringBuffer packageBuf = new StringBuffer();
		packageBuf.append("package " + packageName + "\r\r");
		packageBuf.append("/**\r");
		packageBuf.append(" * 表名：" + tableName + "\r");
		packageBuf.append(" */\r\r");
		return packageBuf;
	}
	
	/**
	 * 在正则排除后获得的数据库表字段循环中，将对应数据存入对应的buffer中
	 * @param m
	 * @param imporBuffer
	 * @param fieldBuf
	 * @param importBuf
	 * @param methodBuf
	 */
	public void dealCodeByMatcher(Matcher m, StringBuffer importBuf, StringBuffer fieldBuf, StringBuffer methodBuf) {
		String columnName = m.group(1);
		columnName = initNameByTable(columnName, false);
		String columnType = m.group(2);
		String columnExplain = m.group(7);
		
		if (columnType.contains("(")) {
			columnType = columnType.substring(0, columnType.indexOf("("));
		}
		columnType = typeTrans(columnType, importBuf);
		
		fieldBuf = getFieldStr(columnName, columnType, columnExplain, fieldBuf, importBuf);
		
		methodBuf = getMethodStr(columnName, columnType, methodBuf);
	}
	
	/**
	 * 将mybatis需要的基础代码写入到xmlBuf中
	 * @param xmlBuf
	 * @param columns
	 * @param tableName
	 */
	public void dealXmlByColumns(StringBuffer xmlBuf, List<String> columns, String tableName) {
		List<String> properties = initProperties(columns);
		
		String baseName = initMapperNameByTable(tableName, true);
		StringBuffer resultMap = new StringBuffer();
		StringBuffer baseSelect = new StringBuffer();
		int length = columns.size();
		
		// 用作司法考试中的freeMarker start
		/*System.out.println("\t\t<!-- 将" + tableName + "的数据拷贝到test中 start -->");
		System.out.println("\t\t<sql id=\"syn" + baseName + "TableData\"><![CDATA[");
		System.out.println("\t\t\tinsert into");
		System.out.print("\t\t\t\ttest." + tableName + "(");
		StringBuffer columnStrBuf = new StringBuffer();*/
		
//		System.err.print("\"syn" + baseName + "TableData\", ");
		// 用作司法考试中的freeMarker end
		
		// 用作司法考试正式数据库中test表的数据清除start
//		System.out.println("delete from " + tableName + ";");
		// 用作司法考试正式数据库中test表的数据清除end
		
		// 用作MyBatis中的Mapper映射start
		System.out.println("<mapper resource=\"config/mybatis/mysql/mapper/" + baseName + "Mapper.xml\"/>");
		// 用作MyBatis中的Mapper映射end
		
		for (int i = 0; i < length; i++) {
			initResultMap(resultMap, columns.get(i), properties.get(i), baseName);
			initBaseSelectSql(baseSelect, columns.get(i), baseName);
			
			// 用作司法考试中的freeMarker start
			/*if (i == length - 1) {
				columnStrBuf.append(columns.get(i));
			} else {
				columnStrBuf.append(columns.get(i) + ", ");
			}*/
			// 用作司法考试中的freeMarker end
			
		}
		
		// 用作司法考试中的freeMarker start
		/*System.out.println(columnStrBuf.toString() + ")");
		System.out.println("\t\t\tselect");
		System.out.println("\t\t\t\t" + columnStrBuf.toString());
		System.out.println("\t\t\tfrom");
		System.out.println("\t\t\t\texam." + tableName);
		System.out.println("\t\t]]></sql>");
		System.out.println("\t\t<!-- 将" + tableName + "的数据拷贝到test中 end -->");
		System.out.println("");*/
		// 用作司法考试中的freeMarker end
		
		xmlBuf.append(resultMap + "\t</resultMap>\r");
		xmlBuf.append(baseSelect + "\r\t</sql>\r");
		initCURD(xmlBuf, tableName, baseName, columns, properties);
		xmlBuf.append("\r</mapper>");
	}
	
	/**
	 * 将mybatis中生成的方法写入service中
	 * @param serviceBuf
	 * @param baseName
	 */
	public StringBuffer dealServiceByTableName(StringBuffer serviceBuf, String tableName) {
		String baseName = initMapperNameByTable(tableName, true);
		serviceBuf.append("import javax.annotation.Resource;\r");
		serviceBuf.append("import org.springframework.stereotype.Service;\r\r");
		serviceBuf.append("import " + basePackageName + ".mapper." + baseName + "Mapper;\r\r");
		serviceBuf.append("import java.util.List;\r");
		serviceBuf.append("import java.util.ArrayList;\r");
		serviceBuf.append("import " + basePackageName + ".vo.Page;\r");
		serviceBuf.append("import " + basePackageName + ".vo." +baseName + "Vo;\r\r");
		serviceBuf.append("@Service\r");
		serviceBuf.append("public class " + baseName + "Service {\r\r");
		serviceBuf.append("\t@Resource\r");
		String firstCodeToLowerCase = firstCodeToLowerCase(baseName);
		String classMapper = baseName + "Mapper";
		String varMapper = firstCodeToLowerCase + "Mapper";
		serviceBuf.append("\tprivate " + classMapper + " " + varMapper + ";\r\r");
		String classVo = baseName + "Vo";
		String varVo = firstCodeToLowerCase + "Vo";
		initMethod(serviceBuf, baseName, classVo, varVo, varMapper, false);
		serviceBuf.append("}");
		return serviceBuf;
	}
	
	public StringBuffer dealMapperByTableName(StringBuffer mapperBuf, String tableName) {
		String mapperPackageNameString = basePackageName + ".mapper;\r";
		mapperBuf = initClassHead(tableName, mapperPackageNameString);
		String baseName = initMapperNameByTable(tableName, true);
		mapperBuf.append("import javax.annotation.Resource;\r");
		mapperBuf.append("import org.springframework.stereotype.Repository;\r\r");
		mapperBuf.append("import java.util.List;\r");
		mapperBuf.append("import java.util.ArrayList;\r");
		mapperBuf.append("import " + basePackageName + ".vo.Page;\r");
		mapperBuf.append("import " + basePackageName + ".vo." +baseName + "Vo;\r\r");
		mapperBuf.append("@Repository\r");
		mapperBuf.append("public interface " + baseName + "Mapper {\r\r");
		String firstCodeToLowerCase = firstCodeToLowerCase(baseName);
		String classVo = baseName + "Vo";
		String varVo = firstCodeToLowerCase + "Vo";
		initMethod(mapperBuf, baseName, classVo, varVo, null, true);
		mapperBuf.append("}");
		return mapperBuf;
	}
	
	public void initMethod(StringBuffer classHead, String baseName, String classVo, String varVo, String varMapper, boolean isMapper) {
		if (isMapper) {
			classHead.append("\tpublic void add" + baseName + "(" + classVo + " " + varVo + ") throws Exception;\r\r");
			classHead.append("\tpublic void update" + baseName + "(" + classVo + " " + varVo + ") throws Exception;\r\r");
			classHead.append("\tpublic List<" + classVo + "> get" + baseName + "ByPage(Page<" + classVo + "> page) throws Exception;\r\r");
			classHead.append("\tpublic void del" + baseName + "ById(" + classVo + " " + varVo + ") throws Exception;\r\r");
		} else {
			classHead.append("\tpublic void add" + baseName + "(" + classVo + " " + varVo + ") throws Exception {\r");
			classHead.append("\t\t" + varMapper + ".add" + baseName + "(" + varVo + ");\r");
			classHead.append("\t}\r\r");
			
			classHead.append("\tpublic void update" + baseName + "(" + classVo + " " + varVo + ") throws Exception {\r");
			classHead.append("\t\t" + varMapper + ".update" + baseName + "(" + varVo + ");\r");
			classHead.append("\t}\r\r");
			
			classHead.append("\tpublic List<" + classVo + "> get" + baseName + "ByPage(Page<" + classVo + "> page) throws Exception {\r");
			classHead.append("\t\treturn " + varMapper + ".get" + baseName + "ByPage(page);\r");
			classHead.append("\t}\r\r");
			
			classHead.append("\tpublic void del" + baseName + "ById(" + classVo + " " + varVo + ") throws Exception {\r");
			classHead.append("\t\t" + varMapper + ".del" + baseName + "ById(" + varVo + ");\r");
			classHead.append("\t}\r\r");
		}
	}
	
	public String firstCodeToLowerCase(String baseName) {
		return baseName.substring(0, 1).toLowerCase() + baseName.substring(1);
	}
	
	/**
	 * 通过sql表中的字段名生成对应的java变量名
	 * @param columns
	 * @return
	 */
	public List<String> initProperties(List<String> columns) {
		List<String> properties = new ArrayList<String>();
		for (String column : columns) {
			String property = initNameByTable(column, false);
			properties.add(property);
		}
		return properties;
	}	
	
	/**
	 * 生成resultMap的代码
	 * @param resultMap
	 * @param column
	 * @param property
	 * @param baseName
	 */
	public void initResultMap(StringBuffer resultMap, String column, String property, String baseName) {
		if (resultMap.length() == 0) {
			resultMap.append("\t<resultMap type=\"" + baseName + "Vo\" id=\"" + baseName + "Result\">\r");
		}
		resultMap.append("\t\t<result column=\"" + column + "\" property=\"" + property + "\"/>\r");
	}
	
	/**
	 * 生成select语句中的字段的基本代码，用于<sql>标签的使用
	 * @param baseSelect
	 * @param column
	 */
	public void initBaseSelectSql(StringBuffer baseSelect, String column, String baseName) {
		if (baseSelect.length() == 0) {
			baseSelect.append("\t<sql id=\"baseSelect\">");
			baseSelect.append("\r\t\t" + column);
		} else {
			baseSelect.append(",\r\t\t" + column);
		}
	}
	
	public void initCURD(StringBuffer CURD, String tableName, String baseName, List<String> columns, List<String> properties) {
		initCreateSql(CURD, tableName, baseName, columns, properties);
		initUpdateSql(CURD, tableName, baseName, columns, properties);
		initSelectSql(CURD, tableName, baseName);
		initDeleteSql(CURD, tableName, baseName);
	}
	
	public void initCreateSql(StringBuffer CURD, String tableName, String baseName, List<String> columns, List<String> properties) {
		CURD.append("\r\t<insert id=\"add" + baseName + "\" parameterType=\"" + baseName + "Vo\">\r");
		CURD.append("\t\tinsert into " + tableName + "(");
		int length = columns.size();
		StringBuffer columnBuf = new StringBuffer();
		StringBuffer valueBuf = new StringBuffer();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				columnBuf.append(columns.get(i));
				valueBuf.append("#{" + properties.get(i) + "}");
			} else {
				columnBuf.append(", " + columns.get(i));
				valueBuf.append(", #{" + properties.get(i) + "}");
			}
		}
		CURD.append(columnBuf + ")\r");
		CURD.append("\t\tvalues (" + valueBuf + ")\r");
		CURD.append("\t</insert>\r");
	}
	
	public void initUpdateSql(StringBuffer CURD, String tableName, String baseName, List<String> columns, List<String> properties) {
		CURD.append("\r\t<update id=\"update" + baseName + "\" parameterType=\"" + baseName + "Vo\">\r");
		CURD.append("\t\tupdate " + tableName + "\r");
		CURD.append("\t\tset\r");
		int length = columns.size();
		StringBuffer setBuf = new StringBuffer();
		StringBuffer whereBuf = new StringBuffer();
		for (int i = 0; i < length; i++) {
			if (i == 0) {
				setBuf.append("\t\t\t" + columns.get(i) + " = #{" + properties.get(i) + "}");
				whereBuf.append("\t\t\t" + columns.get(i) + " = #{" + properties.get(i) + "}");
			} else {
				setBuf.append(",\r\t\t\t" + columns.get(i) + " = #{" + properties.get(i) + "}");
				whereBuf.append(",\r\t\t\t" + columns.get(i) + " = #{" + properties.get(i) + "}");
			}
		}
		CURD.append(setBuf);
		CURD.append("\r\t\twhere\r");
		CURD.append(whereBuf + "\r");
		CURD.append("\t</update>\r");
	}
	
	public void initSelectSql(StringBuffer CURD, String tableName, String baseName) {
		CURD.append("\r\t<select id=\"get" + baseName + "ByPage\" parameterType=\"page\" resultMap=\"" + baseName + "Result\">\r");
		CURD.append("\t\tselect\r");
		CURD.append("\t\t\t<include refid=\"baseSelect\"/>\r");
		CURD.append("\t\tfrom\r");
		CURD.append("\t\t\t" + tableName + "\r");
		CURD.append("\t</select>\r");
	}
	
	public void initDeleteSql(StringBuffer CURD, String tableName, String baseName) {
		CURD.append("\r\t<delete id=\"del" + baseName + "ById\" parameterType=\"int\">\r");
		CURD.append("\t\tdelete from " + tableName + " where \r");
		CURD.append("\t</delete>\r");
	}
	
	/**
	 * 将类代码、import代码、对象代码、getter和setter代码拼接到package代码中
	 * @param packageBuf
	 * @param clazzBuf
	 * @param importBuf
	 * @param fieldBuf
	 * @param methodBuf
	 * @return
	 */
	public StringBuffer appendOtherBuffer(StringBuffer packageBuf, StringBuffer clazzBuf, StringBuffer importBuf, StringBuffer fieldBuf, StringBuffer methodBuf) {
		packageBuf.append(importBuf.toString());
		clazzBuf.append(fieldBuf.toString());
		clazzBuf.append(methodBuf.toString());
		clazzBuf.append("}");
		packageBuf.append(clazzBuf.toString());
		return packageBuf;
	}
	
	/**
	 * 初始化mybatis中的xml的文件头代码
	 * @param tableName
	 * @return
	 */
	public StringBuffer initXmlHead(String tableName) {
		StringBuffer xmlBuf = new StringBuffer();
		xmlBuf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\r");
		xmlBuf.append("<!DOCTYPE mapper \r\tPUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\"\r\t\"http://mybatis.org/dtd/mybatis-3-mapper.dtd\">\r\r");
		String mapperName = initMapperNameByTable(tableName, true);
		xmlBuf.append("<mapper namespace=\"" + xmlBaseNamespace + mapperName + "Mapper\">\r");
		return xmlBuf;
	}
	
	/**
	 * 通过sql的表名生成mapper前面的名字，例：DeptMapper中的Dept就是此类在tg_dept表名的基础上生成的
	 * @param tableName
	 * @return
	 */
	public String initMapperNameByTable(String tableName, boolean isUpperCase) {
		if (isUpperCase) {
			String[] split = tableName.split("_");
			StringBuffer strBuf = new StringBuffer();
			int length = split.length;
//			for (String str : split) {
			for (int i = 1; i < length; i++) {
					strBuf.append(split[i].substring(0, 1).toUpperCase() + split[i].substring(1));
			}
			return strBuf.toString();
		} else {
			return tableName.substring(tableName.indexOf("_") + 1);
		}
	}
	
	/**
	 * 通过表名生成类名
	 * @param tableName
	 * @param isTable
	 * @return
	 */
	public String initNameByTable(String tableName, boolean isTable) {
		String[] split = tableName.split("_");
		StringBuffer strBuf = new StringBuffer();
		int length = split.length;
//		for (String str : split) {
		for (int i = 0; i < length; i++) {
			if (i > 0 || isTable) {
				strBuf.append(split[i].substring(0, 1).toUpperCase() + split[i].substring(1));
			} else {
				strBuf.append(split[i]);
			}
		}
		return strBuf.toString();
	}
	
	/**
	 * 获取sql中对该字段的注释信息和该字段
	 * @param columnName
	 * @param columnType
	 * @param columnExplain
	 * @param fieldBuf
	 * @param importBuf
	 * @return
	 */
	public StringBuffer getFieldStr(String columnName, String columnType, String columnExplain, StringBuffer fieldBuf, StringBuffer importBuf) {
		if (columnExplain != null) {
			fieldBuf.append("\t// " + columnExplain + "\r");
		}
//		columnType = columnType.substring(0, columnType.indexOf("("));
		fieldBuf.append("\tprivate " + columnType + " " + columnName + ";\r\r");
		return fieldBuf;
	}
	
	/**
	 * 将sql中的数据类型转换成java中的数据类型
	 * @param columnType
	 * @param importBuf
	 * @return
	 */
	private String typeTrans(String columnType, StringBuffer importBuf) {
		columnType = columnType.toLowerCase();
		if (columnType.contains("tinyint")) {
			return "boolean";
		} else if (columnType.contains("int")) {
			return "int";
		} else if (columnType.contains("bigint")) {
			return "long";
		} else if (columnType.contains("float")) {
			return "float";
		} else if (columnType.contains("double")) {
			return "double";
		} else if (columnType.contains("varchar") || columnType.contains("text") || 
				columnType.contains("longtext") || columnType.contains("blob")) {
			return "String";
		} else if (columnType.contains("date")) {
			if (columnType.equalsIgnoreCase("date")) {
				if (!importBuf.toString().contains("java.util.Date")) {
					importBuf.append("import java.util.Date;\r");
				}
				return "Date";
			} else if (columnType.equalsIgnoreCase("datetime")) {
				if (!importBuf.toString().contains("java.sql.Timestamp")) {
					importBuf.append("import java.sql.Timestamp;\r\r");
				}
				return "Timestamp";
			}
		}
		return "String";
	}
	
	/**
	 * 生成对应字段的getter和setter
	 * @param columnName
	 * @param columnType
	 * @param methodBuf
	 * @return
	 */
	public StringBuffer getMethodStr(String columnName, String columnType, StringBuffer methodBuf) {
		String tmp = columnName.substring(0, 1).toUpperCase() + columnName.substring(1, columnName.length());
		methodBuf.append("\tpublic " + columnType + " get" + tmp + "() {\r");
		methodBuf.append("\t\treturn " + columnName + ";\r");
		methodBuf.append("\t}\r\r");
		methodBuf.append("\tpublic void set" + tmp + "(" + columnType + " " + columnName + ") {\r");
		methodBuf.append("\t\tthis." + columnName + " = " + columnName + ";\r");
		methodBuf.append("\t}\r\r");
		return methodBuf;
	}
}
