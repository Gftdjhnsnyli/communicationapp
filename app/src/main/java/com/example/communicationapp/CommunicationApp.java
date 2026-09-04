package com.example.communicationapp;

import android.Manifest;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;

import androidx.core.content.ContextCompat;

import com.example.communicationapp.data.SmsRepository;

public class CommunicationApp extends Application {
    public static final String MESSAGE_CHANNEL_ID = "incoming_messages";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable sync = () -> SmsRepository.getInstance(this).requestSync();
    private boolean observingSms;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(
                MESSAGE_CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(getString(R.string.notif_channel_desc));
        getSystemService(NotificationManager.class).createNotificationChannel(channel);

        startSmsObservation();
    }

    public synchronized void startSmsObservation() {
        if (!observingSms && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            getContentResolver().registerContentObserver(
                    Telephony.Sms.CONTENT_URI, true,
                    new ContentObserver(handler) {
                        @Override
                        public void onChange(boolean selfChange) {
                            handler.removeCallbacks(sync);
                            handler.postDelayed(sync, 300);
                        }
                    });
            observingSms = true;
            SmsRepository.getInstance(this).requestSync();
        }
    }
}
