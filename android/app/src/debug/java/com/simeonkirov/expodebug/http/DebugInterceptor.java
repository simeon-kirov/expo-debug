package com.simeonkirov.expodebug.http;

import static com.simeonkirov.expodebug.http.HttpHelper.LOG_TAG;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

public class DebugInterceptor implements Interceptor {
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Log.i(LOG_TAG, "==================REQUEST===================", new Throwable());
        try {
            return chain.proceed(chain.request());
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            throw e;
        }
    }
}
