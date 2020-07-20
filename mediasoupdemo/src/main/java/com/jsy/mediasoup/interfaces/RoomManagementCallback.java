package com.jsy.mediasoup.interfaces;

import com.jsy.mediasoup.vm.MeProps;
import com.jsy.mediasoup.vm.RoomProps;

import org.threeten.bp.Instant;

public interface RoomManagementCallback {
    void onMediasoupReady(boolean isReady);

    void onConnectSuc(boolean isJoinLast);

    void onConnectFail(boolean isJoinLast);

    void onConnectDisconnected(boolean isJoinLast);

    void onConnectClose(boolean isJoinLast);

    void onJoinSuc(int existPeer);

    void onP2PJoinFail();

    void onMediasoupJoinFail();

    RoomProps getRoomProps();

    MeProps getMeProps();

    void onSelfAcceptOrJoin();

    void onDelayedCheckRoom();

    boolean reqShareScreenIntentData();

    void onAnsweredState();

    void onEstablishedState(Instant estabTime);

    void onAllLeaveRoom();

    void onClosedState();

    void onFinishServiceActivity();
}
