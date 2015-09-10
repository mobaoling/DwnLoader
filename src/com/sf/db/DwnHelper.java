package com.sf.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.sf.dwnload.Dwnloader;
import com.sf.dwnload.dwninfo.APKDwnInfo;
import com.sf.dwnload.dwninfo.BaseDwnInfo;

import java.util.List;

public class DwnHelper extends SQLiteOpenHelper {
	
	private static final int DB_VERSION = 1;
	
	private static final String DB_NAME = "dwn_db.db";

    private static final String DB_ID_NAME = "dwn_db";

    private File_DAO mFile_Dao;
	
	private APK_DAO mApk_Dao;

	public DwnHelper(Context context, int ID) {
		super(context, ID == 0 ? DB_NAME : DB_ID_NAME + "_" + ID  + ".db", null, DB_VERSION);
		
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
        try {
            return mFile_Dao.getDwnStatus(getReadableDatabase(), uri);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Dwnloader.DwnStatus.STATUS_NONE;
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	public BaseDwnInfo getDwnInfo(String uri) {
        try {
            return mFile_Dao.getDwnInfo(getReadableDatabase(), uri);
        } catch (Exception e) {
           e.printStackTrace();
        }
        return null;
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	public APKDwnInfo getApkInfo(String uri) {
        try {
            return mApk_Dao.getDwnInfo(getReadableDatabase(), uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
	}
	
	public List<APKDwnInfo> getApkInfoList() {
        try {
            return mApk_Dao.getDwnInfoList(getReadableDatabase());
        } catch (Exception e) {
         e.printStackTrace();
        }
        return null;

	}

    public void resetDB() {
        try {
            mFile_Dao.resetDB(getWritableDatabase());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
	/**
	 * 
	 * @param dwnInfo
	 * @return
	 */
	public boolean insertBaseDwnInfo(BaseDwnInfo dwnInfo) {
        try {
            return mFile_Dao.insert(getWritableDatabase(), dwnInfo);
        } catch (Exception e) {
            return false;
        }
	}
	
	public boolean insertApkDwnInfo(APKDwnInfo apkInfo) {
		
		boolean ret = false;

		try {
            getWritableDatabase().beginTransaction();
			if (mApk_Dao.insert(getWritableDatabase(), apkInfo) && mFile_Dao.insert(getWritableDatabase(), apkInfo)) {
				getWritableDatabase().setTransactionSuccessful();
				ret = true;
			};
		} catch (Exception e) {
		} finally {
            try {
                getWritableDatabase().endTransaction();
            } catch (Exception e) {
                e.printStackTrace();
            }

		}
		
		return ret;
	}
	
	
	public boolean deleteApkDwnInfo(String uri) {
		boolean ret = false;

		try {
            getWritableDatabase().beginTransaction();
			mApk_Dao.delete(getWritableDatabase(), uri);
			mFile_Dao.delete(getWritableDatabase(), uri);
			getWritableDatabase().setTransactionSuccessful();
				ret = true;
		} catch (Exception e) {
		} finally {
            try {
                getWritableDatabase().endTransaction();
            } catch (Exception e) {
                e.printStackTrace();
            }
		}
		
		return ret;
	}
	
	public boolean updateBaseDwnInfo(BaseDwnInfo dwnInfo) {
        try {
            return mFile_Dao.update(getWritableDatabase(), dwnInfo);
        } catch (Exception e) {
            return false;
        }
	}
	

}
