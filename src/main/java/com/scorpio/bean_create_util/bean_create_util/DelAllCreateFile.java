package com.scorpio.bean_create_util.bean_create_util;

import java.io.File;

public class DelAllCreateFile {
	
	String baseFilePath = "D:" + File.separator + "maven_workspace" + File.separator + "bean-create-util" + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator + "com" + File.separator + "scorpio";
	public String[] folders = {"vo", "mapper", "service", "xml"};
	
	public void delAllCreateFile() {
		for (String folder : folders) {
			File classFile = new File(baseFilePath + File.separator + folder);
			del(classFile);
		}
	}
	
	public void del(File file) {
		if (!file.delete()) {
			File[] listFiles = file.listFiles();
			for (File f : listFiles) {
				del(f);
			}
			file.delete();
		}
	}
	
	public static void main(String[] args) {
		DelAllCreateFile del = new DelAllCreateFile();
		del.delAllCreateFile();
		System.out.println("删除完毕");
	}
}
