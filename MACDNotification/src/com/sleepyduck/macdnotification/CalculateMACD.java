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
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class CalculateMACD {
	public static final String DATA_SYMBOL = "symbol";
	public static final String DATA_GROUP = "group";
	public static final String DATA_MACD_LATEST = "latest_macd";
	public static final String DATA_MACD_PREVIOUS = "previous_macd";
	public static final String DATA_VALUE_LATEST = "latest_value";
	public static final String DATA_VALUE_PREVIOUS = "previous_value";

	private static final long ONE_DAY = 1000 * 60 * 60 * 24;
	private URI mUri;
	private String mData;
	ArrayList<Float> mCloseData = new ArrayList<Float>();
	private final Context mContext;

	private MACDListener mListener = null;
	private Bundle data = new Bundle();
	private Handler mHandler;

	public CalculateMACD(final Context context, MACDListener listener) {
		mContext = context;
		mListener = listener;

		mHandler = new Handler();
	}

	public CalculateMACD(final Context context, MACDListener listener, String group) {
		this(context, listener);
		data.putString(DATA_GROUP, group);
	}

	private boolean buildURI(final String symbol) {
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
			mUri = new URI(query);
			return true;
		} catch (final URISyntaxException e) {
			e.printStackTrace();
			mUri = null;
			return false;
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

	private void calculateMACD(final String symbol) {
		if (!validateData()) {
			Log.d(mContext.getString(R.string.log_tag), "Invalid data " + mCloseData);
			return;
		}

		Collections.reverse(mCloseData);
		if (mCloseData.size() >= 26) {
			final List<Float> ema12 = calcEMA(mCloseData, 12);
			final List<Float> ema26 = calcEMA(mCloseData, 26);
			final List<Float> macdLine = diff(ema12, ema26);

			Log.d(mContext.getString(R.string.log_tag), symbol + " MACD is " + macdLine.get(macdLine.size() - 1)
					+ ", based on " + mCloseData.size() + " values");

			data.putFloat(DATA_MACD_LATEST, macdLine.get(macdLine.size() - 1));
			data.putFloat(DATA_MACD_PREVIOUS, macdLine.get(macdLine.size() - 2));
			data.putFloat(DATA_VALUE_LATEST, mCloseData.get(mCloseData.size() - 1));
			data.putFloat(DATA_VALUE_PREVIOUS, mCloseData.get(mCloseData.size() - 2));
		} else if (mCloseData.size() > 0) {
			String message = "Not enough data for " + symbol + ", only " + mCloseData.size() + " values found";
			Log.d(mContext.getString(R.string.log_tag), message);
			publishProgress(message);
		} else {
			String message = symbol + " could not be found";
			Log.d(mContext.getString(R.string.log_tag), message);
			publishProgress(message);
		}
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

	private boolean fetchData() {
		if (mUri == null)
			return false;

		final HttpClient client = new DefaultHttpClient();
		final HttpGet request = new HttpGet();
		request.setURI(mUri);
		try {
			final HttpResponse response = client.execute(request);
			final BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
					.getContent()));
			final StringBuilder sb = new StringBuilder("");
			String l;
			while ((l = in.readLine()) != null) {
				sb.append(l).append("\n");
			}
			in.close();
			mData = sb.toString();
			return true;
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean parseData() {
		try {
			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(new StringReader(mData)), new DefaultHandler() {
				private boolean mClose = false;

				@Override
				public void characters(final char[] ch, final int start, final int length) throws SAXException {
					super.ignorableWhitespace(ch, start, length);
					if (mClose) {
						final String data = String.copyValueOf(ch, start, length);
						mCloseData.add(Float.valueOf(data));
					}
				}

				@Override
				public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
					super.startElement(uri, localName, qName, attributes);
					mClose = qName.toLowerCase().equals("close");
				}
			});
		} catch (final Exception e) {
			e.printStackTrace();
			publishProgress("Failed to parse data from Yahoo: " + e.getMessage());
			return false;
		}
		return true;
	}

	private boolean validateData() {
		for (final float val : mCloseData)
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

	protected void publishResult(final Bundle data) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mListener.onCalculationComplete(data);
			}
		});
	}

	protected void execute(final String... params) {
		Thread thread = new Thread() {
			@Override
			public void run() {
				if (params.length > 0) {
					String symbol = params[0];
					data.putString(DATA_SYMBOL, symbol);
					Log.d(mContext.getString(R.string.log_tag), "Calculate MACD for " + symbol);
					if (buildURI(symbol) && fetchData() && parseData())
						calculateMACD(symbol);
					else
						publishProgress("An error has occurred for " + symbol);
				}
				publishResult(data);
			}
		};
		thread.start();
	}

	public interface MACDListener {
		public void onMessage(String message);
		public void onCalculationComplete(Bundle data);
	}
}
