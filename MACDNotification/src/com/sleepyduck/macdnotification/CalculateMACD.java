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

public class CalculateMACD {
	private static final long ONE_DAY = 1000 * 60 * 60 * 24;
	private URI mUri;
	private String mData;
	ArrayList<Float> mCloseData = new ArrayList<Float>();
	private final Context mContext;

	public CalculateMACD(final Context context) {
		mContext = context;
	}

	private List<Float> calcEMA(final List<Float> values, final int days) {
		final ArrayList<Float> ema = new ArrayList<Float>();
		ema.add(calcFirstSMA(values, days));
		final float multiplier = (2.0f / ((float) days + 1));
		for (int i = days + 1; i < values.size(); i++) {
			ema.add((values.get(i) - ema.get(ema.size() - 1)) * multiplier + ema.get(ema.size() - 1));
		}
		return ema;
	}

	private float calcFirstSMA(final List<Float> values, final int days) {
		float res = 0;
		for (int i = 0; i < days; i++)
			res += values.get(i);
		return res / days;
	}

	private boolean calculateDates() {
		final Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(calendar.getTimeInMillis() - ONE_DAY);
		final String end = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
		calendar.setTimeInMillis(calendar.getTimeInMillis() - (365L * ONE_DAY));
		final String start = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));

		try {
			String query = "select Close from yahoo.finance.historicaldata where startDate=\"" + start
					+ "\" AND symbol=\"GDX\" AND endDate=\"" + end + "\"";
			query = query.replace(" ", "%20").replace("=", "%3D").replace("\"", "%22");
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

	private boolean calculateMACD() {
		if (!validateData()) {
			return false;
		}

		Collections.reverse(mCloseData);
		if (mCloseData.size() >= 26) {
			final List<Float> ema12 = calcEMA(mCloseData, 12);
			final List<Float> ema26 = calcEMA(mCloseData, 26);
			final List<Float> macdLine = diff(ema12, ema26);
			final List<Float> signalLine = calcEMA(macdLine, 9);
			final List<Float> macdHistogram = diff(macdLine, signalLine);

			if (macdHistogram.get(macdHistogram.size() - 1) * macdHistogram.get(macdHistogram.size() - 2) < 0) {
				final NotificationManager notificationManager = (NotificationManager) mContext
						.getSystemService(Context.NOTIFICATION_SERVICE);

				final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
				builder.setContentTitle("MACD Notification");
				builder.setContentText("MACD for GDX = " + macdHistogram.get(macdHistogram.size() - 1));
				builder.setSmallIcon(R.drawable.ic_launcher);

				final PendingIntent intent = PendingIntent.getActivity(mContext, 0, new Intent(),
						PendingIntent.FLAG_UPDATE_CURRENT);
				builder.setFullScreenIntent(intent, true);
				notificationManager.notify((int) (Math.random() * 100000), builder.build());

				Log.d(getClass().getSimpleName(),
						"MACD for GDX = " + macdHistogram.get(macdHistogram.size() - 1));
			}
			return true;
		}
		return false;
	}

	private List<Float> diff(final List<Float> LHS, final List<Float> RHS) {
		final int smallest = Math.min(LHS.size(), RHS.size());
		final List<Float> res = new ArrayList<Float>();
		for (int i = 0; i < smallest; i++)
			res.add(0f);
		for (int i = 1; i <= smallest; i++)
			res.set(smallest - i, LHS.get(LHS.size() - i) - RHS.get(RHS.size() - 1));
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
			if (val <= 0)
				return false;
		return true;
	}

	public void execute() {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(final Void... params) {
				if (calculateDates() && fetchData() && parseData())
					calculateMACD();
				return null;
			}
		}.execute();
	}
}
