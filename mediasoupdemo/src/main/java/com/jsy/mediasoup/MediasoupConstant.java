package com.jsy.mediasoup;

import android.content.Intent;

/**
 * 常量配置类
 */
public class MediasoupConstant {
    public static final String mediasoup_version = "1.0";

    public static final long mediasoup_missed_time = 1 * 60 * 1000 + 30 * 1000;
    public static final long answered_missed_time = 1 * 60 * 1000;
    public static final long mediasoup_connect_wait = 30 * 1000;
    public static final long mediasoup_delayed_check = 3 * 1000;
    public static final long mediasoup_duration_timing = 1000;

    public static final String key_intent_roommode = "room_mode";
    public static final int roommode_video = 1;
    public static final int roommode_audio = 2;
    public static final int roommode_see = 3;
    public static final int roommode_mute = 4;
    public static final int roommode_noall = 5;
    //悬浮窗时音频模式
    public final static String SHOW_FLOAT_WINDOW_MODEO = "fw_audio_mode";

    public static final String key_shared_proxy_host = "key_proxy_host";
    public static final String key_shared_proxy_port = "key_proxy_port";

    public static final String key_service_join = "key_service_join";

    public static final String key_msg_callstate = "call_state";
    public static final String key_msg_calltype = "call_type";
    public static final String key_msg_convtype = "conv_type";
    public static final String key_msg_shouldring = "should_ring";
    public static final String key_msg_membercount = "member_count";
    public static final String key_msg_p2pdata = "p2pdata";
    public static final String key_msg_to = "to";
    public static final String key_msg_to_user = "user_id";
    public static final String key_msg_to_client = "client_id";

    public static final boolean msg_shouldring = true;
    public static final int CAPTURE_PERMISSION_REQUEST_CODE = 0x0021;
    public static Intent mediaProjectionPermissionResultData;
    public static int mediaProjectionPermissionResultCode;

    public static String extraVideoFileAsCamera;

    /**
     * 接受状态
     * SelfCalling   0
     * OtherCalling  1
     * SelfJoining   2
     * SelfConnected 3
     * Ongoing       4
     * Terminating   5
     * Ended         6
     */
    public enum MeidasoupState {
        SelfCalling(0), OtherCalling(1), SelfJoining(2), SelfConnected(3), Ongoing(4), Terminating(5), Ended(6);

        private int index = 0;

        private MeidasoupState(int index) {     //必须是private的，否则编译错误
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static MeidasoupState fromOrdinalType(int index) {
            switch (index) {
                case 0:
                    return SelfCalling;
                case 1:
                    return OtherCalling;
                case 2:
                    return SelfJoining;
                case 3:
                    return SelfConnected;
                case 4:
                    return Ongoing;
                case 5:
                    return Terminating;
                case 6:
                    return Ended;
                default:
                    return SelfCalling;
            }
        }

        public static int fromTypeOrdinal(MeidasoupState type) {
            return null == type ? 0 : type.getIndex();
        }
    }

    /**
     * 呼叫状态
     * 0发起 邀请，
     * 1接受，
     * 2拒绝，
     * 3结束，
     * 4取消 ,
     * 5 未响应,
     * 6 忙碌中
     */
    public enum CallState {
        Started(0), Accepted(1), Rejected(2), Ended(3), Canceled(4), Missed(5), Busyed(6), P2POffer(7), P2PAnswer(8), P2PIce(9);
        private int index = 0;

        private CallState(int index) {     //必须是private的，否则编译错误
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static CallState fromOrdinalType(int index) {
            switch (index) {
                case 0:
                    return Started;
                case 1:
                    return Accepted;
                case 2:
                    return Rejected;
                case 3:
                    return Ended;
                case 4:
                    return Canceled;
                case 5:
                    return Missed;
                case 6:
                    return Busyed;
                case 7:
                    return P2POffer;
                case 8:
                    return P2PAnswer;
                case 9:
                    return P2PIce;
                default:
                    return Started;
            }
        }

        public static int fromTypeOrdinal(CallState type) {
            return null == type ? 0 : type.getIndex();
        }
    }

    /**
     * 发起的类型
     * WCALL_CONV_TYPE_ONEONONE = 0
     * WCALL_CONV_TYPE_GROUP = 1
     * WCALL_CONV_TYPE_CONFERENCE = 2
     */
    public enum ConvType {
        OneOnOne(0), Group(1), Conference(2);
        private int index = 0;

        private ConvType(int index) {     //必须是private的，否则编译错误
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static ConvType fromOrdinalType(int index) {
            switch (index) {
                case 0:
                    return OneOnOne;
                case 1:
                    return Group;
                case 2:
                    return Conference;
                default:
                    return OneOnOne;
            }
        }

        public static int fromTypeOrdinal(ConvType type) {
            return null == type ? 0 : type.getIndex();
        }
    }

