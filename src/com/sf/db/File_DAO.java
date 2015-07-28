package com.sf.db;


public class File_DAO {
	
//	_url, _total, _current, _status,_md5, _path, _during
	
	public static final String TABLE = "_dwn_info";
	
	/**
	 * 下载链接
	 */
	public static final String COL_URL = "_url";
	
	public static final String COL_TOTAL = "_total";
	
	public static final String COL_CURR = "_total";
	
	public static final String COL_STATUS = "_status";
	
	public static final String COL_MD5 = "_md5";
	
	public static final String COL_PATH = "_path";
	
	public static final String COL_DURING = "_during";
	
	public static String getSQL() {
		
		StringBuilder sql = new StringBuilder();
		sql.append("CRATE TABLE IF NOT EXISTS ");
		
		
		
		return null;
	}
	
}
