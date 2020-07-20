package org.mediasoup.droid.lib;

import android.content.Context;

public class MediasoupManager {
    // Android context.
    private final Context mContext;
    //p2p connect mode
    private boolean isP2PMode;

    public MediasoupManager(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public boolean isP2PMode() {
        return isP2PMode;
    }

    public void setEnableSpeaker(boolean enable) {

    }
}
