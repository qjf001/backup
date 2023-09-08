package com.qjf.backup.ui.log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.qjf.backup.ui.log.entity.ScanLogOut;

/**
 *
 * 在MVVM中，数据和业务逻辑处于一个独立的View Model中，viewmodel只要关注数据和业务逻辑，不需要和UI或者控件打交道
 */
public class LogViewModel extends ViewModel {

    private final MutableLiveData<ScanLogOut> mText;

    public LogViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue(new ScanLogOut());
    }

    public LiveData<ScanLogOut> getText() {
        return mText;
    }

    public void setText() {
//        this.mText=;
    }

    public void postValue(){

    }
}