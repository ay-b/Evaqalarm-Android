package com.speind.evaqalarm;

import com.yandex.metrica.YandexMetrica;

import android.app.Application;

public class EvaqalarmApplication extends Application {
    private final static Thread.UncaughtExceptionHandler mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();

    @Override
    public void onCreate() {
        super.onCreate();
        YandexMetrica.initialize(getApplicationContext(), "30258");
    }

    static Thread.UncaughtExceptionHandler getDefaultExceptionHandler() {
        return mDefaultExceptionHandler;
    }
}