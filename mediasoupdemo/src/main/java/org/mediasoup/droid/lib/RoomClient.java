package org.mediasoup.droid.lib;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;

import android.text.TextUtils;

import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.p2p.P2PConnectFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Device;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.MediasoupException;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.RecvTransport;
import org.mediasoup.droid.SendTransport;
import org.mediasoup.droid.Transport;
import org.mediasoup.droid.lib.interfaces.MediasoupConnectCallback;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Producers;
import org.mediasoup.droid.lib.socket.WebSocketTransport;
import org.protoojs.droid.Message;
import org.protoojs.droid.ProtooException;
import org.webrtc.AudioTrack;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.VideoTrack;

import io.reactivex.disposables.CompositeDisposable;

import static org.mediasoup.droid.lib.JsonUtils.jsonPut;
import static org.mediasoup.droid.lib.JsonUtils.toJsonObject;

/**
 * 房间创建和连接状态
 */
public class RoomClient extends RoomMessageHandler {
    // Closed flag.
    private volatile boolean mClosed;
    // Android context.
    private final Context mContext;
    // PeerConnection util.
    private PeerConnectionUtils mPeerConnectionUtils;
    private P2PConnectFactory mP2PConnectFactory;
    private MediasoupManager mMediasoupManager;
    // Room mOptions.
    private @NonNull
    RoomOptions mOptions;
    private final String mRoomId;
    private final String mSelfId;
    // Display name.
    private String mDisplayName;
    private final String mClientId;
    private final boolean mForceVP9;
    private final boolean mForceH264;
    //p2p connect mode
    private boolean isP2PMode;
    // TODO(Haiyangwu):Next expected dataChannel test number.
    private long mNextDataChannelTestNumber;
    // Protoo URL.
    private String mProtooUrl;
    // mProtoo-client Protoo instance.
    private Protoo mProtoo;
    // mediasoup-client Device instance.
    private Device mMediasoupDevice;
    // mediasoup Transport for sending.
    private SendTransport mSendTransport;
    // mediasoup Transport for receiving.
    private RecvTransport mRecvTransport;
    // Local Audio Track for mic.
    private AudioTrack mLocalAudioTrack;
    // Local mic mediasoup Producer.
    private Producer mMicProducer;
    // local Video Track for cam.
    private VideoTrack mLocalVideoTrack;
    // Local cam mediasoup Producer.
    private Producer mCamProducer;
    // TODO(Haiyangwu): Local share mediasoup Producer.
    private Producer mShareProducer;
    // TODO(Haiyangwu): Local chat DataProducer.
    private Producer mChatDataProducer;
    // TODO(Haiyangwu): Local bot DataProducer.
    private Producer mBotDataProducer;
    // jobs worker handler.
    private Handler mWorkHandler;
    // main looper handler.
    private Handler mMainHandler;
    // Disposable Composite. used to cancel running
    private CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    // Share preferences
    private SharedPreferences mPreferences;

    private MediasoupConnectCallback connectCallback;

    public void setOnMediasoupConnectCallback(MediasoupConnectCallback connectCallback) {
        this.connectCallback = connectCallback;
    }

    public void setRoomOptions(RoomOptions options) {
        this.mOptions = options == null ? new RoomOptions() : options;
    }

    public boolean isClosed() {
        return mClosed;
    }

    public boolean isConnecting() {
        return null == connectCallback ? !isClosed() : connectCallback.isConnecting();
    }

    public boolean isConnected() {
        return null == connectCallback ? !isClosed() : connectCallback.isConnected();
    }

    public RoomClient(
            Context context, RoomStore roomStore, String roomId, String peerId, String clientId, String displayName) {
        this(context, roomStore, false, roomId, peerId, clientId, displayName, false, false, null, null);
    }

    public RoomClient(
            Context context,
            RoomStore roomStore,
            String roomId,
            String peerId,
            String clientId,
            String displayName,
            RoomOptions options) {
        this(context, roomStore, false, roomId, peerId, clientId, displayName, false, false, options, null);
    }

    /**
     * 初始化房间信息和连接
     *
     * @param context
     * @param roomStore
     * @param roomId      房间id
     * @param peerId      类似于userid
     * @param displayName 用户名
     * @param forceH264   视频编码H264
     * @param forceVP9    视频编码VP9
     * @param options     房间的配置信息
     */
    public RoomClient(
            Context context,
            RoomStore roomStore,
            boolean isP2PMode,
            String roomId,
            String peerId,
            String clientId,
            String displayName,
            boolean forceH264,
            boolean forceVP9,
            RoomOptions options,
            MediasoupConnectCallback connectCallback) {
        super(roomStore);
        this.mContext = context.getApplicationContext();
        this.mOptions = options == null ? new RoomOptions() : options;
        this.mRoomId = roomId;
        this.mSelfId = peerId;
        this.mDisplayName = displayName;
        this.mClientId = clientId;
        this.isP2PMode = isP2PMode;
        this.mForceVP9 = forceVP9;
        this.mForceH264 = forceH264;
        this.mClosed = false;
        //连接url
        this.mProtooUrl = UrlFactory.getProtooUrl(roomId, peerId, forceH264, forceVP9);
//设置自己信息
        this.mStore.setMe(peerId, displayName, this.mOptions.getDevice(), this.isP2PMode);
        //设置房间的url
        this.mStore.setRoomUrl(roomId, UrlFactory.getInvitationLink(roomId, forceH264, forceVP9));
        this.mStore.setP2PMode(this.isP2PMode);
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        this.connectCallback = connectCallback;
        // init worker handler.
        HandlerThread handlerThread = new HandlerThread("worker");
        handlerThread.start();
        mWorkHandler = new Handler(handlerThread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());
        mWorkHandler.post(() -> {
            mPeerConnectionUtils = new PeerConnectionUtils();
            mMediasoupManager = new MediasoupManager(mContext, isP2PMode);
            if (isP2PMode) {
                mP2PConnectFactory = new P2PConnectFactory(mContext, roomStore, isP2PMode, roomId, peerId,
                        clientId, displayName, forceVP9, forceH264, options, connectCallback,
                        mPeerConnectionUtils);
            }
        });
        Logger.e(TAG, "RoomClient() mDisplayName:" + mDisplayName + ", roomId:" + roomId + ", peerId:" + peerId + ",isP2PMode:" + this.isP2PMode);
    }

    /**
     * 加入房间
     */
    @Async
    public void joinRoom() {
        Logger.d(TAG, "join() mProtooUrl:" + this.mProtooUrl + ",isP2PMode:" + isP2PMode);
        mStore.setRoomState(RoomConstant.ConnectionState.CONNECTING);
        mWorkHandler.post(
                () -> {
                    //初始化连接mediasoup WebSocket服务器
                    WebSocketTransport transport = new WebSocketTransport(mProtooUrl);
                    //初始化Protoo ， 相关监听 并连接WebSocket
                    mProtoo = new Protoo(transport, peerListener);
                });
    }

    /**
     * p2p 和 mediasoup 模式切换
     */
    @Async
    public void switchP2POrMediasoup(boolean isP2PMode) {
        Logger.d(TAG, "switchP2PAndMediasoup isP2PMode:" + isP2PMode);
        if (this.isP2PMode = isP2PMode) {
            return;
        }
        if (!isConnecting()) {
            return;
        }
        this.isP2PMode = isP2PMode;
        this.mStore.clearAllConnectPeer();
        //设置自己信息
        this.mStore.setMe(mSelfId, mDisplayName, this.mOptions.getDevice(), this.isP2PMode);
        this.mStore.setP2PMode(this.isP2PMode);
        mWorkHandler.post(() -> {
            if (isP2PMode) {
                mP2PConnectFactory = new P2PConnectFactory(mContext, mStore, isP2PMode, mRoomId, mSelfId,
                        mClientId, mDisplayName, mForceVP9, mForceH264, mOptions, connectCallback,
                        mPeerConnectionUtils);
                disposeTransportDevice();
            } else {
                if (null != mP2PConnectFactory) {
                    mP2PConnectFactory.destroy();
                    mP2PConnectFactory = null;
                }
            }
            if (null != connectCallback) {
                connectCallback.onConnectSuc();
            }
            joinImpl();
        });
    }

