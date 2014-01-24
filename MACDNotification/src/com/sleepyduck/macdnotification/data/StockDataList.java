package com.sleepyduck.macdnotification.data;

import java.util.ArrayList;

/**
 * @author Fredrik Metcalf
 */
public class StockDataList extends ArrayList<StockData> {
	public void calculateIndicators() {
		for (int i = 0; i < size(); i++) {
			// MACD
			calcMACD_12_26(i);
			calcMACD_12_26(i);

			// MACD Rule #1
			calcMACD_8_17(i);
			calcMACD_8_17(i);
			calcMACD_Signal_8_17_9(i);
			calcMACD_Signal_8_17_9(i);

			// Stochastic
			calcStochastic_14_5_Slow(i);
			calcStochastic_14_5_Slow(i);
			calcStochastic_Signal_14_5_Slow(i);
			calcStochastic_Signal_14_5_Slow(i);

			// Moving Average
			calcClose_SMA_10(i);
			calcClose_SMA_10(i);
		}
	}

	private void calcClose_EMA_8(int i) {
		if (i > 0) {
			get(i).Close_EMA_8 = calcEMA(get(i-1).Close_EMA_8, get(i).Close, 8);
		} else {
			get(i).Close_EMA_8 = get(i).Close;
		}
	}

	private void calcClose_EMA_12(int i) {
		if (i > 0) {
			get(i).Close_EMA_12 = calcEMA(get(i-1).Close_EMA_12, get(i).Close, 12);
		} else {
			get(i).Close_EMA_12 = get(i).Close;
		}
	}

	private void calcClose_EMA_17(int i) {
		if (i > 0) {
			get(i).Close_EMA_17 = calcEMA(get(i-1).Close_EMA_17, get(i).Close, 17);
		} else {
			get(i).Close_EMA_17 = get(i).Close;
		}
	}

	private void calcClose_EMA_26(int i) {
		if (i > 0) {
			get(i).Close_EMA_26 = calcEMA(get(i-1).Close_EMA_26, get(i).Close, 26);
		} else {
			get(i).Close_EMA_26 = get(i).Close;
		}
	}

	private void calcClose_SMA_10(int i) {
		get(i).Close_SMA_10 = calcSMA(i, StockEnum.Close, 10);
	}

	private void calcMACD_8_17(int i) {
		calcClose_EMA_8(i);
		calcClose_EMA_17(i);
	}

	private void calcMACD_12_26(int i) {
		calcClose_EMA_12(i);
		calcClose_EMA_26(i);
	}

	private void calcMACD_Signal_8_17_9(int i) {
		if (i > 0) {
			get(i).MACD_Signal_8_17_9 = calcEMA(get(i-1).MACD_Signal_8_17_9, get(i).get(StockEnum.MACD_8_17), 9);
		} else {
			get(i).MACD_Signal_8_17_9 = get(i).get(StockEnum.MACD_8_17);
		}
	}

	private void calcHigh_14(int i) {
		if (i > 0) {
			get(i).High_14 = calcHighest(i, 14);
		} else {
			get(i).High_14 = get(i).High;
		}
	}

	private void calcLow_14(int i) {
		if (i > 0) {
			get(i).Low_14 = calcLowest(i, 14);
		} else {
			get(i).Low_14 = get(i).Low;
		}
	}

	private void calcStochastic_14_5(int i) {
		calcHigh_14(i);
		calcLow_14(i);
		get(i).Stochastic_14_5 =
				(get(i).Close - get(i).Low_14) / (get(i).High_14 - get(i).Low_14) * 100.0F;
	}

	private void calcStochastic_14_5_Slow(int i) {
		calcStochastic_14_5(i);
		get(i).Stochastic_14_5_Slow = calcSMA(i, StockEnum.Stochastic_14_5, 5);
	}

	private void calcStochastic_Signal_14_5_Slow(int i) {
		get(i).Stochastic_Signal_14_5_Slow = calcSMA(i, StockEnum.Stochastic_14_5_Slow, 5);
	}

	private static float calcEMA(float prev, float curr, final int days) {
		final float multiplier = 2.0f / (days + 1);
		return curr * multiplier + prev * (1.0f - multiplier);
	}

	private float calcSMA(int i, StockEnum e, int days) {
		float res = 0;
		int countDays = 0;
		StockData data;
		while (i >= 0 && countDays < days) {
			data = get(i--);
			res += data.get(e);
			countDays++;
		}
		return res / countDays;
	}

	private float calcHighest(int i, int days) {
		if (days > 1 && i > 0) {
			return Math.max(get(i).High, calcHighest(i-1, days - 1));
		} else {
			return get(i).High;
		}
	}

	private float calcLowest(int i, int days) {
		if (days > 1 && i > 0) {
			return Math.min(get(i).Low, calcHighest(i-1, days-1));
		} else {
			return get(i).Low;
		}
	}
}
