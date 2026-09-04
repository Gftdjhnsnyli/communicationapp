package com.example.communicationapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.communicationapp.worker.SmsStatusWorker;

public class SmsStatusReceiver extends BroadcastReceiver {
    public static final String ACTION_SENT =
            "com.example.communicationapp.action.SMS_SENT";
    public static final String ACTION_DELIVERED =
            "com.example.communicationapp.action.SMS_DELIVERED";
    public static final String EXTRA_MESSAGE_ID = "message_id";
    public static final String EXTRA_PART_INDEX = "part_index";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!ACTION_SENT.equals(action) && !ACTION_DELIVERED.equals(action)) return;
        String messageId = intent.getStringExtra(EXTRA_MESSAGE_ID);
        if (messageId == null) return;
        int partIndex = intent.getIntExtra(EXTRA_PART_INDEX, -1);
        if (partIndex < 0) return;
        int resultCode = getResultCode();
        Data input = new Data.Builder()
                .putString(SmsStatusWorker.KEY_ACTION, action)
                .putString(SmsStatusWorker.KEY_MESSAGE_ID, messageId)
                .putInt(SmsStatusWorker.KEY_PART_INDEX, partIndex)
                .putInt(SmsStatusWorker.KEY_RESULT_CODE, resultCode)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(
                "sms-status-" + action + "-" + messageId + "-" + partIndex,
                ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(SmsStatusWorker.class)
                        .setInputData(input)
                        .build());
    }
}
