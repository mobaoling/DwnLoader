package com.sf.dwnload;

import java.util.HashMap;
import java.util.concurrent.Callable;

public interface Dwnloader extends Callable<Integer> {
	
	public HashMap<String, String> withHeaderParameter();
	
	public int dwnFile();
	
	public static class DwnStatus {
		
		public static final int STATUS_SUCCESS = 1;
		
		public static final int STATUS_DOWNLOADING = 2;
		
		public static final int STATUS_PAUSE = 3;
		
		public static final int STATUS_READY_PAUSE = 12;
		
		public static final int STATUS_NONE = 4;
		
		public static final int STATUS_FAIL = 5;
		
		public static final int STATUS_FAIL_URL_ERROR = 6;
		
		public static final int STATUS_FAIL_CONNECT_ERROR = 7;
		
		public static final int STATUS_FAIL_MKDIR_FAIL = 8;
		
		public static final int STATUS_FAIL_SPACE_NOT_ENO = 9;
		
		public static final int STATUS_FAIL_READ_FILE = 10;  // 读取文件流失败
		
		public static final int STATUS_FAIL_ERROR_CODE = 11;
		
		public static int convert_Status(int status) {
			
			int convert = status & 0x000000ff;
			switch (convert) {
			case STATUS_SUCCESS:
				return STATUS_SUCCESS;
			case STATUS_DOWNLOADING:
				return STATUS_DOWNLOADING;
			case STATUS_PAUSE:
				return STATUS_PAUSE;
			case STATUS_NONE:
				return STATUS_NONE;
			case STATUS_FAIL_URL_ERROR:
			case STATUS_FAIL_CONNECT_ERROR:
			case STATUS_FAIL_MKDIR_FAIL:
			case STATUS_FAIL_SPACE_NOT_ENO:
			case STATUS_FAIL_READ_FILE:
			case STATUS_FAIL_ERROR_CODE:
			case STATUS_FAIL:
				return STATUS_FAIL;
			default:
				break;
			}
			return status;
		}
	}
}
