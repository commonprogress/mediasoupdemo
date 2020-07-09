package org.mediasoup.droid.lib.p2p;

import com.jsy.mediasoup.utils.LogUtils;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class P2PSdpObserver implements SdpObserver {
    public static final String TAG = P2PSdpObserver.class.getSimpleName();
    private String tag;

    public P2PSdpObserver() {
        this("");
    }

    public P2PSdpObserver(String tag) {
        this.tag = "dongxl:" + tag;
    }

    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
        LogUtils.i(TAG, "onCreateSuccess type:" + sessionDescription.type + ",description:" + sessionDescription.description);
    }

    @Override
    public void onSetSuccess() {
        LogUtils.i(TAG, "onSetSuccess ");
    }

    @Override
    public void onCreateFailure(String s) {
        LogUtils.e(TAG, "onCreateFailure " + s);
    }

    @Override
    public void onSetFailure(String s) {
        LogUtils.e(TAG, "onSetFailure " + s);
    }
}
