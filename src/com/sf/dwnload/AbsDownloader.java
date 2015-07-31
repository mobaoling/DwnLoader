package com.sf.dwnload;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import android.os.StatFs;
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
	
	public AbsDownloader(BaseDwnInfo dwnInfo, int mode, String dir, IDwnCallback callback) {
		mMode = mode;
		mDwnInfo = dwnInfo;
		this.mDir = dir;
		this.mCallback = callback;
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
		
		String uri = mDwnInfo.getmUri();
		
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
			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection)url.openConnection();
				if (null != mDwnOption) {
					connection.setConnectTimeout(mDwnOption.mConnOutTime);
					connection.setReadTimeout(mDwnOption.mReadOutTime);
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch ( Exception e) {
				e.printStackTrace();
			}
			
			if (null == connection) {
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
					connection.setRequestMethod("GET");
					connection.addRequestProperty("Accept-Encoding", "identity");
				} catch (ProtocolException e) {
					e.printStackTrace();
				}
				
				// 继续下载
				if (mMode == MODE_CONTINUE) {
					connection.addRequestProperty("Range", "bytes=" + mDwnInfo.getmCurrent_Size() + "-");
				}
				
				addHeaders(connection);  // 添加请求头参数
				
				int responseCode = -1;
				try {
					responseCode = connection.getResponseCode();
				} catch (IOException e) {
					try {
						responseCode = connection.getResponseCode();
					} catch (Exception e2) {
					}
				}
				
				Log.d("caojianbo", "response code " + responseCode);
				
				if (responseCode >= 200 && responseCode < 300) {
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
							long size = connection.getContentLength();
							
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
									is = connection.getInputStream();
									
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
								} catch (Exception e) {
								}
								
								if (connectSuccess) {
									if (mDwnInfo.getmDwnStatus() == DwnStatus.STATUS_READY_PAUSE) {
										dwnstatus = DwnStatus.STATUS_PAUSE;									// 暂停
									} else {
										dwnstatus  = DwnStatus.STATUS_SUCCESS;								// 连接成功
									}
								} else {
									dwnstatus = DwnStatus.STATUS_FAIL_READ_FILE;								// 连接失败
								}
								
								// 关闭文件
								if (null != raf) {
									try {
										raf.close();
									} catch (Exception e) {
									}
								}
							}
						}
						
					} catch (Exception e) {
					}
					
				} else {
					dwnstatus =  (responseCode << 16 ) & DwnStatus.STATUS_FAIL_ERROR_CODE ;  											// 请求错误
				}
				
				if (null != connection) {
					try {
						connection.disconnect();
					} catch (Exception e) {
					}
				}
			}
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
	
}	
