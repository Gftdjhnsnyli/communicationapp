package com.example.communicationapp;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.communicationapp.data.ConversationEntity;
import com.example.communicationapp.data.SmsRepository;

public class NewMessageViewModel extends AndroidViewModel {
    public interface OpenCallback {
        void onReady(ConversationEntity conversation);
    }

    private final SmsRepository repository;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public NewMessageViewModel(@NonNull Application application) {
        super(application);
        repository = SmsRepository.getInstance(application);
    }

    public void openConversation(String address, OpenCallback callback) {
        repository.openConversation(address,
                conversation -> mainHandler.post(() -> callback.onReady(conversation)));
    }
}
