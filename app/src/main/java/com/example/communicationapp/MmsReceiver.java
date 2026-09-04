package com.example.communicationapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import android.provider.Telephony;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MmsReceiver extends BroadcastReceiver {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION.equals(intent.getAction())) return;
        byte[] payload = intent.getByteArrayExtra("data");
        if (payload == null) return;
        PendingResult result = goAsync();
        IO.execute(() -> {
            try (FileOutputStream output = context.openFileOutput(
                    "pending_mms_" + System.currentTimeMillis() + ".pdu",
                    Context.MODE_PRIVATE)) {
                output.write(payload);
            } catch (IOException ignored) {
                // The app cannot process this MMS without a configured carrier transport.
            } finally {
                result.finish();
            }
        });
        Toast.makeText(context, R.string.mms_not_supported, Toast.LENGTH_LONG).show();
    }
}
