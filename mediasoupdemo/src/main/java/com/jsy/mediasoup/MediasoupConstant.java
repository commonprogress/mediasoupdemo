package com.jsy.mediasoup;

/**
 * 常量配置类
 */
public class MediasoupConstant {
    public static final String mediasoup_version = "1.0";

    public static final long mediasoup_missed_time = 3 * 60 * 1000 + 30 * 1000;

    public static final String key_intent_roommode = "room_mode";
    public static final int roommode_video = 1;
    public static final int roommode_audio = 2;
    public static final int roommode_see = 3;
    public static final int roommode_mute = 4;
    public static final int roommode_noall = 5;


    public static final String key_msg_callstate = "call_state";
    public static final String key_msg_calltype = "call_type";
    public static final String key_msg_convtype = "conv_type";
    public static final String key_msg_shouldring = "should_ring";


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
        SelfCalling, OtherCalling, SelfJoining, SelfConnected, Ongoing, Terminating, Ended;

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static MeidasoupState fromOrdinalType(int index) {
            for (MeidasoupState type : MeidasoupState.values()) {
                if (type.ordinal() == index) {
                    return type;
                }
            }
            return SelfCalling;
        }

        public static int fromTypeOrdinal(MeidasoupState type) {
            return null == type ? 0 : type.ordinal();
        }
    }

    /**
     * 呼叫状态
     * 0发起 邀请，
     * 1接受，
     * 2拒绝，
     * 3结束，
     * 4取消 ,
     * 5 未响应
     */
    public enum CallState {
        Started, Accepted, Rejected, Ended, Canceled, Missed;

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static CallState fromOrdinalType(int index) {
            for (CallState type : CallState.values()) {
                if (type.ordinal() == index) {
                    return type;
                }
            }
            return Started;
        }

        public static int fromTypeOrdinal(CallState type) {
            return null == type ? 0 : type.ordinal();
        }
    }

    /**
     * 发起的类型
     * WCALL_CONV_TYPE_ONEONONE = 0
     * WCALL_CONV_TYPE_GROUP = 1
     * WCALL_CONV_TYPE_CONFERENCE = 2
     */
    public enum ConvType {
        OneOnOne, Group, Conference;

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static ConvType fromOrdinalType(int index) {
            for (ConvType type : ConvType.values()) {
                if (type.ordinal() == index) {
                    return type;
                }
            }
            return OneOnOne;
        }

        public static int fromTypeOrdinal(ConvType type) {
            return null == type ? 0 : type.ordinal();
        }
    }

    /**
     * 呼叫类型
     * WCALL_CALL_TYPE_NORMAL = 0
     * WCALL_CALL_TYPE_VIDEO = 1
     * WCALL_CALL_TYPE_FORCED_AUDIO = 2
     */
    public enum CallType {
        Normal, Video, ForcedAudio;

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static CallType fromOrdinalType(int index) {
            for (CallType type : CallType.values()) {
                if (type.ordinal() == index) {
                    return type;
                }
            }
            return Normal;
        }

        public static int fromTypeOrdinal(CallType type) {
            return null == type ? 0 : type.ordinal();
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
        Stopped, Started, BadConnection, Paused, ScreenShare, NoCameraPermission, Unknown;

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static VideoState fromOrdinalType(int index) {
            for (VideoState type : VideoState.values()) {
                if (type.ordinal() == index) {
                    return type;
                }
            }
            return Stopped;
        }

        public static int fromTypeOrdinal(VideoState type) {
            return null == type ? 0 : type.ordinal();
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
        Normal, Error, Timeout, LostMedia, Canceled, AnsweredElsewhere, IOError, StillOngoing, TimeoutEconn, DataChannel, Rejected;

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static ClosedReason fromOrdinalType(int index) {
            for (ClosedReason type : ClosedReason.values()) {
                if (type.ordinal() == index) {
                    return type;
                }
            }
            return Normal;
        }

        public static int fromTypeOrdinal(ClosedReason type) {
            return null == type ? 0 : type.ordinal();
        }
    }

}

