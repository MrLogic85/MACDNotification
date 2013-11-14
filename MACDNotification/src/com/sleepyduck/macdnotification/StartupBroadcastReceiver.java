package com.sleepyduck.macdnotification;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
		final Calendar cal = Calendar.getInstance();
		if (cal.get(Calendar.HOUR_OF_DAY) > 8)
			cal.add(Calendar.DATE, 1);
		cal.set(Calendar.HOUR_OF_DAY, 8);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);

		final int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
			cal.add(Calendar.DATE, 2);

		Log.d(context.getString(R.string.log_tag),
				"Alarm set to "
						+ String.format("%04d-%02d-%02d %02d:%02d", cal.get(Calendar.YEAR),
								cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
								cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
		final Intent intentAlarm = new Intent(context, StartupBroadcastReceiver.class);
		final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
				PendingIntent.getBroadcast(context, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT));

		final Set<String> symbols = context.getSharedPreferences(context.getPackageName(),
				Context.MODE_PRIVATE).getStringSet(ActivityMACD.KEY_SYMBOL, new HashSet<String>());
		for (final String symbol : symbols)
			new CalculateMACD(context).execute(symbol, CalculateMACD.NOTIFICATION);
	}

}
