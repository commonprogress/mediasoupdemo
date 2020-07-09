package com.jsy.mediasoup;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.jsy.mediasoup.interfaces.RoomManagementCallback;
import com.jsy.mediasoup.vm.RoomProps;
import com.jsy.mediasoup.utils.LogUtils;

import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomConstant;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.interfaces.MediasoupConnectCallback;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.model.Peers;

import java.util.List;

public class RoomManagement implements MediasoupConnectCallback {
    private static final String TAG = RoomManagement.class.getSimpleName();
    private final Context mContext;
    //房间id 自己userid 自己名字
    private String mRoomId, mPeerId, mClientId, mDisplayName;
    //视频编码
    private boolean mForceH264, mForceVP9;
    private boolean mP2PMode;
    private RoomConstant.ConnectionState connectionState = RoomConstant.ConnectionState.NEW;
    private RoomOptions mOptions;//房间的配置信息
    private RoomStore mRoomStore;
    private RoomClient mRoomClient;//房间操作类
    private int roomMode;
    private Handler mMainHandler;
    private RoomManagementCallback managementCallback;
    private boolean isOtherJoin = false;
    private boolean isVisibleCall = true;
    private boolean isMediasoupCalling = false;

    public String getRoomId() {
        return mRoomId;
    }

    public void setRoomId(String mRoomId) {
        this.mRoomId = mRoomId;
    }

    public String getPeerId() {
        return mPeerId;
    }

    public void setPeerId(String mPeerId) {
        this.mPeerId = mPeerId;
    }

    public String getClientId() {
        return mClientId;
    }

