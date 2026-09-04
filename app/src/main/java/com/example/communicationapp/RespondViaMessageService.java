package com.example.communicationapp;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

import androidx.annotation.Nullable;
import com.example.communicationapp.data.SmsRepository;

import java.util.UUID;

public class RespondViaMessageService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Uri data = intent == null ? null : intent.getData();
        String body = intent == null ? null : intent.getStringExtra(Intent.EXTRA_TEXT);
        if (data != null && body != null && !body.isBlank()) {
            String address = data.getSchemeSpecificPart().split("\\?", 2)[0];
            SmsRepository.getInstance(this).enqueueSend(
                    UUID.randomUUID().toString(), address, body);
        }
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
