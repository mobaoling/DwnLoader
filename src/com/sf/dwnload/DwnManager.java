package com.sf.dwnload;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.sf.db.DwnHelper;
import com.sf.dwnload.Dwnloader.DwnStatus;
import com.sf.dwnload.dwninfo.APKDwnInfo;
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
	
	private Context mContext;
	
	private DwnHelper mDBHelper;
	
	private ThreadPoolExecutor mExecutor;
	
	protected HashMap<String, BaseDwnInfo> mDwnList;     // 下载列表
	
	protected HashMap<String, Future<Integer>> mFutureList;    // 下载结果列表
	
	protected HashMap<String, AbsDownloader> mTaskList; 
	
	public static final String ACTION_DWN_STATUS_CHANGE = "com.tv.dwn.info.change.sf.action";
	
	public static final String EXTRA_URI = "com.tv.dwn.info.change.extra.uri";
	
	public static final String EXTRA_STATUS = "com.tv.dwn.info.change.extra.sta";
	
	private ContextHandler mContextHandler;
	
	public DwnManager(Context context) {
		
		checkInMainThread();
		
		this.mContext = context;
		
		mContextHandler = new ContextHandler(this.mContext);
		
		mDBHelper = new DwnHelper(mContext);
		
		mDwnList = new HashMap<String, BaseDwnInfo>();
		mFutureList = new HashMap<String, Future<Integer>>();
		mTaskList = new HashMap<String, AbsDownloader>();
		
		mExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(2);
	}
	
	
	
	/**
	 * 下载状态改变回调结果
	 */
	private IDwnCallback mDwnCallback = new IDwnCallback() {

		@Override
		public boolean onDwnStatusChange(String uri, int dwnResult) {
			BaseDwnInfo info = mDwnList.get(uri);
			info.setmDwnStatus(dwnResult);
			Log.d("caojianbo", uri + " onDwnStatusChange " + dwnResult);
			mDBHelper.updateBaseDwnInfo(info);
			mContextHandler.notifyStatusChange(uri, dwnResult);
			return true;
		}
	};
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	public BaseDwnInfo getDwnInfo(String uri) {
		synchronized (DwnManager.class) {
			BaseDwnInfo ret = mDwnList.get(uri);
			if (null == ret) {
				
				APKDwnInfo apkInfo = mDBHelper.getApkInfo(uri);
				if (null != apkInfo) {
					BaseDwnInfo dwnInfo = mDBHelper.getDwnInfo(uri);
					if (null != apkInfo && null != dwnInfo) {
						apkInfo.setmCurrent_Size(dwnInfo.getmCurrent_Size());
						apkInfo.setmDuring(dwnInfo.getmDuring());
						apkInfo.setmDwnStatus(dwnInfo.getmDwnStatus());
						apkInfo.setmMd5(dwnInfo.getmMd5());
						apkInfo.setmSavePath(dwnInfo.getmSavePath());
						apkInfo.setmTotal_Size(dwnInfo.getmTotal_Size());
					}
					
					ret = apkInfo;
					mDwnList.put(uri, ret);
				} else {
					ret = mDBHelper.getDwnInfo(uri);
					if (null != ret) {
						mDwnList.put(uri, ret);
					}
				}
				
				
			}
			return ret;
		}
	}
	
	/**
	 * 
	 * @param uri
	 * @return
	 */
	public APKDwnInfo getApkInfo(String uri) {
		synchronized (DwnManager.class) {
			APKDwnInfo ret = (APKDwnInfo)mDwnList.get(uri);
			if (null == ret) {
				ret = mDBHelper.getApkInfo(uri);
				BaseDwnInfo dwnInfo = mDBHelper.getDwnInfo(uri);
				if (null != ret && null != dwnInfo) {
					ret.setmCurrent_Size(dwnInfo.getmCurrent_Size());
					ret.setmDuring(dwnInfo.getmDuring());
					ret.setmDwnStatus(dwnInfo.getmDwnStatus());
					ret.setmMd5(dwnInfo.getmMd5());
					ret.setmSavePath(dwnInfo.getmSavePath());
					ret.setmTotal_Size(dwnInfo.getmTotal_Size());
				}
				if (null != ret) {
					mDwnList.put(uri, ret);
				}
			}
			return ret;
		}
	}
	
	/**
	 * 
	 * @param dwnIfo 下载信息
	 * @return int  <br/>开始下载 > 0 <br/> 下载失败  < 0
	 */
	public int dwnFile(final BaseDwnInfo dwnIfo, String dir) {
		synchronized (DwnManager.class) {
			if (null != dwnIfo) {
				
				// 判断状态
				int status = mDBHelper.getDwnStatus(dwnIfo.getmUri());  // 转换状态
				switch (DwnStatus.convert_Status(status)) {
				case DwnStatus.STATUS_NONE:
				case DwnStatus.STATUS_FAIL:
					boolean apksuccess = false;
					if (dwnIfo instanceof APKDwnInfo) {
						// 插入版本号等信息
						apksuccess = mDBHelper.insertApkDwnInfo((APKDwnInfo)dwnIfo);
					} 
					
					// 插入基本信息
					dwnIfo.setmDwnStatus(DwnStatus.STATUS_DOWNLOADING);
					if (apksuccess && mDBHelper.insertBaseDwnInfo(dwnIfo)) {
						AbsDownloader task = new AbsDownloader(dwnIfo, AbsDownloader.MODE_NEW, dir, mDwnCallback);
						Future<Integer> ret = mExecutor.submit(task);
						mDwnList.put(dwnIfo.getmUri(), dwnIfo);						// 修改状态
						mFutureList.put(dwnIfo.getmUri(), ret);						// 结果列表
						mTaskList.put(dwnIfo.getmUri(), task);
					} else {
						return -1000;
					}
					
					break;

				default:
					return -1000;
				}
			}
			return 0;
		}	
	}
	
	/**
	 * 
	 * @param dwnIfo 下载信息
	 * @return int  <br/>开始下载 > 0 <br/> 下载失败  < 0
	 */
	public int continueDwnFile(final BaseDwnInfo dwnIfo, String dir) {
		
		synchronized (DwnManager.class) {
			if (null != dwnIfo) {
				// 判断状态
				int status = mDBHelper.getDwnStatus(dwnIfo.getmUri());
				switch (status) {
				case DwnStatus.STATUS_PAUSE:
					Log.d("caojianbo", "继续下载");
					// 插入基本信息
					dwnIfo.setmDwnStatus(DwnStatus.STATUS_DOWNLOADING);
					if (mDBHelper.updateBaseDwnInfo(dwnIfo)) {
						AbsDownloader task = new AbsDownloader(dwnIfo, AbsDownloader.MODE_CONTINUE, dir, mDwnCallback);
						Future<Integer> ret = mExecutor.submit(task);
						mDwnList.put(dwnIfo.getmUri(), dwnIfo);						// 修改状态
						mFutureList.put(dwnIfo.getmUri(), ret);						// 结果列表
						mTaskList.put(dwnIfo.getmUri(), task);
					} else {
						return -1000;
					}
					break;
					
				default:
					return -1000;
				}
			}
			return 0;
		}
	}
	
	/**
	 * 暂停下载
	 * @return
	 */
	public boolean pause(String uri) {
		
		boolean retBool = false;
		long t1 = System.currentTimeMillis();
		if (null != uri) {
			
			Future<Integer> future = null;
			synchronized (uri) {
				BaseDwnInfo dwnInfo = mDwnList.get(uri);
				future = mFutureList.get(uri);
				
				// task 还未被执行
				if (mExecutor.getQueue().contains(future)) {
					
					Log.d("caojianbo", "pause task 未执行");
					synchronized (DwnManager.class) {
						future.cancel(true);
						dwnInfo.setmDwnStatus(DwnStatus.STATUS_PAUSE);
						// 更新
						mDBHelper.updateBaseDwnInfo(dwnInfo);
					}
					retBool  = true;
				} else {
					Log.d("caojianbo", "pause task 已经执行");
					if (null != dwnInfo && null != future) {
						Log.d("caojianbo", "pause");
						synchronized (DwnManager.class) {
							dwnInfo.setmDwnStatus(DwnStatus.STATUS_READY_PAUSE);
							AbsDownloader task = mTaskList.get(dwnInfo.getmUri());
							if (null != task) {
								task.disconnect();
							}
						}
						int ret = -1;						//TODO 初始值
						try {
							if (null != future) {
								ret = future.get();
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch (ExecutionException e) {
							e.printStackTrace();
						} catch (Exception e) {
						}
						
						retBool = ( ret == DwnStatus.STATUS_PAUSE);
					}
				}
				
			}
			
			
			Log.d("caojianbo", "return "  +" time  " + (System.currentTimeMillis() - t1));
		}
		return retBool;
	}
	
	private void checkInMainThread() {
		if (Looper.getMainLooper() != Looper.myLooper()) {
			throw new RuntimeException("dwnFile method must call in Main Thread");
		}
	}
	
	/**
	 * 下载状态改变时候的回掉
	 * @author caojianbo
	 *
	 */
	public static interface IDwnCallback {
		public boolean onDwnStatusChange(String uri, int dwnResult);
	}
	
	
	public static class ContextHandler extends Handler {
		
		private WeakReference<Context> mConReference;
		
		public ContextHandler(Context context) {
			mConReference = new WeakReference<Context>(context);
		}
		
		public void notifyStatusChange(String uri, int status) {
			Context context = mConReference.get();
			if (null != context) {
				
				Intent intent = new Intent();
				intent.setAction(ACTION_DWN_STATUS_CHANGE);
				intent.putExtra(EXTRA_URI, uri);
				intent.putExtra(EXTRA_STATUS, status);
				context.sendBroadcast(intent);
			}
		}
		
	}
	
}
