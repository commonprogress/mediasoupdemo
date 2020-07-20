package com.jsy.mediasoup;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.jsy.mediasoup.interfaces.RoomManagementCallback;
import com.jsy.mediasoup.vm.MeProps;
import com.jsy.mediasoup.vm.RoomProps;
import com.jsy.mediasoup.utils.LogUtils;

import org.json.JSONObject;
import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomConstant;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.Utils;
import org.mediasoup.droid.lib.interfaces.MediasoupConnectCallback;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.model.Peers;
import org.threeten.bp.Duration;
import org.threeten.bp.Instant;

import java.util.List;

public class RoomManagement implements MediasoupConnectCallback {
    private static final String TAG = RoomManagement.class.getSimpleName();
    private final Context mContext;
    private String curRegister;
    //房间id 自己userid 自己名字
    private String mRoomId, mPeerId, mClientId, mDisplayName;
    //视频编码
    private boolean mForceH264, mForceVP9;
    private boolean isP2PMode;
    private RoomConstant.ConnectionState connectionState = RoomConstant.ConnectionState.NEW;
    private MediasoupConstant.NetworkMode networkMode = MediasoupConstant.NetworkMode.UNKNOWN;
    private RoomOptions mOptions;//房间的配置信息
    private RoomStore mRoomStore;
    private RoomClient mRoomClient;//房间操作类
    private int roomMode;
    private Handler mLooperHandler;
    private RoomManagementCallback managementCallback;
    private boolean isJoinSuc = false;//是否加入房间成功过
    private boolean isOtherJoin = false;//是否有用户加入过房间
    private boolean isVisibleCall = true;
    private boolean isMediasoupCalling = false;
    private String mConnectHost;
    private int mConnectPort;
    private boolean isEnableCamera;//摄像头是否可用
    private boolean isMuteAudio;//是否静音模式
    private TalkTimeRunnable mTalkTimeTicker;//通话计时器

    public String getCurRegister() {
        return curRegister;
    }

    public String getRoomId() {
        return mRoomId;
    }

    public boolean isP2PMode() {
        return isP2PMode;
    }

    public String getSelfPeerId() {
        return mPeerId;
    }

    public String getSelfClientId() {
        return mClientId;
    }

    public String getSelfName() {
        return mDisplayName;
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
        MediasoupLoaderUtils.getInstance().joinMediasoupState(getCurRegister(), connectionState.getIndex());
    }

    public RoomConstant.ConnectionState getConnectionState() {
        return connectionState;
    }

    public boolean isRoomConnecting() {
        if (null == connectionState) {
            return false;
        }
        return isMediasoupCalling && (RoomConstant.ConnectionState.CONNECTED.equals(connectionState)
            || RoomConstant.ConnectionState.DISCONNECTED.equals(connectionState)
            || RoomConstant.ConnectionState.CONNECTING.equals(connectionState));
    }

    public boolean isRoomConnected() {
        if (null == connectionState) {
            return false;
        }
        return isMediasoupCalling && RoomConstant.ConnectionState.CONNECTED.equals(connectionState);
    }

    public boolean isRoomClosed() {
        if (null == connectionState) {
            return false;
        }
        return RoomConstant.ConnectionState.CLOSED.equals(connectionState) || (null != mRoomClient ? mRoomClient.isClosed() : true);
    }

    public void setNetworkMode(MediasoupConstant.NetworkMode networkMode) {
        this.networkMode = networkMode;
    }

    public MediasoupConstant.NetworkMode getNetworkMode() {
        return networkMode;
    }

    public boolean isJoinSuc() {
        return isJoinSuc;
    }

    @Override
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

    public void startIfCallIsActive() {
        MediasoupLoaderUtils.getInstance().startIfCallIsActive(getCurRegister());
    }

