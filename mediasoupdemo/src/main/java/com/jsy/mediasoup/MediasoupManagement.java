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

    public static String mediasoupCreate(Context context,
                                         String userId,
                                         String clientId,
                                         String activeId,
                                         String displayName,
                                         MediasoupHandler mediasoupH) {
        return MediasoupLoaderUtils.getInstance().mediasoupCreate(context, userId, clientId, activeId, displayName, mediasoupH);
    }

    public static void setUserChangedHandler(String isRegister, UserChangedHandler userChangedHandler) {
        MediasoupLoaderUtils.getInstance().setUserChangedHandler(isRegister, userChangedHandler);
    }

    public static int mediasoupStartCall(String isRegister, Context context, String rConvId, int call_type, int conv_type, boolean audio_cbr) {
        return MediasoupLoaderUtils.getInstance().mediasoupStartCall(isRegister, context, rConvId, call_type, conv_type, audio_cbr);
    }

    public static void mediasoupAnswerCall(String isRegister, Context context, String rConvId, int call_type, int conv_type, int meidasoup_state, boolean audio_cbr) {
        MediasoupLoaderUtils.getInstance().mediasoupAnswerCall(isRegister, context, rConvId, call_type, conv_type, meidasoup_state, audio_cbr);
    }

    public static void onNetworkChanged(String isRegister, int networkMode) {
        MediasoupLoaderUtils.getInstance().onNetworkChanged(isRegister, networkMode);
    }

    public static void onHttpResponse(String isRegister, int status, String reason) {
        MediasoupLoaderUtils.getInstance().onHttpResponse(isRegister, status, reason);
    }

    public static void onReceiveMessage(String isRegister, Context cxt, String msg, long currTime, long msgTime, String rConvId, String userId, String clientId, boolean isMediasoup) {
        MediasoupLoaderUtils.getInstance().receiveCallMessage(isRegister, cxt, msg, currTime, msgTime, rConvId, userId, clientId, isMediasoup);
    }

    public static void onConfigRequest(String isRegister, int error, String json) {
        MediasoupLoaderUtils.getInstance().onConfigRequest(isRegister, error, json);
    }

    public static void endCall(String isRegister, String rConvId, int meidasoup_state) {
        MediasoupLoaderUtils.getInstance().endMediasoupCall(isRegister, rConvId, meidasoup_state);
    }

    public static void rejectCall(String isRegister, String rConvId, int meidasoup_state) {
        MediasoupLoaderUtils.getInstance().rejectMediasoupCall(isRegister, rConvId, meidasoup_state);
    }

    public static void setVideoSendState(String isRegister, String rConvId, int state) {
        MediasoupLoaderUtils.getInstance().setVideoSendState(isRegister, rConvId, state);
    }

    public static void setCallMuted(String isRegister, boolean muted) {
        MediasoupLoaderUtils.getInstance().setCallMuted(isRegister, muted);
    }

    public static void switchCam() {
        MediasoupLoaderUtils.getInstance().switchCam();
    }

    public static void setProxy(String isRegister, String host, int port) {
        MediasoupLoaderUtils.getInstance().setMediasoupProxy(isRegister, host, port);
    }

    public static boolean isMediasoupConnecting(String isRegister, String rConvId) {
        return MediasoupLoaderUtils.getInstance().isMediasoupConnecting(isRegister, rConvId);
    }

    public static boolean isMediasoupConnected(String isRegister, String rConvId) {
        return MediasoupLoaderUtils.getInstance().isMediasoupConnected(isRegister, rConvId);
    }

    public static void unregisterAccount(String isRegister, Context cxt) {
        MediasoupLoaderUtils.getInstance().closedMediasoup(isRegister, MediasoupConstant.ClosedReason.DataChannel); //账号注销
        MediasoupLoaderUtils.getInstance().stopMediasoupService(cxt);
        MediasoupLoaderUtils.getInstance().setInstanceNull(isRegister);
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

        public void onEndedCall(String rConvId, long msg_time, String userId);

        public void onMetricsReady(String rConvId, String metricsJson);

        public int onConfigRequest(String isRegister, String rConvId, String userId, String clientId, boolean isGroup, boolean isReady);

        public void onBitRateStateChanged(String userId, boolean enabled);

        public void onVideoReceiveStateChanged(String rConvId, String userId, String clientId, int state);

        public void joinMediasoupState(int state);

        public void rejectEndCancelCall();

        public void startIfCallIsActive();

        public void cameraOpenState(boolean isFail);
    }

    public interface UserChangedHandler {
        public void onUserChanged(String convId, String data);
    }
}
