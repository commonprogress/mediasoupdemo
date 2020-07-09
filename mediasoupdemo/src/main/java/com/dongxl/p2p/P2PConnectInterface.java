package com.dongxl.p2p;

public interface P2PConnectInterface {
    void sendOfferSdp(boolean isFaqifang, String jsonData);

    void sendAnswerSdp(boolean isFaqifang, String jsonData);

    void sendIceCandidate(boolean isFaqifang, String jsonData);

    void onP2PConnectSuc(boolean isFaqifang);
}
