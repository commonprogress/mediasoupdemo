package org.mediasoup.droid;

import android.content.Context;

import androidx.annotation.Nullable;

import org.webrtc.Loggable;
import org.webrtc.Logging.Severity;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.PeerConnectionFactory.InitializationOptions;

import static org.webrtc.Logging.Severity.*;

public class MediasoupClient {

    static {
        System.loadLibrary("mediasoupclient_so");
        System.loadLibrary("ffmpeg");
    }

    @Nullable
    private static Severity mLoggableSeverity = LS_VERBOSE;
    @Nullable
    private static Loggable mediasoupLoggable = new MediasoupLoggable();

    public static void initialize(Context appContext) {
        initialize(appContext, mediasoupLoggable, mLoggableSeverity);
    }

    public static void initialize(Context appContext, String fieldTrials) {
        initialize(appContext, fieldTrials, mediasoupLoggable, mLoggableSeverity);
    }

    public static void initialize(Context appContext, @Nullable Severity loggableSeverity) {
        initialize(appContext, mediasoupLoggable, loggableSeverity);
    }

    public static void initialize(Context appContext, String fieldTrials, @Nullable Severity loggableSeverity) {
        initialize(appContext, fieldTrials, mediasoupLoggable, loggableSeverity);
    }

    public static void initialize(Context appContext, @Nullable Loggable loggable, @Nullable Severity loggableSeverity) {
        initialize(appContext, null, loggable, loggableSeverity);
    }

    public static void initialize(Context appContext, String fieldTrials, @Nullable Loggable loggable, @Nullable Severity loggableSeverity) {
        mLoggableSeverity = loggableSeverity;
        mediasoupLoggable = loggable;
        InitializationOptions options =
                InitializationOptions.builder(appContext)
                        .setFieldTrials(fieldTrials)
                        .setEnableInternalTracer(true)
                        .setNativeLibraryName("mediasoupclient_so")
                        .setInjectableLogger(loggable, loggableSeverity)
                        .createInitializationOptions();
        PeerConnectionFactory.initialize(options);
    }

    public static String version() {
        return nativeVersion();
    }

    private static native String nativeVersion();

    private static class MediasoupLoggable implements Loggable {

        @Override
        public void onLogMessage(String message, Severity severity, String tag) {
            if (mediasoupLoggable != null && severity.ordinal() >= mLoggableSeverity.ordinal()) {
                switch (severity) {
                    case LS_ERROR:
                        Logger.e(tag, message);
                        break;
                    case LS_WARNING:
                        Logger.w(tag, message);
                        break;
                    case LS_INFO:
                        Logger.d(tag, message);
                        break;
                    case LS_VERBOSE:
                        Logger.v(tag, message);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
