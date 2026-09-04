package com.example.communicationapp;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.example.communicationapp.data.SmsRepository;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_DELIVER_ACTION.equals(intent.getAction())) return;
        SmsMessage[] parts = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        if (parts.length == 0) return;

        String address = parts[0].getOriginatingAddress();
        StringBuilder body = new StringBuilder();
        for (SmsMessage part : parts) body.append(part.getMessageBody());
        long date = parts[0].getTimestampMillis();
        PendingResult pendingResult = goAsync();

        SmsRepository.getInstance(context).receiveIncoming(
                address == null ? "Unknown" : address,
                body.toString(),
                date,
                fingerprint(intent),
                intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID),
                (conversationKey, displayName) -> {
                    if (ChatActivity.isConversationVisible(conversationKey)) {
                        SmsRepository.getInstance(context).markConversationRead(conversationKey);
                    } else {
                        showNotification(context, conversationKey,
                                address == null ? "Unknown" : address,
                                displayName, body.toString());
                    }
                }, pendingResult::finish);
    }

    private static String fingerprint(Intent intent) {
        Object[] pdus = intent.getSerializableExtra("pdus", Object[].class);
        if (pdus == null || pdus.length == 0) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (Object pdu : pdus) {
                if (pdu instanceof byte[]) digest.update((byte[]) pdu);
            }
            byte[] value = digest.digest();
            StringBuilder result = new StringBuilder(value.length * 2);
            for (byte item : value) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            return null;
        }
    }

    public static int notificationId(String conversationKey) {
        return conversationKey.hashCode();
    }

    private void showNotification(Context context, String conversationKey, String address,
                                  String displayName, String body) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        Intent openChat = new Intent(context, ChatActivity.class)
                .putExtra(ChatActivity.EXTRA_CONVERSATION_KEY, conversationKey)
                .putExtra(ChatActivity.EXTRA_ADDRESS, address)
                .putExtra(ChatActivity.EXTRA_CONTACT_NAME, displayName);
        PendingIntent contentIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(openChat)
                .getPendingIntent(notificationId(conversationKey),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(
                context, CommunicationApp.MESSAGE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle(displayName)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        NotificationManagerCompat.from(context).notify(
                notificationId(conversationKey), notification.build());
    }
}
