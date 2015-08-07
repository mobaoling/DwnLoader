package com.sf.dwnload;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.sf.dwnload.Dwnloader.DwnStatus;
import com.sf.dwnload.dwninfo.BaseDwnInfo;

public class DwnAsker {
	
	private HashMap<String, IDwnWatcher> mWatchList;
	
	private LoopHandler mLoopHandler;
	
	private Context mContext;
	
	private boolean mCanworking = false;    // 处于是否可以工作状态
	
	public boolean  getIsWorking() {
		return mCanworking;
	}
	
	public HashMap<String, IDwnWatcher> getWatchList() {
		return mWatchList;
	}
	
	public DwnAsker(Context context) {
		mContext =  context;
		mLoopHandler = new LoopHandler(this);
		mWatchList = new HashMap<String, DwnAsker.IDwnWatcher>();
	}
	
	public void onResume() {
		mCanworking = true;
		
		mLoopHandler.start();
	}
	
	public void onPause() {
		mCanworking = false;
		mLoopHandler.stop();
	}
	
	/**
	 * 注册监听应用下载状态
	 * @param url
	 * @param watcher
	 */
	public void regeisterProgress(String url, IDwnWatcher watcher) {
		checkInMainThread();
		
		if (!TextUtils.isEmpty(url) && null != watcher) {
			mWatchList.put(url, watcher);
		}
		
		if (mWatchList.size() > 0) {
			mLoopHandler.start();
		} else {
			mLoopHandler.stop();
		}
	}
	/**
	 * 
	 * @param url 下载链接
	 */
	public void unregeisterProgress(String url) {
		checkInMainThread();
		
		if (!TextUtils.isEmpty(url)) {
			mWatchList.remove(url);
		}
		
		if (mWatchList.size() > 0) {
			mLoopHandler.start();
		} else {
			mLoopHandler.stop();
		}
		
	}
	
	
	/**
	 * 下载进度
	 * @author caojianbo
	 *
	 */
	public static class LoopHandler extends Handler {
		
		private final int MSG_LOOP = 1;
		
		private final int DURING = 150;
		
		public WeakReference<DwnAsker> mManager;
		
		public LoopHandler(DwnAsker asker) {
			mManager = new WeakReference<DwnAsker>(asker);
		}
		
		@Override
		public void handleMessage(Message msg) {
			
			switch (msg.what) {
			case MSG_LOOP:
				
				DwnAsker dwnAsker = mManager.get();
				
				if (null != dwnAsker) {
					
					HashMap<String, IDwnWatcher> wathList = dwnAsker.getWatchList();
					if (wathList.size() > 0) {
						
						Iterator<Entry<String, IDwnWatcher>> iterator = wathList.entrySet().iterator();
						List<String> mDwnOverList = new ArrayList<String>();    // 所有下载完成的uri
						while (iterator.hasNext()) {
							Entry<String, IDwnWatcher> value = iterator.next();
							String uri = value.getKey();
							BaseDwnInfo bsDwnInfo = value.getValue().getDwnInfo(uri);
							if (null == bsDwnInfo) {
								mDwnOverList.add(uri);
							}  else {
								
								long current = (null == bsDwnInfo ? 0 : bsDwnInfo.getmCurrent_Size());
								long total = (null == bsDwnInfo ? 0 : bsDwnInfo.getmTotal_Size());
								
								switch (DwnStatus.convert_Status(bsDwnInfo.getmDwnStatus())) {
								case DwnStatus.STATUS_DOWNLOADING:
									if (current == total && total > 0) {
										mDwnOverList.add(uri);
									}
									
									if (null != bsDwnInfo) {
										Log.d("caojianbo", " dwnasker " + " progress change " + current + "  " + total  );
										value.getValue().onProgressChange(uri,current, total);
									}
									break;
								case DwnStatus.STATUS_PAUSE:
								case DwnStatus.STATUS_SUCCESS:	
									if (null != bsDwnInfo) {
										value.getValue().onProgressChange(uri,current, total);
									}
									
									mDwnOverList.add(uri);
									break;
									
								case DwnStatus.STATUS_FAIL:	
								case DwnStatus.STATUS_NONE:
									mDwnOverList.add(uri);
									break;
								default:
									break;
								}
								
							}
						}
						
						// remove from watchList if downOver
						
						if (mDwnOverList.size() > 0) {
							for (String uri : mDwnOverList) {
								wathList.remove(uri);
							}
						}
						
						if (dwnAsker.getIsWorking()) {
							removeMessages(MSG_LOOP);
							sendEmptyMessageDelayed(MSG_LOOP, DURING);
						}
					}
				}
				
				
				break;

			default:
				break;
			}
		}
		
		public void start() {
			
			DwnAsker dwnAsker = mManager.get();
			
			if (null != dwnAsker) {
				
				if (dwnAsker.getIsWorking()) {
					removeMessages(MSG_LOOP);
					sendEmptyMessage(MSG_LOOP);
				}
				
			}
			
		}
		
		public void stop() {
			removeMessages(MSG_LOOP);
		}
	}
	
	private void checkInMainThread() {
		if (Looper.getMainLooper() != Looper.myLooper()) {
			throw new RuntimeException("dwnFile method must call in Main Thread");
		}
	}
	
	/**
	 * 下载进度
	 * @author caojianbo
	 *
	 */
	public static interface IDwnWatcher {
		public void onProgressChange(String uri, long current, long totao);
		
		public BaseDwnInfo getDwnInfo(String uri);
	}
}
