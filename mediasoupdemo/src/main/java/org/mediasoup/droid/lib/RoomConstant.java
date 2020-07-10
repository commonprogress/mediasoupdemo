package org.mediasoup.droid.lib;

/**
 *
 */
public class RoomConstant {

    /**
     * webrtc VideoCapturer 类型
     */
    public enum VideoCapturerType{
        CAMERA,//摄像头
        SCREEN,//共享屏幕
        FILE,//共享文件
    }
    /**
     * websocket连接状态 枚举
     */
    public enum ConnectionState {
        // initial state.初始化
        NEW(0),
        // connecting or reconnecting.连接或者重连中
        CONNECTING(1),
        // connected.已经连接
        CONNECTED(2),
        // disconnected and reconnecting.中断重连中
        DISCONNECTED(3),
        // mClosed.关闭
        CLOSED(4);

        private int index = 0;

        private ConnectionState(int index) {     //必须是private的，否则编译错误
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
        public static ConnectionState fromOrdinalState(int index) {
            switch (index) {
                case 0:
                    return NEW;
                case 1:
                    return CONNECTING;
                case 2:
                    return CONNECTED;
                case 3:
                    return DISCONNECTED;
                case 4:
                    return CLOSED;
                default:
                    return NEW;
            }
        }

        public static int fromTypeOrdinal(ConnectionState state) {
            return null == state ? 0 : state.getIndex();
        }
    }

    /**
     * Transport 连接状态
     * new,checking,disconnected,failed,connected,completed,closed
     */
    public enum TransportConnectionState {
        // initial state.初始化
        NEW(0, "new"),
        // connecting or reconnecting.连接或者重连中
        CHECKING(1, "checking"),
        // connected.已经连接
        DISCONNECTED(2, "disconnected"),
        // disconnected and reconnecting.中断重连中
        FAILED(3, "failed"),
        // connected.已经连接
        CONNECTED(4, "connected"),
        // disconnected and reconnecting.中断重连中
        COMPLETED(5, "completed"),
        // mClosed.关闭
        CLOSED(6, "closed");

        private int index = 0;
        private String name = "";

        private TransportConnectionState(int index, String name) {     //必须是private的，否则编译错误
            this.index = index;
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param index 类型ordinal
         */
        public static TransportConnectionState fromOrdinalType(int index) {
            switch (index) {
                case 0:
                    return NEW;
                case 1:
                    return CHECKING;
                case 2:
                    return DISCONNECTED;
                case 3:
                    return FAILED;
                case 4:
                    return CONNECTED;
                case 5:
                    return COMPLETED;
                case 6:
                    return CLOSED;
                default:
                    return NEW;
            }
        }

        /**
         * 根据类型的ordinal，返回类型的枚举实例。
         *
         * @param name 类型name
         */
        public static TransportConnectionState fromNameType(String name) {
            switch (name) {
                case "new":
                    return NEW;
                case "checking":
                    return CHECKING;
                case "disconnected":
                    return DISCONNECTED;
                case "failed":
                    return FAILED;
                case "connected":
                    return CONNECTED;
                case "completed":
                    return COMPLETED;
                case "closed":
                    return CLOSED;
                default:
                    return NEW;
            }
        }

        public static String fromTypeName(TransportConnectionState state) {
            return null == state ? "new" : state.getName();
        }

        public static int fromTypeOrdinal(TransportConnectionState state) {
            return null == state ? 0 : state.getIndex();
        }
    }

    public enum P2POtherState {
        VIDEO_RESUME,//视频播放
        VIDEO_PAUSE,//视频暂停
        AUDIO_RESUME,//音频播放
        AUDIO_PAUSE,//音频暂停
    }
}
