package com.simeonkirov.expodebug.multipart;

import static com.simeonkirov.expodebug.http.HttpHelper.LOG_TAG;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.modules.network.NetworkingModule;

/**
 * The only task of this module is to get the instance of  {@link com.facebook.react.modules.network.NetworkingModule},
 * and register {@link FormDataRequestBodyHandler} with it.
 */
public class FormDataModule extends ReactContextBaseJavaModule {
    public FormDataModule(@Nullable ReactApplicationContext reactContext) {
        super(reactContext);
        Log.i(LOG_TAG, "Form Data Module created");
        registerHandler();
    }

    private void registerHandler() {
        Log.d(LOG_TAG, "INITIALIZING Custom Form Data Handler");

        // This method "ensures" that ReactInstance is initialized and we need this
        // to get access to NetworkingModule
        ReactApplicationContext rac = getReactApplicationContextIfActiveOrWarn();

        if (rac!=null) {
            onReactContextInitialized(rac);
        } else {
            // ReactInstance is not yet initialized, probably because it happens asynchronously
            // into another Thread. Since ReactInstanceEventListener mechanism is not working
            // we need a kind of "brute force" - this will invoke getReactApplicationContextIfActiveOrWarn()
            // iteratively until it returns result or we get tired.
            new Thread(new ReactContextListener()).start();
        }
    }

    private class ReactContextListener implements Runnable {
        private static final int MAX_RETRIES = 10;

        @Override
        public void run() {
            ReactApplicationContext rac = getReactApplicationContextIfActiveOrWarn();

            int i = 0;
            for (; rac==null && i<MAX_RETRIES; i++) {
                rac = getReactApplicationContextIfActiveOrWarn();
                Log.i(LOG_TAG, "Querying RAC: "+rac);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }

            if (i<MAX_RETRIES) {
                Log.d(LOG_TAG, "!!!!!!!!!!!!! RAC INITIALIZED !!!!!!!!!!!!!");
                onReactContextInitialized(rac);
            } else {
                Log.e(LOG_TAG, "RAC was not initialized after "+MAX_RETRIES+" retries");
            }
        }
    }

    @NonNull
    @Override
    public String getName() {
        return "FormDataModule";
    }

    public void onReactContextInitialized(@NonNull ReactContext reactContext) {
        NetworkingModule networkingModule = reactContext.getNativeModule(NetworkingModule.class);
        if (networkingModule!=null) {
            networkingModule.addRequestBodyHandler(new FormDataRequestBodyHandler((ReactApplicationContext) reactContext));
            Log.i(LOG_TAG, "Custom Form Data Handler was registered with the NetworkingModule");
        } else {
            Log.e(LOG_TAG, "Failed to register custom Form Data Handler. NetworkingModule was not found");
        }
    }
}
