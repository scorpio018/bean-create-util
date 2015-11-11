package com.scorpio.bean_create_util.yy.bean_create_util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class C3P0Utils {
	private static DataSource dataSource;
	
	private static ThreadLocal<Connection> threadLocal = new ThreadLocal<Connection>();
	
	static {
		dataSource = new ComboPooledDataSource();
	}
	
	public static DataSource getDataSource() {
		return dataSource;
	}
	
	public static Connection getConnection() throws SQLException {
		Connection conn = threadLocal.get();
		if (conn == null) {
			conn = dataSource.getConnection();
			threadLocal.set(conn);
		}
		return conn;
	}
	
	public static ResultSet query(Connection conn, String sql) throws SQLException {
		Statement st = conn.createStatement();
		return st.executeQuery(sql);
	}
	
	public static boolean insert(Connection conn, String sql) throws SQLException {
		Statement st = conn.createStatement();
		return st.execute(sql);
	}
	
	public static void startTransaction() throws SQLException {
		Connection conn = getConnection();
		conn.setAutoCommit(false);
	}
	
	public static void commitTransaction() throws SQLException {
		Connection conn = threadLocal.get();
		if (conn != null) {
			conn.commit();
			closeConnection(conn);
		}
	}
	
	public static void closeConnection(Connection conn) throws SQLException {
		conn.close();
		threadLocal.remove();
	}
}
