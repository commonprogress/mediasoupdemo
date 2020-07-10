package org.mediasoup.droid.lib.interfaces;

import org.json.JSONObject;

public interface MediasoupConnectCallback {
    void onConnectSuc();

    void onConnectFail();

    void onConnectDisconnected();

    void onConnectClose();

    boolean isConnecting();

    boolean isConnected();

    boolean reqShareScreenIntentData();

    String getConnectPeerId();

    String getConnectPeerName();

    void sendOfferSdp(String peerId, JSONObject sdpJson);

    void sendAnswerSdp(String peerId, JSONObject sdpJson);

    void sendIceCandidate(String peerId, JSONObject iceJson);

    void onJoinSuc();

    void onJoinFail();
}
