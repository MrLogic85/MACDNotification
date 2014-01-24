package com.sleepyduck.macdnotification.data;

import java.io.Serializable;

/**
 * @author Fredrik Metcalf
 */

public class StockData implements Serializable {
    private static final long serialVersionUID = -2937639073541004552L;

    public float Close;
    public float High;
    public float Low;

    public float Close_EMA_8;
    public float Close_EMA_12;
    public float Close_EMA_17;
    public float Close_EMA_26;

    public float Close_SMA_10;

    public float MACD_Signal_8_17_9;

    public float High_14;
    public float Low_14;

    public float Stochastic_14_5;
    public float Stochastic_14_5_Slow;
    public float Stochastic_Signal_14_5_Slow;

    public StockData() {
    }

    public float get(StockEnum e) {
        switch (e) {
            case Close: return Close;
            case High: return High;
            case Low: return Low;
            case Close_EMA_8: return Close_EMA_8;
            case Close_EMA_12: return Close_EMA_12;
            case Close_EMA_17: return Close_EMA_17;
            case Close_EMA_26: return Close_EMA_26;
            case Close_SMA_10: return Close_SMA_10;
            case MACD_8_17: return Close_EMA_8-Close_EMA_17;
            case MACD_12_26: return Close_EMA_12-Close_EMA_26;
            case MACD_Signal_8_17_9: return MACD_Signal_8_17_9;
            case MACD_Histogram_8_17_9: return get(StockEnum.MACD_8_17) - MACD_Signal_8_17_9;
            case High_14: return High_14;
            case Low_14: return Low_14;
            case Stochastic_14_5: return Stochastic_14_5;
            case Stochastic_14_5_Slow: return Stochastic_14_5_Slow;
            case Stochastic_Signal_14_5_Slow: return Stochastic_Signal_14_5_Slow;
            default: throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        return "{High: " + High + ", Low: " + Low + ", Close: " + Close + "}";
    }
}
