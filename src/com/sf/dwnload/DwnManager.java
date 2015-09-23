package com.sf.dwnload;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.sf.db.DwnHelper;
import com.sf.dwnload.Dwnloader.DwnStatus;
import com.sf.dwnload.dwninfo.APKDwnInfo;
import com.sf.dwnload.dwninfo.BaseDwnInfo;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;


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

    /**
     *
     */
    private ArrayList<IDwnCallback> mStatusWatchList;

    /**
     *
     * @param context
     * @param identfier   唯一标识符，标识下载器（每个应用中应该包含不同的下载器）
     */
	public DwnManager(Context context, int identfier) {
		
		checkInMainThread();
		
		this.mContext = context;
		
		mContextHandler = new ContextHandler(this, this.mContext);
		
		mDBHelper = new DwnHelper(mContext, identfier);
		
		mDwnList = new HashMap<String, BaseDwnInfo>();
		mFutureList = new HashMap<String, Future<Integer>>();
		mTaskList = new HashMap<String, AbsDownloader>();

        mStatusWatchList = new ArrayList<IDwnCallback>();
		
		resetDB();

		mExecutor = (ThreadPoolExecutor)Executors.newFixedThreadPool(2, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("download_manager ");
                return t;
            }
        });
	}
	
	private void resetDB() {
		mDBHelper.resetDB();
	}

    /**
     * 注册下载状态监听
     * @param callback
     */
    public void regeisterDwnStatusCallback(IDwnCallback callback) {
        if (null != mStatusWatchList && !mStatusWatchList.contains(callback)) {
            mStatusWatchList.add(callback);
        }
    }

    protected ArrayList<IDwnCallback> getStatusCallback() {
        return mStatusWatchList;
    }

	/**
	 * 下载状态改变回调结果
	 */
	private IDwnCallback mDwnCallback = new IDwnCallback() {

		@Override
		public boolean onDwnStatusChange(String uri, int dwnResult) {
			BaseDwnInfo info = mDwnList.get(uri);
			info.setmDwnStatus(dwnResult);
			mDBHelper.updateBaseDwnInfo(info);

            // 清空 future
            int status = DwnStatus.convert_Status(dwnResult);
            switch (status){
                case DwnStatus.STATUS_NONE:
                case DwnStatus.STATUS_FAIL:
                case DwnStatus.STATUS_SUCCESS:
                case DwnStatus.STATUS_PAUSE:
                    synchronized (DwnManager.class) {
                        mFutureList.remove(uri);
                        mTaskList.remove(uri);
                    }
                    break;
            }

			mContextHandler.notifyStatusChange(uri, dwnResult);

			return true;
		}
	};
	
	/**
	 * 获取所有APK下载任务
	 * @return
	 */
	public List<APKDwnInfo> getApkDwnList() {
		return mDBHelper.getApkInfoList();
	}
	
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
					ret = apkInfo;
					mDwnList.put(uri, ret);
				} else {
					ret = mDBHelper.getDwnInfo(uri);
					if (null != ret) {
						mDwnList.put(uri, ret);
					}
				}
			}
			
			if (null != ret) {
				
				String path = ret.getmSavePath();
				if (TextUtils.isEmpty(path) || (!(new File(path).exists()))) {
					switch (ret.getmDwnStatus()) {
//					case DwnStatus.STATUS_DOWNLOADING:  /// delete  
//					case DwnStatus.STATUS_PAUSE:
					case DwnStatus.STATUS_SUCCESS:
						ret.setmDwnStatus(DwnStatus.STATUS_NONE);
						mDBHelper.updateBaseDwnInfo(ret);
						break;
					default:
						break;
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
				if (null != ret) {
					mDwnList.put(uri, ret);
				}
			}
			
			if (null != ret) {
				String path = ret.getmSavePath();
				if (TextUtils.isEmpty(path) || (!(new File(path).exists()))) {
					switch (ret.getmDwnStatus()) {
//					case DwnStatus.STATUS_DOWNLOADING:
//					case DwnStatus.STATUS_PAUSE:
					case DwnStatus.STATUS_SUCCESS:
						ret.setmDwnStatus(DwnStatus.STATUS_NONE);
						mDBHelper.updateBaseDwnInfo(ret);
						break;
					default:
						break;
					}
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
	public int dwnFile(final BaseDwnInfo dwnIfo, AbsDownloader.DwnOption option, String ...dirs) {
		synchronized (DwnManager.class) {
			if (null != dwnIfo) {
				
				// 判断状态
				BaseDwnInfo exist = getDwnInfo(dwnIfo.getmUri());
				int status = null == exist ? DwnStatus.STATUS_NONE: exist.getmDwnStatus();  // 转换状态
				switch (DwnStatus.convert_Status(status)) {
				case DwnStatus.STATUS_NONE:
				case DwnStatus.STATUS_FAIL:
					boolean apksuccess = false;
					
					dwnIfo.setmCurrent_Size(0);
					
					// 插入基本信息
					dwnIfo.setmDwnStatus(DwnStatus.STATUS_DOWNLOADING);
					if (dwnIfo instanceof APKDwnInfo) {
						// 插入版本号等信息
						apksuccess = mDBHelper.insertApkDwnInfo((APKDwnInfo)dwnIfo);
					} else {
						apksuccess = mDBHelper.insertBaseDwnInfo(dwnIfo);
					}
					
					if (apksuccess) {
						AbsDownloader task = new AbsDownloader(dwnIfo, AbsDownloader.MODE_NEW, mDwnCallback, option, dirs);
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
	public int continueDwnFile(final BaseDwnInfo dwnIfo, AbsDownloader.DwnOption option, String... dir) {
		
		synchronized (DwnManager.class) {
			if (null != dwnIfo) {
				// 判断状态
				int status = mDBHelper.getDwnStatus(dwnIfo.getmUri());
				switch (status) {
				case DwnStatus.STATUS_PAUSE:
				case DwnStatus.STATUS_NONE:
					// 插入基本信息
					dwnIfo.setmDwnStatus(DwnStatus.STATUS_DOWNLOADING);
					if (mDBHelper.updateBaseDwnInfo(dwnIfo)) {
						AbsDownloader task = new AbsDownloader(dwnIfo, AbsDownloader.MODE_CONTINUE, mDwnCallback, option, dir);
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
			BaseDwnInfo dwnInfo = null;
			synchronized (DwnManager.class) {
				dwnInfo = mDwnList.get(uri);
				future = mFutureList.get(uri);
			}
				
			// task 还未被执行
            if (null == future) {
                synchronized (DwnManager.class) {
                    if(null != dwnInfo) {
                        dwnInfo.setmDwnStatus(DwnStatus.STATUS_PAUSE);
                        // 更新
                        mDBHelper.updateBaseDwnInfo(dwnInfo);

                    }
                    retBool  = true;
                }
            } else if (mExecutor.getQueue().contains(future)) {
				
				synchronized (DwnManager.class) {
					future.cancel(true);
					dwnInfo.setmDwnStatus(DwnStatus.STATUS_PAUSE);
					// 更新
					mDBHelper.updateBaseDwnInfo(dwnInfo);
				}
				retBool  = true;
			} else {
				if (null != dwnInfo && null != future) {
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

		return retBool;
	}
	
	
	public boolean delete(String uri) {
		
		if (pause(uri)) {
			synchronized (DwnManager.class) {
				
				BaseDwnInfo info = getDwnInfo(uri);
				if (null != info && !TextUtils.isEmpty(info.getmSavePath())) {
					try {
						new File(info.getmSavePath()).delete();
					} catch (Exception e) {
					}
				}
				synchronized (DwnManager.class) {
					mDBHelper.deleteApkDwnInfo(uri);
					mDwnList.remove(uri);
					mTaskList.remove(uri);
					mFutureList.remove(uri);
				}
			}

			return true;
			
		}
		return false;
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

        private WeakReference<DwnManager> mDwnManager;
		
		public ContextHandler(DwnManager dm,  Context context) {
			mConReference = new WeakReference<Context>(context);
            mDwnManager = new WeakReference<DwnManager>(dm);
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

            DwnManager dm = mDwnManager.get();
            if (null != dm) {
                ArrayList<IDwnCallback> callbacks = dm.getStatusCallback();
                if (null != callbacks) {
                    for (int i = 0; i < callbacks.size(); i++) {
                        callbacks.get(i).onDwnStatusChange(uri, status);
                    }
                }
            }

		}
		
	}
	
}
