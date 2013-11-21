package com.sleepyduck.macdnotification;

import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * @author Fredrik Metcalf
 */
public class IncomingSmsReceiver extends BroadcastReceiver {
	final SmsManager mSmsManager = SmsManager.getDefault();

	private List<SmsMessage> getSmsMessage(final Intent intent) {
		final Bundle bundle = intent.getExtras();

		try {
			if (bundle != null) {
				final Object[] pdusObj = (Object[]) bundle.get("pdus");
				final List<SmsMessage> messages = new ArrayList<SmsMessage>();
				for (final Object element : pdusObj) {
					messages.add(SmsMessage.createFromPdu((byte[]) element));
				}
				return messages;
			} else {
				Log.e(getClass().getSimpleName(), "No sms data received");
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return new ArrayList<SmsMessage>();
	}

	private void sendSms(final Context context, final String message) {
		final PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent("SMS_SENT"), 0);
		// mSmsManager.sendTextMessage("TODO", null, message, pi, null);
	}

	private boolean validateSender(final String phoneNumber) {
		Log.d(getClass().getSimpleName(), "Validating phone number " + phoneNumber);
		return PhoneNumberUtils.compare("SPNL", phoneNumber);
	}

	@Override
	public void onReceive(final Context context, final Intent intent) {

		final List<SmsMessage> smsMessages = getSmsMessage(intent);
		for (final SmsMessage smsMessage : smsMessages) {
			final String phoneNumber = smsMessage.getDisplayOriginatingAddress();
			final String message = smsMessage.getDisplayMessageBody();
			if (validateSender(phoneNumber)) {
				sendSms(context, message);
			}
		}
	}
}