    public void setClientId(String mClientId) {
        this.mClientId = mClientId;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    public void setDisplayName(String mDisplayName) {
        this.mDisplayName = mDisplayName;
    }

    public RoomManagement(Context context) {
        this.mContext = context;
        MediasoupLoaderUtils.getInstance().mediasoupActivityCreate(mContext);
    }

    public RoomManagement(Context context, RoomManagementCallback callback) {
        this(context);
        setRoomManagementCallback(callback);
    }

    public void setRoomManagementCallback(RoomManagementCallback callback) {
        this.managementCallback = callback;
    }

    public void setRoomMode(int roomMode) {
        this.roomMode = roomMode;
    }

    public RoomStore getRoomStore() {
        return mRoomStore;
    }

    public RoomClient getRoomClient() {
        return mRoomClient;
    }

    public void setConnectionState(RoomConstant.ConnectionState connectionState) {
        this.connectionState = connectionState;
        MediasoupLoaderUtils.getInstance().joinMediasoupState(connectionState.ordinal());
    }

    public boolean isRoomConnecting() {
        if (null == connectionState) {
            return false;
        }
        return isMediasoupCalling && (RoomConstant.ConnectionState.CONNECTED.equals(connectionState) || RoomConstant.ConnectionState.DISCONNECTED.equals(connectionState) || RoomConstant.ConnectionState.CONNECTING.equals(connectionState));
    }

    public boolean isRoomConnected() {
        if (null == connectionState) {
            return false;
        }
        return isMediasoupCalling && (RoomConstant.ConnectionState.CONNECTED.equals(connectionState));
    }

    public boolean isRoomClosed() {
        if (null == connectionState) {
            return false;
        }
        return RoomConstant.ConnectionState.CLOSED.equals(connectionState) || (null != mRoomClient ? mRoomClient.isClosed() : true);
    }

    public boolean isOtherJoin() {
        return isOtherJoin;
    }

    public void setOtherJoin(boolean otherJoin) {
        isOtherJoin = otherJoin;
    }

    public void setVisibleCall(boolean isVisible) {
        this.isVisibleCall = isVisible;
    }

    public boolean isVisibleCall() {
        return isVisibleCall;
    }

    public void create() {
        isMediasoupCalling = true;
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public void acceptAnsweredTime() {
        if (!isRoomConnecting() && MediasoupLoaderUtils.getInstance().isReceiveCall()) {
            mMainHandler.postDelayed(() -> {
                if (!isRoomConnecting()) {
                    MediasoupLoaderUtils.getInstance().sendMediasoupMsg(MediasoupConstant.CallState.Missed);//长时间为响应
                    MediasoupLoaderUtils.getInstance().missedJoinMediasoup();
                    MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.TimeoutEconn);//长时间为响应
                }
            }, MediasoupConstant.mediasoup_missed_time);
        } else {
            mMainHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 创建房间 初始化房间信息
     */
    public void createInitRoom() {
        LogUtils.i(TAG, "==createInitRoom mediasoup==");
        mOptions = new RoomOptions();
        //配置room信息
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        // Room initial config.
//        mRoomId = preferences.getString("roomId", "");
//        mPeerId = preferences.getString("peerId", "");
//        mDisplayName = preferences.getString("displayName", "");
        mForceH264 = preferences.getBoolean("forceH264", false);
        mForceVP9 = preferences.getBoolean("forceVP9", false);
        mP2PMode = MediasoupLoaderUtils.getInstance().isOneOnOneCall();
        mRoomId = MediasoupLoaderUtils.getInstance().getCurRConvId();
        mPeerId = MediasoupLoaderUtils.getInstance().getCurUserId();
        mDisplayName = MediasoupLoaderUtils.getInstance().getDisplayName();
        mClientId = MediasoupLoaderUtils.getInstance().getCurClientId();

//        if (TextUtils.isEmpty(mRoomId)) {
//            mRoomId = Utils.getRandomString(8);
//            preferences.edit().putString("roomId", "mRoomId").apply();
//        }
//        if (TextUtils.isEmpty(mPeerId)) {
//            mPeerId = Utils.getRandomString(8);
//            preferences.edit().putString("peerId", mPeerId).apply();
//        }
//        if (TextUtils.isEmpty(mDisplayName)) {
//            mDisplayName = Utils.getRandomString(8);
//            preferences.edit().putString("displayName", mDisplayName).apply();
//        }

        // Room action config. 房间配置
//        mOptions.setProduce(preferences.getBoolean("produce", true));//是否立即打开摄像头和录音？
//        mOptions.setConsume(preferences.getBoolean("consume", true));//是否立即连接，显示对方音视频等？
        mOptions.setForceTcp(preferences.getBoolean("forceTcp", false));//是否强制tcp 否则rtc
        mOptions.setForceTcp(false);
//        mRoomId = "dongxl";
        mForceH264 = true;
        mForceVP9 = true;
        updateRoomOptions();

        // Device config. 获取上传保存的摄像头信息 默认前置摄像头
        String camera = preferences.getString("camera", "front");
        PeerConnectionUtils.setPreferCameraFace(camera);
        MediasoupLoaderUtils.getInstance().setCurCameraFace(camera);

        mRoomStore = new RoomStore();
        //初始化房间
        mRoomClient =
                new RoomClient(
                        mContext, mRoomStore, mP2PMode, mRoomId, mPeerId, mClientId, mDisplayName, mForceH264, mForceVP9, mOptions, this);
        boolean isReady = MediasoupLoaderUtils.getInstance().setRoomClientStoreOptions(mRoomClient, mRoomStore, mOptions);
        if (null != managementCallback) {
            managementCallback.onMediasoupReady(isReady);
        }
    }

    private void updateRoomOptions() {
        if (null == mOptions || null == mRoomClient) {
            return;
        }
        boolean isVideo = MediasoupLoaderUtils.getInstance().isVideoIncoming();
        roomMode = isVideo ? MediasoupConstant.roommode_video : MediasoupConstant.roommode_audio;
        if (roomMode == MediasoupConstant.roommode_see) {
            //观看模式
            mOptions.setProduce(false);
            mOptions.setConsume(true);
        } else {
            mOptions.setProduce(true);
            mOptions.setConsume(true);
            if (roomMode == MediasoupConstant.roommode_video) {
                mOptions.setEnableAudio(true);
                mOptions.setEnableVideo(true);
            } else if (roomMode == MediasoupConstant.roommode_audio) {
                mOptions.setEnableAudio(true);
                mOptions.setEnableVideo(false);
            } else if (roomMode == MediasoupConstant.roommode_mute) {
                mOptions.setEnableAudio(false);
                mOptions.setEnableVideo(true);
            } else if (roomMode == MediasoupConstant.roommode_noall) {
                mOptions.setEnableAudio(false);
                mOptions.setEnableVideo(false);
            }
        }
        LogUtils.i(TAG, "updateRoomOptions isVideo:" + isVideo + ", roomMode:" + roomMode);
//        mRoomClient.setRoomOptions(mOptions);
    }


    /**
     * 加入房间
     */
    public void joinMediasoupRoom() {
        LogUtils.i(TAG, "joinMediasoupRoom null == mRoomClient:" + (null == mRoomClient));
        if (null == mRoomClient) {
            return;
        }
        updateRoomOptions();
        //有权限加入房间
        mRoomClient.joinRoom();
        waitAnsweredTime();
    }

    /**
     * 发起后 等待响应
     */
    private void waitAnsweredTime() {
        if (isRoomConnecting() || !MediasoupLoaderUtils.getInstance().isSelfCalling()) {
//            mMainHandler.removeCallbacksAndMessages(null);
            return;
        }
        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(MediasoupConstant.CallState.Started);//发起mediasoup 邀请
        mMainHandler.postDelayed(() -> {
            if (!isRoomConnecting() || !isOtherJoin()) {
                MediasoupLoaderUtils.getInstance().sendMediasoupMsg(MediasoupConstant.CallState.Missed);//发起后没人响应
                MediasoupLoaderUtils.getInstance().missedJoinMediasoup();
                MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.TimeoutEconn);//发起后没人响应
                closeWebSocketDestroyRoom(true);
            }
        }, MediasoupConstant.mediasoup_missed_time);
    }

    /**
     * 响应了邀请或者有用户加入房间
     */
    public void establishedJoinMediasoup() {
        mMainHandler.removeCallbacksAndMessages(null);
        MediasoupLoaderUtils.getInstance().establishedJoinMediasoup();
    }

    @Override
    public void onConnectSuc() {
        isOtherJoin = false;
        if (null != managementCallback) {
            managementCallback.onConnectSuc();
        }
        MediasoupLoaderUtils.getInstance().answeredJoinMediasoup();
    }

    @Override
    public void onConnectFail() {
        isOtherJoin = false;
        if (null != managementCallback) {
            managementCallback.onConnectFail();
        }
        webSocketConnectFail();
    }

    @Override
    public void onConnectDisconnected() {
        isOtherJoin = false;
        if (null != managementCallback) {
            managementCallback.onConnectDisconnected();
        }
        //连接中断 重新连接
        LogUtils.e(TAG, "onConnectDisconnected joinMediasoupRoom:");
//        joinMediasoupRoom();
    }

    @Override
    public void onConnectClose() {
        isOtherJoin = false;
        if (null != managementCallback) {
            managementCallback.onConnectClose();
        }
        MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.Normal);//websocket 关闭
    }

    @Override
    public void onJoinSuc() {
        if (null != managementCallback) {
            managementCallback.onJoinSuc();
        }
        if (MediasoupLoaderUtils.getInstance().isReceiveCall()) {
            establishedJoinMediasoup();
        }
    }

    @Override
    public void onJoinFail() {
        isOtherJoin = false;
        if (null != managementCallback) {
            managementCallback.onJoinFail();
        }
        MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.Error);//加入房间失败
    }

