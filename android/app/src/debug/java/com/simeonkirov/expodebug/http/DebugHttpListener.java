package com.simeonkirov.expodebug.http;

import static com.simeonkirov.expodebug.http.HttpHelper.LOG_TAG;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;

import okhttp3.Call;
import okhttp3.EventListener;
import okhttp3.Handshake;
import okhttp3.Protocol;
import okhttp3.Request;

public class DebugHttpListener extends EventListener {
    @Override
    public void callStart(@NonNull Call call) {
        Request rq = call.request();

        Log.i(LOG_TAG, """
                            Call started: ${rq.method()} ${rq.url()}
                            """);
    }

    @Override
    public void callFailed(@NonNull Call call, @NonNull IOException ioe) {
        Request rq = call.request();

        Log.e(LOG_TAG, """
                            Call failed:
                            ${rq.method()} ${rq.url()}
                            Error: ${e.getMessage()}
                            """, ioe);
    }

    @Override
    public void secureConnectStart(@NonNull Call call) {
        Request rq = call.request();

        Log.i(LOG_TAG, """
                            Secure connect started: ${rq.method()} ${rq.url()}
                            """);
    }

    @Override
    public void secureConnectEnd(@NonNull Call call, @Nullable Handshake handshake) {
        super.secureConnectEnd(call, handshake);
        Request rq = call.request();

        Log.i(LOG_TAG, """
                            Secure connect ended: ${rq.method()} ${rq.url()}
                            """);
    }

    @Override
    public void connectFailed(@NonNull Call call, @NonNull InetSocketAddress inetSocketAddress, @NonNull Proxy proxy, @Nullable Protocol protocol, @NonNull IOException ioe) {
        super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe);
        Request rq = call.request();

        Log.e(LOG_TAG, """
                            Connect failed:
                            ${rq.method()} ${rq.url()}
                            Error: ${e.getMessage()}
                            """, ioe);
    }
}
