package com.sf.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.sf.dwnload.dwninfo.APKDwnInfo;


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
	
	public static final String COL_APPNAME = "_appname";
	
	public static String getSQL() {
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE IF NOT EXISTS ").append(TABLE);
		sql.append("   ( ");
		sql.append(COL_URL).append(" TEXT NOT NULL PRIMARY KEY  UNIQUE ");
		sql.append(" , ").append(COL_PKG).append(" TEXT NOT NULL ");
		sql.append(" , ").append(COL_VSNAME).append(" TEXT ");
		sql.append(" , ").append(COL_VSCODE).append(" INTEGER ");
		sql.append(" , ").append(COL_ICON).append(" TEXT ");
		sql.append(" , ").append(COL_APPNAME).append(" TEXT ");
		sql.append("   ) ");
		
		return sql.toString();
	}
	
	public static String getViewSQL() {
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE VIEW  ").append(VIEW);
		sql.append("  AS SELECT * FROM   ").append(File_DAO.TABLE).append(" AS A ").append(" INNER JOIN ").append(TABLE).append(" AS B ");
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
	
	public APKDwnInfo getDwnInfo(SQLiteDatabase db, String uri) {
		
		if (null != db) {
			APKDwnInfo ret = null;
			
			if (null != db && db.isOpen()) {
				
				Cursor cursor = null;
				try {
					cursor = db.query(VIEW, new String[]{COL_URL
							, COL_PKG
							, COL_ICON
							, COL_VSCODE
							, COL_VSNAME
							, COL_APPNAME
							,File_DAO.COL_CURR
							,File_DAO.COL_TOTAL
							,File_DAO.COL_DURING
							,File_DAO.COL_MD5
							,File_DAO.COL_PATH
							,File_DAO.COL_STATUS}
					, COL_URL + " = ? ", new String[]{uri}, null, null, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				if (cursor.moveToNext()) {
					String url = cursor.getString(0);
					String pkg = cursor.getString(1);
					String icon = cursor.getString(2);
					int vscode = cursor.getInt(3);
					String vsname = cursor.getString(4);
					String appName = cursor.getString(5);
					long current = cursor.getLong(6);
					long total = cursor.getLong(7);
					int during = cursor.getInt(8);
					String md5 = cursor.getString(9);
					String path = cursor.getString(10);
					int status = cursor.getInt(11);
					ret = new APKDwnInfo(url, pkg, vsname, vscode, icon, appName);
					
					ret.setmCurrent_Size(current);
					ret.setmTotal_Size(total);
					ret.setmDuring(during);
					ret.setmMd5(md5);
					ret.setmSavePath(path);
					ret.setmDwnStatus(status);
					
				}
				
				if (null != cursor) {
					try {
						cursor.close();
					} catch (Exception e) {
					}
				}
				
			}
			
			return ret;
		}
		
		return null;
	}
	
	
	public List<APKDwnInfo> getDwnInfoList(SQLiteDatabase db) {
		
		if (null != db) {
			
			List<APKDwnInfo> ret = null;
			
			if (null != db && db.isOpen()) {
				
				Cursor cursor = null;
				try {
					cursor = db.query(VIEW, new String[]{
							COL_URL
							,COL_PKG
							, COL_ICON
							, COL_VSCODE
							, COL_VSNAME
							, COL_APPNAME
							, File_DAO.COL_CURR
							, File_DAO.COL_TOTAL
							, File_DAO.COL_DURING
							, File_DAO.COL_MD5
							, File_DAO.COL_PATH
							, File_DAO.COL_STATUS}
					, null, null, null, null, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				while (cursor.moveToNext()) {
					
					if (null == ret) {
						ret = new ArrayList<APKDwnInfo>();
					}
					
					String url = cursor.getString(0);
					String pkg = cursor.getString(1);
					String icon = cursor.getString(2);
					int vscode = cursor.getInt(3);
					String vsname = cursor.getString(4);
					String appName = cursor.getString(5);
					long current = cursor.getLong(6);
					long total = cursor.getLong(7);
					int during = cursor.getInt(8);
					String md5 = cursor.getString(9);
					String path = cursor.getString(10);
					int status = cursor.getInt(11);
					APKDwnInfo info = new APKDwnInfo(url, pkg, vsname, vscode, icon, appName);
					
					info.setmCurrent_Size(current);
					info.setmTotal_Size(total);
					info.setmDuring(during);
					info.setmMd5(md5);
					info.setmSavePath(path);
					info.setmDwnStatus(status);
					ret.add(info);
					
				}
				
				if (null != cursor) {
					try {
						cursor.close();
					} catch (Exception e) {
					}
				}
				
			}
			
			return ret;
		}
		
		return null;
	}
	
	// 获取下载信息， 插入下载记录，
	
	public boolean insert(SQLiteDatabase db, APKDwnInfo apkInfo) {
		
		if (null != db && db.isOpen()) {
			
			try {
				ContentValues values = new ContentValues();
				values.put(COL_ICON, apkInfo.getmIconUri());
				values.put(COL_PKG, apkInfo.getmPkgName());
				values.put(COL_URL, apkInfo.getmUri());
				values.put(COL_VSCODE, apkInfo.getmVsCode());
				values.put(COL_VSNAME, apkInfo.getmVsName());
				values.put(COL_APPNAME, apkInfo.getmAppName());
				long id = db.insertWithOnConflict(TABLE, "", values, SQLiteDatabase.CONFLICT_REPLACE);
				return id != -1;
			} catch (Exception e) {
			}
		}
		return false;
	}
	
	public boolean delete(SQLiteDatabase db, String uri) {
		
		if (null != db && db.isOpen()) {
			
			try {
				return (db.delete(TABLE, COL_URL + " = ? ", new String[]{uri}) != 0);
			} catch (Exception e) {
			}
		}
		return false;
	}
	
}
