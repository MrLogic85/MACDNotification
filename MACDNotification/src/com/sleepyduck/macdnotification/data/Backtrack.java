package com.sleepyduck.macdnotification.data;

import android.util.Log;


public class Backtrack {

	public Report run(Symbol symbol,
			float initialCash,
			boolean startBuy,
			float minBrokerage,
			float minBrokeragePercent,
			float spread) {
		StockDataList dataList = symbol.getStockData();

		// Rule #1
		float cashRuleNo1 = initialCash;
		int stocks = 0;
		int tradeCountRuleNo1 = 0;
		if (startBuy) {
			Log.d("", "Buy " + dataList.get(0).Close);
			stocks = (int) (initialCash / dataList.get(0).Close);
			cashRuleNo1 -= stocks * dataList.get(0).Close;
		}
		for (int i = 0; i < dataList.size(); i++) {
			if (stocks > 0) {
				if (symbol.isRuleNo1Sell(i)) {
					float sellVal = dataList.get(i).Close * (1f-spread/100f);
					Log.d("", "Sell " + sellVal);
					sellVal = stocks * sellVal;
					cashRuleNo1 += sellVal - Math.max(sellVal*minBrokeragePercent/100f, minBrokerage);
					stocks = 0;
					tradeCountRuleNo1++;
				}
			} else {
				if (symbol.isRuleNo1Buy(i)) {
					float buyVal = dataList.get(i).Close * (1f + spread/100f);
					Log.d("", "Buy " + buyVal);
					stocks = (int) (cashRuleNo1 / buyVal);
					buyVal = stocks * buyVal;
					cashRuleNo1 -= buyVal + Math.max(buyVal*minBrokeragePercent/100f, minBrokerage);
					tradeCountRuleNo1++;
				}
			}
		}
		if (stocks > 0) {
			Log.d("", "Sell " + dataList.get(dataList.size()-1).Close);
			cashRuleNo1 += stocks * dataList.get(dataList.size()-1).Close;
		}

		// MACD
		float cashMACD = initialCash;
		stocks = 0;
		int tradeCountMACD = 0;
		if (startBuy) {
			Log.d("", "MACD Buy " + dataList.get(0).Close);
			stocks = (int) (initialCash / dataList.get(0).Close);
			cashMACD -= stocks * dataList.get(0).Close;
		}
		for (int i = 0; i < dataList.size(); i++) {
			if (stocks > 0) {
				if (dataList.get(i).get(StockEnum.MACD_12_26) < 0) {
					float sellVal = dataList.get(i).Close * (1f-spread/100f);
					Log.d("", "MACD Sell " + sellVal);
					sellVal = stocks * sellVal;
					cashMACD += sellVal - Math.max(sellVal*minBrokeragePercent/100f, minBrokerage);
					stocks = 0;
					tradeCountMACD++;
				}
			} else {
				if (dataList.get(i).get(StockEnum.MACD_12_26) > 0) {
					float buyVal = dataList.get(i).Close * (1f + spread/100f);
					Log.d("", "MACD Buy " + buyVal);
					stocks = (int) (cashRuleNo1 / buyVal);
					buyVal = stocks * buyVal;
					cashMACD -= buyVal + Math.max(buyVal*minBrokeragePercent/100f, minBrokerage);
					tradeCountMACD++;
				}
			}
		}
		if (stocks > 0) {
			Log.d("", "MACD Sell " + dataList.get(dataList.size()-1).Close);
			cashMACD += stocks * dataList.get(dataList.size()-1).Close;
		}

		// Buy & Hold
		int initialStocks = (int) (initialCash / dataList.get(0).Close);
		float cashBuyAndHold = initialCash
				- initialStocks * dataList.get(0).Close
				+ initialStocks * dataList.get(dataList.size()-1).Close;

		return new Report(initialCash, cashBuyAndHold, cashRuleNo1, tradeCountRuleNo1, cashMACD, tradeCountMACD, dataList.size());
	}

	public final class Report {
		public final float InitialValue;
		public final float ValueBuyAndHold;
		public final float ValueRuleNo1;
		public final int NumTradesRuleNo1;
		public final float ValueMACD;
		public final int NumTradesMACD;
		public final int Days;

		public Report(float initialValue, float valueBuyAndHold, float valueRuleNo1, int numTradesRuleNo1, float valueRuleMACD, int numTradesMACD, int days) {
			InitialValue = initialValue;
			ValueBuyAndHold = valueBuyAndHold;
			ValueRuleNo1 = valueRuleNo1;
			NumTradesRuleNo1 = numTradesRuleNo1;
			ValueMACD = valueRuleMACD;
			NumTradesMACD = numTradesMACD;
			Days = days;
		}

		@Override
		public String toString() {
			return "Initial value was " + InitialValue + ". Buy and hold resulted in a total value of "
					+ ValueBuyAndHold + ", Rule #1 resulted in " + ValueRuleNo1 + " with a total number of "
					+ NumTradesRuleNo1 + " trades, and MACD resulted in " + ValueMACD + " with a total number of "
					+ NumTradesMACD + " trades. This test was done over a total of " + Days + " trading days.";
		}
	}
}
