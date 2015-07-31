package com.sf.dwnload;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import android.content.Context;
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
	
	public DwnManager(Context context) {
		
		checkInMainThread();
		
		this.mContext = context;
		
		mDBHelper = new DwnHelper(mContext);
		
		mDwnList = new HashMap<String, BaseDwnInfo>();
		mFutureList = new HashMap<String, Future<Integer>>();
		
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
				ret = mDBHelper.getDwnInfo(uri);
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
						
					if (dwnIfo instanceof APKDwnInfo) {
						// 插入版本号等信息
						//TODO
					} 
					
					// 插入基本信息
					dwnIfo.setmDwnStatus(DwnStatus.STATUS_DOWNLOADING);
					if (mDBHelper.insertBaseDwnInfo(dwnIfo)) {
						
						Future<Integer> ret = mExecutor.submit(new AbsDownloader(dwnIfo, AbsDownloader.MODE_NEW, dir, mDwnCallback));
						mDwnList.put(dwnIfo.getmUri(), dwnIfo);						// 修改状态
						mFutureList.put(dwnIfo.getmUri(), ret);						// 结果列表
					} else {
						return -1000;
					}
					
					break;

				default:
					break;
				}
			}
			return -1000;
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
						Future<Integer> ret = mExecutor.submit(new AbsDownloader(dwnIfo, AbsDownloader.MODE_CONTINUE, dir, mDwnCallback));
						mDwnList.put(dwnIfo.getmUri(), dwnIfo);						// 修改状态
						mFutureList.put(dwnIfo.getmUri(), ret);						// 结果列表
					} else {
						return -1000;
					}
					break;
					
				default:
					break;
				}
			}
			return -1000;
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
	
}
