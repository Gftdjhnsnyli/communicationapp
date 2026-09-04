package com.example.communicationapp;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.communicationapp.data.ConversationEntity;
import com.example.communicationapp.data.SmsRepository;

import java.util.List;

public class ConversationListViewModel extends AndroidViewModel {
    private final SmsRepository repository;
    private final MutableLiveData<String> query = new MutableLiveData<>("");
    private final LiveData<List<ConversationEntity>> conversations;

    public ConversationListViewModel(@NonNull Application application) {
        super(application);
        repository = SmsRepository.getInstance(application);
        conversations = Transformations.switchMap(query, repository::observeConversations);
    }

    public LiveData<List<ConversationEntity>> getConversations() {
        return conversations;
    }

    public void setQuery(String value) {
        query.setValue(value == null ? "" : value);
    }

    public void refresh() {
        repository.requestSync();
    }
}
