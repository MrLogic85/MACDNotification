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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class CalculateMACD {
	public static final int NOTIFICATION = 0x1;
	public static final int TOAST = 0x2;

	private static final long ONE_DAY = 1000 * 60 * 60 * 24;
	private URI mUri;
	private String mData;
	ArrayList<Float> mCloseData = new ArrayList<Float>();
	private final Context mContext;

	public CalculateMACD(final Context context) {
		mContext = context;
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

	private String calculateandDisplayMACD(final String symbol, final int flags) {
		if (!validateData()) {
			Log.d(mContext.getString(R.string.log_tag), "Invalid data " + mCloseData);
			return null;
		}

		Collections.reverse(mCloseData);
		if (mCloseData.size() >= 26) {
			final List<Float> ema12 = calcEMA(mCloseData, 12);
			final List<Float> ema26 = calcEMA(mCloseData, 26);
			final List<Float> macdLine = diff(ema12, ema26);

			Log.d(mContext.getString(R.string.log_tag),
					"MACD for " + symbol + " = " + macdLine.get(macdLine.size() - 1));
			// final List<Float> signalLine = calcEMA(macdLine, 9);
			// final List<Float> macdHistogram = diff(macdLine, signalLine);

			if ((flags & NOTIFICATION) > 0
					&& macdLine.get(macdLine.size() - 1) * macdLine.get(macdLine.size() - 2) < 0) {
				final NotificationManager notificationManager = (NotificationManager) mContext
						.getSystemService(Context.NOTIFICATION_SERVICE);

				final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
				builder.setContentTitle("MACD Notification");
				final String buyOrSell = macdLine.get(macdLine.size() - 1) > 0 ? "Buy" : "Sell";
				builder.setContentText(buyOrSell + " " + symbol + ", MACD is "
						+ macdLine.get(macdLine.size() - 1));
				builder.setSmallIcon(R.drawable.ic_launcher);

				final PendingIntent intent = PendingIntent.getActivity(mContext, 0, new Intent(),
						PendingIntent.FLAG_UPDATE_CURRENT);
				builder.setFullScreenIntent(intent, true);
				notificationManager.notify((int) (Math.random() * 100000), builder.build());
			}
			if ((flags & TOAST) > 0) {
				return symbol + " MACD is " + macdLine.get(macdLine.size() - 1) + ", based on "
						+ mCloseData.size() + " values";
			}
			return null;
		} else if (mCloseData.size() > 0) {
			Log.d(mContext.getString(R.string.log_tag), "Not enough data " + mCloseData);
			if ((flags & TOAST) > 0) {
				return "Not enough data for " + symbol + ", only " + mCloseData.size() + " values found";
			}
		} else {
			Log.d(mContext.getString(R.string.log_tag), symbol + " could not be found");
			if ((flags & TOAST) > 0) {
				return symbol + " could not be found";
			}
		}
		return null;
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
			final StringBuffer sb = new StringBuffer("");
			String l = "";
			final String nl = System.getProperty("line.separator");
			while ((l = in.readLine()) != null) {
				sb.append(l + nl);
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
					if (qName.toLowerCase().equals("close")) {
						mClose = true;
					} else {
						mClose = false;
					}
				}
			});
		} catch (final Exception e) {
			e.printStackTrace();
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

	public void execute(final String symbol, final int flags) {
		Log.d(mContext.getString(R.string.log_tag), "Calculate MACD for " + symbol);
		new AsyncTask<Void, String, Void>() {
			@Override
			protected Void doInBackground(final Void... params) {
				if (buildURI(symbol) && fetchData() && parseData())
					publishProgress(calculateandDisplayMACD(symbol, flags));
				else if ((flags & TOAST) > 0) {
					publishProgress("An error has occurred for " + symbol);
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(final String... values) {
				super.onProgressUpdate(values);
				if (values != null && values.length > 0 && values[0] != null)
					Toast.makeText(mContext, values[0], Toast.LENGTH_LONG).show();
			}
		}.execute();
	}
}
