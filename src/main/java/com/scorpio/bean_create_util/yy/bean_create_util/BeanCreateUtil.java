package com.scorpio.bean_create_util.yy.bean_create_util;

import java.io.IOException;
import java.sql.SQLException;


public class BeanCreateUtil {
	public static void main(String[] args) {
		ShowValueByDataBase show = new ShowValueByDataBase();
		try {
			show.init();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
