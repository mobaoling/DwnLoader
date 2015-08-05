package com.sf.dwnload.dwninfo;

import android.os.Parcel;
import android.os.Parcelable;

import com.sf.dwnload.Dwnloader.DwnStatus;

public class BaseDwnInfo implements Parcelable {
	
	public BaseDwnInfo(String uri) {
		this.mUri = uri;
		this.mDwnStatus = DwnStatus.STATUS_NONE;
	}
	
	protected int mDwnStatus;
	
	/**
	 * 下载地址
	 */
	protected String mUri;
	
	/**
	 * 总大小
	 */
	protected long mTotal_Size;
	
	/**
	 * 当前大小
	 */
	protected long mCurrent_Size;
	
	/**
	 * md5
	 */
	protected String mMd5;
	
	protected String mSavePath;
	
	protected int mDuring;

	public int getmDwnStatus() {
		return mDwnStatus;
	}

	public void setmDwnStatus(int mDwnStatus) {
		this.mDwnStatus = mDwnStatus;
	}

	public String getmUri() {
		return mUri;
	}

	public void setmUri(String mUri) {
		this.mUri = mUri;
	}

	public long getmTotal_Size() {
		return mTotal_Size;
	}

	public void setmTotal_Size(long mTotal_Size) {
		this.mTotal_Size = mTotal_Size;
	}

	public long getmCurrent_Size() {
		return mCurrent_Size;
	}

	public void setmCurrent_Size(long mCurrent_Size) {
		this.mCurrent_Size = mCurrent_Size;
	}

	public String getmMd5() {
		return mMd5;
	}

	public void setmMd5(String mMd5) {
		this.mMd5 = mMd5;
	}

	public String getmSavePath() {
		return mSavePath;
	}

	public void setmSavePath(String mSavePath) {
		this.mSavePath = mSavePath;
	}

	public int getmDuring() {
		return mDuring;
	}

	public void setmDuring(int mDuring) {
		this.mDuring = mDuring;
	}

	@Override
	public int describeContents() {
		return 0;
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
	}
	
	public static final Creator<BaseDwnInfo> CREATOR = new Creator<BaseDwnInfo>() {

		@Override
		public BaseDwnInfo createFromParcel(Parcel source) {
			
			String uri = source.readString();
			BaseDwnInfo ret = new BaseDwnInfo(uri);
			ret.mCurrent_Size = source.readLong();
			ret.mTotal_Size = source.readLong();
			ret.mDwnStatus = source.readInt();
			ret.mMd5 = source.readString();
			ret.mDuring = source.readInt();
			ret.mSavePath = source.readString();
			
			return ret;
		}

		@Override
		public BaseDwnInfo[] newArray(int size) {
			return new BaseDwnInfo[size];
		}
	};
	
}
