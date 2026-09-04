package com.example.communicationapp.worker;

import android.content.Context;
import android.telephony.SubscriptionManager;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.communicationapp.data.SmsRepository;

public class SendSmsWorker extends Worker {
    public static final String KEY_MESSAGE_ID = "message_id";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_BODY = "body";
    public static final String KEY_SUBSCRIPTION_ID = "subscription_id";

    public SendSmsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String address = getInputData().getString(KEY_ADDRESS);
        String body = getInputData().getString(KEY_BODY);
        String messageId = getInputData().getString(KEY_MESSAGE_ID);
        int subscriptionId = getInputData().getInt(
                KEY_SUBSCRIPTION_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        if (messageId == null || messageId.isBlank() || address == null || address.isBlank()
                || body == null || body.isBlank()) {
            return Result.failure();
        }
        int result = SmsRepository.getInstance(getApplicationContext())
                .dispatchQueuedMessage(messageId, address, body, subscriptionId);
        return result == SmsRepository.DISPATCH_RESULT_SUCCESS
                ? Result.success() : Result.failure();
    }
}
