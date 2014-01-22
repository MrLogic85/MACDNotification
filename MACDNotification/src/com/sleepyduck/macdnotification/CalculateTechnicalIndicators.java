package com.sleepyduck.macdnotification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.os.Handler;
import android.util.Log;

import com.sleepyduck.macdnotification.data.Symbol;

public class CalculateTechnicalIndicators {
	private static final String LOG_TAG = CalculateTechnicalIndicators.class.getSimpleName();

	private static final long ONE_DAY = 1000 * 60 * 60 * 24;
	private MACDListener mListener = null;
	private Handler mHandler;

	public CalculateTechnicalIndicators(MACDListener listener) {
		mListener = listener;
		mHandler = new Handler();
	}

	private URI buildURI(final String symbol) {
		final Calendar calendar = Calendar.getInstance();
		final String end = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
		calendar.setTimeInMillis(calendar.getTimeInMillis() - (450L * ONE_DAY));
		final String start = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));

		try {
			String query = "select Close,High,Low from yahoo.finance.historicaldata where startDate=\"" + start
					+ "\" AND symbol=\"" + symbol + "\" AND endDate=\"" + end + "\"";
			query = query.replace(" ", "%20").replace("=", "%3D").replace("\"", "%22").replace("^", "%5E").replace(",", "%2C");
			query = "http://query.yahooapis.com/v1/public/yql?q=" + query;
			query += "&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
			return new URI(query);
		} catch (final URISyntaxException e) {
			Log.e(LOG_TAG, "", e);
			return null;
		}
	}

	private String fetchData(URI uri) {
		final HttpClient client = new DefaultHttpClient();
		final HttpGet request = new HttpGet();
		request.setURI(uri);
		try {
			final HttpResponse response = client.execute(request);
			final BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
					.getContent()));
			final StringBuilder sb = new StringBuilder("");
			String l;
			while ((l = in.readLine()) != null) {
				sb.append(l).append("\n");
			}
			return sb.toString();
		} catch (final IOException e) {
			Log.e(LOG_TAG, "", e);
			return null;
		}
	}

	private List<StockData> parseData(String uriData) {
		final List<StockData> stockData = new ArrayList<StockData>();
		try {
			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(new StringReader(uriData)), new DefaultHandler() {
				private boolean mClose = false;
				private boolean mHigh = false;
				private boolean mLow = false;

				@Override
				public void characters(final char[] ch, final int start, final int length) throws SAXException {
					super.ignorableWhitespace(ch, start, length);
					final String data = String.copyValueOf(ch, start, length);
					if (mClose) {
						stockData.get(stockData.size()-1).Close = Float.valueOf(data);
					} else if (mHigh) {
						stockData.get(stockData.size()-1).High = Float.valueOf(data);
					} else if (mLow) {
						stockData.get(stockData.size()-1).Low = Float.valueOf(data);
					}
				}

				@Override
				public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
					super.startElement(uri, localName, qName, attributes);
					mClose = qName.toLowerCase().equals("close");
					mHigh = qName.toLowerCase().equals("high");
					mLow = qName.toLowerCase().equals("low");
					if (qName.toLowerCase().equals("quote")) {
						stockData.add(new StockData());
					}
				}
			});
		} catch (final Exception e) {
			Log.e(LOG_TAG, "", e);
			Log.e(LOG_TAG, "Data: " + uriData);
			return null;
		}
		return stockData;
	}

	private float calcEMA(float prev, float curr, final int days) {
		final float multiplier = 2.0f / (days + 1);
		return curr * multiplier + prev * (1.0f - multiplier);
	}

	private float calcSMA(StockData data, StockEnum e, int days) {
        float res = 0;
        int countDays = 1;
        do {
            res += data.get(e);
            data = data.previous();
            countDays++;
        } while (data.hasPrevious() && countDays < days);
        return res / (float) countDays;
	}

	private List<Float> diff(final List<Float> LHS, final List<Float> RHS) {
		final int smallest = Math.min(LHS.size(), RHS.size());
		final List<Float> res = new ArrayList<Float>();
		for (int i = 0; i < smallest; i++)
			res.add(0f);
		for (int i = 1; i <= smallest; i++) {
			res.set(smallest - i, LHS.get(LHS.size() - i) - RHS.get(RHS.size() - i));
		}
		return res;
	}

	private Float calcHighest(StockData data, int days) {
        if (days > 1 && data.previous != null) {
            return Math.max(data.High, calcHighest(data.previous, days-1));
        } else {
            return data.High;
        }
	}

    private Float calcLowest(StockData data, int days) {
        if (days > 1 && data.previous != null) {
            return Math.min(data.Low, calcHighest(data.previous, days-1));
        } else {
            return data.Low;
        }
    }

	private List<Float> calcPercentile(List<Float> highs, List<Float> lows, List<Float> closeData) {
		final ArrayList<Float> percentile = new ArrayList<Float>();
		final int shortest = Math.min(Math.min(highs.size(), lows.size()), closeData.size());
        List<Float> h = highs.subList(highs.size() - shortest, highs.size());
        List<Float> l = lows.subList(lows.size()-shortest, lows.size());
        List<Float> c = closeData.subList(closeData.size()-shortest, closeData.size());
		for (int i = 0; i < shortest; i++) {
			percentile.add((c.get(i) - l.get(i))/(h.get(i) - l.get(i)) * 100);
		}
		return percentile;
	}

	private boolean calculateMACD(Symbol symbol, List<StockData> stockData) {
		// Reverse the data so that the oldest value is first
		Collections.reverse(stockData);
		if (stockData.size() >= 26) {
			symbol.setDataTime(System.currentTimeMillis());

			// Close
			symbol.setValue(stockData.get(stockData.size() - 1).Close);
			symbol.setValueOld(stockData.get(stockData.size() - 2).Close);

			// MACD
            List<Float> macdLine = diff(calcEMA(stockData, StockEnum.Close, 12),
                    calcEMA(stockData, StockEnum.Close, 26));
			symbol.setMACD(macdLine.get(macdLine.size() - 1));
			symbol.setMACDOld(macdLine.get(macdLine.size() - 2));

			// MACD Rule #1
			macdLine = diff(calcEMA(stockData, StockEnum.Close, 8),
                    calcEMA(stockData, StockEnum.Close, 17));
			List<Float> histogram = diff(macdLine, calcEMA(macdLine, 9));
			symbol.setRuleNo1Histogram(histogram.get(histogram.size() - 1));
			symbol.setRuleNo1HistogramOld(histogram.get(histogram.size() - 2));

			// Stochastic
			List<Float> highData = calcHighest(StockToHighList(stockData), 14);
			List<Float> lowData = calcLowest(StockToLowList(stockData), 14);
			List<Float> k = calcPercentile(highData, lowData, closeData);
            List<Float> kSlow = new ArrayList<Float>();
            for (int i = 0; i < 6; i++) {
                kSlow.add(calcSMA(k, k.size() - 10 + i, 5));
            }
            float d = calcSMA(kSlow, kSlow.size()-5, 5);
            float dOld = calcSMA(kSlow, kSlow.size()-6, 5);
            /*Log.d(LOG_TAG, "\n" + symbol + "\nHigh  = " + highData.subList(highData.size()-kSlow.size(), highData.size())
                    + "\nLow    = " + lowData.subList(lowData.size()-kSlow.size(), lowData.size())
                    + "\nClose  = " + closeData.subList(closeData.size()-kSlow.size(), closeData.size())
                    + "\n%K     = " + k.subList(k.size()-kSlow.size(), k.size())
                    + "\n%KSlow = " + kSlow
                    + "\n%D     = " + "[0, 0, 0, 0, " + dOld + ", " + d + "]");*/
			symbol.setRuleNo1Stochastic(kSlow.get(kSlow.size()-1) - d);
			symbol.setRuleNo1StochasticOld(kSlow.get(kSlow.size()-2) - dOld);
            /*Log.d(LOG_TAG, symbol + " stochastic is %K = " + kSlow.get(kSlow.size()-1) + ", %D = " +
                    d + ". With high = " + highData.get(highData.size() - 1) +
                    " and low = " + lowData.get(lowData.size()-1));*/

			// Moving Average
			float sma10 = calcSMA(closeData, closeData.size()-10, 10);
			float sma10Old = calcSMA(closeData, closeData.size()-11, 10);
			symbol.setRuleNo1SMA(sma10);
			symbol.setRuleNo1SMAOld(sma10Old);
			return true;
		} else if (stockData.size() > 0) {
            symbol.setValue(stockData.get(stockData.size() - 1).Close);
            if (stockData.size() > 1) {
                symbol.setValueOld(stockData.get(stockData.size() - 2).Close);
            }

			String message = "Not enough data for " + symbol + ", only " + stockData.size() + " values found";
			Log.d(LOG_TAG, message);
			publishProgress(message);
		} else {
			String message = symbol + " could not be found";
			Log.d(LOG_TAG, message);
			publishProgress(message);
		}
		return false;
	}

	private boolean validateData(List<StockData> stockData) {
		if (stockData == null)
			return false;
		for (final StockData val : stockData)
			if (val.Close < 0)
				return false;
		return true;
	}

	private void publishProgress(final String string) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mListener != null && string != null)
					mListener.onMessage(string);
			}
		});
	}

	protected void publishResult(final Symbol symbol) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mListener != null)
					mListener.onCalculationComplete(symbol);
			}
		});
	}

	public void execute(final Symbol... symbolList) {
		final List<Symbol> synchedSymbols = Collections.synchronizedList(new LinkedList<Symbol>());
		Collections.addAll(synchedSymbols, symbolList);
		for (int i = 0; i < 10; ++i) {
			new Thread() {
				@Override
				public void run() {
					while (synchedSymbols.size() > 0) {
						Symbol symbol = synchedSymbols.remove(0);
						Log.d(LOG_TAG, "Calculate MACD for " + symbol.getName());
						List<Symbol> symbolAsList = symbol.asList();
						for (Symbol sym : symbolAsList) {
							if (sym.hasValidData()) {
								publishResult(sym);
							} else {
								URI uri = buildURI(sym.getName());
								if (uri != null) {
									String uriData = fetchData(uri);
									if (uriData != null) {
										List<StockData> stockData = parseData(uriData);
										if (validateData(stockData)) {
											calculateMACD(sym, stockData);
										} else {
											break;
										}
									} else {
										break;
									}
								} else {
									break;
								}
							}
						}
						publishResult(symbol);
					}
				}
			}.start();
		}
	}

	public interface MACDListener {
		public void onMessage(String message);
		public void onCalculationComplete(Symbol symbol);
	}

    private enum StockEnum {
        Close,
        High,
        Low,
        Close_EMA_8,
        Close_EMA_12,
        Close_EMA_17,
        Close_EMA_26,
        Close_SMA_10,
        MACD_8_17,
        MACD_12_26,
        MACD_Signal_8_17_9,
        MACD_Histogram_8_17_9,
        High_14,
        Low_14,
        Stochastic_14_5,
        Stochastic_Signal_14_5
    }

	private class StockData implements Iterable<StockData>, Iterator<StockData> {
        private StockData first;
        private StockData previous;
        private StockData next;

		Float Close;
		Float High;
		Float Low;

        Float Close_EMA_8;
        Float Close_EMA_12;
        Float Close_EMA_17;
        Float Close_EMA_26;

        Float Close_SMA_10;

        Float MACD_Signal_8_17_9;

        Float High_14;
        Float Low_14;

        Float Stochastic_14_5;
        Float Stochastic_Signal_14_5;

        StockData() {
            first = this;
        }

        Float get(StockEnum e) {
            switch (e) {
                case Close: return Close;
                case High: return High;
                case Low: return Low;
                case Close_EMA_8: return getClose_EMA_8();
                case Close_EMA_12: return getClose_EMA_12();
                case Close_EMA_17: return getClose_EMA_17();
                case Close_EMA_26: return getClose_EMA_26();
                case Close_SMA_10: return getClose_SMA_10();
                case MACD_8_17: return getMACD_8_17();
                case MACD_12_26: return getMACD_12_26();
                case MACD_Signal_8_17_9: return getMACD_Signal_8_17_9();
                case MACD_Histogram_8_17_9: return getMACD_Histogram_8_17_9();
                case High_14: return getHigh_14();
                case Low_14: return getLow_14();
                case Stochastic_14_5: return getStochastic_14_5();
                case Stochastic_Signal_14_5: return getStochastic_Signal_14_5();
                default: throw new IllegalArgumentException();
            }
        }

        public Float getClose_EMA_8() {
            if (Close_EMA_8 == null) {
                if (previous != null) {
                    Close_EMA_8 = calcEMA(previous.getClose_EMA_8(), Close, 8);
                } else {
                    Close_EMA_8 = Close;
                }
            }
            return Close_EMA_8;
        }

        public Float getClose_EMA_12() {
            if (Close_EMA_12 == null) {
                if (previous != null) {
                    Close_EMA_12 = calcEMA(previous.getClose_EMA_12(), Close, 12);
                } else {
                    Close_EMA_12 = Close;
                }
            }
            return Close_EMA_12;
        }

        public Float getClose_EMA_17() {
            if (Close_EMA_17 == null) {
                if (previous != null) {
                    Close_EMA_17 = calcEMA(previous.getClose_EMA_17(), Close, 17);
                } else {
                    Close_EMA_17 = Close;
                }
            }
            return Close_EMA_17;
        }

        public Float getClose_EMA_26() {
            if (Close_EMA_26 == null) {
                if (previous != null) {
                    Close_EMA_26 = calcEMA(previous.getClose_EMA_26(), Close, 26);
                } else {
                    Close_EMA_26 = Close;
                }
            }
            return Close_EMA_26;
        }

        public float getClose_SMA_10() {
            if (Close_SMA_10 == null) {
                if (previous != null) {
                    Close_SMA_10 = calcSMA(this, StockEnum.Close, 10);
                } else {
                    Close_SMA_10 = Close;
                }
            }
            return Close_SMA_10;
        }

        public float getMACD_8_17() {
            return getClose_EMA_8() - getClose_EMA_17();
        }

        public float getMACD_12_26() {
            return getClose_EMA_12() - getClose_EMA_26();
        }

        public float getMACD_Signal_8_17_9() {
            if (MACD_Signal_8_17_9 == null) {
                if (previous != null) {
                    MACD_Signal_8_17_9 = calcEMA(
                            previous.getMACD_Signal_8_17_9(), getMACD_8_17(), 9);
                } else {
                    MACD_Signal_8_17_9 = getMACD_8_17();
                }
            }
            return MACD_Signal_8_17_9;
        }

        public float getMACD_Histogram_8_17_9() {
            return getMACD_8_17() - getMACD_Signal_8_17_9();
        }

        public float getHigh_14() {
            if (High_14 == null) {
                if (previous != null) {
                    High_14 = calcHighest(this, 14);
                } else {
                    High_14 = High;
                }
            }
            return High_14;
        }

        public float getLow_14() {
            if (Low_14 == null) {
                if (previous != null) {
                    Low_14 = calcLowest(this, 14);
                } else {
                    Low_14 = Low;
                }
            }
            return Low_14;
        }

        void add(StockData data) {
            if (next != null) {
                next.previous = data;
                data.next = next;
            }
            next = data;
            data.previous = this;
            data.first = first;
        }

        @Override
        public String toString() {
            return "{High: " + High + ", Low: " + Low + ", Close: " + Close + "}";
        }

        @Override
        public Iterator<StockData> iterator() {
            return first;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        public boolean hasPrevious() {
            return previous != null;
        }

        @Override
        public StockData next() {
            return next;
        }

        @Override
        public StockData previous() {
            return previous;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
