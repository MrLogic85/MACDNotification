package com.sleepyduck.macdnotification.data;

import com.sleepyduck.macdnotification.data.xml.XMLElement;
import com.sleepyduck.macdnotification.data.xml.XMLParsableAdaptor;

public class Symbol extends XMLParsableAdaptor {
	private static final long serialVersionUID = -2937633173541304552L;

	private String mName = "";
	private Float mValue = -1f;
	private Float mValueOld = -1f;
	private Float mMACD = -1f;
	private Float mMACDOld = -1f;

	public Symbol(String name) {
		mName = name;
	}

	public Symbol(XMLElement element) {
		mName = element.getAttribute("name", "");
		mValue = Float.valueOf(element.getAttribute("value", "-1"));
		mValueOld = Float.valueOf(element.getAttribute("valueOld", "-1"));
		mMACD = Float.valueOf(element.getAttribute("macd", "-1"));
		mMACDOld = Float.valueOf(element.getAttribute("macdOld", "-1"));
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

	@Override
	public void putAttributes(XMLElement element) {
		super.putAttributes(element);
		element.addAttribute("name", mName);
		element.addAttribute("value", String.valueOf(mValue));
		element.addAttribute("valueOld", String.valueOf(mValueOld));
		element.addAttribute("macd", String.valueOf(mMACD));
		element.addAttribute("macdOld", String.valueOf(mMACDOld));
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
