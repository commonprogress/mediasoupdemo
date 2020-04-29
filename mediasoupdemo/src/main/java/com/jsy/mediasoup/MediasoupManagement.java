package com.jsy.mediasoup;

import android.content.Context;

import com.jsy.mediasoup.utils.LogUtils;

public class MediasoupManagement {

    public static boolean mediasoupInit(Context context) {
        try {
            MediasoupLoaderUtils.getInstance().mediasoupInit(context);
            LogUtils.i("MediasoupManagement", "mediasoupInit:");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean isInitMediasoup() {
        return MediasoupLoaderUtils.getInstance().isInitMediasoup();
    }

    public String libraryVersion() {
        return MediasoupLoaderUtils.getInstance().libraryVersion();
    }

    public static boolean mediasoupCreate(Context context,
                                          String userId,
                                          String clientId,
                                          String displayName,
                                          MediasoupHandler mediasoupH) {
        return MediasoupLoaderUtils.getInstance().mediasoupCreate(context, userId, clientId, displayName, mediasoupH);
    }

    public static void setUserChangedHandler(UserChangedHandler userChangedHandler) {
        MediasoupLoaderUtils.getInstance().setUserChangedHandler(userChangedHandler);
    }

    public static int mediasoupStartCall(Context context, String rConvId, int call_type, int conv_type, boolean audio_cbr) {
        return MediasoupLoaderUtils.getInstance().mediasoupStartCall(context, rConvId, call_type, conv_type, audio_cbr);
    }

    public void mediasoupAnswerCall(Context context, String rConvId, int call_type, int conv_type, int meidasoup_state, boolean audio_cbr) {
        MediasoupLoaderUtils.getInstance().mediasoupAnswerCall(context, rConvId, call_type, conv_type, meidasoup_state, audio_cbr);
    }

    public void onNetworkChanged() {
        MediasoupLoaderUtils.getInstance().onNetworkChanged();
    }

    public void onHttpResponse(int status, String reason) {
        MediasoupLoaderUtils.getInstance().onHttpResponse(status, reason);
    }

    public void onReceiveMessage(Context context, String msg, long currTime, long msgTime, String rConvId, String userId, String clientId, boolean isMediasoup) {
        MediasoupLoaderUtils.getInstance().receiveCallMessage(context, msg, currTime, msgTime, rConvId, userId, clientId, isMediasoup);
    }

    public void onConfigRequest(int error, String json) {
        MediasoupLoaderUtils.getInstance().onConfigRequest(error, json);
    }

    public static void endCall(String rConvId, int meidasoup_state) {
        MediasoupLoaderUtils.getInstance().endMediasoupCall(rConvId, meidasoup_state);
    }

    public void rejectCall(String rConvId, int meidasoup_state) {
        MediasoupLoaderUtils.getInstance().rejectMediasoupCall(rConvId, meidasoup_state);
    }

    public static void setVideoSendState(String rConvId, int state) {
        MediasoupLoaderUtils.getInstance().setVideoSendState(rConvId, state);
    }

    public static void setCallMuted(boolean muted) {
        MediasoupLoaderUtils.getInstance().setCallMuted(muted);
    }

    public static void switchCam() {
        MediasoupLoaderUtils.getInstance().switchCam();
    }

    public void setProxy(String host, int port) {
        MediasoupLoaderUtils.getInstance().setMediasoupProxy(host, port);
    }

    public void unregisterAccount(Context cxt) {
        MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.DataChannel); //账号注销
        MediasoupLoaderUtils.getInstance().stopMediasoupService(cxt);
        MediasoupLoaderUtils.getInstance().setInstanceNull();
    }

    public interface MediasoupHandler {
        public String getCurAccountId();

        public String getCurClientId();

        public String getCurDisplayName();

        public void onReady(boolean isReady);

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
        public int onSend(String rConvId, String userid_self, String clientid_self, String userid_dest, String clientid_dest, String data, boolean transients);

        public void onIncomingCall(String rConvId, long msg_time, String userId, boolean video_call, boolean should_ring);

        public void onMissedCall(String rConvId, long msg_time, String userId, boolean video_call);

        public void onAnsweredCall(String rConvId);

        public void onEstablishedCall(String rConvId, String userId);

        /**
         * @param reasonCode
         * @param rConvId
         * @param msg_time   注：未用到
         * @param userId     注：未用到
         */
        public void onClosedCall(int reasonCode, String rConvId, long msg_time, String userId);

        public void onMetricsReady(String rConvId, String metricsJson);

        public int onConfigRequest(boolean isRegister);

        public void onBitRateStateChanged(String userId, boolean enabled);

        public void onVideoReceiveStateChanged(String rConvId, String userId, String clientId, int state);

        public void joinMediasoupState(int state);

        public void rejectEndCancelCall();
    }

    public interface UserChangedHandler {
        public void onUserChanged(String convId, String data);
    }
}
