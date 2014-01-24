package com.sleepyduck.macdnotification.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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

import android.os.Handler;
import android.util.Log;


public class RetrieveDisplayName {
	private static final String LOG_TAG = RetrieveDisplayName.class.getSimpleName();

	private RetrieveDisplayNameListener mListener = null;
	private Handler mHandler;

	public RetrieveDisplayName(RetrieveDisplayNameListener listener) {
		mListener = listener;
		mHandler = new Handler();
	}

	private URI buildURI(final String symbol) {
		try {
			String query = "select Name from yahoo.finance.quote where symbol=\"" + symbol + "\"";
			query = query.replace(" ", "%20").replace("=", "%3D").replace("\"", "%22").replace("^", "%5E");
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

	private String parseName(String uriData) {
		final List<String> name = new ArrayList<String>();
		try {
			final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(new StringReader(uriData)), new DefaultHandler() {
				private boolean bname = false;

				@Override
				public void characters(final char[] ch, final int start, final int length) throws SAXException {
					super.ignorableWhitespace(ch, start, length);
					if (bname) {
						final String data = String.copyValueOf(ch, start, length);
						name.add(data);
					}
				}

				@Override
				public void startElement(final String uri, final String localName, final String qName, final Attributes attributes) throws SAXException {
					super.startElement(uri, localName, qName, attributes);
					bname = qName.toLowerCase().equals("name");
				}
			});
		} catch (final Exception e) {
			Log.e(LOG_TAG, "", e);
			Log.e(LOG_TAG, "Data: " + uriData);
			return "";
		}
		return name.size() > 0 ? name.get(0) : "";
	}

	protected void publishResult(final Symbol symbol) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (mListener != null)
					mListener.onRetrieveComplete(symbol);
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
                        try {
                            Symbol sym = synchedSymbols.remove(0);
                            Log.d(LOG_TAG, "Retrieve display name for " + sym.getName());
                            if (sym.hasDisplayName()) {
                                publishResult(sym);
                            } else {
                                URI uri = buildURI(sym.getName());
                                if (uri != null) {
                                    String uriData = fetchData(uri);
                                    if (uriData != null) {
                                        String name = parseName(uriData);
                                        if (name != null && name.length() > 0) {
                                            sym.setDisplayName(name);
                                        }
                                    }
                                } else {
                                    break;
                                }
                            }
                            publishResult(sym);
                        } catch (IndexOutOfBoundsException e) {
                            Log.e(LOG_TAG, "Synchonization error: ", e);
                        }
					}
				}
			}.start();
		}
	}

	public interface RetrieveDisplayNameListener {
		public void onRetrieveComplete(Symbol symbol);
	}
}
