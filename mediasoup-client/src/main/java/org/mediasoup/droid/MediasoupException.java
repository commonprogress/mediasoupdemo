package org.mediasoup.droid;

import android.os.Build;
import android.support.annotation.RequiresApi;

public class MediasoupException extends Exception {
    public MediasoupException() {
        super();
    }

    public MediasoupException(String message) {
        super(message);
    }

    public MediasoupException(String message, Throwable cause) {
        super(message, cause);
    }

    public MediasoupException(Throwable cause) {
        super(cause);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    protected MediasoupException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
