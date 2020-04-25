// MediasoupAidlInterface.aidl
package com.jsy.mediasoup.services;
import com.jsy.mediasoup.services.RoomAidlInterface;
// Declare any non-default types here with import statements

interface MediasoupAidlInterface {
    void onRegisterRoom(RoomAidlInterface roomInterface);
    void onUnRegisterRoom();
    void onCreateMediasoupRoom();
    void onJoinMediasoupRoom();
    void onResetMediasoupRoom();
    void onDestroyMediasoupRoom();
    boolean isBindService();
    boolean isReceiveCall();
    boolean isSelfCalling();
    boolean isOneOnOneCall();
    boolean isRoomConnecting();
    void callSelfAccept();
    void callSelfEnd();
    void callSelfCancel();
    void callSelfReject();
    void rejectEndCancelCall();
    void setVisibleCall(boolean isVisible);
}
