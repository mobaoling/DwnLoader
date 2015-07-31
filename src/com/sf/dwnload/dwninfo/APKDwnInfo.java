package com.sf.dwnload.dwninfo;


public class APKDwnInfo extends BaseDwnInfo {
	
	public APKDwnInfo(String uri, String pkgname, String vsname, int vsCode, String iconurl, String appName){
		super(uri);
		this.mPkgName = pkgname;
		this.mAppName = appName;
		this.mIconUri = iconurl;
		this.mVsName = vsname;
		this.mVsCode = vsCode;
	}
	
	private String mPkgName;
	
	private String mAppName;
	
	private String mIconUri;
	
	private String mVsName; // 版本名称
	
	private int mVsCode; // 版本号

	public String getmPkgName() {
		return mPkgName;
	}

	public void setmPkgName(String mPkgName) {
		this.mPkgName = mPkgName;
	}

	public String getmAppName() {
		return mAppName;
	}

	public void setmAppName(String mAppName) {
		this.mAppName = mAppName;
	}

	public String getmIconUri() {
		return mIconUri;
	}

	public void setmIconUri(String mIconUri) {
		this.mIconUri = mIconUri;
	}

	public String getmVsName() {
		return mVsName;
	}

	public void setmVsName(String mVsName) {
		this.mVsName = mVsName;
	}

	public int getmVsCode() {
		return mVsCode;
	}

	public void setmVsCode(int mVsCode) {
		this.mVsCode = mVsCode;
	}
	
	
	
	
}
