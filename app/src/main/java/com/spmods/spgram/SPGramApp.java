package com.spmods.spgram;

import android.app.Application;

public class SPGramApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.init(this);
    }
}
