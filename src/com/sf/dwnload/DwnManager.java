package com.sf.dwnload;

import java.util.concurrent.ThreadPoolExecutor;

import android.content.Context;

import com.sf.dwnload.dwninfo.BaseDwnInfo;


/**
 * 下载过程                                    下载状态
 * 1. 保存到数据库，下载信息               
 * 2. 建立网络请求
 * 3. 下载文件
 * 5. 文件校验
 * 
 * @author caojianbo
 *
 */
public class DwnManager {
	
	public static final int STATUS_DOWNLOADING = 1;       // 下载中
	
	public static final int STATUS_PAUSE = 2;             // 暂停
	
	public static final int STATUS_DOWNLOAD_OVER = 3;     // 下载结束
	
	public static final int STATUS_DOWNLOAD_NONE = -1;    // 
	
	public static final int STATUS_DOWNLOAD_FAILED = -2;   //
	
	private DwnWatcher mDwnWatcher;
	
	private Context mContext;
	
	private ThreadPoolExecutor mExecutor;
	
	private DwnManager(Context context) {
		
		this.mContext = context;
		mDwnWatcher =new DwnWatcher();
	}
	
	public BaseDwnInfo getDwnInfo(String uri) {
		
		return null;
	}
	
	
	public void dwnFile(BaseDwnInfo dwnIfo) {
		
	}
	
	
	
}
