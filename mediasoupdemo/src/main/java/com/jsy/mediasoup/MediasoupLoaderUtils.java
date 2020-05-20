package com.jsy.mediasoup;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.jsy.mediasoup.services.MediasoupService;
import com.jsy.mediasoup.utils.LogUtils;
import com.jsy.mediasoup.vm.RoomProps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.MediasoupClient;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Info;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.model.Peers;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MediasoupLoader 加载数据中转
 */
public class MediasoupLoaderUtils {
    private static final String TAG = MediasoupLoaderUtils.class.getSimpleName();
    private static volatile MediasoupLoaderUtils instance;
    private boolean isInitMediasoup;
    private boolean isCreateMediasoup;
    private String curRConvId;
    private String curUserId;
    private String curClientId;
    private String curDisplayName;
    private MediasoupManagement.MediasoupHandler mediasoupHandler;
    private MediasoupManagement.UserChangedHandler userChangedHandler;

    private MediasoupConstant.CallType mediasoupCallType;
    private MediasoupConstant.ConvType mediasoupConvType;
    private boolean isShouldRing = false;

    private MediasoupConstant.MeidasoupState meidasoupStart;
    private MediasoupConstant.MeidasoupState meidasoupTimely;
    private String incomingUserId;
    private String incomingClientId;
    private String incomingDisplayName;
    private long incomingMsgTime;

    private RoomClient mRoomClient;
    private RoomStore mRoomStore;
    private RoomOptions mRoomOptions;
    private RoomManagement roomManagement;

    public static MediasoupLoaderUtils getInstance() {
        if (null == instance) {
            synchronized (MediasoupLoaderUtils.class) {
                if (null == instance) {
                    instance = new MediasoupLoaderUtils();
                }
            }
        }
        return instance;
    }

    public MediasoupLoaderUtils() {

    }

    public void setInstanceNull() {
        this.isInitMediasoup = false;
        this.isCreateMediasoup = false;
        curUserId = "";
        curClientId = "";
        curDisplayName = "";
        mediasoupHandler = null;
        userChangedHandler = null;
        mediasoupDestroy();
        instance = null;
    }

    public void mediasoupDestroy() {
        mediasoupClose();
        roomManagement = null;
        mRoomClient = null;
        mRoomStore = null;
        mRoomOptions = null;
    }

    private void mediasoupClose() {
        if (null == roomManagement || !roomManagement.isRoomConnecting()) {
            LogUtils.e(TAG, "mediasoupClose 可以重置 ,hashCode:" + this.hashCode());
            curRConvId = "";
            meidasoupStart = null;
            meidasoupTimely = null;
            incomingUserId = "";
            incomingClientId = "";
            incomingDisplayName = "";
            incomingMsgTime = 0L;
            mediasoupConvType = null;
            mediasoupCallType = null;
            isShouldRing = false;
            curCameraFace = "";
            clearAllVideoState();
        } else {
            LogUtils.e(TAG, "mediasoupClose 连接中 不可以重置 ,hashCode:" + this.hashCode());
        }
    }

    public void setRoomManagement(RoomManagement roomManagement) {
        this.roomManagement = roomManagement;
    }

    public void mediasoupInit(Context context) {
        LogUtils.i(TAG, "mediasoupInit:" + this.hashCode());
        Logger.setLogLevel(Logger.LogLevel.LOG_TRACE);
        Logger.setDefaultHandler();
        MediasoupClient.initialize(context.getApplicationContext());
        this.isInitMediasoup = true;
    }

    public String libraryVersion() {
//        return MediasoupConstant.mediasoup_version;
        return String.valueOf(MediasoupClient.version());
    }

    public boolean mediasoupCreate(Context context,
                                   String userId,
                                   String clientId,
                                   String displayName,
                                   MediasoupManagement.MediasoupHandler mediasoupH) {
        LogUtils.i(TAG, "mediasoupCreate displayName:" + displayName + ",userId:" + userId + ",hashCode:" + this.hashCode());
        if (!this.isInitMediasoup()) {
            mediasoupInit(context);
        }
        this.curUserId = userId;
        this.curClientId = clientId;
        this.curDisplayName = displayName;
        this.mediasoupHandler = mediasoupH;
        this.isCreateMediasoup = true;
        return isCreateMediasoup && isInitMediasoup();
    }

    public void setUserChangedHandler(MediasoupManagement.UserChangedHandler userChangedHandler) {
        this.userChangedHandler = userChangedHandler;
    }

