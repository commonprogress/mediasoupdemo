// RoomAidlInterface.aidl
package com.jsy.mediasoup.services;

// Declare any non-default types here with import statements

interface RoomAidlInterface {
    void onMediasoupReady(boolean isReady, boolean isReceiveCall, boolean isConnecting);

    void onSelfAcceptOrJoin();

    void onOtherJoin();

    void setConnectionState(int ordinal);

    void setNetworkMode(int index);

    void onOtherUpdate(int count);

    void setCallTiming(String callTiming);

    void onAllLeaveRoom();

    boolean reqShareScreenIntentData();

    void onFinishServiceActivity();
}
