package com.simeonkirov.expodebug;

import static com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load;
import static com.simeonkirov.expodebug.http.HttpHelper.LOG_TAG;

import android.app.Application;
import android.content.res.Configuration;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactHost;
import com.facebook.react.ReactInstanceEventListener;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.soloader.OpenSourceMergedSoMapping;
import com.facebook.soloader.SoLoader;
import com.simeonkirov.expodebug.multipart.DebugReactNativeHost;

import java.io.IOException;

import expo.modules.ApplicationLifecycleDispatcher;
import expo.modules.ReactNativeHostWrapper;

public class DebugApplication extends Application implements ReactApplication {
    @NonNull
    @Override
    public ReactNativeHost getReactNativeHost() {
        ReactNativeHost host = new DebugReactNativeHost(this);
        return new ReactNativeHostWrapper(this, host);
    }

    @Nullable
    @Override
    public ReactHost getReactHost() {
        return  ReactNativeHostWrapper.createReactHost(getApplicationContext(), getReactNativeHost());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            SoLoader.init(this, OpenSourceMergedSoMapping.INSTANCE);
        } catch (IOException e) {
            Log.e(LOG_TAG, "React SoLoader failed: "+e.getMessage(), e);
            return;
        }

        if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
            // If you opted-in for the New Architecture, we load the native entry point for this app.
            load();
        }

        attemptToGetHoldOnReactContextWhichDoesnotWork();

        ApplicationLifecycleDispatcher.onApplicationCreate(this);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ApplicationLifecycleDispatcher.onConfigurationChanged(this, newConfig);
    }

    /**
     * This is supposed to be the "official" way to get access to ReactApplicationContext but it
     * doesn't work. The listener method onReactContextInitialized is never invoked.
     * The code is left here as an example/reminder what has been tried.
     */
    private void attemptToGetHoldOnReactContextWhichDoesnotWork() {
        ReactInstanceManager reactInstanceManager = getReactNativeHost().getReactInstanceManager();
        reactInstanceManager.addReactInstanceEventListener(new ReactInstanceEventListener() {
            @Override
            public void onReactContextInitialized(@NonNull ReactContext reactContext) {
                Log.v(LOG_TAG, "ReactApplicationContext initialized");
            }
        });
    }
}
