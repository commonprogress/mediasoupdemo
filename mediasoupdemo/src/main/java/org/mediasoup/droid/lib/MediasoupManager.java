package org.mediasoup.droid.lib;

import android.content.Context;

public class MediasoupManager {
    // Android context.
    private final Context mContext;
    //p2p connect mode
    private boolean isP2PMode;

    public MediasoupManager(Context context, boolean isP2PMode) {
        this.mContext = context.getApplicationContext();
        this.isP2PMode = isP2PMode;
    }

    public boolean isP2PMode() {
        return isP2PMode;
    }

    public void setEnableSpeaker(boolean enable) {

    }
}
