package com.airtel.embedded.zxing;

import android.app.Application;

/**
 *
 */
public class SampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
       // LeakCanary.install(this);
    }
}
