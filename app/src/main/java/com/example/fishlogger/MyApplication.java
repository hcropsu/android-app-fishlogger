package com.example.fishlogger;

import android.app.Application;
import android.content.Context;


// This is a helper class I got from Stackoverflow to help
// get app context when I couldn't use keyword "this" in
// FishViewHolder class when loading image of fish to ImageView
public class MyApplication extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();
        MyApplication.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return MyApplication.context;
    }
}
