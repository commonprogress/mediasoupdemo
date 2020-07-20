package org.mediasoup.droid.lib.interfaces;

import org.json.JSONObject;
import org.mediasoup.droid.lib.model.Peer;

public interface MediasoupConnectCallback {
    String getConnectHost();

    int getConnectPort();

    boolean isEnableAudioJoin();

    boolean isEnableVideoJoin();

    void onConnectSuc();

    void onConnectFail();

    void onConnectDisconnected();

    void onConnectClose();

    boolean isConnecting();

    boolean isConnected();

    boolean reqShareScreenIntentData();

    boolean isOtherJoin();

    String getConnectPeerId();

    String getConnectPeerName();

    Peer getConnectPeer();

    void sendOfferSdp(String peerId, JSONObject sdpJson);

    void sendAnswerSdp(String peerId, JSONObject sdpJson);

    void sendIceCandidate(String peerId, JSONObject iceJson);

    void onJoinSuc(int existPeer);

    void onP2PJoinFail();

    void onMediasoupJoinFail();

    void onP2PConnectionFailed();

    void onP2PReExchangeSDP(boolean isRenegotiation);
}
