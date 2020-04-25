package org.mediasoup.droid.lib.interfaces;

public interface MediasoupConnectCallback {
    void onConnectSuc();

    void onConnectFail();

    void onConnectDisconnected();

    void onConnectClose();

    void onJoinSuc();

    void onJoinFail();

    boolean isConnecting();
}
