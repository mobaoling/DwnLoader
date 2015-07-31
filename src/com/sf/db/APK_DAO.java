package com.sf.db;

import android.database.sqlite.SQLiteDatabase;

import com.sf.dwnload.dwninfo.APKDwnInfo;
import com.sf.dwnload.dwninfo.BaseDwnInfo;


public class APK_DAO {
	
//	_url, _total, _current, _status,_md5, _path, _during
	
	public static final String TABLE = "_apk_info";
	
	public static final String VIEW = "_view_apk_info";
	
	public static final String INDEX = "_index_apk_info";
	
	/**
	 * 下载链接
	 */
	public static final String COL_URL = "_url";
	
	public static final String COL_PKG = "_pgname";
	
	public static final String COL_VSNAME = "_vsname";
	
	public static final String COL_VSCODE = "_vscode";
	
	public static final String COL_ICON = "_iconurl";
	
	public static String getSQL() {
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE IF NOT EXISTS ").append(TABLE);
		sql.append("   ( ");
		sql.append(COL_URL).append(" TEXT NOT NULL PRIMARY KEY  UNIQUE ");
		sql.append(" , ").append(COL_PKG).append(" TEXT ");
		sql.append(" , ").append(COL_VSNAME).append(" TEXT ");
		sql.append(" , ").append(COL_VSCODE).append(" INTEGER ");
		sql.append(" , ").append(COL_ICON).append(" TEXT ");
		sql.append("   ) ");
		
		return sql.toString();
	}
	
	public static String getViewSQL() {
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE VIEW  ").append(VIEW);
		sql.append("  AS SELECT * FROM   ").append(File_DAO.TABLE).append(" AS A ").append(" LEFT JOIN ").append(TABLE).append(" AS B ");
		sql.append(" ON ");
		sql.append("A.").append(File_DAO.COL_URL).append(" = ").append("B.").append(COL_URL);
		return sql.toString();
	}
	
	public static String getIndexSQL() {
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE INDEX IF NOT EXISTS  ").append(INDEX);
		sql.append(" ON ").append(TABLE).append(" ( ");
		sql.append(COL_URL);
		sql.append(" ) ");
		return sql.toString();
	}
	
	public BaseDwnInfo getDwnInfo(SQLiteDatabase db, String uri) {
		
		if (null != db) {
			
		}
		
		return null;
	}
	
	// 获取下载信息， 插入下载记录，
	
	public boolean insert(SQLiteDatabase db, APKDwnInfo apkInfo) {
		
		
		return false;
	}
	
}
