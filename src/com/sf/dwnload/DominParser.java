package com.sf.dwnload;

import android.net.Uri;
import android.text.TextUtils;

import com.sf.util.ThreadUtil;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DominParser {

	public static HashMap<String, String> mMaps = new HashMap<String, String>();

    public static void readyBackIps (final String host) {

        ThreadUtil.getCachedThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                parser(host);
            }
        });
    }

    /**
     * 将 Uri 替换成 HttpDNS 的 uri
     * @param uri
     * @return
     */
    public static String parseHost(String uri) {

        if (!TextUtils.isEmpty(uri)) {
            Uri myUri = Uri.parse(uri);
            String host = myUri.getAuthority();

            String ip = mMaps.get(host);
            if (!TextUtils.isEmpty(ip)) {
                return  uri.replace(host, ip);
            }
        }

        return null;
    }

    public static void cleaHost(String host){
        if (mMaps != null && null != host) {
            mMaps.remove(host);
        }
    }

	public static String parser(String origin) {
		
		String ret = null;
		//  如果恢复，需要调查一下事项
		// 请求论坛接口 http://172.26.130.153:8899/interface?mod=install_app&data=bbs_%7B%22id%22:%22Mjg0ODcwfGM3MTZhMmE1fDE0Mzc1NjEzMTd8MTIzMnwyNTI0MjU=%22,%22p%22:%22com.lztv.iptv%22%7D&callback=jQuery1910728461008918698_1437561318073&_=1437561318109
		//  当盒子下载软件的时候，ip地址后会跳转，跳转请求头中仍含有HOST（不需要！）
		if (null != origin) {
			ret = mMaps.get(origin);
			if (null == ret) {
				ret = parStringFrom114(origin);
			}
		}
		
		return ret;
	}
	
	
	private static String parStringFrom114(String origin) {
		String ret = null;
		Uri url = null;
		String ipAddress = null;
		String authority = null;
		
		try {
			String url_119 = "http://119.29.29.29/d?dn=%s";
			url = Uri.parse(origin);
			authority = url.getAuthority();
			
			if (null != authority) {

                HttpURLConnection connection = null;
                BufferedReader reader = null;
                StringBuilder result = new StringBuilder();
                try {
                    URL dnsURL = new URL(String.format(url_119, authority));
                    connection = ((HttpURLConnection) dnsURL.openConnection());
                    if (connection.getResponseCode() == 200) {
                        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String line = null;
                        while ((line = reader.readLine()) != null){
                            result.append(line);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    System.gc();
                } finally {
                    if (null != reader) {
                        reader.close();
                    }
                    if (null != connection) {
                        connection.disconnect();
                    }
                }

                Pattern pattern = Pattern.compile("((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))");
                Matcher matcher = pattern.matcher(result.toString());
                if (matcher.find()) {
                    ipAddress = matcher.group();
                }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

        if (null != ipAddress) {
            mMaps.put(authority, ipAddress);
        }

		return ipAddress;
	}
	
	
}