    /**
     * 接收到邀请
     *
     * @param context
     * @param msg
     * @param curTime
     * @param msgTime
     * @param rConvId
     * @param userId
     * @param clientId
     */
    public void receiveCallMessage(Context context,
                                   String msg,
                                   long curTime,
                                   long msgTime,
                                   String rConvId,
                                   String userId,
                                   String clientId,
                                   boolean isMediasoup) {
        LogUtils.i(TAG, "receiveCallMessage 接收到邀请 isMediasoup:" + isMediasoup + " ,curTime:" + curTime + ", msgTime:" + msgTime + ", rConvId:" + rConvId + "，getCurRConvId()：" + getCurRConvId() + " ,userId:" + userId + ", clientId:" + clientId + ", mediasoupmsg:" + msg + ", isInitMediasoup:" + isInitMediasoup() + ", isCreateMediasoup:" + isCreateMediasoup + ",hashCode:" + this.hashCode());
        if (!isMediasoup) {
            //不是Mediasoup消息
//            closedMediasoup(MediasoupConstant.ClosedReason.DataChannel, rConvId, msgTime, userId);//接收的不是Mediasoup消息
            return;
        }
        if (!this.isInitMediasoup()) {
            mediasoupInit(context);
        }
        try {
            JSONObject jsonObject = TextUtils.isEmpty(msg) ? null : new JSONObject(msg);
            if (null == jsonObject) {
                closedMediasoup(MediasoupConstant.ClosedReason.IOError, rConvId, msgTime, userId);//接收的消息为空
            } else {
                boolean isRoomConnecting = isRoomConnecting();
                boolean isSameConv = !TextUtils.isEmpty(getCurRConvId()) && getCurRConvId().equalsIgnoreCase(rConvId);
                MediasoupConstant.CallState callState = MediasoupConstant.CallState.fromOrdinalType(jsonObject.optInt(MediasoupConstant.key_msg_callstate, -1));
                if (callState == MediasoupConstant.CallState.Started) {//0发起的邀请，
                    if (isRoomConnecting) {
                        if (!isSameConv) {
                            //连接中 其他通话进来
                            sendMediasoupMsg(MediasoupConstant.CallState.Canceled, rConvId);//其他通话进行中
                            closedMediasoup(MediasoupConstant.ClosedReason.AnsweredElsewhere, rConvId, msgTime, userId);//其他通话进行中
                        } else {
                            sendMediasoupMsg(MediasoupConstant.CallState.Accepted, rConvId);//当前会话通话进行中
                        }
                    } else {
                        if (curTime - msgTime > MediasoupConstant.mediasoup_missed_time) {
                            //时间已经超时了
                            missedJoinMediasoup();
                            closedMediasoup(MediasoupConstant.ClosedReason.Timeout, rConvId, msgTime, userId);//超时的消息
                        } else {
                            boolean isSameUserId = !TextUtils.isEmpty(getCurUserId()) && getCurUserId().equalsIgnoreCase(userId);
                            if (isSameUserId) {
                                //发送和接收同一个用户
                                boolean isSameClientId = !TextUtils.isEmpty(getCurClientId()) && getCurClientId().equalsIgnoreCase(clientId);
                                if (!isSameClientId) {
                                    //不同的ClientId 收到即结束
                                    closedMediasoup(MediasoupConstant.ClosedReason.AnsweredElsewhere, rConvId, msgTime, userId);//自己账号发起的
                                }
                            } else {
                                //正常响应邀请
                                this.curRConvId = rConvId;
                                this.incomingUserId = userId;
                                this.incomingClientId = clientId;
                                this.incomingDisplayName = "";
                                this.incomingMsgTime = msgTime;
                                this.mediasoupCallType = MediasoupConstant.CallType.fromOrdinalType(jsonObject.optInt(MediasoupConstant.key_msg_calltype, -1));//0音频，1视频，2强制音频
                                this.mediasoupConvType = MediasoupConstant.ConvType.fromOrdinalType(jsonObject.optInt(MediasoupConstant.key_msg_convtype, -1));//0一对一模式，1群聊模式，2会议模式
                                this.isShouldRing = jsonObject.optBoolean(MediasoupConstant.key_msg_shouldring, true);
                                this.meidasoupStart = MediasoupConstant.MeidasoupState.OtherCalling;
                                this.meidasoupTimely = meidasoupStart;
                                incomingJoinMediasoup(rConvId, msgTime, userId, isVideoIncoming(), isShouldRing);
                            }
                        }
                    }
                } else if (callState == MediasoupConstant.CallState.Accepted) {//1接受
                    if (!isSameConv) {
                        closedMediasoup(MediasoupConstant.ClosedReason.AnsweredElsewhere, rConvId, msgTime, userId);//发起和接收不是同一会话
                    } else {
                        boolean isSameUserId = !TextUtils.isEmpty(getCurUserId()) && getCurUserId().equalsIgnoreCase(userId);
                        if (isSameUserId) {
                            boolean isSameClientId = !TextUtils.isEmpty(getCurClientId()) && getCurClientId().equalsIgnoreCase(clientId);
                            if (!isSameClientId) {
                                //接受的用户和当前设备的用户同一个 设备不同
                                if (null != roomManagement) {
                                    roomManagement.onSelfOtherAcceptCall();
                                } else {
                                    closedMediasoup(MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//接收时 自己已经取消
                                }
                            }
                        } else {
                            if (null != roomManagement) {
                                roomManagement.onOtherAcceptCall();
                            } else {
                                if (isOneOnOneCall()) {
                                    closedMediasoup(MediasoupConstant.ClosedReason.Canceled, rConvId, msgTime, userId);//接收时 自己已经取消
                                }
                            }
                        }
                    }
                } else if (callState == MediasoupConstant.CallState.Rejected) {//，2拒绝，
                    if (!isSameConv) {
                        closedMediasoup(MediasoupConstant.ClosedReason.Rejected, rConvId, msgTime, userId);//发起和拒绝不是同一会话
                    } else {
                        boolean isSameUserId = !TextUtils.isEmpty(getCurUserId()) && getCurUserId().equalsIgnoreCase(userId);
                        if (isSameUserId) {
                            boolean isSameClientId = !TextUtils.isEmpty(getCurClientId()) && getCurClientId().equalsIgnoreCase(clientId);
                            if (!isSameClientId) {
                                //接受的用户和当前设备的用户同一个 设备不同

                            }
                        } else {
                            if (null != roomManagement) {
                                roomManagement.onOtherRejectCall();
                            } else {
                                if (isOneOnOneCall()) {
                                    closedMediasoup(MediasoupConstant.ClosedReason.Canceled, rConvId, msgTime, userId);//拒绝时 自己已经取消
                                }
                            }
                        }
                    }
                } else if (callState == MediasoupConstant.CallState.Ended) {//3结束，
                    if (!isSameConv) {
                        closedMediasoup(MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//发起和结束不是同一会话
                    } else {
                        boolean isSameUserId = !TextUtils.isEmpty(getCurUserId()) && getCurUserId().equalsIgnoreCase(userId);
                        if (isSameUserId) {//理论不存在处理这个情况
                            boolean isSameClientId = !TextUtils.isEmpty(getCurClientId()) && getCurClientId().equalsIgnoreCase(clientId);
                            if (!isSameClientId) {
                                //接受的用户和当前设备的用户同一个 设备不同

                            }
                        } else {
                            if (null != roomManagement) {
                                roomManagement.onOtherEndCall();
                            } else {
                                if (isOneOnOneCall()) {
                                    closedMediasoup(MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//结束时 自己已经取消
                                }
                            }
                        }
                    }
                } else if (callState == MediasoupConstant.CallState.Canceled) {//4 取消
                    if (!isSameConv) {
                        closedMediasoup(MediasoupConstant.ClosedReason.Canceled, rConvId, msgTime, userId);//发起和取消不是同一会话
                    } else {
                        boolean isSameUserId = !TextUtils.isEmpty(getCurUserId()) && getCurUserId().equalsIgnoreCase(userId);
                        if (isSameUserId) {//理论不存在处理这个情况
                            boolean isSameClientId = !TextUtils.isEmpty(getCurClientId()) && getCurClientId().equalsIgnoreCase(clientId);
                            if (!isSameClientId) {
                                //接受的用户和当前设备的用户同一个 设备不同

                            }
                        } else {
                            if (null != roomManagement) {
                                roomManagement.onOtherCloseCall();
                            } else {
//                        if (!isGroupConv()) {
                                closedMediasoup(MediasoupConstant.ClosedReason.Canceled, rConvId, msgTime, userId);//还没有接受，已经取消
//                        }
                            }
                        }
                    }
                } else if (callState == MediasoupConstant.CallState.Missed) {//,5 未响应
                    if (!isSameConv) {
                        closedMediasoup(MediasoupConstant.ClosedReason.TimeoutEconn, rConvId, msgTime, userId);//发起和未响应不是同一会话
                    } else {
                        boolean isSameUserId = !TextUtils.isEmpty(getCurUserId()) && getCurUserId().equalsIgnoreCase(userId);
                        if (isSameUserId) {
                            boolean isSameClientId = !TextUtils.isEmpty(getCurClientId()) && getCurClientId().equalsIgnoreCase(clientId);
                            if (!isSameClientId) {
                                //接受的用户和当前设备的用户同一个 设备不同

                            }
                        } else {
                            if (null != roomManagement) {
                                roomManagement.onOtherMissedCall();
                            } else {
                                if (isOneOnOneCall()) {
                                    closedMediasoup(MediasoupConstant.ClosedReason.TimeoutEconn, rConvId, msgTime, userId);//有一方未响应
                                }
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            closedMediasoup(MediasoupConstant.ClosedReason.IOError, rConvId, msgTime, userId);//接收的消息为空 出现异常
        }
    }

    /**
     * 发起通话
     *
     * @param context
     * @param rConvId
     * @param callType WCALL_CALL_TYPE_NORMAL = 0 WCALL_CALL_TYPE_VIDEO = 1 WCALL_CALL_TYPE_FORCED_AUDIO = 2
     * @param convType WCALL_CONV_TYPE_ONEONONE = 0  WCALL_CONV_TYPE_GROUP = 1 WCALL_CONV_TYPE_CONFERENCE = 2
     * @param audioCbr 是否固定频率音频
     * @return
     */
    public int mediasoupStartCall(Context context, String rConvId, int callType, int convType, boolean audioCbr) {
        LogUtils.i(TAG, "mediasoupStartCall 发起通话 rConvId:" + rConvId + ", callType:" + callType + ", convType:" + convType + ", audioCbr:" + audioCbr + ", hashCode:" + this.hashCode());
        this.curRConvId = rConvId;
        this.meidasoupStart = MediasoupConstant.MeidasoupState.SelfCalling;
        this.meidasoupTimely = meidasoupStart;
        this.mediasoupCallType = MediasoupConstant.CallType.fromOrdinalType(callType);
        this.mediasoupConvType = MediasoupConstant.ConvType.fromOrdinalType(convType);
//        this.curAudioCbr = audioCbr;
        mediasoupStart(context);
        return 0;
    }

    /**
     * 响应通话 可能邀请的响应 也有可能从状态从后台在前台响应
     *
     * @param context
     * @param rConvId
     * @param callType       WCALL_CALL_TYPE_NORMAL = 0 WCALL_CALL_TYPE_VIDEO = 1 WCALL_CALL_TYPE_FORCED_AUDIO = 2
     * @param convType       WCALL_CONV_TYPE_ONEONONE = 0  WCALL_CONV_TYPE_GROUP = 1 WCALL_CONV_TYPE_CONFERENCE = 2
     * @param meidasoupState SelfCalling   0    OtherCalling  1   SelfJoining   2    SelfConnected 3     Ongoing       4   Terminating   5     Ended         6
     * @param audioCbr       是否固定频率音频
     */
    public void mediasoupAnswerCall(Context context, String rConvId, int callType, int convType, int meidasoupState, boolean audioCbr) {
        LogUtils.i(TAG, "mediasoupAnswerCall 响应通话 rConvId:" + rConvId + ", callType:" + callType + ", convType:" + convType + ",meidasoupState:" + meidasoupState + ", audioCbr:" + audioCbr + ", hashCode:" + this.hashCode());
//        this.curAudioCbr = audioCbr;
        this.mediasoupCallType = MediasoupConstant.CallType.fromOrdinalType(callType);
        this.mediasoupConvType = MediasoupConstant.ConvType.fromOrdinalType(convType);
        this.meidasoupStart = MediasoupConstant.MeidasoupState.fromOrdinalType(meidasoupState);
        this.meidasoupTimely = meidasoupStart;
        if (null != roomManagement && roomManagement.isVisibleCall() && !TextUtils.isEmpty(rConvId) && rConvId.equals(getCurRConvId())) {
            roomManagement.setSelfAcceptOrJoin();
        } else {
            this.curRConvId = rConvId;
            mediasoupStart(context);
        }
    }

    public void onNetworkChanged() {
        LogUtils.i(TAG, "onNetworkChanged:" + this.hashCode());
        if (null != roomManagement) {
            roomManagement.onNetworkChanged();
        }
    }

    public void onHttpResponse(int status, String reason) {
        LogUtils.i(TAG, "onHttpResponse: status:" + status + ", reason:" + reason + ", hashCode:" + this.hashCode());
    }

    public void onConfigRequest(int error, String json) {
        LogUtils.i(TAG, "onConfigRequest: error:" + error + ", json:" + json + ", hashCode:" + this.hashCode());
    }

    /**
     * @param rConvId
     * @param meidasoupState SelfCalling   0    OtherCalling  1   SelfJoining   2    SelfConnected 3     Ongoing       4   Terminating   5     Ended         6
     */
    public void endMediasoupCall(String rConvId, int meidasoupState) {
        LogUtils.i(TAG, "endMediasoupCall: rConvId:" + rConvId + ",meidasoupState:" + meidasoupState + ", hashCode:" + this.hashCode());
//        this.meidasoupTimely = MeidasoupState.fromTypeOrdinal(meidasoupState);
        if (!TextUtils.isEmpty(rConvId) && rConvId.equals(getCurRConvId())) {
            if (null != roomManagement) {
                roomManagement.callSelfEnd();
            }
        }
    }

    /**
     * @param rConvId
     * @param meidasoupState SelfCalling   0    OtherCalling  1   SelfJoining   2    SelfConnected 3     Ongoing       4   Terminating   5     Ended         6
     */
    public void rejectMediasoupCall(String rConvId, int meidasoupState) {
        LogUtils.i(TAG, "rejectMediasoupCall: rConvId:" + rConvId + ",meidasoupState:" + meidasoupState + ", hashCode:" + this.hashCode());
//        this.meidasoupTimely = MeidasoupState.fromTypeOrdinal(meidasoupState);
        if (!TextUtils.isEmpty(rConvId) && rConvId.equals(getCurRConvId())) {
            if (null != roomManagement) {
                roomManagement.callSelfReject();
            }
        }
    }

    /**
     * @param rConvId
     * @param state   WCALL_VIDEO_STATE_STOPPED           0
     *                WCALL_VIDEO_STATE_STARTED           1
     *                WCALL_VIDEO_STATE_BAD_CONN          2
     *                WCALL_VIDEO_STATE_PAUSED            3
     *                WCALL_VIDEO_STATE_SCREENSHARE       4
     *                NoCameraPermission - internal state 5
     *                Unknown - internal state            6
     */
    public void setVideoSendState(String rConvId, int state) {
        LogUtils.i(TAG, "setVideoSendState: rConvId:" + rConvId + ", state:" + state + ", hashCode:" + this.hashCode());
        if (!TextUtils.isEmpty(rConvId) && rConvId.equals(getCurRConvId())) {
            if (null != roomManagement) {
                MediasoupConstant.VideoState videoState = MediasoupConstant.VideoState.fromOrdinalType(state);
                roomManagement.disAndEnableCam(videoState == MediasoupConstant.VideoState.Started);
            }
        }
    }

    public void setCallMuted(boolean muted) {
        LogUtils.i(TAG, "setCallMuted: muted:" + muted + ", hashCode:" + this.hashCode());
        if (null != roomManagement) {
            roomManagement.setCallMuted(muted);
        }
    }

    public void switchCam() {
        LogUtils.i(TAG, "switchCam: hashCode:" + this.hashCode());
        if (null != roomManagement) {
            roomManagement.switchCam();
        }
    }

    public void setMediasoupProxy(String host, int port) {
        LogUtils.i(TAG, "setMediasoupProxy: host:" + host + ", port:" + port + ", hashCode:" + this.hashCode());
    }

    /**
     * 当前会话id
     *
     * @return
     */
    public String getCurRConvId() {
        return curRConvId;
    }

    public String getCurUserId() {
        if (TextUtils.isEmpty(curUserId) && isInitMediasoup() && null != mediasoupHandler) {
            return mediasoupHandler.getCurAccountId();
        }
        return curUserId;
    }

    public String getCurClientId() {
        if (TextUtils.isEmpty(curClientId) && isInitMediasoup() && null != mediasoupHandler) {
            return mediasoupHandler.getCurClientId();
        }
        return curClientId;
    }

    public String getDisplayName() {
        if (TextUtils.isEmpty(curDisplayName) && isInitMediasoup() && null != mediasoupHandler) {
            return mediasoupHandler.getCurDisplayName();
        }
        return curDisplayName;
    }

    /**
     * 是否初始化 Mediasoup
     *
     * @return
     */
    public boolean isInitMediasoup() {
        return isInitMediasoup;
    }

    /**
     * 是否邀请加入
     *
     * @return
     */
    public boolean isReceiveCall() {
        return null == meidasoupStart ? false : (meidasoupStart == MediasoupConstant.MeidasoupState.OtherCalling);
    }

    /**
     * 是否发起方
     *
     * @return
     */
    public boolean isSelfCalling() {
        return null == meidasoupStart ? false : (meidasoupStart == MediasoupConstant.MeidasoupState.SelfCalling);
    }

    /**
     * 是否邀请的视频通话
     *
     * @return
     */
    public boolean isVideoIncoming() {
        return null == mediasoupCallType ? false : mediasoupCallType == MediasoupConstant.CallType.Video;
    }

    /**
     * 是否一对一视频
     *
     * @return
     */
    public boolean isOneOnOneCall() {
        return null == mediasoupConvType ? true : mediasoupConvType == MediasoupConstant.ConvType.OneOnOne;
    }

    /**
     * Mediasoup 是否初始化并创建
     *
     * @return
     */
    public boolean isInitAndCreate() {
        return isInitMediasoup() && isCreateMediasoup;
    }

    public RoomClient getRoomClient() {
        return mRoomClient;
    }

    public RoomStore getRoomStore() {
        return mRoomStore;
    }

    public RoomOptions getRoomOptions() {
        return mRoomOptions;
    }

    /**
     * 是否连接中
     *
     * @return
     */
    public boolean isRoomConnecting() {
        return null != roomManagement && roomManagement.isRoomConnecting();
    }

    public void setIncomingUser(String userId, String clientId, String displayName) {
        this.incomingUserId = userId;
        this.incomingClientId = clientId;
        this.incomingDisplayName = displayName;
    }

    public String getIncomingUserId() {
        return incomingUserId;
    }

    public String getIncomingClientId() {
        return incomingClientId;
    }

    public String getIncomingDisplayName() {
        return incomingDisplayName;
    }

    /**
     * 设置房间信息 是否都准备好
     *
     * @param roomClient
     * @param roomStore
     * @param roomOptions
     */
    public boolean setRoomClientStoreOptions(RoomClient roomClient, RoomStore roomStore, RoomOptions roomOptions) {
        this.mRoomClient = roomClient;
        this.mRoomStore = roomStore;
        this.mRoomOptions = roomOptions;
        boolean isReady = isMediasoupReady();
        if (null != mediasoupHandler) {
            mediasoupHandler.onReady(isReady);
        }
        return isReady;
    }

    public boolean isMediasoupReady() {
        boolean isReady = !TextUtils.isEmpty(getCurRConvId()) && !TextUtils.isEmpty(getCurUserId()) && !TextUtils.isEmpty(getCurClientId());
        if (isReady && null != mRoomClient && null != mRoomStore && null != mRoomOptions) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 发送secret 信令
     *
     * @param callState 0发起，1接受，2拒绝，3结束，4取消 5 未响应
     */
    public void sendMediasoupMsg(MediasoupConstant.CallState callState) {
        LogUtils.i(TAG, "sendMediasoupMsg: callState.name:" + callState.name() + ",callState.ordinal:" + callState.ordinal() + ", hashCode:" + this.hashCode());
        sendMediasoupMsg(callState, getCurRConvId());
    }

    /**
     * 发送secret 信令
     *
     * @param callState 0发起，1接受，2拒绝，3结束，4取消 5 未响应
     */
    public void sendMediasoupMsg(MediasoupConstant.CallState callState, String rConvId) {
        LogUtils.i(TAG, "startJoinMediasoup: callState.name:" + callState.name() + ",callState.ordinal:" + callState.ordinal() + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate()) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(MediasoupConstant.key_msg_callstate, MediasoupConstant.CallState.fromTypeOrdinal(callState));
            if (callState == MediasoupConstant.CallState.Started) {
                jsonObject.put(MediasoupConstant.key_msg_calltype, MediasoupConstant.CallType.fromTypeOrdinal(mediasoupCallType));
                jsonObject.put(MediasoupConstant.key_msg_convtype, MediasoupConstant.ConvType.fromTypeOrdinal(mediasoupConvType));
//                jsonObject.put(MediasoupConstant.key_msg_shouldring, true);
            }
            if (null != mediasoupHandler && !TextUtils.isEmpty(rConvId)) {
                mediasoupHandler.onSend(rConvId, getCurUserId(), getCurClientId(), getIncomingUserId(), getIncomingClientId(), jsonObject.toString(), true);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            closedMediasoup(MediasoupConstant.ClosedReason.IOError, rConvId, incomingMsgTime, getIncomingUserId());//发送消息出现异常
        }
    }

    /**
     * 加入邀请
     *
     * @param rConvId
     * @param msgTime
     * @param userId
     * @param video_call
     * @param should_ring
     */
    public void incomingJoinMediasoup(String rConvId, long msgTime, String userId, boolean video_call, boolean should_ring) {
        LogUtils.i(TAG, "incomingJoinMediasoup: rConvId:" + rConvId + ", msgTime:" + msgTime + ", userId:" + userId + ", video_call:" + video_call + ", should_ring:" + should_ring + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate() || !isReceiveCall()) {
            return;
        }
        if (null != mediasoupHandler && !TextUtils.isEmpty(rConvId)) {
            mediasoupHandler.onIncomingCall(rConvId, msgTime, userId, video_call, should_ring);
        }
    }

    /**
     * 加入邀请长时间未响应
     */
    public void missedJoinMediasoup() {
        LogUtils.i(TAG, "missedJoinMediasoup: hashCode:" + this.hashCode());
        if (!isInitAndCreate()) {
            return;
        }
        if (null != mediasoupHandler && !TextUtils.isEmpty(getCurRConvId())) {
            mediasoupHandler.onMissedCall(getCurRConvId(), incomingMsgTime, getIncomingUserId(), isVideoIncoming());
        }
    }

    /**
     * 响应了邀请
     */
    public void answeredJoinMediasoup() {
        LogUtils.i(TAG, "answeredJoinMediasoup: hashCode:" + this.hashCode());
        if (!isInitAndCreate()) {
            return;
        }
        if (null != mediasoupHandler && !TextUtils.isEmpty(getCurRConvId())) {
            mediasoupHandler.onAnsweredCall(getCurRConvId());
        }
    }

    /**
     * 建立通话
     */
    public void establishedJoinMediasoup() {
        LogUtils.i(TAG, "establishedJoinMediasoup: hashCode:" + this.hashCode());
        if (!isInitAndCreate()) {
            return;
        }
        if (null != mediasoupHandler && !TextUtils.isEmpty(getCurRConvId())) {
            mediasoupHandler.onEstablishedCall(getCurRConvId(), getIncomingUserId());
        }
    }

    /**
     * 关闭当前通话
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
    public void closedMediasoup(MediasoupConstant.ClosedReason closedReason) {
        LogUtils.i(TAG, "closedMediasoup: closedReason:" + closedReason + ", closedReason.name:" + closedReason.name() + ", hashCode:" + this.hashCode());
        closedMediasoup(closedReason, getCurRConvId(), incomingMsgTime, getIncomingUserId());
    }

    /**
     * 关闭指定通话
     *
     * @param closedReason
     * @param rConvId
     * @param msgTime
     * @param userId
     */
    public void closedMediasoup(MediasoupConstant.ClosedReason closedReason, String rConvId, long msgTime, String userId) {
        LogUtils.i(TAG, "closedMediasoup: closedReason.ordinal:" + closedReason.ordinal() + ", closedReason.name:" + closedReason.name() + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate()) {
            return;
        }
        if (null != mediasoupHandler && !TextUtils.isEmpty(rConvId)) {
            mediasoupHandler.onClosedCall(closedReason.ordinal(), rConvId, msgTime, userId);
        }
        mediasoupClose();
    }

    public void metricsReadyMediasoup(String metricsJson) {
        LogUtils.i(TAG, "metricsReadyMediasoup: metricsJson:" + metricsJson + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate()) {
            return;
        }
        if (null != mediasoupHandler) {
            mediasoupHandler.onMetricsReady(getCurRConvId(), metricsJson);
        }
    }

    public void configRequestMediasoup() {
        LogUtils.i(TAG, "configRequestMediasoup: hashCode:" + this.hashCode());
        if (!isInitAndCreate()) {
            return;
        }
        if (null != mediasoupHandler) {
            boolean isReady = !TextUtils.isEmpty(getCurRConvId()) && !TextUtils.isEmpty(getCurUserId()) && !TextUtils.isEmpty(getCurClientId());
            mediasoupHandler.onConfigRequest(isReady);
        }
    }

    public void onBitRateStateChanged(String userId, Boolean enabled) {
        LogUtils.i(TAG, "onBitRateStateChanged: userId:" + userId + ", enabled:" + enabled + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate()) {
            return;
        }
        if (null != mediasoupHandler) {
            mediasoupHandler.onBitRateStateChanged(userId, enabled);
        }
    }

    /**
     * WCALL_VIDEO_STATE_STOPPED           0
     * WCALL_VIDEO_STATE_STARTED           1
     * WCALL_VIDEO_STATE_BAD_CONN          2
     * WCALL_VIDEO_STATE_PAUSED            3
     * WCALL_VIDEO_STATE_SCREENSHARE       4
     * NoCameraPermission - internal state 5
     * Unknown - internal state            6
     *
     * @param userId
     * @param clientId
     * @param state
     */
    public void onVideoReceiveStateChanged(String userId, String clientId, String displayName, MediasoupConstant.VideoState state) {
//        LogUtils.i(TAG, "onVideoReceiveStateChanged: selfUserId:" + getCurUserId() + ", getCurRConvId()):" + getCurRConvId() + ", userId:" + userId + ", displayName:" + displayName + ", clientId:" + clientId + ", state.name:" + state.name() + ", state.ordinal:" + state.ordinal() + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate()) {
            return;
        }
        putPeerVideoState(userId, clientId, state);
        if (null != mediasoupHandler && !TextUtils.isEmpty(getCurRConvId())) {
            mediasoupHandler.onVideoReceiveStateChanged(getCurRConvId(), userId, clientId, state.ordinal());
        }
    }

    /**
     * 加入的状态
     *
     * @param state initial state.初始化
     *              NEW  1,
     *              connecting or reconnecting.连接或者重连中
     *              CONNECTING 2,
     *              connected.已经连接
     *              CONNECTED 3,
     *              mClosed.关闭
     *              CLOSED 4,
     */
    public void joinMediasoupState(int state) {
//        LogUtils.i(TAG, "joinMediasoupState: state:" + state + ", hashCode:" + this.hashCode());
//        if (!isInitAndCreate()) {
//            return;
//        }
//        if (null != mediasoupHandler) {
//            mediasoupHandler.joinMediasoupState(state);
//        }
    }

    /**
     * 拒绝 离开 取消
     *
     * @param
     */
    public void rejectEndCancelCall() {
        LogUtils.i(TAG, "rejectEndCancelCall: hashCode:" + this.hashCode());
        if (!isInitAndCreate()) {
            return;
        }
        if (null != mediasoupHandler) {
            mediasoupHandler.rejectEndCancelCall();
        }
    }

    /**
     * 加入房间用户变化
     *
     * @param jsonArray
     */
    public void mediasoupUserChanged(JSONArray jsonArray) throws JSONException {
        LogUtils.i(TAG, "mediasoupUserChanged: jsonArray:" + jsonArray.toString() + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate()) {
            return;
        }
        if (null != userChangedHandler && !TextUtils.isEmpty(getCurRConvId())) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("rconvid", getCurRConvId());
            jsonObject.put("peerusers", jsonArray);
            userChangedHandler.onUserChanged(getCurRConvId(), jsonObject.toString());
        }
    }

    public void mediasoupActivityCreate(Context context) {
        LogUtils.i(TAG, "mediasoupActivityCreate:" + this.hashCode());
        if (!this.isInitMediasoup()) {
            mediasoupInit(context);
        }
    }

    /**
     * 启动呼叫收听界面
     *
     * @param context
     */
    public void mediasoupStart(Context context) {
        LogUtils.i(TAG, "mediasoupStart:" + this.hashCode());
        if (!this.isInitMediasoup()) {
            mediasoupInit(context);
        }
        if (TextUtils.isEmpty(getCurRConvId())) {
            closedMediasoup(MediasoupConstant.ClosedReason.Error);//当前会话id为空
            rejectEndCancelCall();
            return;
        }
//        startMediasoupActivity(context);
    }

    /**
     * 启动 room界面
     *
     * @param context
     */
    public void startMediasoupActivity(Context context) {
        LogUtils.i(TAG, "startMediasoupActivity:");
        Intent intent = new Intent();
        if (context instanceof Application) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
//        intent.setClass(context, RoomConfigurationActivity.class);
        intent.setClass(context, RoomActivity.class);
        context.startActivity(intent);
    }

    /**
     * 启动服务
     *
     * @param context
     */
    public void startMediasoupService(Context context) {
        LogUtils.i(TAG, "startMediasoupService:");
        context.startService(new Intent(context, MediasoupService.class));
    }

    /**
     * 停止服务
     *
     * @param context
     */
    public void stopMediasoupService(Context context) {
        LogUtils.e(TAG, "stopMediasoupService:");
        context.stopService(new Intent(context, MediasoupService.class));
    }

    private String curCameraFace;//当前摄像头面向

    public void setCurCameraFace(String camera) {
        this.curCameraFace = camera;
    }

    public String getCurCameraFace() {
        return curCameraFace;
    }

    /**
     * peer 视频的状态集合
     */
    private Map<String, Boolean> peerVideoState = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * 获取当前房间除了自己之外所有用户
     *
     * @return
     */
    public List<Peer> getCurAllPeers() {
        return null == roomManagement ? null : roomManagement.getCurRoomPeerList();
    }

    /**
     * @return 获取视频的状态集合
     */
    public Map<String, Boolean> getPeerVideoState() {
        return peerVideoState;
    }

    /**
     * 清空所有视频的状态集合
     */
    public void clearAllVideoState() {
        peerVideoState.clear();
    }

    /**
     * 清空单个peer 视频状态
     *
     * @param userId
     * @param clientId
     */
    public void removePeerVideoState(String userId, String clientId) {
        if (!TextUtils.isEmpty(userId) && peerVideoState.containsKey(userId)) {
            peerVideoState.remove(userId);
        }
    }

    /**
     * 获取单个 peer 视频状态
     *
     * @param info
     * @return
     */
    public boolean getPeerVideoState(Info info) {
        LogUtils.i(TAG, "getPeerVideoState getCurUserId:" + getCurUserId() + ", userId:" + info.getId() + ", displayName:" + info.getDisplayName());
        if (null == roomManagement) {
            LogUtils.i(TAG, "getPeerVideoState null == roomManagement = true fanhui:false");
            peerVideoState.clear();
            return false;
        }
        if (peerVideoState.containsKey(info.getId())) {
            return peerVideoState.get(info.getId());
        } else {
            Peers peers = roomManagement.getCurRoomPeers();
            Peer peer = null == peers ? null : peers.getPeer(info.getId());
            LogUtils.i(TAG, "getPeerVideoState peerVideoState.containsKey(info.getId() = false, null == peer=" + (null == peer));
            if (null != peer) {
                return peer.isVideoVisible();
            } else {
                RoomProps roomProps = roomManagement.getRoomProps();
                LogUtils.i(TAG, "getPeerVideoState null == peer = true, null == roomProps" + (null == roomProps));
                if (null != roomProps) {
                    return !roomProps.getAudioOnly().get();
                }
                return false;
            }
        }
    }

    /**
     * 保存user视频状态
     *
     * @param userId
     * @param clientId
     * @param state
     */
    public void putPeerVideoState(String userId, String clientId, MediasoupConstant.VideoState state) {
//        LogUtils.i(TAG, "putPeerVideoState getCurUserId:" + getCurUserId() + ", userId:" + userId + ", state:" + state);
        peerVideoState.put(userId, state == MediasoupConstant.VideoState.Started);
    }

    public static void startMediasoupRoom(Context context) {
        LogUtils.i(TAG, "startMediasoupRoom:");
        MediasoupLoaderUtils.getInstance().mediasoupStart(context);
    }

    public static Intent getMediasoupIntent(Context context) {
        LogUtils.i(TAG, "getMediasoupIntent:");
        return new Intent(context, RoomActivity.class);
    }
}