    @Override
    public boolean isConnecting() {
        return isRoomConnecting();
    }

    @Override
    public boolean isConnected() {
        return isRoomConnected();
    }

    /**
     * 获取共享屏幕需要参数
     */
    @Override
    public boolean reqShareScreenIntentData() {
        if (null != managementCallback) {
            return managementCallback.reqShareScreenIntentData();
        }
        return false;
    }

    public RoomProps getRoomProps() {
        if (null != managementCallback) {
            return managementCallback.getRoomProps();
        }
        return null;
    }

    public Peers getCurRoomPeers() {
        return null == mRoomStore ? null : mRoomStore.getPeers().getValue();
    }

    public List<Peer> getCurRoomPeerList() {
        Peers peers = getCurRoomPeers();
        return null == peers ? null : peers.getAllPeers();
    }

    /**
     * 自己的其他设备已经接受
     */
    public void onSelfOtherAcceptCall() {
        MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.Normal);//自己的其他设备已经接受
        closeWebSocketDestroyRoom(true);
    }

    /**
     * 对方接受邀请
     */
    public void onOtherAcceptCall() {
        if (MediasoupLoaderUtils.getInstance().isSelfCalling()) {
            establishedJoinMediasoup();
        }
    }

    /**
     * 对方拒绝
     */
    public void onOtherRejectCall() {
        if (MediasoupLoaderUtils.getInstance().isOneOnOneCall()) {
            MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.Rejected);//对方拒绝
            closeWebSocketDestroyRoom(true);
        }
    }

    /**
     * 对方挂掉
     */
    public void onOtherEndCall() {
        if (MediasoupLoaderUtils.getInstance().isOneOnOneCall()) {
            MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.Normal);//对方挂掉
            closeWebSocketDestroyRoom(true);
        }
    }

    /**
     * 对方取消呼叫
     */
    public void onOtherCloseCall() {
        MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.Canceled);//对方取消呼叫
        closeWebSocketDestroyRoom(false);
        if (null != managementCallback) {
            managementCallback.onFinishServiceActivity();
        }
    }

    /**
     * 对方未响应呼叫
     */
    public void onOtherMissedCall() {
        if (MediasoupLoaderUtils.getInstance().isOneOnOneCall()) {
            MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.TimeoutEconn);//对方未响应呼叫
            closeWebSocketDestroyRoom(true);
        }
    }

    /**
     * 自己接受或加入
     */
    public void setSelfAcceptOrJoin() {
        if (null != managementCallback) {
            managementCallback.onSelfAcceptOrJoin();
        }
    }

    /**
     * 自己接受
     */
    public void callSelfAccept() {
        //0发起，1接受，2拒绝，3结束，4取消
        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(MediasoupConstant.CallState.Accepted);//自己接受
        setSelfAcceptOrJoin();
    }

    /**
     * 自己结束
     */
    public void callSelfEnd() {
        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(MediasoupConstant.CallState.Ended);//自己结束
        MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.Normal);//自己结束
        closeWebSocketDestroyRoom(true);
    }

    /**
     * 自己取消
     */
    public void callSelfCancel() {
        MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.Canceled);//自己取消
        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(MediasoupConstant.CallState.Canceled);//自己取消
        closeWebSocketDestroyRoom(true);
    }

    /**
     * 自己拒绝
     */
    public void callSelfReject() {
        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(MediasoupConstant.CallState.Rejected);//自己拒绝
        MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.Rejected);//自己拒绝
        closeWebSocketDestroyRoom(false);
        if (null != managementCallback) {
            managementCallback.onFinishServiceActivity();
        }
    }

    /**
     * 拒绝 结束 取消
     */
    public void rejectEndCancelCall() {
        MediasoupLoaderUtils.getInstance().rejectEndCancelCall();
    }

    /**
     * 是否是否打开摄像头
     *
     * @param isEnable true 启用摄像头
     */
    public void disAndEnableCam(boolean isEnable) {
        if (null != mRoomClient && isRoomConnecting()) {
            if (isEnable) {
                //启用摄像头
                mRoomClient.enableCam();
            } else {
                //关闭摄像头
                mRoomClient.disableCam();
            }
        } else {

        }
    }

    /**
     * 是否静音
     *
     * @param muted
     */
    public void setCallMuted(boolean muted) {
        if (null != mRoomClient) {
            if (muted) {
                mRoomClient.muteAudio();
            } else {
                mRoomClient.unmuteAudio();
            }
        }
    }

    /**
     * 切换摄像头
     */
    public void switchCam() {
        if (null != mRoomClient) {
            mRoomClient.changeCam();
        }
    }

    /**
     * websocket 连接失败
     */
    private void webSocketConnectFail() {
        MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.Error);//webSocket连接失败
        if (mRoomClient != null) {
            mRoomClient.close();
        }
    }

    /**
     * 网络状态改变重启ice
     */
    public void onNetworkChanged() {
        if (mRoomClient != null && !isRoomClosed()) {
            mRoomClient.restartIce();
        }
    }

    /**
     * 关闭WebSocket销毁房间
     */
    public void closeWebSocketDestroyRoom(boolean isFinish) {
        if (closeVideoAndAudio()) {
            mMainHandler.postDelayed(() -> {
                destroyRoom();
                if (isFinish) {
                    if (null != managementCallback) {
                        managementCallback.onFinishServiceActivity();
                    }
                }
            }, 200);
        } else {
            destroyRoom();
            if (isFinish) {
                if (null != managementCallback) {
                    managementCallback.onFinishServiceActivity();
                }
            }
        }
    }

    /**
     * 关闭摄像头和暂停音频
     */
    public boolean closeVideoAndAudio() {
        if (null != mRoomClient && !isRoomClosed() && isRoomConnecting()) {
//            mRoomClient.disableCam();
//            mRoomClient.muteMic();

            mRoomClient.enableAudioOnly();
            mRoomClient.muteAudio();
            mRoomClient.muteMic();
            LogUtils.e(TAG, "closeVideoAndAudio 1 close ：运行中");
            return true;
        } else {
            return false;
        }
    }

    /**
     * 销毁房间
     */
    public void destroyRoom() {
        isOtherJoin = false;
        isVisibleCall = false;
        if (mRoomClient != null) {
            mRoomClient.close();
            mRoomClient = null;
        }
        if (mRoomStore != null) {
            mRoomStore = null;
        }
        LogUtils.e(TAG, "destroyRoom 2 close ：销毁房间");
    }

    public void destroy() {
        closeWebSocketDestroyRoom(false);
        isMediasoupCalling = false;
        managementCallback = null;
        mMainHandler.removeCallbacksAndMessages(null);
        MediasoupLoaderUtils.getInstance().closedMediasoup(MediasoupConstant.ClosedReason.Normal);//mediasoup  销毁
        MediasoupLoaderUtils.getInstance().mediasoupDestroy();
        LogUtils.e(TAG, "destroy 3 close ：服务销毁");
    }

    /**
     * 音视频都有
     */
    public void disableAudioOnly() {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.disableAudioOnly();
    }

    /**
     * 仅音频无视频
     */
    public void enableAudioOnly() {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.enableAudioOnly();
    }

    /**
     * 取消静音
     */
    public void unmuteAudio() {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.unmuteAudio();
    }

    /**
     * 静音
     */
    public void muteAudio() {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.muteAudio();
    }

    /**
     * 是否重启ice
     */
    public void restartIce() {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.restartIce();
    }

    /**
     * false 关闭扬声器打开听筒
     * true 打开扬声器 关闭听筒
     *
     * @param speaker
     */
    public void setSpeakerMute(boolean speaker) {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.setSpeakerMute(speaker);
    }

    /**
     * 修改自己名字
     *
     * @param displayName
     */
    public void changeDisplayName(String displayName) {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.changeDisplayName(displayName);
    }

    /**
     * 是否静音
     */
    public void muteMic() {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.muteMic();
    }

    /**
     * 是否静音
     */
    public void unmuteMic() {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.unmuteMic();
    }

    /**
     * 是否是否打开摄像头
     */
    public void disableCam() {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.disableCam();
    }

    /**
     * 是否是否打开摄像头
     */
    public void enableCam() {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.enableCam();
    }

    /**
     * 前后摄像头切换
     */
    public void changeCam() {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.changeCam();
    }

    /**
     * 屏幕共享 （功能暂未实现）
     */
    public void disableShare() {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.disableShare();
    }

    /**
     * 屏幕共享 （功能暂未实现）
     */
    public void enableShare() {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.enableShare();
    }

    /**
     * 设置共享屏幕共享的intent数据
     */
    public void setShareScreenIntentData(boolean isReqSuc) {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.startEnableShare(isReqSuc);
    }
}
