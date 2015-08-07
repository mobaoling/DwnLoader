package com.sf.dwnload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.StatFs;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.sf.dwnload.DwnManager.IDwnCallback;
import com.sf.dwnload.dwninfo.BaseDwnInfo;


public class AbsDownloader implements Dwnloader{
	
	private DwnOption mDwnOption;
	
	public static final int MODE_NEW = 0;
	
	public static final int MODE_CONTINUE = 1;
	
	private int mMode = MODE_NEW;
	
	private BaseDwnInfo mDwnInfo;
	
	private String mDir = null;
	
	private IDwnCallback mCallback;
	
	private HttpURLConnection mConnection;
	
	private Thread mThread;
	
	private boolean mManualDisconnect = false;
	
	public AbsDownloader(BaseDwnInfo dwnInfo, int mode, String dir, IDwnCallback callback) {
		mMode = mode;
		mDwnInfo = dwnInfo;
		this.mDir = dir;
		this.mCallback = callback;
	}
	
	public void disconnect() {
		if (null != mConnection) {
			try {
				if (null != mThread) {
					Log.d("caojianbo", "interrupt");
					mThread.interrupt();
				}
				mConnection.disconnect();
				
				mManualDisconnect = true;
				Log.d("caojianbo", "disconnect");
			} catch (Exception e) {
			}
		}
	}
	
	@Override
	public Integer call() throws Exception {
		return dwnFile(); 
	}
	
	public static class DwnOption {
		
		public  int mConnOutTime;
		
		public int mReadOutTime;
	}

