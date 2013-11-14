package com.sleepyduck.macdnotification;

import java.util.Calendar;
import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StartupBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(final Context context, final Intent intent) {
        Log.d(context.getString(R.string.log_tag), "StartupBroadcastReceiver.onReceive");
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.HOUR_OF_DAY) > 8)
            cal.add(Calendar.DATE, 1);
        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

		final Intent intentAlarm = new Intent(context, StartupBroadcastReceiver.class);
		final AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
				PendingIntent.getBroadcast(context, 1, intentAlarm,
                        PendingIntent.FLAG_UPDATE_CURRENT));

		new CalculateMACD(context).execute("GDX", CalculateMACD.NOTIFICATION);
	}

}
