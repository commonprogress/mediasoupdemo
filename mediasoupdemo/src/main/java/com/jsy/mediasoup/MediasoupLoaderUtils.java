package com.jsy.mediasoup;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.jsy.mediasoup.services.MediasoupService;
import com.jsy.mediasoup.vm.MeProps;
import com.jsy.mediasoup.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.MediasoupClient;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.Utils;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.model.Peers;
import org.threeten.bp.Instant;

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
    private Context mContext;
    private boolean isInitMediasoup;//
    //
    private RoomClient mRoomClient;
    private RoomStore mRoomStore;
    private RoomOptions mRoomOptions;
    private RoomManagement roomManagement;

    private String lastRegister;
    private String curActiveId;
    private String curRegister;

    public String getCurRegister() {
        return Utils.isEmptyString(curRegister) ? getLastRegister() : curRegister;
    }

    public String getLastRegister() {
        return Utils.isEmptyString(lastRegister) ? getCurActiveId() : lastRegister;
    }

    /**
     * 当前活跃的账号
     *
     * @return
     */
    public String getCurActiveId() {
        return curActiveId;
    }

    /**
     * Mediasoup register 用户集合
     */
    private Map<String, MediasoupManageBean> registerMediasoup = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * 更新当前通话MediasoupManageBean
     *
     * @param isRegister
     * @param manageBean
     */
    private synchronized void updateRegisterMediasoup(String isRegister, MediasoupManageBean manageBean) {
        if (!Utils.isEmptyString(isRegister)) {
            registerMediasoup.put(isRegister, null == manageBean ? new MediasoupManageBean(true) : manageBean);
        }
    }

    /**
     * 获取当前通话MediasoupManageBean
     *
     * @param isRegister
     * @return
     */
    private synchronized MediasoupManageBean getCurMediasoupManage(String isRegister) {
        return Utils.isEmptyString(isRegister) ? new MediasoupManageBean(true) : (registerMediasoup.containsKey(isRegister) ? registerMediasoup.get(isRegister) : new MediasoupManageBean(true));
    }

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

    public synchronized void setInstanceNull(String isRegister) {
        LogUtils.e(TAG, "setInstanceNull mediasoupHandler==null isRegister:" + isRegister + ",hashCode:" + this.hashCode());
        getCurMediasoupManage(isRegister).setInstanceNull();
        this.isInitMediasoup = false;
        mediasoupDestroy(isRegister);
        mediasoupClose(isRegister);
//        mContext = null;
//        lastRegister = "";
//        curRegister = "";
//        curActiveId = "";
//        instance = null;
    }

    public synchronized void mediasoupDestroy(String isRegister) {
        LogUtils.e(TAG, "mediasoupDestroy 房间信息销毁 ,isRegister:" + isRegister + ",hashCode:" + this.hashCode());
        roomManagement = null;
        mRoomClient = null;
        mRoomStore = null;
        mRoomOptions = null;
    }

    private synchronized void mediasoupClose(String isRegister) {
        if (!isMediasoupConnecting(isRegister, getCurRConvId(isRegister))) {
            LogUtils.e(TAG, "mediasoupClose 可以重置 ,isRegister:" + isRegister + ",hashCode:" + this.hashCode());
            getCurMediasoupManage(isRegister).mediasoupClose();
            curCameraFace = "";
            clearAllVideoState(isRegister);
        } else {
            LogUtils.e(TAG, "mediasoupClose 连接中 不可以重置 ,isRegister:" + isRegister + " ,hashCode:" + this.hashCode());
        }
    }

    public void mediasoupActivityCreate(Context context) {
        LogUtils.i(TAG, "mediasoupActivityCreate:" + this.hashCode());
        if (!this.isInitMediasoup()) {
            mediasoupInit(context);
        }
    }

    public void setRoomManagement(RoomManagement roomManagement) {
        this.roomManagement = roomManagement;
    }

    public synchronized void mediasoupInit(Context context) {
        LogUtils.i(TAG, "mediasoupInit context:" + context + ",hashCode" + this.hashCode());
        this.mContext = context.getApplicationContext();
        Logger.setLogLevel(Logger.LogLevel.LOG_TRACE);
        Logger.setDefaultHandler();
        MediasoupClient.initialize(context.getApplicationContext());
        this.isInitMediasoup = true;
    }

    public String libraryVersion() {
//        return MediasoupConstant.mediasoup_version;
        return String.valueOf(MediasoupClient.version());
    }

    public synchronized String mediasoupCreate(Context context,
                                               String userId,
                                               String clientId,
                                               String activeId,
                                               String displayName,
                                               MediasoupManagement.MediasoupHandler mediasoupH) {
        String isRegister = userId;
        boolean isReady = isMediasoupReady(isRegister);
        boolean isConnecting = isMediasoupConnecting(isRegister, "");
        LogUtils.i(TAG, "mediasoupCreate not exist activeId:" + activeId + ",userId:" + userId + ", clientId:" + clientId + ", isReady:" + isReady + ", isConnecting:" + isConnecting + ", displayName:" + displayName + ", hashCode:" + this.hashCode());
        if (!this.isInitMediasoup()) {
            mediasoupInit(context);
        }
        if (isReady || isConnecting) {
            if (Utils.isEmptyString(getCurRegister())) {
                getRoomManagement().closeWebSocketDestroyRoom(true);
            }
        }
        if (Utils.isEmptyString(getCurRegister())) {
            this.curRegister = isRegister;
        }
        this.lastRegister = isRegister;
        this.curActiveId = activeId;
        MediasoupManageBean manageBean = new MediasoupManageBean(isRegister, userId, clientId, activeId, displayName, mediasoupH);
        updateRegisterMediasoup(isRegister, manageBean);
        return isRegister;
    }

    public void setUserChangedHandler(String isRegister, MediasoupManagement.UserChangedHandler userChangedHandler) {
        LogUtils.i(TAG, "setUserChangedHandler isRegister:" + isRegister);
        MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
        manageBean.setUserChangedHandler(userChangedHandler);
        updateRegisterMediasoup(isRegister, manageBean);
    }

    /**
     * 接收到邀请
     *
     * @param context
     * @param msg
     * @param curTime
     * @param msgTime
     * @param rConvId  来自的会话
     * @param userId   发送者id
     * @param clientId 发送者 clientid
     */
    public synchronized void receiveCallMessage(String isRegister,
                                                Context context,
                                                String msg,
                                                long curTime,
                                                long msgTime,
                                                String rConvId,
                                                String userId,
                                                String clientId,
                                                boolean isMediasoup) {
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(getCurRegister());
        LogUtils.i(TAG, "receiveCallMessage 接收到消息 isMediasoup:" + isMediasoup + ", isSameRegister:" + isSameRegister + ", mediasoupmsg:" + msg + " ,curTime:" + curTime + ", msgTime:" + msgTime + ", rConvId:" + rConvId + "，getCurRConvId()：" + getCurRConvId(isRegister) + " ,userId:" + userId + ", clientId:" + clientId + ", isInitMediasoup:" + isInitMediasoup() + ", isCreateMediasoup:" + isCreateMediasoup(isRegister) + ",hashCode:" + this.hashCode());
        if (!isMediasoup) {
            //不是Mediasoup消息
            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.DataChannel, rConvId, msgTime, userId);//接收的不是Mediasoup消息
            return;
        }
        if (!this.isInitMediasoup()) {
            mediasoupInit(context);
        }
        try {
            JSONObject jsonObject = Utils.isEmptyString(msg) ? null : new JSONObject(msg);
            if (null == jsonObject) {
                closedMediasoup(isRegister, MediasoupConstant.ClosedReason.IOError, rConvId, msgTime, userId);//接收的消息为空
            } else {
                boolean isRoomConnecting = isMediasoupConnecting(isRegister, rConvId);
                boolean isMissedTime = curTime - msgTime > MediasoupConstant.mediasoup_missed_time;
                if (isMissedTime && !isRoomConnecting) {
                    LogUtils.e(TAG, "receiveCallMessage 接收的消息超时 curTime:" + curTime + ", msgTime:" + msgTime + ", 时间差：" + (curTime - msgTime) + ", rConvId:" + rConvId + ", isRegister:" + isRegister);
                    closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Timeout, rConvId, msgTime, userId);//接收的超时的消息
                    return;
                }
                boolean isSameConv = !Utils.isEmptyString(getCurRConvId(isRegister)) && getCurRConvId(isRegister).equalsIgnoreCase(rConvId);
                boolean isSameUserId = !Utils.isEmptyString(getCurUserId(isRegister)) && getCurUserId(isRegister).equalsIgnoreCase(userId);
                boolean isSameClientId = !Utils.isEmptyString(getCurClientId(isRegister)) && getCurClientId(isRegister).equalsIgnoreCase(clientId);
                MediasoupConstant.CallState callState = MediasoupConstant.CallState.fromOrdinalType(jsonObject.optInt(MediasoupConstant.key_msg_callstate, -1));
                LogUtils.i(TAG, "receiveCallMessage 接收到消息解析 callState:" + callState + ", isRoomConnecting:" + isRoomConnecting + ", isSameConv：" + isSameConv + ", isSameUserId:" + isSameUserId + ", isSameClientId:" + isSameClientId + ", rConvId:" + rConvId + ", isMissedTime:" + isMissedTime + ", isRegister:" + isRegister);
                if (callState == MediasoupConstant.CallState.Started) {//0发起的邀请，
                    if (isRoomConnecting) {
                        if (isSameRegister) {
                            if (!isSameConv) {
                                //连接中 其他通话进来
                                sendMediasoupMsg(isRegister, MediasoupConstant.CallState.Busyed, rConvId);//其他通话进行中
                                closedMediasoup(isRegister, MediasoupConstant.ClosedReason.AnsweredElsewhere, rConvId, msgTime, userId);//其他通话进行中
                            } else {
                                if (isSameUserId) {
                                    //发送和接收同一个用户
                                    if (!isSameClientId) {
                                        //发送和接收同一个用户 设备不同
                                    } else {

                                    }
                                } else {
                                    //同一个会话，不同用户
                                    MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
                                    if (manageBean.isOneOnOneCall()) {
                                        manageBean.setStartMediasoupUser(rConvId,
                                            userId,
                                            clientId,
                                            "",
                                            msgTime);
                                        updateRegisterMediasoup(isRegister, manageBean);
                                    }
                                    if (!getRoomManagement().isOtherJoin()) {
                                        getRoomManagement().onOtherAcceptCall(isRegister);
                                    }
                                    sendMediasoupMsg(isRegister, MediasoupConstant.CallState.Accepted, rConvId);//当前会话通话进行中
                                }
                            }
                        } else {
                            if (!isSameUserId) {
                                sendMediasoupMsg(isRegister, MediasoupConstant.CallState.Busyed, rConvId);//其他通话进行中
                                closedMediasoup(isRegister, MediasoupConstant.ClosedReason.AnsweredElsewhere, rConvId, msgTime, userId);//其他通话进行中
                            }
                        }
                    } else {
                        //没有通话中
                        if (isMissedTime) {
                            //时间已经超时了
                            missedJoinMediasoup(isRegister);
                            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Timeout, rConvId, msgTime, userId);//超时的消息
                        } else {
                            if (isSameUserId) {
                                //发送和接收同一个用户
                                if (!isSameClientId) {
                                    //不同的ClientId 收到即结束
                                    closedMediasoup(isRegister, MediasoupConstant.ClosedReason.AnsweredElsewhere, rConvId, msgTime, userId);//自己账号发起的
                                }
                            } else {
                                //正常响应邀请
                                boolean isShouldRing = jsonObject.optBoolean(MediasoupConstant.key_msg_shouldring, MediasoupConstant.msg_shouldring);
                                MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
                                manageBean.setIncomingMediasoup(rConvId,
                                    userId,
                                    clientId,
                                    "",
                                    msgTime,
                                    MediasoupConstant.CallType.fromOrdinalType(jsonObject.optInt(MediasoupConstant.key_msg_calltype, -1)),//0音频，1视频，2强制音频
                                    MediasoupConstant.ConvType.fromOrdinalType(jsonObject.optInt(MediasoupConstant.key_msg_convtype, -1)),//0一对一模式，1群聊模式，2会议模式
                                    isShouldRing,
                                    MediasoupConstant.MeidasoupState.OtherCalling,
                                    MediasoupConstant.MeidasoupState.OtherCalling);
                                updateRegisterMediasoup(isRegister, manageBean);

                                setMediasoupVideoState(isRegister, null);
                                incomingJoinMediasoup(isRegister, rConvId, msgTime, userId, isVideoIncoming(isRegister), isShouldRing);
                            }
                        }
                    }
                } else if (callState == MediasoupConstant.CallState.Accepted) {//1接受
                    if (isSameRegister) {
                        if (!isSameConv) {
                            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.AnsweredElsewhere, rConvId, msgTime, userId);//发起和接收不是同一会话
                        } else {
                            if (isSameUserId) {
                                if (!isSameClientId) {
                                    //接受的用户和当前设备的用户同一个 设备不同
                                    if (null != getRoomManagement()) {
                                        getRoomManagement().onSelfOtherAcceptCall(isRegister);
                                    } else {
                                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//接收时 自己已经取消
                                    }
                                }
                            } else {
                                MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
                                if (manageBean.isOneOnOneCall()) {
                                    manageBean.setStartMediasoupUser(rConvId,
                                        userId,
                                        clientId,
                                        "",
                                        msgTime);
                                    updateRegisterMediasoup(isRegister, manageBean);
                                }
                                JSONObject toJson = jsonObject.optJSONObject(MediasoupConstant.key_msg_to);
                                String msgToUser = null != toJson ? toJson.optString(MediasoupConstant.key_msg_to_user) : null;
                                String msgToClient = null != toJson ? toJson.optString(MediasoupConstant.key_msg_to_client) : null;
                                if (isOneOnOneCall(isRegister) || Utils.isEmptyString(msgToUser) || msgToUser.equals(getCurUserId(isRegister))) {
                                    if (null != getRoomManagement()) {
                                        if (!getRoomManagement().isOtherJoin()) {
                                            getRoomManagement().onOtherAcceptCall(isRegister);
                                        }
                                    } else {
//                                if (isOneOnOneCall(isRegister)) {
                                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Canceled, rConvId, msgTime, userId);//接收时 自己已经取消
//                                }
                                    }
                                }
                            }
                        }
                    } else {
                        //不是同一个账号 收到接收信令不做处理
                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.AnsweredElsewhere, rConvId, msgTime, userId);//发起和接收不是同一会话
                    }
                } else if (callState == MediasoupConstant.CallState.Rejected) {//，2拒绝，
                    if (isSameRegister) {
                        if (!isSameConv) {
                            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Rejected, rConvId, msgTime, userId);//发起和拒绝不是同一会话
                        } else {
                            if (isSameUserId) {
                                if (!isSameClientId) {
                                    //接受的用户和当前设备的用户同一个 设备不同

                                }
                            } else {
                                if (null != getRoomManagement()) {
                                    getRoomManagement().onOtherRejectCall(isRegister);
                                } else {
                                    if (isOneOnOneCall(isRegister)) {
                                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Canceled, rConvId, msgTime, userId);//拒绝时 自己已经取消
                                    }
                                }
                            }
                        }
                    } else {
                        //不是同一个账号 收到接收信令不做处理
                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Rejected, rConvId, msgTime, userId);//发起和接收不是同一会话
                    }
                } else if (callState == MediasoupConstant.CallState.Ended) {//3结束，
                    //出现了消息时间比本地时间大的情况
                    if (isSameRegister) {
                        if (!isSameConv) {
                            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//发起和结束不是同一会话
                        } else {
                            if (isSameUserId) {//理论不存在处理这个情况
                                if (!isSameClientId) {
                                    //接受的用户和当前设备的用户同一个 设备不同

                                }
                            } else {
                                if (null != getRoomManagement()) {
                                    getRoomManagement().onOtherEndCall(isRegister, jsonObject.optInt(MediasoupConstant.key_msg_membercount, 0));
                                } else {
                                    if (isOneOnOneCall(isRegister)) {
                                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//结束时 自己已经取消
                                    }
                                }
                            }
                        }
                    } else {
                        //不是同一个账号 收到接收信令不做处理
                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//发起和接收不是同一会话
                    }
                } else if (callState == MediasoupConstant.CallState.Canceled) {//4 取消
                    if (isSameRegister) {
                        if (!isSameConv) {
                            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Canceled, rConvId, msgTime, userId);//发起和取消不是同一会话
                        } else {
                            if (isSameUserId) {//理论不存在处理这个情况
                                if (!isSameClientId) {
                                    //接受的用户和当前设备的用户同一个 设备不同

                                }
                            } else {
                                if (null != getRoomManagement()) {
                                    getRoomManagement().onOtherCloseCall(isRegister);
                                } else {
                                    if (isOneOnOneCall(isRegister)) {
                                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Canceled, rConvId, msgTime, userId);//还没有接受，已经取消
                                    }
                                }
                            }
                        }
                    } else {
                        //不是同一个账号 收到接收信令不做处理
                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Canceled, rConvId, msgTime, userId);//发起和取消不是同一会话
                    }
                } else if (callState == MediasoupConstant.CallState.Missed) {//,5 未响应
                    if (isSameRegister) {
                        if (!isSameConv) {
                            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.TimeoutEconn, rConvId, msgTime, userId);//发起和未响应不是同一会话
                        } else {
                            if (isSameUserId) {
                                if (!isSameClientId) {
                                    //接受的用户和当前设备的用户同一个 设备不同

                                }
                            } else {
                                if (null != getRoomManagement()) {
                                    getRoomManagement().onOtherMissedCall(isRegister);
                                } else {
                                    if (isOneOnOneCall(isRegister)) {
                                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.TimeoutEconn, rConvId, msgTime, userId);//有一方未响应
                                    }
                                }
                            }
                        }
                    } else {
                        //不是同一个账号 收到接收信令不做处理
                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.TimeoutEconn, rConvId, msgTime, userId);//发起和未响应不是同一会话
                    }
                } else if (callState == MediasoupConstant.CallState.Busyed) { //6: 忙碌中
                    if (isSameRegister) {
                        if (!isSameConv) {
                            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//发起和未响应不是同一会话
                        } else {
                            if (isSameUserId) {
                                if (!isSameClientId) {
                                    //接受的用户和当前设备的用户同一个 设备不同

                                }
                            } else {
                                if (null != getRoomManagement()) {
                                    getRoomManagement().onOtherBusyedCall(isRegister);
                                } else {
                                    if (isOneOnOneCall(isRegister)) {
                                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//有一方未响应
                                    }
                                }
                            }
                        }
                    } else {
                        //不是同一个账号 收到接收信令不做处理
                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//发起和未响应不是同一会话
                    }
                } else if (callState == MediasoupConstant.CallState.P2POffer) {//,7 p2p 发起,
                    if (isSameRegister) {
                        if (!isSameConv) {
                            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.DataChannel, rConvId, msgTime, userId);//发起和未响应不是同一会话
                        } else {
                            if (isSameUserId) {
                                if (!isSameClientId) {
                                    //接受的用户和当前设备的用户同一个 设备不同

                                }
                            } else {
                                if (null != roomManagement) {
                                    roomManagement.onReceiveP2POffer(userId, jsonObject.optJSONObject(MediasoupConstant.key_msg_p2pdata));
                                } else {
                                    if (isOneOnOneCall(isRegister)) {
                                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.DataChannel, rConvId, msgTime, userId);//有一方未响应
                                    }
                                }
                            }
                        }
                    } else {
                        //不是同一个账号 收到接收信令不做处理
                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//发起和未响应不是同一会话
                    }
                } else if (callState == MediasoupConstant.CallState.P2PAnswer) {//,8 p2p 响应,
                    if (isSameRegister) {
                        if (!isSameConv) {
                            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.DataChannel, rConvId, msgTime, userId);//发起和未响应不是同一会话
                        } else {
                            if (isSameUserId) {
                                if (!isSameClientId) {
                                    //接受的用户和当前设备的用户同一个 设备不同

                                }
                            } else {
                                if (null != roomManagement) {
                                    roomManagement.onReceiveP2PAnswer(userId, jsonObject.optJSONObject(MediasoupConstant.key_msg_p2pdata));
                                } else {
                                    if (isOneOnOneCall(isRegister)) {
                                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.DataChannel, rConvId, msgTime, userId);//有一方未响应
                                    }
                                }
                            }
                        }
                    } else {
                        //不是同一个账号 收到接收信令不做处理
                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//发起和未响应不是同一会话
                    }
                } else if (callState == MediasoupConstant.CallState.P2PIce) {//,9 p2p ice
                    if (isSameRegister) {
                        if (!isSameConv) {
                            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.DataChannel, rConvId, msgTime, userId);//发起和未响应不是同一会话
                        } else {
                            if (isSameUserId) {
                                if (!isSameClientId) {
                                    //接受的用户和当前设备的用户同一个 设备不同

                                }
                            } else {
                                if (null != roomManagement) {
                                    roomManagement.onReceiveP2PIce(userId, jsonObject.optJSONObject(MediasoupConstant.key_msg_p2pdata));
                                } else {
                                    if (isOneOnOneCall(isRegister)) {
                                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.DataChannel, rConvId, msgTime, userId);//有一方未响应
                                    }
                                }
                            }
                        }
                    } else {
//不是同一个账号 收到接收信令不做处理
                        closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal, rConvId, msgTime, userId);//发起和未响应不是同一会话
                    }
                } else {

                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.IOError, rConvId, msgTime, userId);//接收的消息为空 出现异常
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
    public synchronized int mediasoupStartCall(String isRegister, Context context, String rConvId, int callType, int convType, boolean audioCbr) {
        LogUtils.i(TAG, "mediasoupStartCall 发起通话 rConvId:" + rConvId + ", callType:" + callType + ", convType:" + convType + ", audioCbr:" + audioCbr + ", is Connecting:" + isMediasoupConnecting(isRegister, rConvId) + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(getCurRegister());
        boolean isRoomConnecting = isMediasoupConnecting(isRegister, rConvId);
        if (!isSameRegister && isRoomConnecting) {
            return 1;
        }
        this.curRegister = isRegister;
        //        this.curAudioCbr = audioCbr;
        MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
        manageBean.setStartMediasoup(rConvId,
            MediasoupConstant.MeidasoupState.SelfCalling,
            MediasoupConstant.MeidasoupState.SelfCalling,
            MediasoupConstant.CallType.fromOrdinalType(callType),
            MediasoupConstant.ConvType.fromOrdinalType(convType),
            MediasoupConstant.msg_shouldring);
        updateRegisterMediasoup(isRegister, manageBean);
        setMediasoupVideoState(isRegister, null);
        mediasoupStart(isRegister, context);
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
    public synchronized void mediasoupAnswerCall(String isRegister, Context context, String rConvId, int callType, int convType, int meidasoupState, boolean audioCbr) {
        LogUtils.i(TAG, "mediasoupAnswerCall 响应通话 rConvId:" + rConvId + ", callType:" + callType + ", convType:" + convType + ",meidasoupState:" + meidasoupState + ", audioCbr:" + audioCbr + ", is null roomManagement:" + (null == getRoomManagement()) + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(getCurRegister());
        boolean isRoomConnecting = isMediasoupConnecting(isRegister, rConvId);
        if ((!isSameRegister || !Utils.isEmptyString(getCurRConvId(isRegister)) && !getCurRConvId(isRegister).equals(rConvId)) && isRoomConnecting) {
            LogUtils.e(TAG, "mediasoupAnswerCall 响应通话 rConvId:" + rConvId + ", getCurRConvId():" + getCurRConvId(isRegister) + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
//            getRoomManagement().setSelfAcceptOrJoin(isRegister);
            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.AnsweredElsewhere, rConvId, getIncomingMsgTime(isRegister), getIncomingUserId(isRegister));
        } else {
            this.curRegister = isRegister;
//        this.curAudioCbr = audioCbr;
            MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
            manageBean.setAnswerMediasoup(rConvId,
                MediasoupConstant.CallType.fromOrdinalType(callType),
                MediasoupConstant.ConvType.fromOrdinalType(convType),
                MediasoupConstant.MeidasoupState.fromOrdinalType(meidasoupState),
                MediasoupConstant.MeidasoupState.fromOrdinalType(meidasoupState));
            updateRegisterMediasoup(isRegister, manageBean);

            setMediasoupVideoState(isRegister, null);
            //0发起，1接受，2拒绝，3结束，4取消
            sendMediasoupMsg(isRegister, MediasoupConstant.CallState.Accepted, rConvId);//自己接受
            if (null != getRoomManagement() && getRoomManagement().isVisibleCall()) {
                getRoomManagement().setSelfAcceptOrJoin(isRegister);
            } else {
                if (!this.isInitMediasoup()) {
                    mediasoupInit(context);
                }
                startMediasoupService(context, true);
            }
        }
    }

    /**
     * @param isRegister
     * @param mode       1 ,_2G,
     *                   2, EDGE, //A.K.A 2.5G
     *                   3,  _3G,
     *                   4,  _4G,
     *                   5, WIFI,
     *                   6,  OFFLINE,
     *                   7, UNKNOWN;
     */
    public void onNetworkChanged(String isRegister, int mode) {
        MediasoupConstant.NetworkMode networkMode = MediasoupConstant.NetworkMode.fromOrdinalType(mode);
        LogUtils.i(TAG, "onNetworkChanged, isRegister:" + isRegister + ", networkMode:" + networkMode + ", mode:" + mode + ", hashCode:" + this.hashCode());
        if (null != getRoomManagement()) {
            getRoomManagement().onNetworkChanged(isRegister, networkMode);
        }
    }

    public void onHttpResponse(String isRegister, int status, String reason) {
        LogUtils.i(TAG, "onHttpResponse: status:" + status + ", reason:" + reason + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
    }

    public void onConfigRequest(String isRegister, int error, String json) {
        LogUtils.i(TAG, "onConfigRequest: error:" + error + ", json:" + json + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
//        setMediasoupProxy(isRegister, json, error);
    }

    /**
     * 自己结束通话
     *
     * @param rConvId
     * @param meidasoupState SelfCalling   0    OtherCalling  1   SelfJoining   2    SelfConnected 3     Ongoing       4   Terminating   5     Ended         6
     */
    public synchronized void endMediasoupCall(String isRegister, String rConvId, int meidasoupState) {
        LogUtils.i(TAG, "endMediasoupCall: rConvId:" + rConvId + ",meidasoupState:" + meidasoupState + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(getCurRegister());
        //        this.meidasoupTimely = MeidasoupState.fromTypeOrdinal(meidasoupState);
        if (!Utils.isEmptyString(rConvId) && rConvId.equals(getCurRConvId(isRegister)) && null != getRoomManagement()) {
            getRoomManagement().callSelfEnd(MediasoupConstant.ClosedReason.Normal);
        } else {
            sendMediasoupMsg(isRegister, MediasoupConstant.CallState.Ended, rConvId);//自己结束
            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal, rConvId, getIncomingMsgTime(isRegister), getIncomingUserId(isRegister));//自己结束
//            endedMediasoup(isRegister, rConvId);
        }
    }

    /**
     * 拒绝通话
     *
     * @param rConvId
     * @param meidasoupState SelfCalling   0    OtherCalling  1   SelfJoining   2    SelfConnected 3     Ongoing       4   Terminating   5     Ended         6
     */
    public synchronized void rejectMediasoupCall(String isRegister, String rConvId, int meidasoupState) {
        LogUtils.i(TAG, "rejectMediasoupCall: rConvId:" + rConvId + ",meidasoupState:" + meidasoupState + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(getCurRegister());
        //        this.meidasoupTimely = MeidasoupState.fromTypeOrdinal(meidasoupState);
        if (!Utils.isEmptyString(rConvId) && rConvId.equals(getCurRConvId(isRegister)) && null != getRoomManagement()) {
            getRoomManagement().callSelfReject();
        } else {
            sendMediasoupMsg(isRegister, MediasoupConstant.CallState.Rejected, rConvId);//自己拒绝
            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Rejected, rConvId, getIncomingMsgTime(isRegister), getIncomingUserId(isRegister));//自己拒绝
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
    public void setVideoSendState(String isRegister, String rConvId, int state) {
        LogUtils.i(TAG, "setVideoSendState: enableCam disableCam rConvId:" + rConvId + ", state:" + state + ", roomManagement is null:" + (null == getRoomManagement()) + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(getCurRegister());
        if (isSameRegister && null != getRoomManagement() && !Utils.isEmptyString(rConvId) && rConvId.equals(getCurRConvId(isRegister))) {
            MediasoupConstant.VideoState videoState = MediasoupConstant.VideoState.fromOrdinalType(state);
            setMediasoupVideoState(isRegister, videoState);
            getRoomManagement().disableAndEnableCam(videoState == MediasoupConstant.VideoState.Started);
        }
    }

    public synchronized void setCallMuted(String isRegister, boolean muted) {
        LogUtils.i(TAG, "setCallMuted: muteMic muted:" + muted + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
        if (null != getRoomManagement()) {
            getRoomManagement().setCallMuted(muted);
        }
    }

    public synchronized void switchCam() {
        LogUtils.i(TAG, "switchCam: hashCode:" + this.hashCode());
        if (null != getRoomManagement()) {
            getRoomManagement().switchCam();
        }
    }

    public void setMediasoupProxy(String isRegister, String host, int port) {
        LogUtils.i(TAG, "setMediasoupProxy: host:" + host + ", port:" + port + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
//        if (null != getRoomManagement()) {
//            getRoomManagement().setMediasoupProxy(host, port);
//        }
//        if (null != mContext) {
//            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
//            preferences.edit().putString(MediasoupConstant.key_shared_proxy_host, host).apply();
//            preferences.edit().putInt(MediasoupConstant.key_shared_proxy_port, port).apply();
//        }
    }

    public String getConnectHost() {
//        if (null == mContext) {
//            return "";
//        }
//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
//        return preferences.getString(MediasoupConstant.key_shared_proxy_host, "");
        return "";
    }

    public int getConnectPort() {
//        if (null == mContext) {
//            return 0;
//        }
//        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
//        return preferences.getInt(MediasoupConstant.key_shared_proxy_port, 0);
        return 0;
    }

    /**
     * 是否连接中
     *
     * @return
     */
    public boolean isMediasoupConnecting(String isRegister, String rConvId) {
        boolean isConnecting = null != getRoomManagement() && getRoomManagement().isRoomConnecting();
        LogUtils.i(TAG, "isMediasoupConnecting: rConvId:" + rConvId + ", isConnecting:" + isConnecting + ", roomManagement is null:" + (null == getRoomManagement()) + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
        return isConnecting;
    }

    /**
     * 是否已经连接
     *
     * @return
     */
    public boolean isMediasoupConnected(String isRegister, String rConvId) {
        boolean isConnected = null != getRoomManagement() && getRoomManagement().isRoomConnected();
        LogUtils.i(TAG, "isMediasoupConnected: rConvId:" + rConvId + ", isConnected:" + isConnected + ", getRoomManagement() is null:" + (null == getRoomManagement()) + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
        return isConnected;
    }

    public String getCurRConvId(String isRegister) {
        return getCurMediasoupManage(isRegister).getCurRConvId();
    }

    public String getCurUserId(String isRegister) {
        MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
        String curUserId = manageBean.getCurUserId();
        MediasoupManagement.MediasoupHandler mediasoupHandler = manageBean.getMediasoupHandler();
        if (Utils.isEmptyString(curUserId) && isInitMediasoup() && null != mediasoupHandler) {
            return mediasoupHandler.getCurAccountId();
        }
        return curUserId;
    }

    public String getCurClientId(String isRegister) {
        MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
        String curClientId = manageBean.getCurClientId();
        MediasoupManagement.MediasoupHandler mediasoupHandler = manageBean.getMediasoupHandler();
        if (Utils.isEmptyString(curClientId) && isInitMediasoup() && null != mediasoupHandler) {
            return mediasoupHandler.getCurClientId();
        }
        return curClientId;
    }

    public String getDisplayName(String isRegister) {
        MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
        String curDisplayName = manageBean.getCurDisplayName();
        MediasoupManagement.MediasoupHandler mediasoupHandler = manageBean.getMediasoupHandler();
        if (Utils.isEmptyString(curDisplayName) && isInitMediasoup() && null != mediasoupHandler) {
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

    public boolean isCreateMediasoup(String isRegister) {
        return getCurMediasoupManage(isRegister).isCreateMediasoup();
    }

    public MediasoupConstant.CallType getMediasoupCallType(String isRegister) {
        return getCurMediasoupManage(isRegister).getMediasoupCallType();
    }

    public MediasoupConstant.ConvType getMediasoupConvType(String isRegister) {
        return getCurMediasoupManage(isRegister).getMediasoupConvType();
    }

    public RoomManagement getRoomManagement() {
        return roomManagement;
    }

    /**
     * 是否邀请加入
     *
     * @return
     */
    public boolean isReceiveCall(String isRegister) {
        return getCurMediasoupManage(isRegister).isReceiveCall();
    }

    /**
     * 是否发起方
     *
     * @return
     */
    public boolean isSelfCalling(String isRegister) {
        return getCurMediasoupManage(isRegister).isSelfCalling();
    }

    /**
     * 是否邀请的视频通话
     *
     * @return
     */
    public boolean isVideoIncoming(String isRegister) {
        return getCurMediasoupManage(isRegister).isVideoIncoming();
    }

    /**
     * 当前状态是否视频通话中
     *
     * @return
     */
    public MediasoupConstant.VideoState getMediasoupVideoState(String isRegister) {
        return getCurMediasoupManage(isRegister).getMediasoupVideoState();
    }

    /**
     * 当前状态是否视频通话中
     *
     * @return
     */
    public boolean isMediasoupVideoState(String isRegister) {
        return getCurMediasoupManage(isRegister).isMediasoupVideoState();
    }

    /**
     * 设置当前视频状态
     *
     * @param videoState
     */
    public void setMediasoupVideoState(String isRegister, MediasoupConstant.VideoState videoState) {
        MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
        manageBean.setMediasoupVideoState(videoState);
        updateRegisterMediasoup(isRegister, manageBean);
    }

    /**
     * 是否一对一
     *
     * @return
     */
    public boolean isOneOnOneCall(String isRegister) {
        return getCurMediasoupManage(isRegister).isOneOnOneCall();
    }

    /**
     * Mediasoup 是否初始化并创建
     *
     * @return
     */
    public boolean isInitAndCreate(String isRegister) {
//        LogUtils.i(TAG, "isInitAndCreate: is null mediasoupHandler:" + (null == getMediasoupHandler(isRegister)) + ",is null userChangedHandler:" + (null == getUserChangedHandler(isRegister)) + ", hashCode:" + this.hashCode());
        return isInitMediasoup() && isCreateMediasoup(isRegister);
    }

    public MediasoupManagement.MediasoupHandler getMediasoupHandler(String isRegister) {
        return getCurMediasoupManage(isRegister).getMediasoupHandler();
    }

    public MediasoupManagement.UserChangedHandler getUserChangedHandler(String isRegister) {
        return getCurMediasoupManage(isRegister).getUserChangedHandler();
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

    public void setIncomingUser(String isRegister, String userId, String clientId, String displayName) {
        MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
        manageBean.setIncomingUser(userId, clientId, displayName);
        updateRegisterMediasoup(isRegister, manageBean);
    }

    public String getIncomingUserId(String isRegister) {
        return getCurMediasoupManage(isRegister).getIncomingUserId();
    }

    public String getIncomingClientId(String isRegister) {
        return getCurMediasoupManage(isRegister).getIncomingClientId();
    }

    public String getIncomingDisplayName(String isRegister) {
        return getCurMediasoupManage(isRegister).getIncomingDisplayName();
    }

    public long getIncomingMsgTime(String isRegister) {
        return getCurMediasoupManage(isRegister).getIncomingMsgTime();
    }

    /**
     * 设置房间信息 是否都准备好
     *
     * @param isRegister
     * @param roomClient
     * @param roomStore
     * @param roomOptions
     */
    public boolean setRoomClientStoreOptions(String isRegister, RoomClient roomClient, RoomStore roomStore, RoomOptions roomOptions) {
        this.mRoomClient = roomClient;
        this.mRoomStore = roomStore;
        this.mRoomOptions = roomOptions;
        boolean isReady = isMediasoupReady(isRegister);
        if (null != getMediasoupHandler(isRegister)) {
            getMediasoupHandler(isRegister).onReady(isReady);
        }
        return isReady;
    }

    public boolean isMediasoupReady(String isRegister) {
        boolean isReady = !Utils.isEmptyString(getCurRConvId(isRegister)) && !Utils.isEmptyString(getCurUserId(isRegister)) && !Utils.isEmptyString(getCurClientId(isRegister));
        if (isReady && null != getRoomClient() && null != getRoomStore() && null != getRoomOptions()) {
            return true;
        } else {
            return false;
        }
    }

    public void sendMediasoupMsg(String isRegister, MediasoupConstant.CallState callState) {
        LogUtils.i(TAG, "sendMediasoupMsg: callState.name:" + callState.name() + ",callState.ordinal:" + callState.getIndex() + ", hashCode:" + this.hashCode());
        sendMediasoupMsg(isRegister, callState, getCurRConvId(isRegister));
    }

    public void sendMediasoupMsg(String isRegister, MediasoupConstant.CallState callState, String rConvId) {
        sendMediasoupMsg(isRegister, callState, getCurRConvId(isRegister), null);
    }

    /**
     * 发送secret 信令
     *
     * @param callState 0发起，1接受，2拒绝，3结束，4取消 5 未响应
     */
    public void sendMediasoupMsg(String isRegister, MediasoupConstant.CallState callState, String rConvId, JSONObject jsonData) {
        LogUtils.i(TAG, "发送 sendMediasoupMsg: callState.name:" + callState.name() + ", rConvId:" + rConvId + ", getCurUserId:" + getCurUserId(isRegister) + ", mediasoupHandler is null:" + (null == getMediasoupHandler(isRegister)) + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(MediasoupConstant.key_msg_callstate, MediasoupConstant.CallState.fromTypeOrdinal(callState));
            switch (callState) {
                case Started:
                    jsonObject.put(MediasoupConstant.key_msg_calltype, MediasoupConstant.CallType.fromTypeOrdinal(getMediasoupCallType(isRegister)));
                    jsonObject.put(MediasoupConstant.key_msg_convtype, MediasoupConstant.ConvType.fromTypeOrdinal(getMediasoupConvType(isRegister)));
//                jsonObject.put(MediasoupConstant.key_msg_shouldring, true);
                    break;
                case Accepted:
                    if (!Utils.isEmptyString(getIncomingUserId(isRegister))) {
                        JSONObject toJson = new JSONObject();
                        toJson.put(MediasoupConstant.key_msg_to_user, getIncomingUserId(isRegister));
                        toJson.put(MediasoupConstant.key_msg_to_client, getIncomingClientId(isRegister));
                        jsonObject.put(MediasoupConstant.key_msg_to, toJson);
                    }
                    break;
                case Rejected:
                    break;
                case Ended:
                    int count = (null == getRoomManagement() || isOneOnOneCall(isRegister)) ? 0 : getRoomManagement().getCurRoomPeerSize();
                    jsonObject.put(MediasoupConstant.key_msg_membercount, count);
                    break;
                case Canceled:
                    break;
                case Missed:
                    break;
                case Busyed:
                    break;
                case P2POffer:
                case P2PAnswer:
                case P2PIce:
                    jsonObject.put(MediasoupConstant.key_msg_p2pdata, jsonData);
                    break;
                default:
                    break;
            }
            if (null != getMediasoupHandler(isRegister) && !Utils.isEmptyString(rConvId)) {
                getMediasoupHandler(isRegister).onSend(rConvId, getCurUserId(isRegister), getCurClientId(isRegister), getIncomingUserId(isRegister), getIncomingClientId(isRegister), jsonObject.toString(), true);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.IOError, rConvId, getIncomingMsgTime(isRegister), getIncomingUserId(isRegister));//发送消息出现异常
        }
    }

    /**
     * 加入邀请
     *
     * @param isRegister
     * @param rConvId
     * @param msgTime
     * @param userId
     * @param video_call
     * @param should_ring
     */
    public void incomingJoinMediasoup(String isRegister, String rConvId, long msgTime, String userId, boolean video_call, boolean should_ring) {
        LogUtils.i(TAG, "incomingJoinMediasoup: rConvId:" + rConvId + ", msgTime:" + msgTime + ", userId:" + userId + ", video_call:" + video_call + ", should_ring:" + should_ring + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister) || !isReceiveCall(isRegister)) {
            return;
        }
        if (null != getMediasoupHandler(isRegister) && !Utils.isEmptyString(rConvId)) {
            getMediasoupHandler(isRegister).onIncomingCall(rConvId, msgTime, userId, video_call, should_ring);
        }
    }

    /**
     * 邀请加入长时间未响应
     */
    public void missedJoinMediasoup(String isRegister) {
        LogUtils.i(TAG, "missedJoinMediasoup: hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        if (null != getMediasoupHandler(isRegister) && !Utils.isEmptyString(getCurRConvId(isRegister))) {
            getMediasoupHandler(isRegister).onMissedCall(getCurRConvId(isRegister), getIncomingMsgTime(isRegister), getIncomingUserId(isRegister), isVideoIncoming(isRegister));
        }
    }

    /**
     * 响应了邀请
     *
     * @param isRegister
     * @param isConnected 是否 socket 连接成功的响应
     */
    public void answeredJoinMediasoup(String isRegister, boolean isConnected) {
        LogUtils.i(TAG, "answeredJoinMediasoup: hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
        boolean isValid = manageBean.answeredJoinMediasoup(isConnected);
        if (isValid && null != getMediasoupHandler(isRegister) && !Utils.isEmptyString(getCurRConvId(isRegister))) {
            manageBean.setJoinedTime(Instant.now());
            getMediasoupHandler(isRegister).onAnsweredCall(getCurRConvId(isRegister));
        }
        updateRegisterMediasoup(isRegister, manageBean);
    }

    /**
     * 建立通话
     */
    public Instant establishedJoinMediasoup(String isRegister) {
        LogUtils.i(TAG, "establishedJoinMediasoup: hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister)) {
            return Instant.EPOCH;
        }
        MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
        boolean isValid = manageBean.establishedJoinMediasoup();
        if (isValid) {
            manageBean.setEstabTime(Instant.now());
        }
        if (null != getMediasoupHandler(isRegister) && !Utils.isEmptyString(getCurRConvId(isRegister))) {
            getMediasoupHandler(isRegister).onEstablishedCall(getCurRConvId(isRegister), getIncomingUserId(isRegister));
        }
        updateRegisterMediasoup(isRegister, manageBean);
        return manageBean.getEstabTime();
    }


    public void closedMediasoup(String isRegister, MediasoupConstant.ClosedReason closedReason) {
        LogUtils.i(TAG, "closedMediasoup: closedReason:" + closedReason + ", closedReason.name:" + closedReason.name() + ", isRegister:" + isRegister + ", hashCode:" + this.hashCode());
        closedMediasoup(isRegister, closedReason, getCurRConvId(isRegister), getIncomingMsgTime(isRegister), getIncomingUserId(isRegister));
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
    public void closedMediasoup(String isRegister, MediasoupConstant.ClosedReason closedReason, String rConvId, long msgTime, String userId) {
        LogUtils.i(TAG, "closedMediasoup:rConvId :" + rConvId + ", closedReason.name:" + closedReason.name() + ", isRegister:" + isRegister + ", mediasoupHandler is null:" + (null == getMediasoupHandler(isRegister)) + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
        boolean isValid = manageBean.endedMediasoup();
        manageBean.setEndReason(closedReason);
        manageBean.setEndTime(Instant.now());
        if (null != getMediasoupHandler(isRegister) && !Utils.isEmptyString(rConvId)) {
            getMediasoupHandler(isRegister).onClosedCall(closedReason.getIndex(), rConvId, msgTime, userId);
        }
        updateRegisterMediasoup(isRegister, manageBean);
        mediasoupClose(isRegister);
    }

    /**
     * 通话结束
     *
     * @param isRegister
     * @param rConvId
     */
    public void endedMediasoup(String isRegister, String rConvId) {
        LogUtils.i(TAG, "endedMediasoup:rConvId :" + rConvId + ", isRegister:" + isRegister + ", mediasoupHandler is null:" + (null == getMediasoupHandler(isRegister)) + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        MediasoupManageBean manageBean = getCurMediasoupManage(isRegister);
        boolean isValid = manageBean.endedMediasoup();
        manageBean.setEndTime(Instant.now());
        if (null != getMediasoupHandler(isRegister) && !Utils.isEmptyString(rConvId)) {
            getMediasoupHandler(isRegister).onEndedCall(rConvId, getIncomingMsgTime(isRegister), getIncomingUserId(isRegister));
        }
    }

    /**
     * @param metricsJson
     */
    public void metricsReadyMediasoup(String isRegister, String metricsJson) {
        LogUtils.i(TAG, "metricsReadyMediasoup: metricsJson:" + metricsJson + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        if (null != getMediasoupHandler(isRegister)) {
            getMediasoupHandler(isRegister).onMetricsReady(getCurRConvId(isRegister), metricsJson);
        }
    }

    /**
     * 获取配置信息
     */
    public void configRequestMediasoup(String isRegister) {
        LogUtils.i(TAG, "configRequestMediasoup: hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        if (null != getMediasoupHandler(isRegister)) {
            boolean isReady = !Utils.isEmptyString(getCurRConvId(isRegister)) && !Utils.isEmptyString(getCurUserId(isRegister)) && !Utils.isEmptyString(getCurClientId(isRegister));
            getMediasoupHandler(isRegister).onConfigRequest(isRegister, getCurRConvId(isRegister), getCurUserId(isRegister), getCurClientId(isRegister), !isOneOnOneCall(isRegister), isReady);
        }
    }

    /**
     * 某个用户的 码率的改变
     *
     * @param userId
     * @param enabled
     */
    public void onBitRateStateChanged(String isRegister, String userId, Boolean enabled) {
        LogUtils.i(TAG, "onBitRateStateChanged: userId:" + userId + ", enabled:" + enabled + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        if (null != getMediasoupHandler(isRegister)) {
            getMediasoupHandler(isRegister).onBitRateStateChanged(userId, enabled);
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
    public void onVideoReceiveStateChanged(String isRegister, String userId, String clientId, String displayName, MediasoupConstant.VideoState state) {
//        LogUtils.i(TAG, "onVideoReceiveStateChanged: selfUserId:" + getCurUserId() + ", getCurRConvId()):" + getCurRConvId(isRegister) + ", userId:" + userId + ", displayName:" + displayName + ", clientId:" + clientId + ", state.name:" + state.name() + ", state.ordinal:" + state.getIndex() + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        putPeerVideoState(isRegister, userId, clientId, state);
        if (null != getMediasoupHandler(isRegister) && !Utils.isEmptyString(getCurRConvId(isRegister))) {
            getMediasoupHandler(isRegister).onVideoReceiveStateChanged(getCurRConvId(isRegister), userId, clientId, state.getIndex());
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
    public void joinMediasoupState(String isRegister, int state) {
//        LogUtils.i(TAG, "joinMediasoupState: state:" + state + ", hashCode:" + this.hashCode());
//        if (!isInitAndCreate(isRegister)) {
//            return;
//        }
//        if (null != getMediasoupHandler(isRegister)) {
//            getMediasoupHandler(isRegister).joinMediasoupState(state);
//        }
    }

    /**
     * 拒绝 离开 取消
     *
     * @param
     */
    public void rejectEndCancelCall(String isRegister) {
        LogUtils.i(TAG, "rejectEndCancelCall: hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        if (null != getMediasoupHandler(isRegister)) {
            getMediasoupHandler(isRegister).rejectEndCancelCall();
        }
    }

    public void startIfCallIsActive(String isRegister) {
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        if (null != getMediasoupHandler(isRegister)) {
            getMediasoupHandler(isRegister).startIfCallIsActive();
        }
    }

    /**
     * 相机打开失败
     *
     * @param isRegister
     * @param isFail
     */
    public void cameraOpenState(String isRegister, boolean isFail) {
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        if (null != getMediasoupHandler(isRegister)) {
            getMediasoupHandler(isRegister).cameraOpenState(isFail);
        }
    }

    /**
     * 加入房间用户变化
     *
     * @param jsonArray
     */
    public void mediasoupUserChanged(String isRegister, JSONArray jsonArray) throws JSONException {
        LogUtils.i(TAG, "mediasoupUserChanged: jsonArray:" + jsonArray.toString() + ", hashCode:" + this.hashCode());
        if (!isInitAndCreate(isRegister)) {
            return;
        }
        if (null != getUserChangedHandler(isRegister) && !Utils.isEmptyString(getCurRConvId(isRegister))) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("rconvid", getCurRConvId(isRegister));
            jsonObject.put("peerusers", jsonArray);
            getUserChangedHandler(isRegister).onUserChanged(getCurRConvId(isRegister), jsonObject.toString());
        }
    }

    /**
     * 启动呼叫收听界面
     *
     * @param context
     */
    public boolean mediasoupStart(String isRegister, Context context) {
        LogUtils.i(TAG, "mediasoupStart getCurRConvId():" + getCurRConvId(isRegister) + ", roomManagement is null:" + (null == getRoomManagement()) + ", hashCode:" + this.hashCode());
        if (!this.isInitMediasoup()) {
            mediasoupInit(context);
        }
        if (!Utils.isEmptyString(isRegister) && Utils.isEmptyString(getCurRConvId(isRegister))) {
            closedMediasoup(isRegister, MediasoupConstant.ClosedReason.Error, getCurRConvId(isRegister), getIncomingMsgTime(isRegister), getIncomingUserId(isRegister));//当前会话id为空
            rejectEndCancelCall(isRegister);
            return false;
        }
//        startMediasoupActivity(context);
        return true;
    }

    private String curCameraFace;//当前摄像头面向

    public void setCurCameraFace(String camera) {
        this.curCameraFace = camera;
    }

    public String getCurCameraFace() {
        return curCameraFace;
    }

    /**
     * 获取当前房间除了自己之外所有用户
     *
     * @return
     */
    public List<Peer> getCurAllPeers() {
        return null == getRoomManagement() ? null : getRoomManagement().getCurRoomPeerList();
    }

    /**
     * peer 视频的状态集合
     */
    private Map<String, Boolean> peerVideoState = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * @return 获取视频的状态集合
     */
    public Map<String, Boolean> getPeerVideoState() {
        return peerVideoState;
    }

    /**
     * 清空所有视频的状态集合
     */
    public void clearAllVideoState(String isRegister) {
        peerVideoState.clear();
    }

    /**
     * 清空单个peer 视频状态
     *
     * @param userId
     * @param clientId
     */
    public void removePeerVideoState(String isRegister, String userId, String clientId) {
        if (!Utils.isEmptyString(userId) && peerVideoState.containsKey(userId)) {
            peerVideoState.remove(userId);
        }
    }

    /**
     * 获取单个 peer 视频状态
     *
     * @param peerId
     * @return
     */
    public boolean getPeerVideoState(String isRegister, String peerId) {
        LogUtils.i(TAG, "getPeerVideoState getCurUserId:" + getCurUserId(isRegister) + ", userId:" + peerId);
        if (Utils.isEmptyString(peerId)) {
            return false;
        }
        if (null == getRoomManagement()) {
            LogUtils.i(TAG, "getPeerVideoState null == roomManagement = true 返回:false");
            peerVideoState.clear();
            return false;
        }
        if (peerVideoState.containsKey(peerId)) {
            return peerVideoState.get(peerId);
        } else {
            Peers peers = getRoomManagement().getCurRoomPeers();
            Peer peer = null == peers ? null : peers.getPeer(peerId);
            LogUtils.i(TAG, "getPeerVideoState peerVideoState.containsKey(info.getId() = false, null == peer=" + (null == peer));
            if (null != peer) {
                return peer.isVideoVisible();
            } else {
                MeProps meProps = getRoomManagement().getMeProps();
                LogUtils.i(TAG, "getPeerVideoState null == peer = true, null == meProps:" + (null == meProps));
                if (null != meProps && null != meProps.getCamState()) {
                    return meProps.getCamState().get() == MeProps.DeviceState.ON;
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
    public void putPeerVideoState(String isRegister, String userId, String clientId, MediasoupConstant.VideoState state) {
//        LogUtils.i(TAG, "putPeerVideoState getCurUserId:" + getCurUserId() + ", userId:" + userId + ", state:" + state);
        peerVideoState.put(userId, state == MediasoupConstant.VideoState.Started);
    }

    /**
     * 启动 room界面
     *
     * @param context
     */
    public void startMediasoupActivity(Context context) {
        LogUtils.i(TAG, "startMediasoupActivity context:" + context + ", hashCode:" + this.hashCode());
        if (null == context) {
            return;
        }
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
     * @param isJoin
     */
    private void startMediasoupService(Context context, boolean isJoin) {
        LogUtils.i(TAG, "startMediasoupService context:" + context + ", isJoin:" + isJoin + ", hashCode:" + this.hashCode());
        if (null == context) {
            return;
        }
        Intent intent = new Intent();
        if (context instanceof Application) {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        intent.putExtra(MediasoupConstant.key_service_join, isJoin);
        intent.setClass(context, MediasoupService.class);
        context.startService(intent);
    }

    /**
     * 停止服务
     *
     * @param context
     */
    public void stopMediasoupService(Context context) {
        LogUtils.e(TAG, "stopMediasoupService context:" + context);
        if (null == context) {
            return;
        }
        context.stopService(new Intent(context, MediasoupService.class));
    }

    public static void startMediasoupRoom(Context context) {
        LogUtils.i(TAG, "startMediasoupRoom:");
        MediasoupLoaderUtils.getInstance().mediasoupStart("", context);
    }

    public static Intent getMediasoupIntent(Context context) {
        LogUtils.i(TAG, "getMediasoupIntent:");
        return new Intent(context, RoomActivity.class);
    }
}