	/**
	 * 在非主线程中执行，通知下载结果
	 * @param uri
	 * @param dir
	 */
	@Override
	public int dwnFile() {
		
		long beginTime = SystemClock.uptimeMillis();
		
		String uri = mDwnInfo.getmUri();
		synchronized (DwnManager.class) {
			mThread = Thread.currentThread();
		}
		
		URL url = null;
		int dwnstatus = DwnStatus.STATUS_NONE;
		try {
			url = new URL(uri);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		if (null == uri) {
			dwnstatus = DwnStatus.STATUS_FAIL_URL_ERROR;  													// uri 格式错误d
		} else {
			mConnection = null;
			try {
				mConnection = (HttpURLConnection)url.openConnection();
				if (null != mDwnOption) {
					mConnection.setConnectTimeout(mDwnOption.mConnOutTime);
					mConnection.setReadTimeout(mDwnOption.mReadOutTime);
				} else {
					mConnection.setConnectTimeout(5000);
					mConnection.setReadTimeout(5000);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch ( Exception e) {
				e.printStackTrace();
			}
			
			if (null == mConnection) {
				dwnstatus = DwnStatus.STATUS_FAIL_CONNECT_ERROR;													// 连接错误
			} else {
				
				// 智能调节当前模式
				if (mMode == MODE_CONTINUE) {
					String file = mDwnInfo.getmSavePath();
					if (null != file && new File(file).exists()) {
						
					} else {
						mMode = MODE_NEW;
					}
				}
				
				try {
					mConnection.setRequestMethod("GET");
					mConnection.addRequestProperty("Accept-Encoding", "identity");
				} catch (ProtocolException e) {
					e.printStackTrace();
				}
				
				// 继续下载
				if (mMode == MODE_CONTINUE) {
					mConnection.addRequestProperty("Range", "bytes=" + mDwnInfo.getmCurrent_Size() + "-");
				}
				
				addHeaders(mConnection);  // 添加请求头参数
				int responseCode = -1;
				ResThread t = new ResThread(){
					
					public void run() {
						try {
							int res = mConnection.getResponseCode();
							setResponseCode(res);
						} catch (SocketTimeoutException e) {
							setDwnStatus(DwnStatus.STATUS_FAIL_CONNECT_TIME_OUTL);
							e.printStackTrace();
						} catch (IOException e) {
							setDwnStatus(DwnStatus.STATUS_FAIL_CONNECT_ERROR);
						}
					};
				};
				t.start();
				try {
					t.join(1000 * 30);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				dwnstatus = t.getDwnStatus();
				responseCode = t.getResponseCode();
				
				Log.d("caojianbo", " get responsecode over " + responseCode);
				
				// 暂停
				synchronized (DwnManager.class) {
					if (mManualDisconnect && mDwnInfo.getmDwnStatus() == DwnStatus.STATUS_READY_PAUSE) {
						dwnstatus = DwnStatus.STATUS_PAUSE;
					}
				}
				
				Log.d("caojianbo", "response code " + responseCode);
				
				if (dwnstatus == DwnStatus.STATUS_PAUSE) {
					
				} else if (dwnstatus == DwnStatus.STATUS_FAIL_CONNECT_TIME_OUTL) {
					
				} else if (responseCode >= 200 && responseCode < 300) {
					try {
						File dir_File = new File(mDir);
						if (!dir_File.exists()) {
							try {
								dir_File.mkdirs();
							} catch (Exception e) {
							}
						}
						
						boolean dstExist = false;
						File finalPath = null;
						
						if (mMode == MODE_CONTINUE) {
							finalPath = new File(mDwnInfo.getmSavePath());
							if (finalPath.exists() && finalPath.isFile() && finalPath.canWrite()) {
								dstExist = true;
							}
						} else {
							if (dir_File.exists() && dir_File.isDirectory() ) {
								
								int firstIndex = uri.lastIndexOf("/") + 1;
								int index2 = uri.indexOf("?");
								String path = null;
								
								if (index2 == -1) {
									path = uri.substring(firstIndex);
								} else {
									path = uri.substring(firstIndex, index2);
								}
								
								finalPath = new File(dir_File, path);
								if (finalPath.exists()) {
									finalPath.delete();
								}
								boolean succ  = finalPath.createNewFile();
								if (succ && finalPath.exists() && finalPath.isFile() && finalPath.canWrite()) {
									dstExist = true;
								}
							} 
						}
						
						if (!dstExist) {
							dwnstatus = DwnStatus.STATUS_FAIL_MKDIR_FAIL;		// 文件创建失败
						} else {
							StatFs statFs = new StatFs(mDir);
							long size = mConnection.getContentLength();
							
							if (mMode == MODE_NEW) {
								mDwnInfo.setmTotal_Size(size);    // 设置文件大小
							}
							
							if (size > (long)statFs.getAvailableBlocks() * (long)statFs.getBlockSize()) {
								dwnstatus = DwnStatus.STATUS_FAIL_SPACE_NOT_ENO;  								// 磁盘剩余空间不足
							} else {
								boolean connectSuccess = false;// 是否连接成功
								RandomAccessFile raf = new RandomAccessFile(finalPath, "rws");
								mDwnInfo.setmSavePath(finalPath.getAbsolutePath());
								InputStream is = null;
								try {
									int count = 0;
									int current = 0;
									is = mConnection.getInputStream();
									
									if (mMode == MODE_CONTINUE) {
										current = (int)mDwnInfo.getmCurrent_Size();
										raf.seek(mDwnInfo.getmCurrent_Size());
									}
									
									byte[] tmp = new byte[1024 * 100];
									while ((count = is.read(tmp)) > 0) {
										raf.write(tmp, 0, count);
										current += count;
										mDwnInfo.setmCurrent_Size(current);
										if (mDwnInfo.getmDwnStatus() == DwnStatus.STATUS_READY_PAUSE) {
											break;
										}
									}
									connectSuccess = true;
								} catch (SocketTimeoutException e) {
									dwnstatus = DwnStatus.STATUS_FAIL_READ_TIME_OUTL;
								} catch (Exception e) {
									e.printStackTrace();
								}
								
								if (null != is) {
									try {
										is.close();
									} catch (Exception e) {
									}
								}
								
								// 关闭文件
								if (null != raf) {
									try {
										raf.close();
									} catch (Exception e) {
									}
								}
								
								synchronized (DwnManager.class) {
									if (mManualDisconnect) {
										connectSuccess = true;   // 手动断连，判断为连接成功
									}
								}
								
								if (dwnstatus == DwnStatus.STATUS_FAIL_READ_TIME_OUTL) {
									
								} else if (connectSuccess) {
									if (mDwnInfo.getmDwnStatus() == DwnStatus.STATUS_READY_PAUSE) {
										dwnstatus = DwnStatus.STATUS_PAUSE;									// 暂停
									} else {
										dwnstatus  = DwnStatus.STATUS_SUCCESS;								// 连接成功
									}
								} else {
									dwnstatus = DwnStatus.STATUS_FAIL_READ_FILE;							// 连接失败
								}
							}
						}
						
					} catch (Exception e) {
					}
					
				} else if (responseCode != -1) {
					dwnstatus =  (responseCode << 16 ) & DwnStatus.STATUS_FAIL_ERROR_CODE ;  											// 请求错误
				} else  {
					dwnstatus = DwnStatus.STATUS_FAIL_CONNECT_ERROR;
				}
				
				if (null != mConnection) {
					try {
						mConnection.disconnect();
					} catch (Exception e) {
					}
				}
			}
		}
		
		if (dwnstatus == DwnStatus.STATUS_SUCCESS && !TextUtils.isEmpty(mDwnInfo.getmMd5())) {
			// check md5
		}
		
		
		// 时间统计
		
		switch (dwnstatus) {
		case DwnStatus.STATUS_SUCCESS:
		case DwnStatus.STATUS_PAUSE:
			mDwnInfo.setmDuring(mDwnInfo.getmDuring() + (int)(SystemClock.uptimeMillis() - beginTime));
			break;

		default:
			break;
		}
		
		synchronized (DwnManager.class) {
			if (null != mCallback) {
				mCallback.onDwnStatusChange(uri, dwnstatus);   					// 
			}
		}
		
		return dwnstatus;
	}
	
	
	
	private void addHeaders(HttpURLConnection connection){
		
		HashMap<String, String> headerParameter = withHeaderParameter();
		if (null != headerParameter) {
			Set<Entry<String, String>> sets = headerParameter.entrySet();
			Iterator<Entry<String, String>> iterator = sets.iterator();
			while (iterator.hasNext()) {
				Entry<String, String> entry  = iterator.next();
				String header = entry.getKey();
				String value = entry.getValue();
				if (TextUtils.isEmpty(header) || TextUtils.isEmpty(value)) {
					
				} else {
					connection.addRequestProperty(header, value);
				}
			}
		}
	}

	@Override
	public HashMap<String, String> withHeaderParameter() {
		return null;
	}
	
	public static class ResThread extends Thread {
		
		public AtomicInteger mResDwnStatus = new AtomicInteger(DwnStatus.STATUS_NONE);
		
		public AtomicInteger mResponseCode = new AtomicInteger(-1);
		
		public int getDwnStatus() {
			return mResDwnStatus.get();
		}
		
		public void setDwnStatus(int status) {
			mResDwnStatus.set(status);
		}
		
		public int getResponseCode() {
			return mResponseCode.get();
		}
		
		public void setResponseCode(int status) {
			mResponseCode.set(status);
		}
		
	}
	
}	
