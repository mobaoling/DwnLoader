package com.sf.dwnload;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import android.text.TextUtils;


public class AbsDownloader implements Downloader{
	
	
	/**
	 * 在非主线程中执行，通知下载结果
	 * @param uri
	 * @param dir
	 */
	@Override
	public int dwnFile(String uri, String dir) {
		URL url = null;
		
		int dwnstatus = -1;
		try {
			url = new URL(uri);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		if (null == uri) {
			dwnstatus = -2;
		} else {
			HttpURLConnection connection = null;
			try {
				connection = (HttpURLConnection)url.openConnection();
			} catch (IOException e) {
				e.printStackTrace();
			} catch ( Exception e) {
				e.printStackTrace();
			}
			
			if (null == connection) {
				dwnstatus = -3;
			} else {
				try {
					connection.setRequestMethod("GET");
				} catch (ProtocolException e) {
					e.printStackTrace();
				}
				addHeaders(connection);  // 添加请求头参数
				
				String header  = connection.getHeaderField(0);
				
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
