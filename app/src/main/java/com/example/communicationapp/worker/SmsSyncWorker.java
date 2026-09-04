package com.example.communicationapp.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.communicationapp.data.SmsRepository;

public class SmsSyncWorker extends Worker {
    public SmsSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SmsRepository.getInstance(getApplicationContext()).syncFromSystem();
            return Result.success();
        } catch (SecurityException exception) {
            return Result.failure();
        } catch (RuntimeException exception) {
            return Result.retry();
        }
    }
}
