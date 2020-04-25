package com.jsy.mediasoup.interfaces;

import com.jsy.mediasoup.vm.RoomProps;

public interface RoomManagementCallback {
    void onMediasoupReady(boolean isReady);

    void onConnectSuc();

    void onConnectFail();

    void onConnectDisconnected();

    void onConnectClose();

    void onJoinSuc();

    void onJoinFail();

    RoomProps getRoomProps();

    void onSelfAcceptOrJoin();

    void onFinishServiceActivity();

}
