// RoomAidlInterface.aidl
package com.jsy.mediasoup.services;

// Declare any non-default types here with import statements

interface RoomAidlInterface {
    void onMediasoupReady(boolean isReady, boolean isReceiveCall, boolean isConnecting);

    void onSelfAcceptOrJoin();

    void onOtherJoin();

    void onOtherLeave();

    void onFinishServiceActivity();
}
