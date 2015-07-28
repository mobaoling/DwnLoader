package com.sf.dwnload;

import java.util.HashMap;

public interface Downloader {
	
	public HashMap<String, String> withHeaderParameter();
	
	public int dwnFile(String uri, String dir);
	
}