    /**
     * 启动麦克风 录音
     */
    @Async
    public void enableMic() {
        Logger.d(TAG, "enableMic()" + ",isP2PMode:" + isP2PMode);
        mWorkHandler.post(this::enableMicImpl);
    }

    /**
     * 禁止使用麦克风 录音
     */
    @Async
    public void disableMic() {
        Logger.d(TAG, "disableMic()" + ",isP2PMode:" + isP2PMode);
        mWorkHandler.post(this::disableMicImpl);
    }

    /**
     * 静音麦克风
     */
    @Async
    public void muteMic() {
        Logger.d(TAG, "muteMic()" + ",isP2PMode:" + isP2PMode);
        mWorkHandler.post(this::muteMicImpl);
    }

    /**
     * 取消静音麦克风
     */
    @Async
    public void unmuteMic() {
        Logger.d(TAG, "unmuteMic()" + ",isP2PMode:" + isP2PMode);
        mWorkHandler.post(this::unmuteMicImpl);
    }

    /**
     * 启用摄像头
     */
    @Async
    public void enableCam() {
        Logger.d(TAG, "enableCam()" + ",isP2PMode:" + isP2PMode);
        mStore.setCamInProgress(true);
        mWorkHandler.post(
                () -> {
                    enableCamImpl();
                    mStore.setCamInProgress(false);
                });
    }

    /**
     * 改变摄像头采集的分辨率
     *
     * @param videoSize
     */
    @Async
    public void changeCaptureFormat(int videoSize) {
        Logger.d(TAG, "changeCaptureFormat() videoSize：" + videoSize + ",isP2PMode:" + isP2PMode);
        mWorkHandler.post(
                () -> {
                    changeCaptureFormatImpl(videoSize);
                });
    }

    /**
     * 禁用摄像头
     */
    @Async
    public void disableCam() {
        Logger.d(TAG, "disableCam()" + ",isP2PMode:" + isP2PMode);
        mWorkHandler.post(this::disableCamImpl);
    }

    /**
     * 摄像头 改变
     */
    @Async
    public void changeCam() {
        Logger.d(TAG, "changeCam() switchCam Camera " + ",isP2PMode:" + isP2PMode);
        mStore.setCamInProgress(true);
        mWorkHandler.post(() -> {
            RoomConstant.VideoCapturerType capturerType = null != mPeerConnectionUtils ? mPeerConnectionUtils.getCurrentVideoCapturer() : null;
            if (capturerType == RoomConstant.VideoCapturerType.CAMERA) {
                mPeerConnectionUtils.switchCam(
                        new CameraVideoCapturer.CameraSwitchHandler() {
                            @Override
                            public void onCameraSwitchDone(boolean isFrontCamera) {
                                Logger.w(TAG, "changeCam() onCameraSwitchDone isFrontCamera:" + isFrontCamera + ",isP2PMode:" + isP2PMode);
//                                mStore.cameraSwitchDone(isFrontCamera);
                                mStore.setCamInProgress(false);
                            }

                            @Override
                            public void onCameraSwitchError(String errorDescription) {
                                Logger.w(TAG, "changeCam() onCameraSwitchError | failed: " + errorDescription + ",isP2PMode:" + isP2PMode);
                                mStore.addNotify("error", "Could not change cam: " + errorDescription);
                                mStore.setCamInProgress(false);
                            }
                        });
            } else {
                mStore.setCamInProgress(false);
            }
        });
    }

    /**
     * 听筒和扬声器切换
     *
     * @param enable true 扬声器
     */
    @Async
    public void setEnableSpeaker(boolean enable) {
        Logger.d(TAG, "disableShare()" + ",isP2PMode:" + isP2PMode);
        mWorkHandler.post(new Runnable() {
            @Override
            public void run() {
                if (null != mMediasoupManager) {
                    mMediasoupManager.setEnableSpeaker(enable);
                }
            }
        });
    }