    /**
     * 呼叫类型
     * WCALL_CALL_TYPE_NORMAL = 0
     * WCALL_CALL_TYPE_VIDEO = 1
     * WCALL_CALL_TYPE_FORCED_AUDIO = 2
     */
    public enum CallType {
        Normal(0), Video(1), ForcedAudio(2);
        private int index = 0;

        private CallType(int index) {     //必须是private的，否则编译错误
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static CallType fromOrdinalType(int index) {
            switch (index) {
                case 0:
                    return Normal;
                case 1:
                    return Video;
                case 2:
                    return ForcedAudio;
                default:
                    return Normal;
            }
        }

        public static int fromTypeOrdinal(CallType type) {
            return null == type ? 0 : type.getIndex();
        }
    }

    /**
     * 视频状态
     * WCALL_VIDEO_STATE_STOPPED           0
     * WCALL_VIDEO_STATE_STARTED           1
     * WCALL_VIDEO_STATE_BAD_CONN          2
     * WCALL_VIDEO_STATE_PAUSED            3
     * WCALL_VIDEO_STATE_SCREENSHARE       4
     * NoCameraPermission - internal state 5
     * Unknown - internal state            6
     */
    public enum VideoState {
        Stopped(0), Started(1), BadConnection(2), Paused(3), ScreenShare(4), NoCameraPermission(5), Unknown(6);
        private int index = 0;

        private VideoState(int index) {     //必须是private的，否则编译错误
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static VideoState fromOrdinalType(int index) {
            switch (index) {
                case 0:
                    return Stopped;
                case 1:
                    return Started;
                case 2:
                    return BadConnection;
                case 3:
                    return Paused;
                case 4:
                    return ScreenShare;
                case 5:
                    return NoCameraPermission;
                case 6:
                    return Unknown;
                default:
                    return Stopped;
            }
        }

        public static int fromTypeOrdinal(VideoState type) {
            return null == type ? 0 : type.getIndex();
        }
    }

    /**
     * 关闭原因
     * val Normal             = 0
     * val Error              = 1
     * val Timeout            = 2
     * val LostMedia          = 3
     * val Canceled           = 4
     * val AnsweredElsewhere  = 5
     * val IOError            = 6
     * val StillOngoing       = 7
     * val TimeoutEconn       = 8
     * val DataChannel        = 9
     * val Rejected           = 10
     */
    public enum ClosedReason {
        Normal(0), Error(1), Timeout(2), LostMedia(3), Canceled(4), AnsweredElsewhere(5), IOError(6), StillOngoing(7), TimeoutEconn(8), DataChannel(9), Rejected(10);
        private int index = 0;

        private ClosedReason(int index) {     //必须是private的，否则编译错误
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static ClosedReason fromOrdinalType(int index) {
            switch (index) {
                case 0:
                    return Normal;
                case 1:
                    return Error;
                case 2:
                    return Timeout;
                case 3:
                    return LostMedia;
                case 4:
                    return Canceled;
                case 5:
                    return AnsweredElsewhere;
                case 6:
                    return IOError;
                case 7:
                    return StillOngoing;
                case 8:
                    return TimeoutEconn;
                case 9:
                    return DataChannel;
                case 10:
                    return Rejected;
                default:
                    return Normal;
            }
        }

        public static int fromTypeOrdinal(ClosedReason type) {
            return null == type ? 0 : type.getIndex();
        }
    }

    public enum NetworkMode {
        _2G(0),
        EDGE(1), //A.K.A 2.5G
        _3G(2),
        _4G(3),
        _5G(4),
        WIFI(5),
        OFFLINE(6),
        UNKNOWN(7);
        private int index = 0;

        private NetworkMode(int index) {     //必须是private的，否则编译错误
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static NetworkMode fromOrdinalType(int index) {
            switch (index) {
                case 0:
                    return _2G;
                case 1:
                    return EDGE;
                case 2:
                    return _3G;
                case 3:
                    return _4G;
                case 4:
                    return _5G;
                case 5:
                    return WIFI;
                case 6:
                    return OFFLINE;
                case 7:
                    return UNKNOWN;
                default:
                    return UNKNOWN;
            }
        }

        public static int fromTypeOrdinal(NetworkMode type) {
            return null == type ? 0 : type.getIndex();
        }
    }

    public static boolean isAvailableNetwork(NetworkMode networkMode) {
        return networkMode == NetworkMode._2G
            || networkMode == NetworkMode.EDGE
            || networkMode == NetworkMode._3G
            || networkMode == NetworkMode._4G
            || networkMode == NetworkMode._5G
            || networkMode == NetworkMode.WIFI;

    }
}