    public void create() {
        isMediasoupCalling = true;
        mLooperHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 创建房间 初始化房间信息
     */
    public void createInitRoom() {
        LogUtils.i(TAG, "==createInitRoom mediasoup==");
        mOptions = new RoomOptions();
        //配置room信息
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        curRegister = MediasoupLoaderUtils.getInstance().getCurRegister();
        isP2PMode = MediasoupLoaderUtils.getInstance().isOneOnOneCall(getCurRegister());
        mRoomId = MediasoupLoaderUtils.getInstance().getCurRConvId(getCurRegister());
        mRoomId = MediasoupLoaderUtils.getInstance().getCurRConvId(getCurRegister());
        mPeerId = MediasoupLoaderUtils.getInstance().getCurUserId(getCurRegister());
        mDisplayName = MediasoupLoaderUtils.getInstance().getDisplayName(getCurRegister());
        mClientId = MediasoupLoaderUtils.getInstance().getCurClientId(getCurRegister());
//        isP2PMode = false;
//        mRoomId = "dongxl";
        updateRoomOptions();
        // Device config. 获取上传保存的摄像头信息 默认前置摄像头
        String camera = preferences.getString("camera", "front");
        PeerConnectionUtils.setPreferCameraFace(camera);
        MediasoupLoaderUtils.getInstance().setCurCameraFace(camera);

        mRoomStore = new RoomStore();
        //初始化房间
        mRoomClient =
            new RoomClient(
                mContext, mRoomStore, curRegister, isP2PMode, mRoomId, mPeerId, mClientId, mDisplayName, mForceH264, mForceVP9, mOptions, this);
        boolean isReady = MediasoupLoaderUtils.getInstance().setRoomClientStoreOptions(getCurRegister(), mRoomClient, mRoomStore, mOptions);
        if (null != managementCallback) {
            managementCallback.onMediasoupReady(isReady);
        }
    }

    private void updateRoomOptions() {
        if (null == mOptions || null == mRoomClient) {
            return;
        }

        if (Utils.isEmptyString(mRoomClient.getIsRegister()) || !mRoomClient.getIsRegister().equals(MediasoupLoaderUtils.getInstance().getCurRegister())) {
            curRegister = MediasoupLoaderUtils.getInstance().getCurRegister();
            isP2PMode = MediasoupLoaderUtils.getInstance().isOneOnOneCall(getCurRegister());
            mRoomId = MediasoupLoaderUtils.getInstance().getCurRConvId(getCurRegister());
            mPeerId = MediasoupLoaderUtils.getInstance().getCurUserId(getCurRegister());
            mDisplayName = MediasoupLoaderUtils.getInstance().getDisplayName(getCurRegister());
            mClientId = MediasoupLoaderUtils.getInstance().getCurClientId(getCurRegister());
            mRoomClient.updateRoomClient(curRegister, isP2PMode, mRoomId, mPeerId, mDisplayName, mClientId);
        }

//        isP2PMode = false;
//        mRoomId = "dongxl";
        mForceH264 = false;
        mForceVP9 = false;
        mOptions.setProduce(true);
        mOptions.setConsume(true);
        mOptions.setForceTcp(false);

        boolean isVideo = MediasoupLoaderUtils.getInstance().isMediasoupVideoState(getCurRegister());
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
        isEnableCamera = mOptions.isEnableVideo();
        isMuteAudio = !mOptions.isEnableAudio();
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
        if (MediasoupLoaderUtils.getInstance().isSelfCalling(getCurRegister())) {
            waitAnsweredTime();//自己发起等待对方响应且加入房间 等待对方响应加入
        } else if (MediasoupLoaderUtils.getInstance().isReceiveCall(getCurRegister())) {
            acceptAnsweredTime(false);//响应对方邀请 自己加入房间的等待
        }
        //有权限加入房间
        isJoinSuc = false;
        mRoomClient.joinRoom();
        MediasoupLoaderUtils.getInstance().configRequestMediasoup(getCurRegister());
    }

    /**
     * 自己发起等待对方响应且加入房间 等待对付响应
     */
    private void waitAnsweredTime() {
        if (!isRoomConnecting()) {
            MediasoupLoaderUtils.getInstance().sendMediasoupMsg(getCurRegister(), MediasoupConstant.CallState.Started);//发起mediasoup 邀请
            if (null != mLooperHandler) {
                mLooperHandler.postDelayed(() -> {
                    if (!isRoomConnected() || !isOtherJoin()) {
                        callSelfCancel(MediasoupConstant.ClosedReason.TimeoutEconn);
                    }
                }, MediasoupConstant.mediasoup_missed_time);
            }
        } else {
//            mMainHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 响应对方邀请 自己加入房间的等待
     *
     * @param isDisconnected 是否连接中断的等待
     */
    public void acceptAnsweredTime(boolean isDisconnected) {
        if (null != mLooperHandler && !isRoomConnecting()) {
            mLooperHandler.postDelayed(() -> {
                if (!isRoomConnected() || !isOtherJoin()) {
                    if (isDisconnected) {
                        callSelfEnd(MediasoupConstant.ClosedReason.TimeoutEconn);
                    } else {
                        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(getCurRegister(), MediasoupConstant.CallState.Missed);//长时间为响应
                        MediasoupLoaderUtils.getInstance().missedJoinMediasoup(getCurRegister());
                        closedJoinMediasoup(getCurRegister(), MediasoupConstant.ClosedReason.TimeoutEconn);//长时间为响应
                        closeWebSocketDestroyRoom(true);
                    }
                }
            }, MediasoupConstant.answered_missed_time);
        } else {
//            mMainHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * socket 连接的响应
     */
    public void answeredJoinMediasoup() {
        LogUtils.i(TAG, "answeredJoinMediasoup  connectionState:" + getConnectionState());
        MediasoupLoaderUtils.getInstance().answeredJoinMediasoup(getCurRegister(), isConnected());
        if (getConnectionState() == RoomConstant.ConnectionState.DISCONNECTED) {
            acceptAnsweredTime(true);
        }
        if (null != managementCallback) {
            managementCallback.onAnsweredState();
        }
    }

    /**
     * 建立通话或者有用户加入房间
     */
    public void establishedJoinMediasoup() {
        if (null != mLooperHandler) {
            mLooperHandler.removeCallbacksAndMessages(null);
        }
        Instant estabTime = MediasoupLoaderUtils.getInstance().establishedJoinMediasoup(getCurRegister());
        talkTimeCalculation(estabTime);
        if (null != managementCallback) {
            managementCallback.onEstablishedState(estabTime);
        }
    }

    /**
     * 通话时间开始计时
     *
     * @param estabTime
     */
    private void talkTimeCalculation(Instant estabTime) {
        LogUtils.i(TAG, "talkTimeCalculation  estabTime:" + estabTime);
        if (null == estabTime || estabTime == Instant.EPOCH) {
            if (null != mRoomStore) {
                mRoomStore.setCallTiming("");
            }
            return;
        }
        if (null != mLooperHandler) {
            mLooperHandler.removeCallbacksAndMessages(null);
            if (null == mTalkTimeTicker) {
                mTalkTimeTicker = new TalkTimeRunnable();
            }
            mTalkTimeTicker.setEstabTime(estabTime);
            mLooperHandler.postDelayed(mTalkTimeTicker, MediasoupConstant.mediasoup_duration_timing);
        }
    }

    /**
     * 通话时间实时计算
     */
    class TalkTimeRunnable implements Runnable {
        private Instant estabTime; //the time that a joined call was established, if any

        public void setEstabTime(Instant estabTime) {
            this.estabTime = estabTime;
        }

        @Override
        public void run() {
            if (isConnected()) {
                Duration duration = Duration.between(estabTime, Instant.now());
                if (null != mLooperHandler && null != mTalkTimeTicker) {
                    mTalkTimeTicker.setEstabTime(estabTime);
                    mLooperHandler.postDelayed(mTalkTimeTicker, MediasoupConstant.mediasoup_duration_timing);
                }
                String callTiming;
                if (null != duration && !duration.isNegative() && !duration.isZero()) {
                    int seconds = (int) ((duration.toMillis() / 1000) % 60);
                    int minutes = (int) ((duration.toMillis() / 1000) / 60);
                    callTiming = String.format("%02d:%02d", minutes, seconds);
                } else {
                    callTiming = "";
                }
//                LogUtils.i(TAG, "talkTimeCalculation estabTime:" + estabTime + ",duration:" + duration + ",callTiming:" + callTiming);
                if (null != mRoomStore) {
                    mRoomStore.setCallTiming(callTiming);
                }
            }
        }
    }

    /**
     * 关闭了通话
     *
     * @param isRegister
     * @param closedReason
     */
    public void closedJoinMediasoup(String isRegister, MediasoupConstant.ClosedReason closedReason) {
        MediasoupLoaderUtils.getInstance().closedMediasoup(isRegister, closedReason);
        if (null != managementCallback) {
            managementCallback.onClosedState();
        }
    }

    /**
     * 设置连接的 host和port
     *
     * @param host
     * @param port
     */
    public void setMediasoupProxy(String host, int port) {
        this.mConnectHost = host;
        this.mConnectPort = port;
    }

    @Override
    public String getConnectHost() {
        return Utils.isEmptyString(mConnectHost) ? MediasoupLoaderUtils.getInstance().getConnectHost() : mConnectHost;
    }

    @Override
    public int getConnectPort() {
        return mConnectPort <= 0 ? MediasoupLoaderUtils.getInstance().getConnectPort() : mConnectPort;
    }

    @Override
    public boolean isEnableAudioJoin() {
        return isJoinSuc() ? !isMuteAudio : mOptions.isEnableAudio();
    }

    @Override
    public boolean isEnableVideoJoin() {
        return isJoinSuc() ? isEnableCamera : mOptions.isEnableVideo();
    }

    @Override
    public void onConnectSuc() {
        LogUtils.i(TAG, "onConnectSuc isOtherJoin:" + isOtherJoin());
        if (null != managementCallback) {
            managementCallback.onConnectSuc(isOtherJoin());
        }
        setOtherJoin(false);
    }

    @Override
    public void onConnectFail() {
        LogUtils.e(TAG, "onConnectFail isOtherJoin:" + isOtherJoin());
        if (null != managementCallback) {
            managementCallback.onConnectFail(isOtherJoin());
        }
        setOtherJoin(false);
        answeredJoinMediasoup();

    }

    @Override
    public void onConnectDisconnected() {
        //连接中断 重新连接
        LogUtils.e(TAG, "onConnectDisconnected isOtherJoin:" + isOtherJoin());
        if (null != managementCallback) {
            managementCallback.onConnectDisconnected(isOtherJoin());
        }
        setOtherJoin(false);
        answeredJoinMediasoup();
    }

    @Override
    public void onConnectClose() {
        LogUtils.e(TAG, "onConnectClose isOtherJoin:" + isOtherJoin());
        if (null != managementCallback) {
            managementCallback.onConnectClose(isOtherJoin());
        }
        callSelfEnd(MediasoupConstant.ClosedReason.Normal);
        setOtherJoin(false);
        isJoinSuc = false;
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

    @Override
    public String getConnectPeerId() {
        return MediasoupLoaderUtils.getInstance().getIncomingUserId(getCurRegister());
    }

    @Override
    public String getConnectPeerName() {
        return MediasoupLoaderUtils.getInstance().getIncomingDisplayName(getCurRegister());
    }

    @Override
    public Peer getConnectPeer() {
        return new Peer(getConnectPeerId(), getConnectPeerName());
    }

    @Override
    public void sendOfferSdp(String peerId, JSONObject sdpJson) {
        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(getCurRegister(), MediasoupConstant.CallState.P2POffer, getRoomId(), sdpJson);
    }

    @Override
    public void sendAnswerSdp(String peerId, JSONObject sdpJson) {
        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(getCurRegister(), MediasoupConstant.CallState.P2PAnswer, getRoomId(), sdpJson);
    }

    @Override
    public void sendIceCandidate(String peerId, JSONObject iceJson) {
        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(getCurRegister(), MediasoupConstant.CallState.P2PIce, getRoomId(), iceJson);
    }

    @Override
    public void onJoinSuc(int existPeer) {
        LogUtils.i(TAG, "onJoinSuc isOtherJoin:" + isOtherJoin() + ", existPeer:" + existPeer);
        isJoinSuc = true;
        if (null != managementCallback) {
            managementCallback.onJoinSuc(existPeer);
        }
        if (existPeer > 0 /*&& MediasoupLoaderUtils.getInstance().isReceiveCall(getCurRegister())*/) {
            establishedJoinMediasoup();
        } else {
            answeredJoinMediasoup();
        }
    }

    /**
     * p2p 连接模式 加入或者连接失败
     */
    @Override
    public void onP2PJoinFail() {
        LogUtils.e(TAG, "onP2PJoinFail");
        isJoinSuc = false;
        if (null != managementCallback) {
            managementCallback.onP2PJoinFail();
        }
//        if (null != mRoomClient) {
//            mRoomClient.switchP2POrMediasoup(false);
//        }
    }

    /**
     * mediasoup 连接模式 加入房间失败
     */
    @Override
    public void onMediasoupJoinFail() {
        LogUtils.e(TAG, "onMediasoupJoinFail");
        isJoinSuc = false;
        if (null != managementCallback) {
            managementCallback.onMediasoupJoinFail();
        }
        callSelfEnd(MediasoupConstant.ClosedReason.Error);//加入房间失败
    }

    /**
     * p2p 建立通话后，连接断开 失败
     */
    @Override
    public void onP2PConnectionFailed() {
        LogUtils.e(TAG, "onP2PConnectionFailed");
        callSelfEnd(MediasoupConstant.ClosedReason.Error);//p2p 建立通话后，连接断开 失败
    }

    /**
     * 是否需要重新交换sdp
     *
     * @param isRenegotiation true 重新交换
     */
    @Override
    public void onP2PReExchangeSDP(boolean isRenegotiation) {
        LogUtils.e(TAG, "onP2PReExchangeSDP isRenegotiation:" + isRenegotiation);
        if (null == mRoomClient || !isRenegotiation) {
            return;
        }
        mRoomClient.onP2PReExchangeSDP(isRenegotiation);
    }

    public RoomProps getRoomProps() {
        if (null != managementCallback) {
            return managementCallback.getRoomProps();
        }
        return null;
    }

    public MeProps getMeProps() {
        if (null != managementCallback) {
            return managementCallback.getMeProps();
        }
        return null;
    }

    public Peers getCurRoomPeers() {
        return null == mRoomStore ? null : mRoomStore.getPeers().getValue();
    }

    public boolean isContainsCurPeer(String peerId) {
        Peers peers = getCurRoomPeers();
        return null == peers ? false : peers.isContainsCurPeer(peerId);
    }

    public List<Peer> getCurRoomPeerList() {
        Peers peers = getCurRoomPeers();
        return null == peers ? null : peers.getAllPeers();
    }

    public int getCurRoomPeerSize() {
        List<Peer> peers = getCurRoomPeerList();
        return null == peers ? 0 : peers.size();
    }

    public void updatePeerVideoAudioState(String peerId, boolean isVideoVisible, boolean isAudioEnabled) {
        if (null != mRoomStore) {
            mRoomStore.updatePeerVideoAudioState(peerId, isVideoVisible, isAudioEnabled);
        }
    }

    public void onReceiveP2POffer(String peerId, JSONObject jsonData) {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.createP2PAnswerSdp(peerId, jsonData);
    }

    public void onReceiveP2PAnswer(String peerId, JSONObject jsonData) {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.setP2PAnswerSdp(peerId, jsonData);
    }

    public void onReceiveP2PIce(String peerId, JSONObject jsonData) {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.addP2PIceCandidate(peerId, jsonData);
    }

    /**
     * 自己的其他设备已经接受
     */
    public void onSelfOtherAcceptCall(String isRegister) {
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(getCurRegister());
        if (isSameRegister) {
            closedJoinMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal);//自己的其他设备已经接受
            closeWebSocketDestroyRoom(true);
        } else {
            closedJoinMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal);//自己的其他设备已经接受
        }
    }

    /**
     * 对方接受邀请
     */
    public void onOtherAcceptCall(String isRegister) {
        //自己发起 且连接
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(getCurRegister());
        LogUtils.i(TAG, "onOtherAcceptCall isMediasoupCalling:" + isMediasoupCalling + ", connectionState:" + getConnectionState() + ", isSameRegister:" + isSameRegister + ",isP2PMode:" + isP2PMode());
        if (isSameRegister) {
            if (MediasoupLoaderUtils.getInstance().isSelfCalling(isRegister) && isRoomConnecting()) {
                if (isOtherJoin()) {
                    establishedJoinMediasoup();
                } else {
                    if (isP2PMode()) {
                        if (null == mRoomClient) {
                            return;
                        }
                        mRoomClient.createP2POfferSdp(getConnectPeerId());
                    }
                    answeredJoinMediasoup();
                }
            } else if (MediasoupLoaderUtils.getInstance().isSelfCalling(isRegister) && !isRoomConnecting()) {
                LogUtils.e(TAG, "onOtherAcceptCall isMediasoupCalling:" + isMediasoupCalling + ", connectionState:" + getConnectionState() + ", isSameRegister:" + isSameRegister + ",isP2PMode:" + isP2PMode());
                callSelfEnd(MediasoupConstant.ClosedReason.IOError);
            }
        } else {

        }
    }

    /**
     * 对方拒绝
     */
    public void onOtherRejectCall(String isRegister) {
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(isRegister);
        LogUtils.e(TAG, "onOtherRejectCall isMediasoupCalling:" + isMediasoupCalling + ", connectionState:" + getConnectionState() + ", isSameRegister:" + isSameRegister);
        if (isSameRegister) {
            if (MediasoupLoaderUtils.getInstance().isOneOnOneCall(isRegister) || (!isRoomConnecting() && !MediasoupLoaderUtils.getInstance().isReceiveCall(isRegister))) {
                closedJoinMediasoup(isRegister, MediasoupConstant.ClosedReason.Rejected);//对方拒绝
                closeWebSocketDestroyRoom(true);
            }
        } else {
            closedJoinMediasoup(isRegister, MediasoupConstant.ClosedReason.Rejected);//对方拒绝
        }
    }

    /**
     * 对方挂掉
     */
    public void onOtherEndCall(String isRegister, int count) {
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(getCurRegister());
        int peerSize = getCurRoomPeerSize();
        LogUtils.e(TAG, "onOtherEndCall count :" + count + ", peerSize:" + peerSize + ", isMediasoupCalling:" + isMediasoupCalling + ", connectionState:" + getConnectionState() + ", isSameRegister:" + isSameRegister);
        if (isSameRegister) {
            if (null == managementCallback || (!isRoomConnecting() && !MediasoupLoaderUtils.getInstance().isReceiveCall(isRegister)) || (MediasoupLoaderUtils.getInstance().isOneOnOneCall(isRegister) && (peerSize <= 0 || isP2PMode()))) {
                closedJoinMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal);//对方挂掉
                closeWebSocketDestroyRoom(true);
            } else {
                if (MediasoupLoaderUtils.getInstance().isOneOnOneCall(isRegister) || peerSize <= 0 || count <= 1) {
                    //一对一 连接中 且有人
                    managementCallback.onAllLeaveRoom();
                    managementCallback.onDelayedCheckRoom();
                }
            }
        } else {
            closedJoinMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal);//对方挂掉
        }
    }

    /**
     * 对方取消呼叫
     */
    public void onOtherCloseCall(String isRegister) {
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(getCurRegister());
        LogUtils.e(TAG, "onOtherCloseCall isMediasoupCalling:" + isMediasoupCalling + ", connectionState:" + getConnectionState() + ", isSameRegister:" + isSameRegister);
        if (isSameRegister) {
            if (MediasoupLoaderUtils.getInstance().isOneOnOneCall(isRegister) || (!isRoomConnecting() && !MediasoupLoaderUtils.getInstance().isReceiveCall(isRegister))) {
                closedJoinMediasoup(isRegister, MediasoupConstant.ClosedReason.Canceled);//对方取消呼叫
                closeWebSocketDestroyRoom(true);
            }
        } else {
            closedJoinMediasoup(isRegister, MediasoupConstant.ClosedReason.Canceled);//对方取消呼叫
        }
    }

    /**
     * 对方未响应呼叫
     */
    public void onOtherMissedCall(String isRegister) {
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(getCurRegister());
        LogUtils.e(TAG, "onOtherMissedCall isMediasoupCalling:" + isMediasoupCalling + ", connectionState:" + getConnectionState() + ", isSameRegister:" + isSameRegister);
        if (isSameRegister) {
            if (MediasoupLoaderUtils.getInstance().isOneOnOneCall(isRegister) || (!isRoomConnecting() && !MediasoupLoaderUtils.getInstance().isReceiveCall(isRegister))) {
                closedJoinMediasoup(isRegister, MediasoupConstant.ClosedReason.TimeoutEconn);//对方未响应呼叫
                closeWebSocketDestroyRoom(true);
            }
        } else {
            closedJoinMediasoup(isRegister, MediasoupConstant.ClosedReason.TimeoutEconn);//对方未响应呼叫
        }
    }

    /**
     * 对方忙碌中
     */
    public void onOtherBusyedCall(String isRegister) {
        boolean isSameRegister = !Utils.isEmptyString(isRegister) && isRegister.equals(getCurRegister());
        LogUtils.e(TAG, "onOtherBusyedCall isMediasoupCalling:" + isMediasoupCalling + ", connectionState:" + getConnectionState() + ", isSameRegister:" + isSameRegister);
        if (isSameRegister) {
            if (MediasoupLoaderUtils.getInstance().isOneOnOneCall(isRegister) || (!isRoomConnecting() && !MediasoupLoaderUtils.getInstance().isReceiveCall(isRegister))) {
                closedJoinMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal);//对方忙碌中
                closeWebSocketDestroyRoom(true);
            }
        } else {
            closedJoinMediasoup(isRegister, MediasoupConstant.ClosedReason.Normal);//对方忙碌中
        }
    }

    /**
     * 自己接受或加入
     */
    public void setSelfAcceptOrJoin(String isRegister) {
        LogUtils.i(TAG, "setSelfAcceptOrJoin isRegister:" + isRegister + ", connectionState:" + getConnectionState() + ", peersize:" + getCurRoomPeerSize());
        if (null != managementCallback) {
            managementCallback.onSelfAcceptOrJoin();
        }
    }

    /**
     * 自己接受
     */
    public void callSelfAccept() {
        LogUtils.i(TAG, "callSelfAccept isRegister:" + getCurRegister() + ", connectionState:" + getConnectionState() + ", peersize:" + getCurRoomPeerSize());
        //0发起，1接受，2拒绝，3结束，4取消
        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(getCurRegister(), MediasoupConstant.CallState.Accepted);//自己接受
        setSelfAcceptOrJoin(getCurRegister());
    }

    /**
     * 自己结束
     */
    public void callSelfEnd(MediasoupConstant.ClosedReason closedReason) {
        LogUtils.i(TAG, "callSelfEnd isRegister:" + getCurRegister() + ", connectionState:" + getConnectionState() + ", peersize:" + getCurRoomPeerSize());
        leaveRoom();
        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(getCurRegister(), MediasoupConstant.CallState.Ended);//自己结束
        closedJoinMediasoup(getCurRegister(), closedReason);//自己结束
        closeWebSocketDestroyRoom(true);
    }

    /**
     * 自己取消
     */
    public void callSelfCancel(MediasoupConstant.ClosedReason closedReason) {
        LogUtils.i(TAG, "callSelfCancel isRegister:" + getCurRegister() + ", connectionState:" + getConnectionState() + ", peersize:" + getCurRoomPeerSize());
        leaveRoom();
        closedJoinMediasoup(getCurRegister(), closedReason);//自己取消
        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(getCurRegister(), MediasoupConstant.CallState.Canceled);//自己取消
        closeWebSocketDestroyRoom(true);
    }

    /**
     * 自己拒绝
     */
    public void callSelfReject() {
        LogUtils.i(TAG, "callSelfReject isRegister:" + getCurRegister() + ", connectionState:" + getConnectionState() + ", peersize:" + getCurRoomPeerSize());
        leaveRoom();
        MediasoupLoaderUtils.getInstance().sendMediasoupMsg(getCurRegister(), MediasoupConstant.CallState.Rejected);//自己拒绝
        closedJoinMediasoup(getCurRegister(), MediasoupConstant.ClosedReason.Rejected);//自己拒绝
        closeWebSocketDestroyRoom(true);
    }

    /**
     * 自己  拒绝 结束 取消
     */
    public void rejectEndCancelCall() {
        MediasoupLoaderUtils.getInstance().rejectEndCancelCall(getCurRegister());
    }

    /**
     * 禁用和启用摄像头
     *
     * @param isEnable true 启用摄像头
     */
    public void disableAndEnableCam(boolean isEnable) {
        LogUtils.i(TAG, "disableAndEnableCam enableCam disableCam isEnable:" + isEnable + ", connectionState:" + getConnectionState());
        if (mRoomClient != null && isRoomConnected() && this.isEnableCamera != isEnable) {
            this.isEnableCamera = isEnable;
            if (isEnable) {
                //启用摄像头
                mRoomClient.enableCam();
            } else {
                //关闭摄像头
                mRoomClient.disableCam();
            }
        }
    }

    /**
     * 是否静音
     *
     * @param muted
     */
    public void setCallMuted(boolean muted) {
        LogUtils.i(TAG, "setCallMuted muteMic isRegister:" + getCurRegister() + ", connectionState:" + getConnectionState() + ", peersize:" + getCurRoomPeerSize());
        if (mRoomClient != null && isRoomConnected() && this.isMuteAudio != muted) {
            this.isMuteAudio = muted;
            if (muted) {
                mRoomClient.muteMic();
            } else {
                mRoomClient.unmuteMic();
            }
        }
    }

    /**
     * 切换摄像头
     */
    public void switchCam() {
        LogUtils.i(TAG, "switchCam isRegister:" + getCurRegister() + ", connectionState:" + getConnectionState() + ", peersize:" + getCurRoomPeerSize());
        if (null != mRoomClient) {
            mRoomClient.changeCam();
        }
    }

    /**
     * 相机打开状态
     */
    public void cameraOpenState(boolean isFail) {
        MediasoupLoaderUtils.getInstance().cameraOpenState(getCurRegister(), isFail);
    }

    /**
     * 网络状态改变
     */
    public void onNetworkChanged(String isRegister, MediasoupConstant.NetworkMode networkMode) {
        LogUtils.v(TAG, "onNetworkChanged isMediasoupCalling:" + isMediasoupCalling + ", connectionState:" + getConnectionState() + ", networkMode:" + networkMode);
        setNetworkMode(networkMode);
        if (null != mRoomStore) {
            mRoomStore.setNetworkMode(networkMode);
        }
        if (mRoomClient != null && isRoomConnected() && MediasoupConstant.isAvailableNetwork(networkMode)) {
            //重启ice
            mRoomClient.restartIce();
        }
    }

    /**
     * 离开房间
     */
    public void leaveRoom() {
        LogUtils.i(TAG, "leaveRoom isRegister");
        if (mRoomClient != null && isRoomConnected() && isP2PMode()) {
            mRoomClient.leaveRoom();
        }
    }

    /**
     * 关闭WebSocket销毁房间
     */
    public void closeWebSocketDestroyRoom(boolean isFinish) {
        LogUtils.i(TAG, "closeWebSocketDestroyRoom isRegister:" + getCurRegister() + " ,isFinish:" + isFinish);
//        if (closeVideoAndAudio()) {
//            mMainHandler.postDelayed(() -> {
//                destroyRoom();
//                if (isFinish) {
//                    if (null != managementCallback) {
//                        managementCallback.onFinishServiceActivity();
//                    }
//                }
//            }, 200);
//        } else {
        MediasoupLoaderUtils.getInstance().endedMediasoup(getCurRegister(), getRoomId());
        destroyRoom();
        if (isFinish) {
            if (null != managementCallback) {
                managementCallback.onFinishServiceActivity();
            }
        }
//        }
    }

    /**
     * 关闭摄像头和暂停音频
     */
    public boolean closeVideoAndAudio() {
        if (null != mRoomClient) {
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
        isJoinSuc = false;
        isVisibleCall = false;
        mConnectHost = "";
        mConnectPort = 0;
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
        isEnableCamera = false;
        isMuteAudio = false;
        managementCallback = null;
        if (null != mLooperHandler) {
            mLooperHandler.removeCallbacksAndMessages(null);
        }
        mLooperHandler = null;
        mTalkTimeTicker = null;
        closedJoinMediasoup(getCurRegister(), MediasoupConstant.ClosedReason.Normal);//mediasoup  销毁
        MediasoupLoaderUtils.getInstance().mediasoupDestroy(getCurRegister());
        curRegister = "";
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
    public void setEnableSpeaker(boolean speaker) {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.setEnableSpeaker(speaker);
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
     * 改变摄像头采集的分辨率
     *
     * @param videoSize
     */
    public void changeCaptureFormat(int videoSize) {
        if (null == mRoomClient) {
            return;
        }
        mRoomClient.changeCaptureFormat(videoSize);
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
