package com.sf.dwnload.dwninfo;

import android.os.Parcel;


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
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mUri);
		dest.writeLong(mCurrent_Size);
		dest.writeLong(mTotal_Size);
		dest.writeInt(mDwnStatus);
		dest.writeString(mMd5);
		dest.writeInt(mDuring);
		dest.writeString(mSavePath);
		dest.writeString(mPkgName);
		dest.writeInt(mVsCode);
		dest.writeString(mVsName);
		dest.writeString(mAppName);
		dest.writeString(mIconUri);
	}
	
	public static final Creator<APKDwnInfo> CREATOR = new Creator<APKDwnInfo>() {

		@Override
		public APKDwnInfo createFromParcel(Parcel source) {
			String uri = source.readString();
			long current = source.readLong();
			long total = source.readLong();
			int status = source.readInt();
			String md5 = source.readString();
			int during = source.readInt();
			String savePath = source.readString();
			String pkgname = source.readString();
			int vsCode = source.readInt();
			String vsName = source.readString();
			String appName = source.readString();
			String iconUri = source.readString();
			
			APKDwnInfo dwnInfo = new APKDwnInfo(uri, pkgname, vsName, vsCode, iconUri, appName);
			dwnInfo.mCurrent_Size = current;
			dwnInfo.mTotal_Size = total;
			dwnInfo.mDwnStatus = status;
			dwnInfo.mMd5 = md5;
			dwnInfo.mDuring = during;
			dwnInfo.mSavePath = savePath;
			
			return dwnInfo;
		}

		@Override
		public APKDwnInfo[] newArray(int size) {
			return new APKDwnInfo[size];
		}
	};
	
	
	
}
