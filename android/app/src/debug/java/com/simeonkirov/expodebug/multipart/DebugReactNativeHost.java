package com.simeonkirov.expodebug.multipart;

import static com.simeonkirov.expodebug.http.HttpHelper.LOG_TAG;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.PackageList;
import com.facebook.react.ReactPackage;
import com.facebook.react.defaults.DefaultReactNativeHost;
import com.simeonkirov.expodebug.BuildConfig;

import java.util.List;

public class DebugReactNativeHost extends DefaultReactNativeHost {
    public DebugReactNativeHost(@NonNull Application application) {
        super(application);
    }

    @Override
    protected List<ReactPackage> getPackages() {
        List<ReactPackage> result = new PackageList(this).getPackages();
        Log.d(LOG_TAG, "ADDING Custom Form Data support");
        result.add(new FormDataPackage());
        return result;
    }

    @Override
    public boolean getUseDeveloperSupport() {
        return BuildConfig.DEBUG;
    }

    @Override
    public String getJSMainModuleName() {
        return ".expo/.virtual-metro-entry";
    }

    @Override
    public boolean isNewArchEnabled() {
        return BuildConfig.IS_NEW_ARCHITECTURE_ENABLED;
    }

    @Override
    public Boolean isHermesEnabled () {
        return BuildConfig.IS_HERMES_ENABLED;
    }
}
