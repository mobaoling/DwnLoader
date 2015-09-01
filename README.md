#  DwmManager 说明 ![img](https://raw.githubusercontent.com/jerboy/DwnLoader/master/res/drawable-hdpi/ic_launcher.png)

##  下载类型
+ `APKDwnInfo`     apk 文件
+ `BaseDwnInfo`    普通文件


## 下载文件

```java
	mDwnManager.dwnFile(info, mDwnOption, ShafaDwnHelper.getDwnDirs(getApplicationContext()))
```
  + mDwnOption 下载属性
  + dirs       支持下载目录多个，当前一个空间不够，或者读写权限不够，自动下一个

 ## 暂停下载

 ```java
 	mDwnManager.pause(uri)
 ```

 ## 获取下载信息

 ```java
 	mDwnManager.getDwnInfo(uri)
 ```

 ## 继续下载

 ```java
 mDwnManager.continueDwnFile(dwnIfo, mDwnOption, ShafaDwnHelper.getDwnDirs(getApplicationContext())
 ```

 + dwnInfo   通过 mDwnManager.getDwnInfo 获取到的dwnInfo
 + 其他同  dwnFile


 ## 获取下载进度

 ```java
 	mDwnAsker.regeisterProgress(info.getmUri(), mDwnWatcher);

	/**
	 * 进度监视
	 */
	private IDwnWatcher mDwnWatcher = new IDwnWatcher() {
		
		@Override
		public void onProgressChange(String uri, long current, long totao) {
			onDwnProgressChange(uri, current, totao);
		}
		
		@Override
		public BaseDwnInfo getDwnInfo(String uri) {
			
			try {
				ret = mDwmManager.getDwnInfo(uri);
			} catch (Exception e) {
			}
			return ret;
		}
	};

 ```

 + 需要调用 `mDwnAsker.onResume()` `mDwnAsker.onPause`
 + 切不要跨进程调用 dwmManager.getDwnInfo()

 ## 获取下载状态变化

 ```java
 	/**
	 * 下载结束
	 */
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (DwnManager.ACTION_DWN_STATUS_CHANGE.equals(action)) {
				String uri = intent.getStringExtra(DwnManager.EXTRA_URI);
				int status = intent.getIntExtra(DwnManager.EXTRA_STATUS, DwnStatus.STATUS_NONE);
				onDwnStatusChange(uri, status);
			}
		}
	};
 ```

 ## 下载状态详解

 ```java
 		public static final int STATUS_SUCCESS = 1;           // 下载成功
		
		public static final int STATUS_DOWNLOADING = 2;       // 下载中
		
		public static final int STATUS_PAUSE = 3;              // 下载失败
		
		public static final int STATUS_READY_PAUSE = 12;       // 专用，无需要判断
		
		public static final int STATUS_NONE = 4;               // 初始状态
		
		public static final int STATUS_FAIL = 5;                // 统一所有下载失败原因 ：下载失败
		
		public static final int STATUS_FAIL_URL_ERROR = 6;      // 下载链接错误
		
		public static final int STATUS_FAIL_CONNECT_ERROR = 7;   // 连接出错
		
		public static final int STATUS_FAIL_MKDIR_FAIL = 8;      // 文件创建失败
		
		public static final int STATUS_FAIL_SPACE_NOT_ENO = 9;    // 空间不够
		
		public static final int STATUS_FAIL_READ_FILE = 10;  // 读取文件流失败
		
		public static final int STATUS_FAIL_ERROR_CODE = 11;       // 404 等具体错误
		
		public static final int STATUS_FAIL_MD5_CHECK_FAIL = 13;  // md5 验证失败
		
		public static final int STATUS_FAIL_CONNECT_TIME_OUTL = 14;   // 连接超时
		
		public static final int STATUS_FAIL_READ_TIME_OUTL = 15;      // 读取超时
 ```

 **归纳下载失败**

 ```java
 int status = convert_Status(int status);
 ```

 + 会将所有失败归纳未 `STATUS_FAIL`

