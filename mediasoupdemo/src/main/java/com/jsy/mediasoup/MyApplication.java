package com.jsy.mediasoup;

import android.app.Application;

import com.jsy.mediasoup.utils.CrashHandler;

import org.mediasoup.droid.Logger;
import org.mediasoup.droid.MediasoupClient;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Logger.setLogLevel(Logger.LogLevel.LOG_DEBUG);
        Logger.setDefaultHandler();
        MediasoupClient.initialize(getApplicationContext());
        CrashHandler.init(this);
    }
}
