package com.sleepyduck.macdnotification.data;

import com.sleepyduck.macdnotification.data.xml.XMLElement;
import com.sleepyduck.macdnotification.data.xml.XMLParsableAdaptor;

public class Symbol extends XMLParsableAdaptor {
	private static final long serialVersionUID = -2937633173541304552L;

	private String mName = "";
	private String mDisplayName = "";
	private Float mRuleNo1Valuation;
	private StockDataList mData;
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

	public void setDisplayName(String name) {
		mDisplayName = name;
	}

	public void setStockData(StockDataList data) {
		mData = data;
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

	public StockDataList getStockData() {
		return mData;
	}

	public CharSequence getDataText() {
		if (hasStockData()) {
			return String.format("Price %2.2f (%2.2f), MACD %2.2f (%2.2f)",
					mData.get(mData.size()-1).Close,
					mData.get(mData.size()-2).Close,
					mData.get(mData.size()-1).get(StockEnum.MACD_12_26),
					mData.get(mData.size()-2).get(StockEnum.MACD_12_26));
		}
		return "";
	}

	public boolean hasStockData() {
		return mData != null;
	}

	public boolean hasDisplayName() {
		return mDisplayName.length() > 0;
	}

	public boolean hasRuleNo1Valuation() {
		return mRuleNo1Valuation != null;
	}

	public boolean doRetry() {
		return mRetryCounter -- > 0;
	}

	public boolean isRuleNo1SMALessThanValue() {
		return mData.get(mData.size()-1).Close_SMA_10 <= mData.get(mData.size()-1).Close;
	}

	public boolean isRuleNo1HistogramPositive() {
		return mData.get(mData.size()-1).get(StockEnum.MACD_Histogram_8_17_9)
				<= mData.get(mData.size()-1).get(StockEnum.MACD_8_17);
	}

	public boolean isRuleNo1StochasticPositive() {
		return mData.get(mData.size()-1).Stochastic_Signal_14_5_Slow
				<= mData.get(mData.size()-1).Stochastic_14_5_Slow;
	}

	public boolean isRuleNo1StochasticAbove80() {
		return mData.get(mData.size()-1).Stochastic_14_5_Slow >= 80;
	}

	public boolean isRuleNo1StochasticBelow20() {
		return mData.get(mData.size()-1).Stochastic_14_5_Slow <= 20;
	}

	public boolean isValueAboveValuation() {
		return mRuleNo1Valuation != null
				&& mData.get(mData.size()-1).Close > mRuleNo1Valuation;
	}

	public boolean isValueBelowValuation50() {
		return mRuleNo1Valuation != null
				&& mData.get(mData.size()-1).Close < mRuleNo1Valuation/2F;
	}

	public boolean wasRuleNo1SMALessThanValue() {
		return mData.get(mData.size()-2).Close_SMA_10 <= mData.get(mData.size()-2).Close;
	}

	public boolean wasRuleNo1HistogramPositive() {
		return mData.get(mData.size()-2).get(StockEnum.MACD_Histogram_8_17_9)
				<= mData.get(mData.size()-2).get(StockEnum.MACD_8_17);
	}

	public boolean wasRuleNo1StochasticPositive() {
		return mData.get(mData.size()-2).get(StockEnum.Stochastic_Signal_14_5_Slow)
				<= mData.get(mData.size()-2).get(StockEnum.Stochastic_14_5_Slow);
	}
}
