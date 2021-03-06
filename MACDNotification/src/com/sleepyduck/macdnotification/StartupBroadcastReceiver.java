package com.sleepyduck.macdnotification;

import java.util.Calendar;
import java.util.List;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.sleepyduck.macdnotification.data.DataController;
import com.sleepyduck.macdnotification.data.StockDataFetcher;
import com.sleepyduck.macdnotification.data.StockDataList;
import com.sleepyduck.macdnotification.data.StockEnum;
import com.sleepyduck.macdnotification.data.Symbol;

public class StartupBroadcastReceiver extends BroadcastReceiver {
	public static final int ALARM_HOUR = 8;
	private static final String LOG_TAG = StartupBroadcastReceiver.class.getSimpleName();
	private Context mContext;
	private int mIdCounter = 0;

	private StockDataFetcher.StockDataListener listener = new StockDataFetcher.StockDataListener() {

		@Override
		public void onMessage(String message) {}

		@Override
		public void onCalculationComplete(Symbol symbol) {
			if (!symbol.hasStockData() && symbol.doRetry())
				mStockDataFetcher.execute(symbol);
			else
				displayNotification(symbol);
		}
	};
	private StockDataFetcher mStockDataFetcher = new StockDataFetcher(listener);

	@Override
	public void onReceive(final Context context, final Intent intent) {
		mContext = context;

		setNewAlarm();

		if (!checkInternetConnection()) {
			final Handler handler = new Handler();
			final long startTime = System.currentTimeMillis();
			new Thread() {
				@Override
				public void run() {
					boolean running = true;
					while (running && startTime + 3 * 60 * 60 * 1000 > System.currentTimeMillis()) {
						try {
							Thread.sleep(60 * 1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (checkInternetConnection()) {
							running = false;
							handler.post(new Runnable() {
								@Override
								public void run() {
									fetchStockData(context);
								}
							});
						}
					}
				}
			}.start();
		} else {
			fetchStockData(context);
		}
	}

	private void fetchStockData(Context context) {
		DataController dataController = new DataController();
		dataController.loadFromFile(context);
		List<Symbol> dataList = dataController.getAllSymbols();
		mStockDataFetcher.execute(dataList.toArray(new Symbol[dataList.size()]));
	}

	private boolean checkInternetConnection() {
		ConnectivityManager cm = (ConnectivityManager) mContext
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
	}

	private void setNewAlarm() {
		final Calendar cal = Calendar.getInstance();
		if (cal.get(Calendar.HOUR_OF_DAY) > ALARM_HOUR - 1)
			cal.add(Calendar.DATE, 1);
		cal.set(Calendar.HOUR_OF_DAY, ALARM_HOUR);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);

		final int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
			cal.add(Calendar.DATE, 2);

		Log.d(LOG_TAG,
				"Alarm set to "
						+ String.format("%04d-%02d-%02d %02d:%02d", cal.get(Calendar.YEAR),
								cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
								cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));

		final Intent intentAlarm = new Intent(mContext, StartupBroadcastReceiver.class);
		final AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
				PendingIntent.getBroadcast(mContext, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));
	}

	private void displayNotification(Symbol symbol) {
		if (!symbol.hasStockData() || symbol.getStockData().size() < 2)
			return;

		notifyMACD(symbol);
		notifyRuleNo1(symbol);
	}

	private void notifyMACD(Symbol symbol) {
		String buyOrSell;
		StockDataList data = symbol.getStockData();
		if (data.get(data.size()-1).get(StockEnum.MACD_12_26) >= 0)
			if (data.get(data.size()-1).get(StockEnum.MACD_12_26)
					* data.get(data.size()-2).get(StockEnum.MACD_12_26) > 0)
				buyOrSell = "Keep";
			else
				buyOrSell = "Buy";
		else if (data.get(data.size()-1).get(StockEnum.MACD_12_26)
				* data.get(data.size()-2).get(StockEnum.MACD_12_26) > 0)
			buyOrSell = "Don't buy";
		else
			buyOrSell = "Sell";

		// Calculate the value of MACD after three days with the same trend
		if (buyOrSell.equals("Don't buy")) {
			float trend = data.get(data.size()-1).get(StockEnum.MACD_12_26)
					- data.get(data.size()-2).get(StockEnum.MACD_12_26);
			float days = -data.get(data.size()-1).get(StockEnum.MACD_12_26) / trend;
			if (days > 0 && days < 4)
				buyOrSell = "Possible buy in " + ((int) (days + 1f)) + " days for";
		}

		if (buyOrSell.equals("Keep") || buyOrSell.equals("Don't buy")) {
			return;
		}

		notify(buyOrSell + " " + (symbol.getDisplayName().length() > 0 ? symbol.getDisplayName() + " (" + symbol.getName() + ")" : symbol.getName()), "MACD Notification");
	}

	private void notifyRuleNo1(Symbol symbol) {
		if (symbol.isRuleNo1Buy() && !symbol.wasRuleNo1Buy()) {
			notify("Buy " + (symbol.getDisplayName().length() > 0 ? symbol.getDisplayName() + " (" + symbol.getName() + ")" : symbol.getName()), "Rule #1 Technical Indicators");
		} else if (symbol.isRuleNo1Sell() && !symbol.wasRuleNo1Sell()) {
			notify("Sell " + (symbol.getDisplayName().length() > 0 ? symbol.getDisplayName() + " (" + symbol.getName() + ")" : symbol.getName()), "Rule #1 Technical Indicators");
		}
	}

	private void notify(String text, String title) {

		final NotificationManager notificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		builder.setContentTitle(title);

		builder.setContentText(text);
		builder.setSmallIcon(R.drawable.ic_launcher_small);
		final PendingIntent intent = PendingIntent.getActivity(mContext, 0, new Intent(),
				PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setFullScreenIntent(intent, true);
		notificationManager.notify(++mIdCounter, builder.build());
	}
}
