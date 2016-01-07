package com.sf.dwnload;

import android.os.StatFs;
import android.os.SystemClock;
import android.text.TextUtils;

import com.sf.DwnMd5;
import com.sf.dwnload.DwnManager.IDwnCallback;
import com.sf.dwnload.dwninfo.BaseDwnInfo;
import com.sf.util.ThreadUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class AbsDownloader implements Dwnloader{
	
	private DwnOption mDwnOption;
	
	public static final int MODE_NEW = 0;
	
	public static final int MODE_CONTINUE = 1;
	
	private int mMode = MODE_NEW;
	
	private BaseDwnInfo mDwnInfo;
	
	private String[] mDirs = null;
	
	private IDwnCallback mCallback;
	
	private HttpURLConnection mConnection;

    private InputStream mIs;
	
	private Thread mThread;

    private Future<Integer> mFuture;

    FileOutputStream mFos = null;
	
	private AtomicBoolean mManualDisconnect = new AtomicBoolean(false);

	public static final String DIR_ERROR_MKFAIL = "dir_error_mkfailed";

	public static final String DIR_ERROR_NOT_ENOUGH = "dir_error_not_enough";

	public AbsDownloader(BaseDwnInfo dwnInfo, int mode, IDwnCallback callback, DwnOption option,  String...  dirs) {
		mMode = mode;
		mDwnInfo = dwnInfo;
		this.mDirs = dirs;
		this.mCallback = callback;
		this.mDwnOption = option;
	}

	public void disconnect() {
        mManualDisconnect.set(true);

        if (null != mThread) {
            try {
                mThread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (null != mFuture) {
            try {
                mFuture.cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
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

		public String mSubfix;

        public String mSaveName;

        public boolean mStopOthers;

        /**
         * 自定义请求头
         */
        public HashMap<String, String> mRequestHeaders;
	}

	@Override
	public int dwnFile() {
		long beginTime = SystemClock.uptimeMillis();
		
		String uri = mDwnInfo.getmUri();
        String host = "";
        try {
            host = new URL(uri).getAuthority();
        } catch (Exception e) {
            e.printStackTrace();
        }

        String dwnUri = DominParser.parseHost(uri);
        boolean useHTTPDNS = false;
        if (null != dwnUri) {
            useHTTPDNS = true;
        } else {
            dwnUri = mDwnInfo.getmUri();
            useHTTPDNS = false;
        }
		URL url = null;
		int dwnstatus = DwnStatus.STATUS_NONE;
		try {
			url = new URL(dwnUri);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		if (null == url) {
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
//					mConnection.setReadTimeout(5000);
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
                    if (useHTTPDNS) {
                        mConnection.addRequestProperty("HOST", host);
                    }
				} catch (ProtocolException e) {
					e.printStackTrace();
				}

                long point = 0l;
                // 继续下载
				if (mMode == MODE_CONTINUE) {
                    point = new File(mDwnInfo.getmSavePath()).length();
					mConnection.addRequestProperty("Range", "bytes=" + point + "-");
				}
                final long insertPoint = point;
				
				addHeaders(mConnection);  // 添加请求头参数
				int responseCode = -1;
                final boolean finalhasUseHTTPDNS = useHTTPDNS;
                final String finalhost  = host;
				ResThread t = new ResThread(){
					
					public void run() {

                        synchronized (DwnManager.class) {
                            mThread = Thread.currentThread();
                        }

						try {
							int res = mConnection.getResponseCode();
							setResponseCode(res);
						} catch (SocketTimeoutException e) {

                            if (finalhasUseHTTPDNS) {
                                DominParser.cleaHost(finalhost);
                            }

							setDwnStatus(DwnStatus.STATUS_FAIL_CONNECT_TIME_OUTL);
							e.printStackTrace();
						} catch (Throwable e) {
							setDwnStatus(DwnStatus.STATUS_FAIL_CONNECT_ERROR);
						}
					};
				};
				t.start();
				try {
					t.join(30 * 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				dwnstatus = t.getDwnStatus();
				responseCode = t.getResponseCode();
				
				// 暂停
                if (mManualDisconnect.get() || mDwnInfo.getmDwnStatus() == DwnStatus.STATUS_READY_PAUSE) {
                    dwnstatus = DwnStatus.STATUS_PAUSE;
                }

				if (dwnstatus == DwnStatus.STATUS_PAUSE) {
					
				} else if (dwnstatus == DwnStatus.STATUS_FAIL_CONNECT_TIME_OUTL) {
					
				} else if (responseCode >= 200 && responseCode < 300) {

					long size = mConnection.getContentLength();
					String dirResult = createDirFile(size, uri, mDirs);

                    if (mMode == MODE_NEW) {
                        mDwnInfo.setmTotal_Size(size); // 设置总大小
                    }

					if (DIR_ERROR_MKFAIL.equals(dirResult)) {
						dwnstatus = DwnStatus.STATUS_FAIL_MKDIR_FAIL;		// 文件创建失败
					} else if (DIR_ERROR_NOT_ENOUGH.equals(dirResult)) {
						dwnstatus = DwnStatus.STATUS_FAIL_SPACE_NOT_ENO;
					} else  {
						try {
                            mFos = new FileOutputStream(new File(dirResult), true);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
                            e.printStackTrace();
                        }
                        mDwnInfo.setmSavePath(dirResult);
                       mFuture = ThreadUtil.getCachedThreadPool().submit(new Callable<Integer>() {
                           @Override
                           public Integer call() throws Exception {
                               int ret = DwnStatus.STATUS_NONE;        // 初始状态
                               try {
                                   int count = 0;
                                   int current = 0;
                                   mIs = mConnection.getInputStream();
                                   if (mMode == MODE_CONTINUE) {
                                       current = (int) insertPoint;
                                   }

                                   byte[] tmp = new byte[1024 * 30];
                                   while ((count = mIs.read(tmp)) > 0) {
                                       if (mManualDisconnect.get()) {
                                           break;
                                       }

                                       mFos.write(tmp, 0, count);
                                       if ((mDwnInfo.getmDwnStatus() == DwnStatus.STATUS_READY_PAUSE)
                                               || (mDwnInfo.getmDwnStatus() == DwnStatus.STATUS_PAUSE)
                                               || (null != mFuture && mFuture.isCancelled()) || mManualDisconnect.get()) {
                                           mManualDisconnect.set(true);// 手动暂定
                                           break;
                                       } else {
                                           current += count;
                                           mDwnInfo.setmCurrent_Size(current);
                                       }
                                   }
                                   if (!mManualDisconnect.get()){
                                       ret = DwnStatus.STATUS_SUCCESS;
                                   }

                               } catch (SocketTimeoutException e) {
                                   ret = DwnStatus.STATUS_FAIL_READ_TIME_OUTL;
                                   e.printStackTrace();
                               } catch (final Exception e) {
                                   ret = DwnStatus.STATUS_FAIL_READ_FILE;
                                   e.printStackTrace();
                               }

                               return ret;
                           }
                       });

                        int ret = dwnstatus;
                        try {
                            ret = mFuture.get();   // 获取读取结果
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (mDwnInfo.getmDwnStatus() == DwnStatus.STATUS_READY_PAUSE || mManualDisconnect.get()) {
                            dwnstatus = DwnStatus.STATUS_PAUSE;
                        }
                        // 关闭文件
						if (null != mFos) {
							try {
                                mFos.close();
							} catch (Exception e) {
							}
						}

                        if (dwnstatus == DwnStatus.STATUS_PAUSE) {

                        } else {
                            dwnstatus = ret;
                        }
					}

				} else if (responseCode != -1) {
					dwnstatus =  (responseCode << 16 ) | DwnStatus.STATUS_FAIL_ERROR_CODE ;  											// 请求错误
				} else  {
					dwnstatus = DwnStatus.STATUS_FAIL_CONNECT_ERROR;
				}
			}
		}

        ThreadUtil.getCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (null != mIs) {
                        mIs.close();                        // 耗时操作
                    }

                    if (null != mConnection) {
                        try {
                            mConnection.disconnect();       // 耗时操作
                        } catch (Exception e) {
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    ;
                }
            }
        });

        if (dwnstatus == DwnStatus.STATUS_SUCCESS && !TextUtils.isEmpty(mDwnInfo.getmMd5())) {
			// check md5
            long t1 = System.currentTimeMillis();

            String md5 = DwnMd5.getMD5(new File(mDwnInfo.getmSavePath()));
            if(!TextUtils.isEmpty(md5) && !md5.equals(mDwnInfo.getmMd5())) {
                synchronized (DwnManager.class) {
                    dwnstatus = DwnStatus.STATUS_FAIL_MD5_CHECK_FAIL;
                }
            }
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
        if (null != mCallback) {
            mCallback.onDwnStatusChange(uri, dwnstatus);   					//
        }
		return dwnstatus;
	}

	public String createDirFile(long size, String uri, String... dirs) {

		String ret = DIR_ERROR_MKFAIL;     // 默认创建失败

		String dir = null;
		boolean last = false;
		for (int i = 0; i < dirs.length ; i++) {

			dir = dirs[i];
			if (i == dirs.length - 1) {
				last = true;
			}

			File dir_File = new File(dir);
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
				if (dir_File.exists() && dir_File.isDirectory()) {

					int firstIndex = uri.lastIndexOf("/") + 1;
                    String path = "";
                    if (null != mDwnOption && !TextUtils.isEmpty(mDwnOption.mSaveName)) {
                        path = mDwnOption.mSaveName;
                    } else {
                        path = DwnMd5.getMD5(uri.substring(firstIndex));
                        if (null != mDwnOption) {
                            path += mDwnOption.mSubfix;
                        }
                    }

					finalPath = new File(dir_File, path);
					if (finalPath.exists()) {
						finalPath.delete();
					}
					boolean succ = false;
					try {
						succ = finalPath.createNewFile();
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (succ && finalPath.exists() && finalPath.isFile() && finalPath.canWrite()) {
						dstExist = true;
					}
				}
			}

			if (dstExist) {
			StatFs statFs = new StatFs(dir);
			if (size > (long)statFs.getAvailableBlocks() * (long)statFs.getBlockSize()) {
				if (last) {
					ret = DIR_ERROR_NOT_ENOUGH;
				}
			} else {
				ret = finalPath.getAbsolutePath();
				break;
			}

		} else {
			if (last) {
				ret = DIR_ERROR_MKFAIL;
			}
		}
	}

		return ret;
	};

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

        if (null != mDwnOption) {
            return mDwnOption.mRequestHeaders;
        }

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
