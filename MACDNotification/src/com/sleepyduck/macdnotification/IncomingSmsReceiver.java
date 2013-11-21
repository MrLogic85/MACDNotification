package com.sleepyduck.macdnotification;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Fredrik Metcalf
 */
public class IncomingSmsReceiver extends BroadcastReceiver {
    final SmsManager mSmsManager = SmsManager.getDefault();

    @Override
    public void onReceive(Context context, Intent intent) {

        List<SmsMessage> smsMessages = getSmsMessage(intent);
        for (SmsMessage smsMessage : smsMessages) {
            String phoneNumber = smsMessage.getDisplayOriginatingAddress();
            String message = smsMessage.getDisplayMessageBody();
            if (validateSender(phoneNumber)) {
                sendSms(context, message);
            }
        }
    }

    private void sendSms(Context context, String message) {
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent("SMS_SENT"), 0);
        mSmsManager.sendTextMessage("TODO", null, message, pi, null);
    }

    private boolean validateSender(String phoneNumber) {
        Log.d(getClass().getSimpleName(), "Validating phone number " + phoneNumber);
        return PhoneNumberUtils.compare("SPNL", phoneNumber);
    }

    private List<SmsMessage> getSmsMessage(Intent intent) {
        final Bundle bundle = intent.getExtras();

        try {
            if (bundle != null) {
                final Object[] pdusObj = (Object[]) bundle.get("pdus");
                List<SmsMessage> messages = new ArrayList<SmsMessage>();
                for (int i = 0; i < pdusObj.length; i++) {
                    messages.add(SmsMessage.createFromPdu((byte[]) pdusObj[i]));
                }
                return messages;
            } else {
                Log.e(getClass().getSimpleName(), "No sms data received");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<SmsMessage>();
    }
}