    /**
     * 禁用屏幕共享（功能暂未实现）测试版
     */
    @Async
    public void disableShare() {
        Logger.d(TAG, "disableShare()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): share
        mWorkHandler.post(
                () ->
                        disableShareImpl(true)
        );
    }

    /**
     * 启用屏幕共享（功能暂未实现）测试版
     */
    @Async
    public void enableShare() {
        // TODO(feature): share
        boolean isEnable = null != connectCallback ? connectCallback.reqShareScreenIntentData() : false;
        Logger.d(TAG, "enableShare() isEnable:" + isEnable + ",isP2PMode:" + isP2PMode);
        mStore.setShareInProgress(!isEnable);
    }

    /**
     * 开始启用屏幕共享（功能暂未实现）测试版
     */
    @Async
    public void startEnableShare(boolean isReqSuc) {
        Logger.d(TAG, "startEnableShare() isReqSuc:" + isReqSuc + ",isP2PMode:" + isP2PMode);
        // TODO(feature): share
        mStore.setShareInProgress(isReqSuc);
        if (isReqSuc) {
            mWorkHandler.post(
                    () -> {
                        enableShareImpl();
                        mStore.setShareInProgress(false);
                    });
        }
    }

    /**
     * 只有音频 暂停对方和自己的视频
     */
    @Async
    public void enableAudioOnly() {
        Logger.d(TAG, "enableAudioOnly()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        mStore.setAudioOnlyInProgress(true);
        disableCam();
        if (!isP2PMode) {
            mWorkHandler.post(
                    () -> {
                        for (ConsumerHolder holder : mConsumers.values()) {
                            if (!"video".equals(holder.mConsumer.getKind())) {
                                continue;
                            }
                            pauseConsumer(holder.mConsumer);
                        }
                        mStore.setAudioOnlyState(true);
                        mStore.setAudioOnlyInProgress(false);
                    });
        } else {
            setP2POtherState(RoomConstant.P2POtherState.VIDEO_PAUSE);
        }
    }

    /**
     * 音视频都有 启动对方和自己的视频
     */
    @Async
    public void disableAudioOnly() {
        Logger.d(TAG, "disableAudioOnly()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        mStore.setAudioOnlyInProgress(true);
        if ((!isP2PMode && mCamProducer == null && mOptions.isProduce()) || (isP2PMode && null != mP2PConnectFactory)) {
            enableCam();
        }
        if (!isP2PMode) {
            mWorkHandler.post(
                    () -> {
                        for (ConsumerHolder holder : mConsumers.values()) {
                            if (!"video".equals(holder.mConsumer.getKind())) {
                                continue;
                            }
                            resumeConsumer(holder.mConsumer);
                        }
                        mStore.setAudioOnlyState(false);
                        mStore.setAudioOnlyInProgress(false);
                    });
        } else {
            setP2POtherState(RoomConstant.P2POtherState.VIDEO_RESUME);
        }
    }

    /**
     * 停止接收对方的音频
     */
    @Async
    public void muteAudio() {
        Logger.d(TAG, "muteAudio()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        mStore.setAudioMutedState(true);
        if (!isP2PMode) {
            mWorkHandler.post(
                    () -> {
                        for (ConsumerHolder holder : mConsumers.values()) {
                            if (!"audio".equals(holder.mConsumer.getKind())) {
                                continue;
                            }
                            pauseConsumer(holder.mConsumer);
                        }
                    });
        } else {
            setP2POtherState(RoomConstant.P2POtherState.AUDIO_PAUSE);
        }
    }

    /**
     * 开始接收对方的音频
     */
    @Async
    public void unmuteAudio() {
        Logger.d(TAG, "unmuteAudio()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        mStore.setAudioMutedState(false);
        if (!isP2PMode) {
            mWorkHandler.post(
                    () -> {
                        for (ConsumerHolder holder : mConsumers.values()) {
                            if (!"audio".equals(holder.mConsumer.getKind())) {
                                continue;
                            }
                            resumeConsumer(holder.mConsumer);
                        }
                    });
        } else {
            setP2POtherState(RoomConstant.P2POtherState.AUDIO_RESUME);
        }
    }

    /**
     * 重新启动ice
     */
    @Async
    public void restartIce() {
        Logger.d(TAG, "restartIce()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        mStore.setRestartIceInProgress(true);
        mWorkHandler.post(
                () -> {
                    if (!isP2PMode) {
                        try {
                            if (mSendTransport != null) {
                                String iceParameters =
                                        mProtoo.syncRequest(
                                                "restartIce", req -> jsonPut(req, "transportId", mSendTransport.getId()));
                                mSendTransport.restartIce(iceParameters);
                            }
                            if (mRecvTransport != null) {
                                String iceParameters =
                                        mProtoo.syncRequest(
                                                "restartIce", req -> jsonPut(req, "transportId", mRecvTransport.getId()));
                                mRecvTransport.restartIce(iceParameters);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            logError("restartIce() | failed:", e);
                            mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
                        }
                        mStore.setRestartIceInProgress(false);
                    } else {
                        if (null != mP2PConnectFactory) {
                            mP2PConnectFactory.restartP2PIce();
                        }
                    }
                });
    }

    @Async
    public void setMaxSendingSpatialLayer() {
        Logger.d(TAG, "setMaxSendingSpatialLayer()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): layer
    }

    @Async
    public void setConsumerPreferredLayers(String spatialLayer) {
        Logger.d(TAG, "setConsumerPreferredLayers()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): layer
    }

    @Async
    public void setConsumerPreferredLayers(
            String consumerId, String spatialLayer, String temporalLayer) {
        Logger.d(TAG, "setConsumerPreferredLayers()" + ",isP2PMode:" + isP2PMode);
        // TODO: layer
    }

    @Async
    public void requestConsumerKeyFrame(String consumerId) {
        Logger.d(TAG, "requestConsumerKeyFrame()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        if (!isP2PMode) {
            mWorkHandler.post(
                    () -> {
                        try {
                            mProtoo.syncRequest(
                                    "requestConsumerKeyFrame", req -> jsonPut(req, "consumerId", "consumerId"));
                            mStore.addNotify("Keyframe requested for video consumer");
                        } catch (ProtooException e) {
                            e.printStackTrace();
                            logError("restartIce() | failed:", e);
                            mStore.addNotify("error", "ICE restart failed: " + e.getMessage());
                        }
                    });
        }
    }

    @Async
    public void enableChatDataProducer() {
        Logger.d(TAG, "enableChatDataProducer()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): data channel
    }

    @Async
    public void enableBotDataProducer() {
        Logger.d(TAG, "enableBotDataProducer()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): data channel
    }

    @Async
    public void sendChatMessage(String txt) {
        Logger.d(TAG, "sendChatMessage() txt:" + txt + ",isP2PMode:" + isP2PMode);
        // TODO(feature): data channel
    }

    @Async
    public void sendBotMessage(String txt) {
        Logger.d(TAG, "sendBotMessage() txt:" + txt + ",isP2PMode:" + isP2PMode);
        // TODO(feature): data channel
    }

    /**
     * 改变自己显示的名字
     */
    @RequiresApi(api = Build.VERSION_CODES.GINGERBREAD)
    @Async
    public void changeDisplayName(String displayName) {
        Logger.d(TAG, "changeDisplayName() displayName:" + displayName + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        if (isP2PMode) {
            return;
        }
        // Store in cookie.
        mPreferences.edit().putString("displayName", displayName).apply();

        mWorkHandler.post(
                () -> {
                    try {
                        mProtoo.syncRequest(
                                "changeDisplayName", req -> jsonPut(req, "displayName", displayName));
                        mDisplayName = displayName;
                        mStore.setDisplayName(displayName);
                        mStore.addNotify("Display name change");
                    } catch (ProtooException e) {
                        e.printStackTrace();
                        logError("changeDisplayName() | failed:", e);
                        mStore.addNotify("error", "Could not change display name: " + e.getMessage());

                        // We need to refresh the component for it to render the previous
                        // displayName again.
                        mStore.setDisplayName(mDisplayName);
                    }
                });
    }

    @Async
    public void getSendTransportRemoteStats() {
        Logger.d(TAG, "getSendTransportRemoteStats()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void getRecvTransportRemoteStats() {
        Logger.d(TAG, "getRecvTransportRemoteStats()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void getAudioRemoteStats() {
        Logger.d(TAG, "getAudioRemoteStats()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void getVideoRemoteStats() {
        Logger.d(TAG, "getVideoRemoteStats()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void getConsumerRemoteStats(String consumerId) {
        Logger.d(TAG, "getConsumerRemoteStats() consumerId:" + consumerId + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void getChatDataProducerRemoteStats(String consumerId) {
        Logger.d(TAG, "getChatDataProducerRemoteStats() consumerId:" + consumerId + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void getBotDataProducerRemoteStats() {
        Logger.d(TAG, "getBotDataProducerRemoteStats()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void getDataConsumerRemoteStats(String dataConsumerId) {
        Logger.d(TAG, "getDataConsumerRemoteStats() dataConsumerId:" + dataConsumerId + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void getSendTransportLocalStats() {
        Logger.d(TAG, "getSendTransportLocalStats()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void getRecvTransportLocalStats() {
        Logger.d(TAG, "getRecvTransportLocalStats()" + ",isP2PMode:" + isP2PMode);
        /// TODO(feature): stats
    }

    @Async
    public void getAudioLocalStats() {
        Logger.d(TAG, "getAudioLocalStats()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void getVideoLocalStats() {
        Logger.d(TAG, "getVideoLocalStats()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void getConsumerLocalStats(String consumerId) {
        Logger.d(TAG, "getConsumerLocalStats()" + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void applyNetworkThrottle(String uplink, String downlink, String rtt, String secret) {
        Logger.d(TAG, "applyNetworkThrottle() uplink:" + uplink + ",downlink:" + downlink + ",rtt:" + rtt + ",secret:" + secret + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    @Async
    public void resetNetworkThrottle(boolean silent, String secret) {
        Logger.d(TAG, "applyNetworkThrottle() silent:" + silent + ",secret:" + secret + ",isP2PMode:" + isP2PMode);
        // TODO(feature): stats
    }

    /**
     * 关闭房间
     */
    @Async
    public void close() {
        Logger.e(TAG, "close() mClosed：start1:" + mClosed + ",isP2PMode:" + isP2PMode);
        if (this.mClosed) {
            return;
        }
        this.mClosed = true;
        mWorkHandler.post(
                () -> {
                    Logger.e(TAG, "close() start mClosed：" + mClosed);
                    // Close mProtoo Protoo
                    if (mProtoo != null) {
                        mProtoo.close();
                        mProtoo = null;
                    }

                    if (null != mP2PConnectFactory) {
                        mP2PConnectFactory.destroy();
                        mP2PConnectFactory = null;
                    }

                    // dispose all transport and device.
                    disposeTransportDevice();

                    // dispose peerConnection.
                    if (null != mPeerConnectionUtils) {
                        mPeerConnectionUtils.dispose();
                        mPeerConnectionUtils = null;
                    }

                    // quit worker handler thread.
                    mWorkHandler.getLooper().quit();
                    Logger.e(TAG, "close() end mClosed：" + mClosed + ",isP2PMode:" + isP2PMode);
                }
        );
        // dispose request.
        mCompositeDisposable.dispose();
        // Set room state.
        mStore.setRoomState(RoomConstant.ConnectionState.CLOSED);
        Logger.e(TAG, "close() mClosed：end1:" + mClosed + ",isP2PMode:" + isP2PMode);
    }

    /**
     * 销毁关闭连接的 Device
     */
    @WorkerThread
    private void disposeTransportDevice() {
        Logger.e(TAG, "disposeTransportDevice()" + ",isP2PMode:" + isP2PMode);
        if (null != mCamProducer) {
            if (!mCamProducer.isClosed()) {
                mCamProducer.close();
            }
            mCamProducer = null;
        }

        if (null != mMicProducer) {
            if (!mMicProducer.isClosed()) {
                mMicProducer.close();
            }
            mMicProducer = null;
        }

        // Close mediasoup Transports.
        if (mSendTransport != null) {
            mSendTransport.close();
            mSendTransport.dispose();
            mSendTransport = null;
        }

        if (mRecvTransport != null) {
            mRecvTransport.close();
            mRecvTransport.dispose();
            mRecvTransport = null;
        }

        // dispose device.
        if (mMediasoupDevice != null) {
            mMediasoupDevice.dispose();
            mMediasoupDevice = null;
        }

        Logger.e(TAG, "disposeTransportDevice() mClosed：" + mClosed + ",isP2PMode:" + isP2PMode);
        // dispose audio track.
        if (mLocalAudioTrack != null) {
            mLocalAudioTrack.setEnabled(false);
            mLocalAudioTrack.dispose();
            mLocalAudioTrack = null;
        }

        // dispose video track.
        if (mLocalVideoTrack != null) {
            mLocalVideoTrack.setEnabled(false);
            mLocalVideoTrack.dispose();
            mLocalVideoTrack = null;
        }
    }

    /**
     * 释放mLocalVideoTrack
     */
    @WorkerThread
    private void releaseVideoTrack() {
        if (mLocalVideoTrack != null) {
            mLocalVideoTrack.setEnabled(false);
            mLocalVideoTrack.dispose();
            mLocalVideoTrack = null;
        }
        if (null != mPeerConnectionUtils) {
            mPeerConnectionUtils.releaseVideoCapturer();
        }
    }

    /**
     * 连接WebSocket 回调监听
     */
    private Protoo.Listener peerListener =
            new Protoo.Listener() {
                @Override
                public void onOpen() {
                    //websocket 连接成功 加入房间
                    mWorkHandler.post(() -> {
                        if (null != connectCallback) {
                            connectCallback.onConnectSuc();
                        }
                        joinImpl();
                    });
                }

                @Override
                public void onFail() {
                    // websocket 连接失败
                    mWorkHandler.post(
                            () -> {
                                mStore.addNotify("error", "WebSocket connection failed" + ",isP2PMode:" + isP2PMode);
                                mStore.setRoomState(RoomConstant.ConnectionState.CONNECTING);
                                if (null != connectCallback) {
                                    connectCallback.onConnectFail();
                                }
                            });
                }

                //WebSocket 连接 成功后消息的接收 请求消息
                @Override
                public void onRequest(
                        @NonNull Message.Request request, @NonNull Protoo.ServerRequestHandler handler) {
                    Logger.d(TAG, "onRequest() " + request.getData().toString() + ",mClosed:" + mClosed + ",isP2PMode:" + isP2PMode);
                    if (mClosed) {
                        return;
                    }
                    mWorkHandler.post(
                            () -> {
                                try {
                                    switch (request.getMethod()) {
                                        case "newConsumer": {
                                            onNewConsumer(request, handler);
                                            break;
                                        }
                                        case "newDataConsumer": {
                                            onNewDataConsumer(request, handler);
                                            break;
                                        }
                                        default: {
                                            handler.reject(403, "unknown protoo request.method " + request.getMethod());
                                            Logger.w(TAG, "unknown protoo request.method " + request.getMethod() + ",isP2PMode:" + isP2PMode);
                                        }
                                    }
                                } catch (Exception e) {
                                    Logger.e(TAG, "handleRequestError.", e);
                                }
                            });
                }

                //WebSocket 连接 成功后消息的接收 通知消息
                @Override
                public void onNotification(@NonNull Message.Notification notification) {
                    Logger.d(
                            TAG,
                            "onNotification() "
                                    + notification.getMethod()
                                    + ", "
                                    + notification.getData().toString() + ",mClosed:" + mClosed + ",isP2PMode:" + isP2PMode);
                    if (mClosed) {
                        return;
                    }
                    mWorkHandler.post(
                            () -> {
                                try {
                                    handleNotification(notification);
                                } catch (Exception e) {
                                    Logger.e(TAG, "handleNotification error.", e);
                                }
                            });
                }

                //websocket 连接中断
                @Override
                public void onDisconnected() {
                    mWorkHandler.post(
                            () -> {
                                mStore.addNotify("error", "WebSocket disconnected" + ",isP2PMode:" + isP2PMode);
                                mStore.setRoomState(RoomConstant.ConnectionState.DISCONNECTED);

                                // Close All Transports created by device.
                                // All will reCreated After ReJoin.
                                disposeTransportDevice();
                                if (null != connectCallback) {
                                    connectCallback.onConnectDisconnected();
                                }
                            });
                }

                //websocket 连接关闭
                @Override
                public void onClose() {
                    if (mClosed) {
                        return;
                    }
                    mWorkHandler.post(
                            () -> {
                                if (mClosed) {
                                    return;
                                }
                                close();
                                if (null != connectCallback) {
                                    connectCallback.onConnectClose();
                                }
                            });
                }
            };

    /**
     * WebSocket连接成功 加入房间
     */
    @WorkerThread
    private void joinImpl() {
        Logger.d(TAG, "joinImpl() isP2PMode:" + isP2PMode);
        if (isP2PMode) {
            if (null != mP2PConnectFactory) {
                mP2PConnectFactory.joinImpl();
            }
            createP2POfferSdp(null == connectCallback ? "" : connectCallback.getConnectPeerId());
            return;
        }
        try {
            mMediasoupDevice = new Device();
            String routerRtpCapabilities = mProtoo.syncRequest("getRouterRtpCapabilities");//getRouterRtpCapabilities 获取路由rtp
            Logger.d(TAG, "joinImpl() routerRtpCapabilities：" + routerRtpCapabilities);
            mMediasoupDevice.load(routerRtpCapabilities);
            String rtpCapabilities = mMediasoupDevice.getRtpCapabilities();//获取MediasoupDevice rtp
            Logger.d(TAG, "joinImpl() rtpCapabilities：" + rtpCapabilities);
            // Create mediasoup Transport for sending (unless we don't want to produce).
            if (mOptions.isProduce()) {//是否创建 生产完成
                createSendTransport();
            }

            // Create mediasoup Transport for sending (unless we don't want to consume).
            if (mOptions.isConsume()) { //是否创建 消费
                createRecvTransport();
            }

            // Join now into the room. 加入房间
            // TODO(HaiyangWu): Don't send our RTP capabilities if we don't want to consume.
            String joinResponse =
                    mProtoo.syncRequest(
                            "join",
                            req -> {
                                jsonPut(req, "displayName", mDisplayName);
                                jsonPut(req, "device", mOptions.getDevice().toJSONObject());
                                jsonPut(req, "rtpCapabilities", toJsonObject(rtpCapabilities));
                                // TODO (HaiyangWu): add sctpCapabilities
                                jsonPut(req, "sctpCapabilities", "");
                            });
//      mOptions.isUseDataChannel() ? mMediasoupDevice.getRtpCapabilities() : "";
            Logger.d(TAG, "joinImpl() joinResponse：" + joinResponse);
            mStore.setRoomState(RoomConstant.ConnectionState.CONNECTED);//设置状态 已经连接
            mStore.addNotify("You are in the room!", 3000);//添加一个已经加入房间通知

            JSONObject resObj = JsonUtils.toJsonObject(joinResponse);
            JSONArray peers = resObj.optJSONArray("peers");//解析房间已经有的用户
            for (int i = 0; peers != null && i < peers.length(); i++) {
                JSONObject peer = peers.getJSONObject(i);
                mStore.addPeer(peer.optString(Peer.KEY_PEER_ID), peer);
            }

            // Enable mic/webcam.
            if (mOptions.isProduce()) {//是否创建 生产完成
                boolean canSendMic = mMediasoupDevice.canProduce("audio");//麦克风是否可用
                boolean canSendCam = mMediasoupDevice.canProduce("video");//摄像头是否可用
                Logger.d(TAG, "joinImpl() canSendCam：" + canSendCam + ", canSendCam:" + canSendCam + ", mOptions.isEnableAudio:" + mOptions.isEnableAudio() + ", mOptions.isEnableVideo:" + mOptions.isEnableVideo());
                mStore.setMediaCapabilities(canSendMic, canSendCam);
                if (mOptions.isEnableAudio()) {
                    mMainHandler.post(this::enableMic);//启动麦克风 录音
                }
                if (mOptions.isEnableVideo()) {
                    mMainHandler.post(this::enableCam);//启用摄像头
                }
            }
            if (null != connectCallback) {
                connectCallback.onJoinSuc();
            }
        } catch (Exception e) {
            e.printStackTrace();
            logError("joinRoom() failed:", e);
            if (TextUtils.isEmpty(e.getMessage())) {
                mStore.addNotify("error", "Could not join the room, internal error");
            } else {
                mStore.addNotify("error", "Could not join the room: " + e.getMessage());
            }
            mMainHandler.post(this::close);
            if (null != connectCallback) {
                connectCallback.onJoinFail();
            }
        }
    }

    /**
     * 启用麦克风
     */
    @WorkerThread
    private void enableMicImpl() {
        Logger.d(TAG, "enableMicImpl()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        if (isP2PMode) {
            if (null != mP2PConnectFactory) {
                mP2PConnectFactory.enableMicImpl();
            }
            return;
        }
        try {
            if (null == mPeerConnectionUtils) {
                return;
            }
            if (mMicProducer != null) {
                return;
            }
            if (!mMediasoupDevice.isLoaded()) {
                Logger.w(TAG, "enableMic() | not loaded");
                return;
            }
            if (!mMediasoupDevice.canProduce("audio")) {
                Logger.w(TAG, "enableMic() | cannot produce audio");
                return;
            }
            if (mSendTransport == null) {
                Logger.w(TAG, "enableMic() | mSendTransport doesn't ready");
                return;
            }
            if (mLocalAudioTrack == null) {
                mLocalAudioTrack = mPeerConnectionUtils.createAudioTrack(mContext);
                mLocalAudioTrack.setEnabled(true);
                mPeerConnectionUtils.addAudioTrackMediaStream(mLocalAudioTrack);
            }
//            String codecOptions = "[{\"opusStereo\":true},{\"opusDtx\":true}]";
            mMicProducer =
                    mSendTransport.produce(
                            producer -> {
                                Logger.e(TAG, "onTransportClose(), micProducer");
                                if (isConnecting()) {
                                    if (mMicProducer != null) {
                                        mStore.removeProducer(mMicProducer.getId());
                                        mMicProducer = null;
                                    }
                                }
                            },
                            mLocalAudioTrack,
                            null,
                            /*codecOptions*/null);
            mStore.addProducer(mMicProducer);
            Logger.d(TAG, "mMicProducer," + mMicProducer.getId() + "," + mMicProducer.getKind());
        } catch (MediasoupException e) {
            e.printStackTrace();
            logError("enableMic() | failed:", e);
            mStore.addNotify("error", "Error enabling microphone: " + e.getMessage());
            if (mLocalAudioTrack != null) {
                mLocalAudioTrack.setEnabled(false);
            }
        }
    }

    /**
     * 禁止使用麦克风 录音
     */
    @WorkerThread
    private void disableMicImpl() {
        Logger.d(TAG, "disableMicImpl()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        if (isP2PMode) {
            if (null != mP2PConnectFactory) {
                mP2PConnectFactory.disableMicImpl();
            }
            return;
        }
        if (mMicProducer == null) {
            return;
        }
        mMicProducer.close();
        mStore.removeProducer(mMicProducer.getId());

        try {
            mProtoo.syncRequest("closeProducer", req -> jsonPut(req, "producerId", mMicProducer.getId()));
        } catch (ProtooException e) {
            e.printStackTrace();
            mStore.addNotify("error", "Error closing server-side mic Producer: " + e.getMessage());
        }
        mMicProducer = null;
    }

    /**
     * 静音麦克风
     */
    @WorkerThread
    private void muteMicImpl() {
        Logger.d(TAG, "muteMicImpl()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        if (isP2PMode) {
            if (null != mP2PConnectFactory) {
                mP2PConnectFactory.muteMicImpl();
            }
            return;
        }
        if (mMicProducer == null) {
            return;
        }
        mMicProducer.pause();
        try {
            mProtoo.syncRequest("pauseProducer", req -> jsonPut(req, "producerId", mMicProducer.getId()));
            mStore.setProducerPaused(mMicProducer.getId());
        } catch (ProtooException e) {
            e.printStackTrace();
            logError("muteMic() | failed:", e);
            mStore.addNotify("error", "Error pausing server-side mic Producer: " + e.getMessage());
        }
    }

    /**
     * 取消静音麦克风
     */
    @WorkerThread
    private void unmuteMicImpl() {
        Logger.d(TAG, "unmuteMicImpl()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        if (isP2PMode) {
            if (null != mP2PConnectFactory) {
                mP2PConnectFactory.unmuteMicImpl();
            }
            return;
        }
        if (mMicProducer == null && mOptions.isProduce()) {
            Logger.e(TAG, "unmuteMicImpl()");
            enableMic();
        }
        if (null == mMicProducer) {
            Logger.e(TAG, "unmuteMicImpl() null == mMicProducer");
            return;
        }
        mMicProducer.resume();
        try {
            mProtoo.syncRequest(
                    "resumeProducer", req -> jsonPut(req, "producerId", mMicProducer.getId()));
            mStore.setProducerResumed(mMicProducer.getId());
        } catch (ProtooException e) {
            e.printStackTrace();
            logError("unmuteMic() | failed:", e);
            mStore.addNotify("error", "Error resuming server-side mic Producer: " + e.getMessage());
        }
    }

    /**
     * 启用摄像头
     */
    @WorkerThread
    private void enableCamImpl() {
        Logger.d(TAG, "enableCamImpl()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        if (null == mPeerConnectionUtils) {
            return;
        }
        if (isP2PMode) {
            if (null != mP2PConnectFactory) {
                mP2PConnectFactory.enableCamImpl();
            }
            return;
        }
        try {
            if (mCamProducer != null) {
                RoomConstant.VideoCapturerType capturerType = mPeerConnectionUtils.getCurrentVideoCapturer();
                if (capturerType == RoomConstant.VideoCapturerType.SCREEN) {
                    disableShareImpl(false);
                } else if (capturerType == RoomConstant.VideoCapturerType.FILE) {

                } else {
                    return;
                }
            }
            if (!mMediasoupDevice.isLoaded()) {
                Logger.w(TAG, "enableCam() | not loaded");
                return;
            }
            if (!mMediasoupDevice.canProduce("video")) {
                Logger.w(TAG, "enableCam() | cannot produce video");
                return;
            }
            if (mSendTransport == null) {
                Logger.w(TAG, "enableCam() | mSendTransport doesn't ready");
                return;
            }
            if (mLocalVideoTrack == null || mPeerConnectionUtils.getCurrentVideoCapturer() != RoomConstant.VideoCapturerType.CAMERA) {
                releaseVideoTrack();
                mLocalVideoTrack = mPeerConnectionUtils.createVideoTrack(mContext, RoomConstant.VideoCapturerType.CAMERA);
                mLocalVideoTrack.setEnabled(true);
                mPeerConnectionUtils.addVideoTrackMediaStream(mLocalVideoTrack);
            }
//            String codecOptions = "[{\"videoGoogleStartBitrate\":1000}]";
//            List<RtpParameters.Encoding> encodings = new ArrayList<>();
//            encodings.add(RTCUtils.genRtpEncodingParameters(null, false, 500000, 0, 60, 0, 0.0d, 0L));
//            encodings.add(RTCUtils.genRtpEncodingParameters(null, false, 1000000, 0, 60, 0, 0.0d, 0L));
//            encodings.add(RTCUtils.genRtpEncodingParameters(null, false, 1500000, 0, 60, 0, 0.0d, 0L));
            mCamProducer =
                    mSendTransport.produce(
                            producer -> {
                                Logger.e(TAG, "onTransportClose(), camProducer");
                                if (isConnecting()) {
                                    if (mCamProducer != null) {
                                        mStore.removeProducer(mCamProducer.getId());
                                        mCamProducer = null;
                                    }
                                }
                            },
                            mLocalVideoTrack,
                            /*encodings*/null,
                            /*codecOptions*/ null);
//            mStore.addProducer(mCamProducer);
            mStore.addProducer(mCamProducer, Producers.ProducersWrapper.TYPE_CAM);
//            mStore.setProducerType(mCamProducer.getId(), Producers.ProducersWrapper.TYPE_CAM);
            Logger.d(TAG, "mCamProducer," + mCamProducer.getId() + "," + mCamProducer.getKind());
        } catch (MediasoupException e) {
            e.printStackTrace();
            logError("enableWebcam() | failed:", e);
            mStore.addNotify("error", "Error enabling webcam: " + e.getMessage());
            if (mLocalVideoTrack != null) {
                mLocalVideoTrack.setEnabled(false);
            }
        }
    }

    /**
     * 改变摄像头采集的分辨率
     *
     * @param videoSize
     */
    @WorkerThread
    private void changeCaptureFormatImpl(int videoSize) {
        if (!isConnected()) {
            return;
        }
        if ((!isP2PMode && mCamProducer == null) || (isP2PMode && null == mP2PConnectFactory)) {
            return;
        }

//        RoomConstant.VideoCapturerType capturerType = null != mPeerConnectionUtils ? mPeerConnectionUtils.getCurrentVideoCapturer() : null;
//        if (capturerType != RoomConstant.VideoCapturerType.CAMERA) {
//            return;
//        }
        boolean isChange = mPeerConnectionUtils.changeCaptureFormat(videoSize);
        Logger.d(TAG, "changeCaptureFormatImpl() videoSize:" + videoSize + ",isChange:" + isChange + ",isP2PMode:" + isP2PMode);
    }

    /**
     * 禁用摄像头
     */
    @WorkerThread
    private void disableCamImpl() {
        Logger.d(TAG, "disableCamImpl()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        if (isP2PMode) {
            if (null != mP2PConnectFactory) {
                mP2PConnectFactory.disableCamImpl();
            }
            return;
        }
        if (mCamProducer == null) {
            return;
        }
        RoomConstant.VideoCapturerType capturerType = null != mPeerConnectionUtils ? mPeerConnectionUtils.getCurrentVideoCapturer() : null;
        if (capturerType != RoomConstant.VideoCapturerType.CAMERA) {
            return;
        }

        mCamProducer.close();
        mStore.removeProducer(mCamProducer.getId());
        try {
            mProtoo.syncRequest("closeProducer", req -> jsonPut(req, "producerId", mCamProducer.getId()));
        } catch (ProtooException e) {
            e.printStackTrace();
            mStore.addNotify("error", "Error closing server-side webcam Producer: " + e.getMessage());
        }
        mCamProducer = null;
    }

    /**
     * 启用屏幕共享（功能暂未实现）测试版
     */
    @WorkerThread
    private void enableShareImpl() {
        Logger.d(TAG, "enableShareImpl()" + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        if (mPeerConnectionUtils == null) {
            return;
        }
        if (isP2PMode) {
            if (null != mP2PConnectFactory) {
                mP2PConnectFactory.enableShareImpl();
            }
            return;
        }
        try {
            if (null == mMediasoupDevice) {
                return;
            }
            if (mCamProducer != null) {
                RoomConstant.VideoCapturerType capturerType = mPeerConnectionUtils.getCurrentVideoCapturer();
                if (capturerType == RoomConstant.VideoCapturerType.CAMERA) {
//                    mMainHandler.post(this::disableCamImpl);
                    disableCamImpl();
                } else if (capturerType == RoomConstant.VideoCapturerType.FILE) {

                } else {
                    return;
                }
            }
            if (!mMediasoupDevice.isLoaded()) {
                Logger.w(TAG, "enableShare | not loaded");
                return;
            }
            if (!mMediasoupDevice.canProduce("video")) {
                Logger.w(TAG, "enableShare | cannot produce video");
                return;
            }
            if (mSendTransport == null) {
                Logger.w(TAG, "enableShare | mSendTransport doesn't ready");
                return;
            }

            if (mLocalVideoTrack == null || mPeerConnectionUtils.getCurrentVideoCapturer() != RoomConstant.VideoCapturerType.SCREEN) {
                releaseVideoTrack();
                mLocalVideoTrack = mPeerConnectionUtils.createVideoTrack(mContext, RoomConstant.VideoCapturerType.SCREEN);
                mLocalVideoTrack.setEnabled(true);
                mPeerConnectionUtils.addVideoTrackMediaStream(mLocalVideoTrack);
            }

//            String codecOptions = "[{\"videoGoogleStartBitrate\":1000}]";
//            List<RtpParameters.Encoding> encodings = new ArrayList<>();
//            encodings.add(RTCUtils.genRtpEncodingParameters(null, false, 500000, 0, 60, 0, 0.0d, 0L));
//            encodings.add(RTCUtils.genRtpEncodingParameters(null, false, 1000000, 0, 60, 0, 0.0d, 0L));
//            encodings.add(RTCUtils.genRtpEncodingParameters(null, false, 1500000, 0, 60, 0, 0.0d, 0L));
            mCamProducer =
                    mSendTransport.produce(
                            producer -> {
                                Logger.e(TAG, "onTransportClose(), shareProducer");
                                if (mCamProducer != null) {
                                    mStore.removeProducer(mCamProducer.getId());
                                    mCamProducer = null;
                                }
                            },
                            mLocalVideoTrack,
                            /*encodings*/null,
                            /*codecOptions*/null);
//            mStore.addProducer(mCamProducer);
            mStore.addProducer(mCamProducer, Producers.ProducersWrapper.TYPE_SHARE);
//            mStore.setProducerType(mCamProducer.getId(), Producers.ProducersWrapper.TYPE_SHARE);
            Logger.d(TAG, "mShareProducer," + mCamProducer.getId() + "," + mCamProducer.getKind());
        } catch (MediasoupException e) {
            e.printStackTrace();
            logError("enableWebShare() | failed:", e);
            mStore.addNotify("error", "Error enabling webShare: " + e.getMessage());
            if (mLocalVideoTrack != null) {
                mLocalVideoTrack.setEnabled(false);
            }
        }
    }

    /**
     * 禁用屏幕共享（功能暂未实现）测试版
     */
    @WorkerThread
    private void disableShareImpl(boolean isContinue) {
        Logger.d(TAG, "disableShareImpl() isContinue:" + isContinue + ",isP2PMode:" + isP2PMode);
        if (!isConnected()) {
            return;
        }
        if (isP2PMode) {
            if (null != mP2PConnectFactory) {
                mP2PConnectFactory.disableShareImpl(isContinue);
            }
            return;
        }
        if (mCamProducer == null) {
            return;
        }
        RoomConstant.VideoCapturerType capturerType = null != mPeerConnectionUtils ? mPeerConnectionUtils.getCurrentVideoCapturer() : null;
        if (capturerType != RoomConstant.VideoCapturerType.SCREEN) {
            return;
        }

        releaseVideoTrack();

        mCamProducer.close();
        mStore.removeProducer(mCamProducer.getId());

        try {
            mProtoo.syncRequest(ActionEvent.CLOSE_PRODUCER, req -> jsonPut(req, "producerId", mCamProducer.getId()));
        } catch (ProtooException e) {
            e.printStackTrace();
            mStore.addNotify("error", "Error closing server-side webShare Producer: " + e.getMessage());
        }
        mCamProducer = null;

        //关闭屏幕共享后如果之前是摄像头模式 ，继续启用摄像头
        if (isContinue && mOptions.isEnableVideo()) {
            mMainHandler.post(this::enableCam);//启用摄像头
        }
    }

    /**
     * 创建发送的 音视频 参数和ice相关
     *
     * @throws ProtooException
     * @throws JSONException
     * @throws MediasoupException
     */
    @WorkerThread
    private void createSendTransport() throws ProtooException, JSONException, MediasoupException {
        Logger.d(TAG, "createSendTransport()" + ",isP2PMode:" + isP2PMode);
        String res =
                mProtoo.syncRequest(
                        "createWebRtcTransport",
                        (req -> {
                            jsonPut(req, "forceTcp", mOptions.isForceTcp());
                            jsonPut(req, "producing", true);//生产 true 自己相关
                            jsonPut(req, "consuming", false);//消费 true 他人相关
                            // TODO: sctpCapabilities
                            jsonPut(req, "sctpCapabilities", "");
                        }));

        JSONObject info = new JSONObject(res);

        Logger.d(TAG, "device#createSendTransport() " + info);
        String id = info.optString("id");
        String iceParameters = info.optString("iceParameters");
        String iceCandidates = info.optString("iceCandidates");
        String dtlsParameters = info.optString("dtlsParameters");
        String sctpParameters = info.optString("sctpParameters");

        mSendTransport =
                mMediasoupDevice.createSendTransport(
                        sendTransportListener, id, iceParameters, iceCandidates, dtlsParameters);
    }

    /**
     * 创建接收 音视频 参数和ice相关
     *
     * @throws ProtooException
     * @throws JSONException
     * @throws MediasoupException
     */
    @WorkerThread
    private void createRecvTransport() throws ProtooException, JSONException, MediasoupException {
        Logger.d(TAG, "createRecvTransport()" + ",isP2PMode:" + isP2PMode);

        String res =
                mProtoo.syncRequest(
                        "createWebRtcTransport",
                        req -> {
                            jsonPut(req, "forceTcp", mOptions.isForceTcp());
                            jsonPut(req, "producing", false);//生产 true 自己相关
                            jsonPut(req, "consuming", true);//消费 true 他人相关
                            // TODO (HaiyangWu): add sctpCapabilities
                            jsonPut(req, "sctpCapabilities", "");
                        });

        JSONObject info = new JSONObject(res);
        Logger.d(TAG, "device#createRecvTransport() " + info);
        String id = info.optString("id");
        String iceParameters = info.optString("iceParameters");
        String iceCandidates = info.optString("iceCandidates");
        String dtlsParameters = info.optString("dtlsParameters");
        String sctpParameters = info.optString("sctpParameters");

        mRecvTransport =
                mMediasoupDevice.createRecvTransport(
                        recvTransportListener, id, iceParameters, iceCandidates, dtlsParameters, null);
    }

    /**
     * 创建发送 音视频 参数和ice相关  回调监听
     */
    private SendTransport.Listener sendTransportListener =
            new SendTransport.Listener() {

                private String listenerTAG = TAG + "_SendTrans";

                @Override
                public String onProduce(
                        Transport transport, String kind, String rtpParameters, String appData) {
                    if (mClosed) {
                        return "";
                    }
                    Logger.d(listenerTAG, "onProduce() " + ",isP2PMode:" + isP2PMode);
                    String producerId =
                            fetchProduceId(
                                    req -> {
                                        jsonPut(req, "transportId", transport.getId());
                                        jsonPut(req, "kind", kind);
                                        jsonPut(req, "rtpParameters", toJsonObject(rtpParameters));
                                        jsonPut(req, "appData", appData);
                                    });
                    Logger.d(listenerTAG, "producerId: " + producerId);
                    return producerId;
                }

                @Override
                public void onConnect(Transport transport, String dtlsParameters) {
                    if (mClosed) {
                        return;
                    }
                    Logger.d(listenerTAG + "_send", "onConnect()" + ",isP2PMode:" + isP2PMode);
                    mCompositeDisposable.add(
                            mProtoo
                                    .request(
                                            "connectWebRtcTransport",
                                            req -> {
                                                jsonPut(req, "transportId", transport.getId());
                                                jsonPut(req, "dtlsParameters", toJsonObject(dtlsParameters));
                                            })
                                    .subscribe(
                                            d -> Logger.d(listenerTAG, "connectWebRtcTransport res: " + d),
                                            t -> logError("connectWebRtcTransport for mSendTransport failed", t)));
                }

                @Override
                public void onConnectionStateChange(Transport transport, String connectionState) {
                    Logger.d(listenerTAG, "onConnectionStateChange: " + connectionState + ",isP2PMode:" + isP2PMode);
                }
            };

    /**
     * 创建接收 音视频 参数和ice相关 回调监听
     */
    private RecvTransport.Listener recvTransportListener =
            new RecvTransport.Listener() {

                private String listenerTAG = TAG + "_RecvTrans";

                @Override
                public void onConnect(Transport transport, String dtlsParameters) {
                    if (mClosed) {
                        return;
                    }
                    Logger.d(listenerTAG, "onConnect()" + ",isP2PMode:" + isP2PMode);
                    mCompositeDisposable.add(
                            mProtoo
                                    .request(
                                            "connectWebRtcTransport",
                                            req -> {
                                                jsonPut(req, "transportId", transport.getId());
                                                jsonPut(req, "dtlsParameters", toJsonObject(dtlsParameters));
                                            })
                                    .subscribe(
                                            d -> Logger.d(listenerTAG, "connectWebRtcTransport res: " + d),
                                            t -> logError("connectWebRtcTransport for mRecvTransport failed", t)));
                }

                @Override
                public void onConnectionStateChange(Transport transport, String connectionState) {
                    Logger.d(listenerTAG, "onConnectionStateChange: " + connectionState + ",isP2PMode:" + isP2PMode);
                }
            };

    /**
     * 获取生成id
     *
     * @param generator
     * @return
     */
    private String fetchProduceId(Protoo.RequestGenerator generator) {
        Logger.d(TAG, "fetchProduceId:()" + ",isP2PMode:" + isP2PMode);
        try {
            String response = mProtoo.syncRequest("produce", generator);
            return new JSONObject(response).optString("id");
        } catch (ProtooException | JSONException e) {
            e.printStackTrace();
            logError("send produce request failed", e);
            return "";
        }
    }

    private void logError(String message, Throwable throwable) {
        Logger.e(TAG, message, throwable);
    }

    /**
     * 新的消费用户
     *
     * @param request
     * @param handler
     */
    private void onNewConsumer(Message.Request request, Protoo.ServerRequestHandler handler) {
        if (!mOptions.isConsume()) {
            handler.reject(403, "I do not want to consume");
            return;
        }
        try {
            JSONObject data = request.getData();
            String peerId = data.optString("peerId");//类似于userid
            String producerId = data.optString("producerId");//peerId对于的生产者id
            String id = data.optString("id");// Consumer对于的Consumer 。id
            String kind = data.optString("kind");//peerId 对于的 当前模式 视频还是音频
            String rtpParameters = data.optString("rtpParameters");
            String type = data.optString("type");
            String appData = data.optString("appData");
            boolean producerPaused = data.optBoolean("producerPaused");

            //添加一个Consumer（消费者）到C 层
            Consumer consumer =
                    mRecvTransport.consume(
                            c -> {
                                if (isConnecting()) {
                                    mConsumers.remove(c.getId());
                                    Logger.w(TAG, "onTransportClose for consume");
                                }
                            },
                            id,
                            producerId,
                            kind,
                            rtpParameters,
                            appData);

            mConsumers.put(consumer.getId(), new ConsumerHolder(peerId, consumer));
            mStore.addConsumer(peerId, type, consumer, producerPaused);

            // We are ready. Answer the protoo request so the server will
            // resume this Consumer (which was paused for now if video).
            handler.accept();//准备好了接收服务器消息？视频要停顿一下？

            Logger.d(TAG, "onNewConsumer isAudioOnly:" + mStore.getMe().getValue().isAudioOnly());
            // If audio-only mode is enabled, pause it.
            if ("video".equals(consumer.getKind()) && mStore.getMe().getValue().isAudioOnly()) {
                pauseConsumer(consumer);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logError("\"newConsumer\" request failed:", e);
            mStore.addNotify("error", "Error creating a Consumer: " + e.getMessage());
        }
    }

    /**
     * 新的消费用户的消费数据
     *
     * @param request
     * @param handler
     */
    private void onNewDataConsumer(Message.Request request, Protoo.ServerRequestHandler handler) {
        handler.reject(403, "I do not want to data consume" + ",isP2PMode:" + isP2PMode);
        // TODO(HaiyangWu): support data consume
    }

    /**
     * 暂停 消费用户
     *
     * @param consumer
     */
    @WorkerThread
    private void pauseConsumer(Consumer consumer) {
        Logger.d(TAG, "pauseConsumer() " + consumer.getId() + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        if (consumer.isPaused()) {
            return;
        }

        try {
            mProtoo.syncRequest("pauseConsumer", req -> jsonPut(req, "consumerId", consumer.getId()));
            consumer.pause();
            mStore.setConsumerPaused(consumer.getId(), "local");
        } catch (ProtooException e) {
            e.printStackTrace();
            logError("pauseConsumer() | failed:", e);
            mStore.addNotify("error", "Error pausing Consumer: " + e.getMessage());
        }
    }

    @WorkerThread
    private void resumeConsumer(Consumer consumer) {
        Logger.d(TAG, "resumeConsumer() " + consumer.getId() + ",isP2PMode:" + isP2PMode);
        if (!isConnecting()) {
            return;
        }
        if (!consumer.isPaused()) {
            return;
        }

        try {
            mProtoo.syncRequest("resumeConsumer", req -> jsonPut(req, "consumerId", consumer.getId()));
            consumer.resume();
            mStore.setConsumerResumed(consumer.getId(), "local");
        } catch (Exception e) {
            e.printStackTrace();
            logError("resumeConsumer() | failed:", e);
            mStore.addNotify("error", "Error resuming Consumer: " + e.getMessage());
        }
    }

    /**
     * 设置p2p连接 对方状态
     *
     * @param otherState
     */
    @WorkerThread
    private void setP2POtherState(RoomConstant.P2POtherState otherState) {
        Logger.d(TAG, "setP2POtherState() otherState:" + otherState + ",isP2PMode:" + isP2PMode);
        if (null != mP2PConnectFactory) {
            mP2PConnectFactory.setP2POtherState(otherState);
        }
    }

    /**
     * 设置p2p模式 创建 Offer sdp
     *
     * @param peerId
     */
    @Async
    public void createP2POfferSdp(String peerId) {
        Logger.d(TAG, "createP2POfferSdp() peerId:" + peerId + ",isP2PMode:" + isP2PMode);
        if (!isP2PMode) {
            return;
        }
        if (null == mP2PConnectFactory) {
            return;
        }
        mWorkHandler.post(
                () -> {
                    Peer peer = new Peer(peerId, null == connectCallback ? "" : connectCallback.getConnectPeerName());
                    mP2PConnectFactory.createP2POfferSdp(peer);
                });
    }

    /**
     * 设置p2p模式 接收到对方发来的Offer sdp 设置 setRemote sdp 并创建Answer sdp
     *
     * @param peerId
     * @param jsonData
     */
    @Async
    public void createP2PAnswerSdp(String peerId, JSONObject jsonData) {
        Logger.d(TAG, "createP2PAnswerSdp() peerId:" + peerId + ",isP2PMode:" + isP2PMode + ",jsonData:" + jsonData);
        if (!isP2PMode) {
            return;
        }
        if (null == mP2PConnectFactory) {
            return;
        }
        mWorkHandler.post(
                () -> {
                    Peer peer = new Peer(peerId, null == connectCallback ? "" : connectCallback.getConnectPeerName());
                    mP2PConnectFactory.createP2PAnswerSdp(peer, jsonData);
                });
    }

    /**
     * 设置p2p模式 接收到对方发来的Answer sdp  设置 setRemote sdp
     *
     * @param peerId
     * @param jsonData
     */
    @Async
    public void setP2PAnswerSdp(String peerId, JSONObject jsonData) {
        Logger.d(TAG, "setP2PAnswerSdp() peerId:" + peerId + ",isP2PMode:" + isP2PMode + ",jsonData:" + jsonData);
        if (!isP2PMode) {
            return;
        }
        if (null == mP2PConnectFactory) {
            return;
        }
        mWorkHandler.post(
                () -> {
                    Peer peer = new Peer(peerId, null == connectCallback ? "" : connectCallback.getConnectPeerName());
                    mP2PConnectFactory.setP2PAnswerSdp(peer, jsonData);
                });

    }

    /**
     * 设置p2p模式 接收到对方发来的 ice
     *
     * @param peerId
     * @param jsonData
     */
    @Async
    public void addP2PIceCandidate(String peerId, JSONObject jsonData) {
        Logger.d(TAG, "addP2PIceCandidate() peerId:" + peerId + ",isP2PMode:" + isP2PMode + ",jsonData:" + jsonData);
        if (!isP2PMode) {
            return;
        }
        if (null == mP2PConnectFactory) {
            return;
        }
        mWorkHandler.post(
                () -> {
                    Peer peer = new Peer(peerId, null == connectCallback ? "" : connectCallback.getConnectPeerName());
                    mP2PConnectFactory.addP2PIceCandidate(peer, jsonData);
                });
    }
}
