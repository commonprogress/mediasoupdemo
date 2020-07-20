package org.mediasoup.droid.lib.p2p;

import com.jsy.mediasoup.utils.LogUtils;

import org.mediasoup.droid.lib.interfaces.MediasoupConnectCallback;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

public class P2PSdpObserver implements SdpObserver {
    public static final String TAG = P2PSdpObserver.class.getSimpleName();
    private String peerTag;
    private final MediasoupConnectCallback connectCallback;

    public P2PSdpObserver() {
        this(TAG);
    }

    public P2PSdpObserver(String peerTag) {
        this(peerTag, null);
    }

    public P2PSdpObserver(String peerTag, MediasoupConnectCallback connectCallback) {
        this.peerTag = peerTag;
        this.connectCallback = connectCallback;
    }

    /**
     * Called on success of Create{Offer,Answer}().
     */
    @Override
    public void onCreateSuccess(SessionDescription sessionDescription) {
//        LogUtils.i(TAG, "onCreateSuccess " + peerTag + " ,type:" + sessionDescription.type
//            + ",description:" + sessionDescription.description);
    }

    /**
     * Called on success of Set{Local,Remote}Description().
     */
    @Override
    public void onSetSuccess() {
        LogUtils.i(TAG, "onSetSuccess " + peerTag);
        if (null != connectCallback) {

        }
    }

    /**
     * Called on error of Create{Offer,Answer}().
     */
    @Override
    public void onCreateFailure(String error) {
        LogUtils.e(TAG, "onCreateFailure " + peerTag + ", error:" + error);
        if (null != connectCallback && !connectCallback.isOtherJoin()) {
            connectCallback.onP2PJoinFail();
        }
    }

    /**
     * Called on error of Set{Local,Remote}Description().
     */
    @Override
    public void onSetFailure(String error) {
        LogUtils.e(TAG, "onSetFailure " + peerTag + ", error:" + error);
        if (null != connectCallback) {

        }
    }
}
