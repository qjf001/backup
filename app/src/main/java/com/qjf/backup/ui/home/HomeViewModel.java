package com.qjf.backup.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Random;

public class HomeViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public HomeViewModel() {
        mText = new MutableLiveData<>();
        Random r  = new Random();
        mText.setValue("This is home fragment-" + r.nextInt());
    }

    public LiveData<String> getText() {
        return mText;
    }
}