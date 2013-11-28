package com.sleepyduck.macdnotification;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.sleepyduck.macdnotification.CalculateMACD.MACDListener;

public class StartupBroadcastReceiver extends BroadcastReceiver {
	private static final String LOG_TAG = StartupBroadcastReceiver.class.getSimpleName();
	private Context mContext;
	private int mIdCounter = 0;

	private MACDListener listener = new MACDListener() {

		@Override
		public void onMessage(String message) {
			Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
		}

		@Override
		public void onCalculationComplete(Bundle data) {
			displayNotification(data);
		}
	};

	BroadcastReceiver mInternetReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!checkInternetConnection()) {
				mContext.registerReceiver(mInternetReceiver, new IntentFilter(
						"android.net.conn.CONNECTIVITY_CHANGE"));
			} else {
				calculateMACDs(context);
				mContext.unregisterReceiver(mInternetReceiver);
			}
		}
	};

	@Override
	public void onReceive(final Context context, final Intent intent) {
		mContext = context;

		setNewAlarm();

		if (!checkInternetConnection()) {
			mContext.registerReceiver(mInternetReceiver, new IntentFilter(
					"android.net.conn.CONNECTIVITY_CHANGE"));
		} else {
			calculateMACDs(context);
		}
	}

	private void calculateMACDs(Context context) {
		final SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(),
				Context.MODE_PRIVATE);

		int symbolCount;
		final List<String> symbols = new ArrayList<String>();
		final int groupCount = prefs.getInt(ActivityMACD.KEY_COUNT, 0);
		for (int i = 0; i < groupCount; i++) {
			symbolCount = prefs.getInt(ActivityMACD.KEY_GROUP + ActivityMACD.KEY_SEPPARATOR + i
					+ ActivityMACD.KEY_SEPPARATOR + ActivityMACD.KEY_COUNT, 0);
			for (int j = 0; j < symbolCount; j++) {
				symbols.add(prefs.getString(ActivityMACD.KEY_GROUP + ActivityMACD.KEY_SEPPARATOR + i
						+ ActivityMACD.KEY_SEPPARATOR + j + ActivityMACD.KEY_SEPPARATOR
						+ ActivityMACD.KEY_NAME, "-SYM-"));
			}
		}

		for (final String symbol : symbols)
			new CalculateMACD(context, listener).execute(symbol);
	}

	private boolean checkInternetConnection() {
		ConnectivityManager cm = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
	}

	private void setNewAlarm() {
		final Calendar cal = Calendar.getInstance();
		if (cal.get(Calendar.HOUR_OF_DAY) > 7)
			cal.add(Calendar.DATE, 1);
		cal.set(Calendar.HOUR_OF_DAY, 8);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);

		final int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
			cal.add(Calendar.DATE, 2);

		Log.d(LOG_TAG,
				"Alarm set to " + String.format("%04d-%02d-%02d %02d:%02d", cal.get(Calendar.YEAR),
						cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
						cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));

		final Intent intentAlarm = new Intent(mContext, StartupBroadcastReceiver.class);
		final AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
				PendingIntent.getBroadcast(mContext, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void displayNotification(Bundle data) {
		if (!data.containsKey(CalculateMACD.DATA_MACD_LATEST)
				|| !data.containsKey(CalculateMACD.DATA_MACD_PREVIOUS))
			return;

		float macd = data.getFloat(CalculateMACD.DATA_MACD_LATEST);
		float macdPrev = data.getFloat(CalculateMACD.DATA_MACD_PREVIOUS);
		String symbol = data.getString(CalculateMACD.DATA_SYMBOL);

		final NotificationManager notificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		builder.setContentTitle("MACD Notification");

		String buyOrSell = "";
		if (macd >= 0)
			if (macd * macdPrev > 0)
				buyOrSell = "Keep";
			else
				buyOrSell = "Buy";
		else if (macd * macdPrev > 0)
			buyOrSell = "Don't buy";
		else
			buyOrSell = "Sell";

		// Calculate the value of MACD after three days with the same trend
		if (buyOrSell.equals("Don't buy")) {
			float trend = macd - macdPrev;
			float days = (-macd) / trend;
			if (days > 0 && days < 5)
				buyOrSell = "Possible buy in " + ((int) (days + 1f)) + " days for";
		}

		builder.setContentText(buyOrSell + " " + symbol);
		builder.setSmallIcon(R.drawable.ic_launcher);
		final PendingIntent intent = PendingIntent.getActivity(mContext, 0, new Intent(),
				PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setFullScreenIntent(intent, true);
		notificationManager.notify(++mIdCounter, builder.build());
	}
}
