package com.example.communicationapp.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.communicationapp.SmsStatusReceiver;
import com.example.communicationapp.data.SmsRepository;

public class SmsStatusWorker extends Worker {
    public static final String KEY_ACTION = "action";
    public static final String KEY_MESSAGE_ID = "message_id";
    public static final String KEY_PART_INDEX = "part_index";
    public static final String KEY_RESULT_CODE = "result_code";

    public SmsStatusWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String action = getInputData().getString(KEY_ACTION);
        String messageId = getInputData().getString(KEY_MESSAGE_ID);
        int partIndex = getInputData().getInt(KEY_PART_INDEX, -1);
        int resultCode = getInputData().getInt(KEY_RESULT_CODE, Integer.MIN_VALUE);
        if ((!SmsStatusReceiver.ACTION_SENT.equals(action)
                && !SmsStatusReceiver.ACTION_DELIVERED.equals(action))
                || messageId == null || partIndex < 0 || resultCode == Integer.MIN_VALUE) {
            return Result.failure();
        }
        SmsRepository.getInstance(getApplicationContext())
                .handleStatusNow(action, messageId, partIndex, resultCode);
        return Result.success();
    }
}
