package com.simeonkirov.expodebug.http;

import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.facebook.react.modules.network.CustomClientBuilder;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;

public class HttpHelper {
    public static final String KEYSTORE_TYPE = "PKCS12";
    public static final String TRUSTED_CERTIFICATES = "trusted_certificates";
    public static final List<String> ALLOWED_HOSTS = List.of("localhost", "127.0.0.1", "10.0.2.2");

    public static final String LOG_TAG = "BS_LOG";

    public static CustomClientBuilder createCustomClientBuilder(Context context) {
        Log.i(LOG_TAG, ">>>>> CUSTOMIZING THE CLIENT");
        return builder -> {
            customizeOKHttpClientBuilder(context, builder);
        };
    }

    public static OkHttpClient.Builder customizeOKHttpClientBuilder(Context context, OkHttpClient.Builder builder) {
        OkHttpClient.Builder result = builder!=null ? builder : new OkHttpClient.Builder();

        Pair<SSLSocketFactory, X509TrustManager> sslArtifacts =
                createSSLArtifacts(loadTrustedCertificatesKeystore(context));
        Log.v(LOG_TAG, ">>>>> CUSTOMIZING THE REQUEST <<<<<<<\n ", new Throwable());
        return result
                .sslSocketFactory(sslArtifacts.first, sslArtifacts.second)
                .hostnameVerifier((hostname, session) -> {
                    boolean isAllowed = ALLOWED_HOSTS.contains(hostname);
                    if (!isAllowed) {
                        Log.w(LOG_TAG, """
                                SSL>> could not find hostname=${hostname} in allowed hosts list
                                """);
                    }
                    return isAllowed;
                })
                .addInterceptor(new DebugInterceptor())
                .eventListener(new DebugHttpListener());
    }

    private static KeyStore loadTrustedCertificatesKeystore(Context context) {
        try (InputStream is = context.getAssets().open(TRUSTED_CERTIFICATES)) {
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            keyStore.load(is, "changeit".toCharArray());
            return keyStore;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load keystore: "+e.getMessage(), e);
        }
    }

    private static Pair<SSLSocketFactory, X509TrustManager> createSSLArtifacts(KeyStore trustedCertificates) {
        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustedCertificates);

            X509TrustManager trustManager = (X509TrustManager)trustManagerFactory.getTrustManagers()[0];
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            return new Pair<>(sslContext.getSocketFactory(), trustManager);
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare SSLContext: "+e.getMessage(), e);
        }
    }
}