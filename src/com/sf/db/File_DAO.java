package com.sf.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.sf.dwnload.Dwnloader.DwnStatus;
import com.sf.dwnload.dwninfo.BaseDwnInfo;


public class File_DAO {
	
//	_url, _total, _current, _status,_md5, _path, _during
	
	public static final String TABLE = "_dwn_info";
	
	public static final String INDEX = "_index_info";
	
	/**
	 * 下载链接
	 */
	public static final String COL_URL = "_url";
	
	public static final String COL_TOTAL = "_total";
	
	public static final String COL_CURR = "_current";
	
	public static final String COL_STATUS = "_status";
	
	public static final String COL_MD5 = "_md5";
	
	public static final String COL_PATH = "_path";
	
	public static final String COL_DURING = "_during";
	
	public static String getSQL() {
		
		StringBuilder sql = new StringBuilder();
		sql.append("CREATE TABLE IF NOT EXISTS ").append(TABLE);
		sql.append("   ( ");
		sql.append(COL_URL).append(" TEXT NOT NULL PRIMARY KEY  UNIQUE ");
		sql.append(" , ").append(COL_TOTAL).append(" INTEGER ");
		sql.append(" , ").append(COL_CURR).append(" INTEGER ");
		sql.append(" , ").append(COL_STATUS).append(" INTEGER ");
		sql.append(" , ").append(COL_MD5).append(" TEXT ");
		sql.append(" , ").append(COL_PATH).append(" TEXT ");
		sql.append(" , ").append(COL_DURING).append(" INTEGER ");
		sql.append("   ) ");
		
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
	
	/**
	 * 
	 * @param db
	 * @param uri
	 * @return
	 */
	public int getDwnStatus(SQLiteDatabase db, String uri) {
		
		int ret = DwnStatus.STATUS_NONE;
		
		if (null != db && db.isOpen()) {
			
			Cursor cursor = null;
			try {
				cursor = db.query(TABLE, new String[]{COL_STATUS}
				, COL_URL + " = ? ", new String[]{uri}, null, null, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (cursor.moveToNext()) {
				int status = cursor.getInt(0);
				ret = status;
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
	
	
	public BaseDwnInfo getDwnInfo(SQLiteDatabase db, String uri) {
		
		BaseDwnInfo ret = null;
		
		if (null != db && db.isOpen()) {
			
			Cursor cursor = null;
			try {
				cursor = db.query(TABLE, new String[]{COL_URL, COL_CURR, COL_TOTAL, COL_STATUS, COL_DURING, COL_MD5, COL_PATH}
				, COL_URL + " = ? ", new String[]{uri}, null, null, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if (cursor.moveToNext()) {
				String url = cursor.getString(0);
				long current = cursor.getLong(1);
				long total = cursor.getLong(2);
				int status = cursor.getInt(3);
				int during = cursor.getInt(4);
				String md5 = cursor.getString(5);
				String path = cursor.getString(6);
				
				ret = new BaseDwnInfo(url);
				ret.setmCurrent_Size(current);
				ret.setmTotal_Size(total);
				ret.setmMd5(md5);
				ret.setmDwnStatus(status);
				ret.setmSavePath(path);
				ret.setmDuring(during);;
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
	
	// 获取下载信息， 插入下载记录，
	public boolean insert(SQLiteDatabase db, BaseDwnInfo info) {
		if (null != info && null != db && db.isOpen()) {
			
			ContentValues values = new ContentValues();
			values.put(COL_URL, info.getmUri());
			values.put(COL_TOTAL, info.getmTotal_Size());
			values.put(COL_CURR, info.getmCurrent_Size());
			values.put(COL_STATUS, info.getmDwnStatus());
			values.put(COL_PATH, info.getmSavePath());
			long id = -1;
			try {
				id = db.insert(TABLE, "", values);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return id != -1;
		}
		return false;
	}
	
	// 获取下载信息， 插入下载记录，
	public boolean update(SQLiteDatabase db, BaseDwnInfo info) {
		if (null != info && null != db && db.isOpen()) {
			
			ContentValues values = new ContentValues();
			values.put(COL_TOTAL, info.getmTotal_Size());
			values.put(COL_CURR, info.getmCurrent_Size());
			values.put(COL_STATUS, info.getmDwnStatus());
			values.put(COL_PATH, info.getmSavePath());
			values.put(COL_DURING, info.getmDuring());
			values.put(COL_MD5, info.getmMd5());
			long id = -1;
			try {
				id = db.update(TABLE, values, COL_URL + " = ? ", new String[]{info.getmUri()});
			} catch (Exception e) {
				e.printStackTrace();
			}
			return id > 0;
		}
		return false;
	}
	
}
