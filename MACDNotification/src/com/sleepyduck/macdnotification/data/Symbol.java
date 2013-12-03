package com.sleepyduck.macdnotification.data;

import android.os.Parcel;
import android.os.Parcelable;

public class Symbol implements Parcelable {
	public static final Parcelable.Creator<Symbol> CREATOR = new Parcelable.Creator<Symbol>() {
		@Override
		public Symbol createFromParcel(Parcel in) {
			return new Symbol(in);
		}

		@Override
		public Symbol[] newArray(int size) {
			return new Symbol[size];
		}
	};

	private String mName = "";
	private Float mValue = -1f;
	private Float mValueOld = -1f;
	private Float mMACD = -1f;
	private Float mMACDOld = -1f;

	public Symbol(Parcel in) {
		mName = in.readString();
		mValue = in.readFloat();
		mValueOld = in.readFloat();
		mMACD = in.readFloat();
		mMACDOld = in.readFloat();
	}

	public Symbol(String name) {
		mName = name;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mName);
		dest.writeFloat(mValue);
		dest.writeFloat(mValueOld);
		dest.writeFloat(mMACD);
		dest.writeFloat(mMACDOld);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof String) {
			return mName.equals(other);
		}
		return super.equals(other);
	}

	@Override
	public String toString() {
		return mName;
	}

	public String getName() {
		return mName;
	}

	public void setValue(Float val) {
		mValue = val;
	}

	public void setValueOld(Float val) {
		mValueOld = val;
	}

	public void setMACD(Float val) {
		mMACD = val;
	}

	public void setMACDOld(Float val) {
		mMACDOld = val;
	}

	public float getValue() {
		return mValue;
	}

	public float getValueOld() {
		return mValueOld;
	}

	public float getMACD() {
		return mMACD;
	}

	public float getMACDOld() {
		return mMACDOld;
	}
}