package com.sleepyduck.macdnotification;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.sleepyduck.macdnotification.data.Symbol;

public class CalculateMACD {
	private static final String LOG_TAG = CalculateMACD.class.getSimpleName();

	private static final long ONE_DAY = 1000 * 60 * 60 * 24;
	//private URI mUri;
	//private String mData;
	//ArrayList<Float> mCloseData = new ArrayList<Float>();
	private MACDListener mListener = null;
	private Handler mHandler;

	public CalculateMACD(final Context context, MACDListener listener) {
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
			String query = "select Close from yahoo.finance.historicaldata where startDate=\"" + start
					+ "\" AND symbol=\"" + symbol + "\" AND endDate=\"" + end + "\"";
			query = query.replace(" ", "%20").replace("=", "%3D").replace("\"", "%22").replace("^", "%5E");
			query = "http://query.yahooapis.com/v1/public/yql?q=" + query;
			query += "&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
			return new URI(query);
		} catch (final URISyntaxException e) {
			Log.e(LOG_TAG, "", e);
			return null;
		}
	}

	private List<Float> calcEMA(final List<Float> values, final int days) {
		final ArrayList<Float> ema = new ArrayList<Float>();
		ema.add(calcFirstSMA(values, days));
		final float multiplier = 2.0f / (days + 1);
		for (int i = days; i < values.size(); i++) {
			ema.add(values.get(i) * multiplier + ema.get(ema.size() - 1) * (1.0f - multiplier));
		}
		return ema;
	}

	private float calcFirstSMA(final List<Float> values, final int days) {
		float res = 0;
		for (int i = 0; i < days; i++)
			res += values.get(i);
		return res / days;
	}

	private boolean calculateMACD(Symbol symbol, List<Float> closeData, final Symbol symbol2) {
		Collections.reverse(closeData);
		if (closeData.size() >= 26) {
			final List<Float> ema12 = calcEMA(closeData, 12);
			final List<Float> ema26 = calcEMA(closeData, 26);
			final List<Float> macdLine = diff(ema12, ema26);

			Log.d(LOG_TAG, symbol2 + " MACD is " + macdLine.get(macdLine.size() - 1)
					+ ", based on " + closeData.size() + " values");

			symbol.setMACD(macdLine.get(macdLine.size() - 1));
			symbol.setMACDOld(macdLine.get(macdLine.size() - 2));
			symbol.setValue(closeData.get(closeData.size() - 1));
			symbol.setValueOld(closeData.get(closeData.size() - 2));
			return true;
		} else if (closeData.size() > 0) {
			String message = "Not enough data for " + symbol2 + ", only " + closeData.size() + " values found";
			Log.d(LOG_TAG, message);
			publishProgress(message);
		} else {
			String message = symbol2 + " could not be found";
			Log.d(LOG_TAG, message);
			publishProgress(message);
		}
		return false;
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

	private List<Float> parseData(String uriData) {
		final List<Float> closeData = new ArrayList<Float>();
		try {
			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(new StringReader(uriData)), new DefaultHandler() {
				private boolean mClose = false;

				@Override
				public void characters(final char[] ch, final int start, final int length) throws SAXException {
					super.ignorableWhitespace(ch, start, length);
					if (mClose) {
						final String data = String.copyValueOf(ch, start, length);
						closeData.add(Float.valueOf(data));
					}
				}

				@Override
				public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
					super.startElement(uri, localName, qName, attributes);
					mClose = qName.toLowerCase().equals("close");
				}
			});
		} catch (final Exception e) {
			Log.e(LOG_TAG, "", e);
			publishProgress("Failed to parse data from Yahoo: " + e.getMessage());
			publishProgress("Data: " + closeData);
			return null;
		}
		return closeData;
	}

	private boolean validateData(List<Float> closeData) {
		if (closeData == null)
			return false;
		for (final float val : closeData)
			if (val < 0)
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

	protected void execute(final Symbol... symbolList) {
		final List<Symbol> synchedSymbols = Collections.synchronizedList(new LinkedList<Symbol>());
		Collections.addAll(synchedSymbols, symbolList);
		for (int i = 0; i < 10; ++i) {
			new Thread() {
				@Override
				public void run() {
					while (synchedSymbols.size() > 0) {
						Symbol symbol = synchedSymbols.remove(0);
						Log.d(LOG_TAG, "Calculate MACD for " + symbol.getName());

						URI uri = buildURI(symbol.getName());
						if (uri != null) {
							String uriData = fetchData(uri);
							if (uriData != null) {
								List<Float> closeData = parseData(uriData);
								if (validateData(closeData)) {
									calculateMACD(symbol, closeData, symbol);
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
}
