package com.jsy.mediasoup;

public class MediasoupManagement {
    public interface MediasoupHandler {
        String getCurAccountId();

        String getCurClientId();

        String getCurDisplayName();

        void onReady(boolean isReady);

        /**
         * @param rConvId
         * @param userid_self   自己 注：未用到
         * @param clientid_self 自己 注：未用到
         * @param userid_dest   接收人 注：未用到
         * @param clientid_dest 接收人 注：未用到
         * @param data
         * @param transients
         * @return
         */
        int onSend(String rConvId, String userid_self, String clientid_self, String userid_dest, String clientid_dest, String data, boolean transients);

        void onIncomingCall(String rConvId, long msg_time, String userId, boolean video_call, boolean should_ring);

        void onMissedCall(String rConvId, long msg_time, String userId, boolean video_call);

        void onAnsweredCall(String rConvId);

        void onEstablishedCall(String rConvId, String userId);

        /**
         * @param reasonCode
         * @param rConvId
         * @param msg_time   注：未用到
         * @param userId     注：未用到
         */
        void onClosedCall(int reasonCode, String rConvId, long msg_time, String userId);

        void onMetricsReady(String rConvId, String metricsJson);

        int onConfigRequest(boolean isRegister);

        void onBitRateStateChanged(String userId, boolean enabled);

        void onVideoReceiveStateChanged(String rConvId, String userId, String clientId, int state);

        void joinMediasoupState(int state);

        void rejectEndCancelCall();
    }

    public interface UserChangedHandler {
        void onUserChanged(String convId, String data);
    }
}
