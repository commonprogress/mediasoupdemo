package com.dongxl.p2p;

public interface P2PConnectCallback {
    void onSendSelfState(String peerId);

    void onReceiveSelfState(String peerId);

    void sendOfferSdpToReceive(String peerId, String type, String description);

    void sendAnswerSdpToSend(String peerId, String type, String description);

    void sendIceCandidateOtherSide(String socketId, String sdpMid, int sdpMLineIndex, String sdp, String serverUrl, Integer bitMask);

    void onP2PConnectSuc(String socketId);
}
