package com.sf.db;

import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sf.dwnload.dwninfo.APKDwnInfo;
import com.sf.dwnload.dwninfo.BaseDwnInfo;

public class DwnHelper extends SQLiteOpenHelper {
	
	private static final int DB_VERSION = 1;
	
	private static final String DB_NAME = "dwn_db.db"; 
	
	private File_DAO mFile_Dao;
	
	private APK_DAO mApk_Dao;

	public DwnHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		
		mFile_Dao = new File_DAO();
		mApk_Dao = new APK_DAO();
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		
		if (null != db) {
			db.execSQL(File_DAO.getSQL());
			db.execSQL(APK_DAO.getSQL());
			db.execSQL(APK_DAO.getViewSQL());
			db.execSQL(File_DAO.getIndexSQL());
			db.execSQL(APK_DAO.getIndexSQL());
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
	
	
	// *************************************************************************
	//   所有增删查改操作
	//**************************************************************************
	
	public int getDwnStatus(String uri) {
		 return mFile_Dao.getDwnStatus(getReadableDatabase(), uri);
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	public BaseDwnInfo getDwnInfo(String uri) {
		 return mFile_Dao.getDwnInfo(getReadableDatabase(), uri);
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	public APKDwnInfo getApkInfo(String uri) {
		 return mApk_Dao.getDwnInfo(getReadableDatabase(), uri);
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	public List<APKDwnInfo> getApkInfoList() {
		 return mApk_Dao.getDwnInfoList(getReadableDatabase());
	}

    public void resetDB() {
        mFile_Dao.resetDB(getWritableDatabase());
    }
	
	/**
	 * 
	 * @param dwnInfo
	 * @return
	 */
	public boolean insertBaseDwnInfo(BaseDwnInfo dwnInfo) {
		return mFile_Dao.insert(getWritableDatabase(), dwnInfo);
	}
	
	public boolean insertApkDwnInfo(APKDwnInfo apkInfo) {
		
		boolean ret = false;
		getWritableDatabase().beginTransaction();
		try {
			if (mApk_Dao.insert(getWritableDatabase(), apkInfo) && mFile_Dao.insert(getWritableDatabase(), apkInfo)) {
				getWritableDatabase().setTransactionSuccessful();
				ret = true;
			};
		} catch (Exception e) {
		} finally {
			getWritableDatabase().endTransaction();
		}
		
		return ret;
	}
	
	
	public boolean deleteApkDwnInfo(String uri) {
		boolean ret = false;
		getWritableDatabase().beginTransaction();
		try {
			mApk_Dao.delete(getWritableDatabase(), uri);
			mFile_Dao.delete(getWritableDatabase(), uri);
			getWritableDatabase().setTransactionSuccessful();
				ret = true;
		} catch (Exception e) {
		} finally {
			getWritableDatabase().endTransaction();
		}
		
		return ret;
	}
	
	public boolean updateBaseDwnInfo(BaseDwnInfo dwnInfo) {
		return mFile_Dao.update(getWritableDatabase(), dwnInfo);
	}
	

}
