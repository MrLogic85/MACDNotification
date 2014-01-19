package com.sleepyduck.macdnotification.data;

import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import com.sleepyduck.macdnotification.StartupBroadcastReceiver;
import com.sleepyduck.macdnotification.data.xml.XMLElement;
import com.sleepyduck.macdnotification.data.xml.XMLParsableAdaptor;

public class Symbol extends XMLParsableAdaptor {
	private static final long serialVersionUID = -2937633173541304552L;

	private String mName = "";
	private String mDisplayName = "";
	private Float mRuleNo1Valuation;
	private Float mValue = -99999F;
	private Float mValueOld = -99999F;
	private Float mMACD = -99999F;
	private Float mMACDOld = -99999F;
	private float mRuleNo1Histogram = -99999F;
	private float mRuleNo1HistogramOld = -99999F;
	private float mRuleNo1Stochastic = -99999F;
	private float mRuleNo1StochasticOld = -99999F;
	private float mRuleNo1SMA = -99999F;
	private float mRuleNo1SMAOld = -99999F;
	private long mDataTime = 0L;
	private int mRetryCounter = 3;

	public Symbol(String name, Float ruleNo1Valuation) {
		mName = name;
		mRuleNo1Valuation = ruleNo1Valuation;
	}

	public Symbol(XMLElement element) {
		mName = element.getAttribute("name", "");
		mDisplayName = element.getAttribute("displayName", "");
		if (element.getAttribute("ruleNo1Valuation") != null) {
			mRuleNo1Valuation = Float.valueOf(element.getAttribute("ruleNo1Valuation", ""));
		}
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
		element.addAttribute("displayName", mDisplayName);
		if (mRuleNo1Valuation != null) {
			element.addAttribute("ruleNo1Valuation", String.valueOf(mRuleNo1Valuation));
		}
	}

	public void populateList(List<Symbol> list) {
		list.add(this);
	}

	public List<Symbol> asList() {
		List<Symbol> list = new LinkedList<Symbol>();
		populateList(list);
		return list;
	}

	public void setDisplayName(String name) {
		mDisplayName = name;
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

	public void setDataTime(long dataTime) {
		mDataTime = dataTime;
	}

	public void setRuleNo1Histogram(float hist) {
		mRuleNo1Histogram = hist;
	}

	public void setRuleNo1HistogramOld(float hist) {
		mRuleNo1HistogramOld = hist;
	}

	public void setRuleNo1Stochastic(float stoch) {
		mRuleNo1Stochastic = stoch;
	}

	public void setRuleNo1StochasticOld(float stoch) {
		mRuleNo1StochasticOld = stoch;
	}

	public void setRuleNo1SMA(float sma) {
		mRuleNo1SMA = sma;
	}

	public void setRuleNo1SMAOld(float smaOld) {
		mRuleNo1SMAOld = smaOld;
	}

	public String getName() {
		return mName;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public Float getRuleNo1Valuation() {
		return mRuleNo1Valuation;
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

	public CharSequence getDataText() {
		if (hasValidData()) {
			String text = String.format("Price %2.2f (%2.2f), MACD %2.2f (%2.2f)",
					mValue,
					mValueOld,
					mMACD,
					mMACDOld);
			return text;
		}
		return "";
	}

	public boolean isRuleNo1SMALessThanValue() {
		return mValue > mRuleNo1SMA;
	}

	public boolean isRuleNo1HistogramPositive() {
		return mRuleNo1Histogram > 0;
	}

	public boolean isRuleNo1StochasticPositive() {
		return mRuleNo1Stochastic > 0;
	}

	public boolean isNewDataDay(long newTime) {
		if (mDataTime <= 0)
			return true;

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(mDataTime);
		int dataYear = cal.get(Calendar.YEAR);
		int dataDay = cal.get(Calendar.DAY_OF_YEAR);
		int dataHour = cal.get(Calendar.HOUR_OF_DAY);

		cal.setTimeInMillis(newTime);
		int newYear = cal.get(Calendar.YEAR);
		int newDay = cal.get(Calendar.DAY_OF_YEAR);
		int newHour = cal.get(Calendar.HOUR_OF_DAY);

		if (dataYear == newYear) {
			if (newDay == dataDay) {
				return dataHour < StartupBroadcastReceiver.ALARM_HOUR && newHour >= StartupBroadcastReceiver.ALARM_HOUR;
			} else {
				return newDay > dataDay;
			}
		} else {
			return newYear > dataYear;
		}
	}

	public boolean hasValidData() {
		return mMACD > -99999f;
	}

	public boolean hasDisplayName() {
		return mDisplayName.length() > 0;
	}

	public boolean doRetry() {
		return mRetryCounter -- > 0;
	}
}
