package org.mediasoup.droid.lib.p2p;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jsy.mediasoup.utils.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.RoomConstant;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.Utils;
import org.mediasoup.droid.lib.interfaces.MediasoupConnectCallback;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.model.Producers;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class P2PConnectFactory {
    public static final String TAG = P2PConnectFactory.class.getSimpleName();
    public static final String TAG_OFFER_SDP = "createOfferSdp:";
    public static final String TAG_ANSWER_SDP = "createAnswerSdp:";
    public static final String TAG_LOCAL_SDP = "setLocalSdp:";
    public static final String TAG_REMOTE_SDP = "setRemoteSdp:";
    public static final String KEY_SDP_CONTENT = "sdp";
    public static final String KEY_SDP_TYPE = "type";
    public static final String KEY_ICE_ID = "id";//IceCandidate. sdpMid
    public static final String KEY_ICE_LABEL = "label";//IceCandidate .sdpMLineIndex
    public static final String KEY_ICE_CANDIDATE = "candidate";//IceCandidate sdp

    // Android context.
    @NonNull
    private final Context mContext;
    @NonNull
    private final RoomStore mStore;
    private final RoomOptions mOptions;
    private final String mRoomId;
    private final String mSelfId;
    private final String mSelfName;
    private final String mClientId;
    private final boolean isP2PMode;
    private AudioTrack mLocalAudioTrack;
    private VideoTrack mLocalVideoTrack;
    private RoomConstant.P2PConnectState mP2PConnectState;
    private Map<String, PeerConnection> peerConnectionMap = new HashMap<>();
    private PeerConnectionUtils mPeerConnectionUtils;
    private final MediasoupConnectCallback connectCallback;
    private final ThreadUtils.ThreadChecker mThreadChecker;
    private boolean isP2PRecipient;//是否接受方(邀请方)
    private boolean isNeedCreateOfferSdp;//是否需要创建OfferSdp
    private boolean isNeedCreateAnswerSdp;//是否需要创建AnswerSdp
    private boolean isNeedAddIce;//是否需要添加ice
    private Peer sdpPeer;
    private JSONObject sdpJSON;
    private List<JSONObject> iceJSONList = new ArrayList<>();

    public P2PConnectFactory(Context context, RoomStore roomStore,
                             boolean isP2PMode, String roomId, String peerId,
                             String clientId, String displayName,
                             boolean forceVP9, boolean forceH264,
                             RoomOptions options,
                             MediasoupConnectCallback connectCallback,
                             PeerConnectionUtils peerConnectionUtils) {
        LogUtils.i(TAG, "init P2PConnectFactory");
        this.mContext = context.getApplicationContext();
        this.mOptions = options;
        this.mStore = roomStore;
        this.connectCallback = connectCallback;
        this.isP2PMode = isP2PMode;
        this.mRoomId = roomId;
        this.mSelfId = peerId;
        this.mClientId = clientId;
        this.mSelfName = displayName;
        this.mPeerConnectionUtils = peerConnectionUtils;
        peerConnectionMap.clear();
        sdpPeer = null;
        sdpJSON = null;
        isP2PRecipient = false;
        isNeedCreateOfferSdp = false;
        isNeedCreateAnswerSdp = false;
        isNeedAddIce = false;
        iceJSONList.clear();
        mP2PConnectState = RoomConstant.P2PConnectState.NEW;
        mThreadChecker = new ThreadUtils.ThreadChecker();
    }

    private boolean isConnecting() {
        return null == connectCallback ? false : connectCallback.isConnecting();
    }

    private boolean isConnected() {
        return null == connectCallback ? false : connectCallback.isConnected();
    }

    private Peer getConnectPeer() {
        return null == connectCallback ? null : connectCallback.getConnectPeer();
    }

    private String getVideoType() {
        RoomConstant.VideoCapturerType capturerType = null == mPeerConnectionUtils ? RoomConstant.VideoCapturerType.CAMERA : mPeerConnectionUtils.getCurrentVideoCapturer();
        return getVideoType(capturerType);
    }

    private String getVideoType(RoomConstant.VideoCapturerType capturerType) {
        if (null == capturerType) {
            return Producers.ProducersWrapper.TYPE_CAM;
        }
        switch (capturerType) {
            case SCREEN:
                return Producers.ProducersWrapper.TYPE_SHARE;
            case CAMERA:
            case FILE:
            default:
                return Producers.ProducersWrapper.TYPE_CAM;
        }
    }

    /**
     * 加入房间初始化创建相关
     */
    public void joinImpl() {
        LogUtils.i(TAG, "joinImpl 创建 isNeedCreateOfferSdp:" + isNeedCreateOfferSdp + ",isNeedCreateAnswerSdp:"
            + isNeedCreateAnswerSdp + ", isNeedAddIce:" + isNeedAddIce);
        mThreadChecker.checkIsOnValidThread();
        if (isConnected()) {
            LogUtils.e(TAG, "joinImpl isConnected=true,mOptions.isEnableAudio:" + mOptions.isEnableAudio()
                + ", null != mLocalAudioTrack:" + (null != mLocalAudioTrack) + ", mOptions.isEnableVideo:"
                + mOptions.isEnableVideo() + ", null != mLocalVideoTrack:" + (null != mLocalVideoTrack));
            return;
        }
        mP2PConnectState = RoomConstant.P2PConnectState.CREATE;
        releaseAudioTrack(true);
        releaseVideoTrack(true);
        mStore.setMediaCapabilities(true, true);
        if (mOptions.isEnableAudio()) {
            getLocalAudioTrack();//启动麦克风 录音
        }
        if (mOptions.isEnableVideo()) {
            getLocalVideoTrack(RoomConstant.VideoCapturerType.CAMERA);//启用摄像头
        }

        addLocalTrack();

        mStore.setRoomState(RoomConstant.ConnectionState.CONNECTED);
        mStore.addNotify("You are in the room!", 3000);//添加一个已经加入房间通知

        if (isNeedCreateOfferSdp) {
            isNeedCreateOfferSdp = false;
            createP2POfferSdp(sdpPeer, true);
        }

        if (isNeedCreateAnswerSdp) {
            isNeedCreateAnswerSdp = false;
            setP2POfferCreateAnswer(sdpPeer, sdpJSON, true);
        }

        if (isNeedAddIce) {
            isNeedAddIce = false;
            for (JSONObject data : iceJSONList) {
                addP2PIceCandidate(sdpPeer, data);
            }
            iceJSONList.clear();
        }
    }

    private AudioTrack getLocalAudioTrack() {
        if (mLocalAudioTrack == null) {
            LogUtils.i(TAG, "创建 getLocalAudioTrack:");
            mLocalAudioTrack = mPeerConnectionUtils.createAudioTrack(mContext);
            mLocalAudioTrack.setEnabled(true);
            mPeerConnectionUtils.addAudioTrackMediaStream(mLocalAudioTrack);
        }
        return mLocalAudioTrack;
    }

    private VideoTrack getLocalVideoTrack(RoomConstant.VideoCapturerType capturerType) {
        if (mLocalVideoTrack == null || mPeerConnectionUtils.getCurrentVideoCapturer() != capturerType) {
            LogUtils.i(TAG, "创建 getLocalVideoTrack:");
            releaseVideoTrack(false);
            mLocalVideoTrack = mPeerConnectionUtils.createVideoTrack(mContext, capturerType);
            mLocalVideoTrack.setEnabled(true);
            mPeerConnectionUtils.addVideoTrackMediaStream(mLocalVideoTrack);
        }
        return mLocalVideoTrack;
    }

    /**
     * 创建数据通道
     *
     * @param peer
     * @param isForceCreation
     */
    public void createP2PDataChannel(Peer peer, boolean isForceCreation) {
        mThreadChecker.checkIsOnValidThread();
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        LogUtils.i(TAG, "创建 createP2PDataChannel peerId:" + peerId + ",isConnected():" + isConnected()
            + ", isForceCreation:" + isForceCreation);
        if (!isConnected()) {
            return;
        }
        mP2PConnectState = RoomConstant.P2PConnectState.NEW;
        PeerConnection peerConnection = getOrCreatePeerConnection(peer, isForceCreation);
        peerConnection.createDataChannel("dongxl", new DataChannel.Init());
    }

    /**
     * 重新交换sdp
     *
     * @param peer
     * @param isForceCreation
     */
    public void createP2POfferOrAnswer(Peer peer, boolean isForceCreation) {
        mThreadChecker.checkIsOnValidThread();
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        boolean isCreateOffer = isCreateOffer();
        boolean isCreateAnswer = isCreateAnswer();
        LogUtils.i(TAG, "操作 createP2POfferOrAnswer peerId:" + peerId + ",isP2PRecipient:" + isP2PRecipient
            + ", isForceCreation:" + isForceCreation + ", isCreateAnswer:" + isCreateAnswer + ", isCreateOffer:"
            + isCreateOffer + ",isConnected():" + isConnected());
        if (!isConnected() || (!isCreateOffer && !isCreateAnswer)) {
            return;
        }
        if (!isP2PRecipient) {//发起方
            createP2POfferSdp(peer, isForceCreation);
        } else {//接收方（邀请方）
            createP2PAnswerSdp(peer, isForceCreation);
        }
    }

    /**
     * 发起方 步骤一：发起Offer
     * 创建 createOffer
     * 设置 setLocalDescription
     * 发送 offer sdp 给接收方id  进入 @see #jieshoufang1
     *
     * @param peer            对方用户信息
     * @param isForceCreation 是否强制创建createOffer
     */
    public void createP2POfferSdp(Peer peer, boolean isForceCreation) {
        mThreadChecker.checkIsOnValidThread();
        isP2PRecipient = false;
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        boolean isCreateOffer = isCreateOffer();
        LogUtils.i(TAG, "创建 createP2POfferSdp peerId:" + peerId + ",isConnected():" + isConnected()
            + ", isForceCreation:" + isForceCreation + ", isCreateOffer:" + isCreateOffer);
        if (!isConnected()) {
            this.sdpPeer = peer;
            isNeedCreateOfferSdp = true;
            return;
        }
        if (!isForceCreation && isCreateOffer) {
            return;
        }
        mP2PConnectState = RoomConstant.P2PConnectState.OFFERSDP;
        PeerConnection peerConnection = getOrCreatePeerConnection(peer, isForceCreation);
        peerConnection.createOffer(new P2PSdpObserver(TAG_OFFER_SDP + peerId, connectCallback) {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                isP2PRecipient = false;
                LogUtils.i(P2PConnectFactory.TAG, "回调：createP2POfferSdp onCreateSuccess peerId:"
                    + peerId + ",sessionDescription.type:" + sessionDescription.type + ",sessionDescription.description:"
                    + sessionDescription.description + ", null != connectCallback:" + (null != connectCallback));
                if (!isWebrtcConnected()) {
                    peerConnection.setLocalDescription(new P2PSdpObserver(TAG_LOCAL_SDP + peerId), sessionDescription);
                }
                if (null != connectCallback) {
                    connectCallback.sendOfferSdp(peerId, getSDPJSONObject(sessionDescription));
                }
//                addP2PConnectPeer(peer);
            }
        }, getOfferMediaConstraints());
    }

    private MediaConstraints getOfferMediaConstraints() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "getOfferMediaConstraints");
        MediaConstraints mediaConstraints = new MediaConstraints();

        mediaConstraints.mandatory.add(
            new MediaConstraints.KeyValuePair("IceRestart", "true"));
        mediaConstraints.mandatory.add(
            new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(
            new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        return mediaConstraints;
    }

    /**
     * 接收方 接收发送方的 Offer 创建Answer
     * 接收到发送方发送的 Offer SDP 设置setRemoteDescription
     * 创建 createAnswer
     * 设置 setLocalDescription
     * 发送 answer sdp 给发送方(邀请方)  进入 @see #faqifang2
     *
     * @param peer            对方用户信息
     * @param data            Offer SDP  数据
     * @param isForceCreation 是否强制创建createAnswer
     *                        <p>
     *                        SessionDescription.Type.offer: 发起方提供的自己对本次通话的描述；
     *                        SessionDescription.Type.answer: 其他方收到 offer 后，给出的回应；
     *                        SessionDescription.Type.pranswer: provisional answer，非最终 answer，之后可能被 pranswer 或 answer 更新；
     */
    public void setP2POfferCreateAnswer(Peer peer, JSONObject data, boolean isForceCreation) {
        mThreadChecker.checkIsOnValidThread();
        isP2PRecipient = true;
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        boolean isCreateAnswer = isCreateAnswer();
        LogUtils.i(TAG, "接收 setP2POfferCreateAnswer peerId:" + peerId + ",data:"
            + (null == data ? "null" : data.toString()) + ",isConnected:" + isConnected()
            + ", isForceCreation:" + isForceCreation + ",isCreateAnswer:" + isCreateAnswer);
        if (null == data) {
            return;
        }
        if (!isConnected()) {
            this.sdpPeer = peer;
            this.sdpJSON = data;
            isNeedCreateAnswerSdp = true;
            return;
        }

        mP2PConnectState = RoomConstant.P2PConnectState.SETOFFER;
        PeerConnection peerConnection = getOrCreatePeerConnection(peer, isForceCreation);
        peerConnection.setRemoteDescription(new P2PSdpObserver(TAG_REMOTE_SDP + peerId),
            new SessionDescription(SessionDescription.Type.OFFER, data.optString(KEY_SDP_CONTENT)));
        LogUtils.i(TAG, "接收 setP2POfferCreateAnswer setRemoteDescription Going to createAnswer isP2PRecipient:"
            + isP2PRecipient + ", isForceCreation:" + isForceCreation + ",isCreateAnswer:" + isCreateAnswer);
        if (!isForceCreation && isCreateAnswer) {
            return;
        }
        mP2PConnectState = RoomConstant.P2PConnectState.ANSWERSDP;
        peerConnection.createAnswer(new P2PSdpObserver(TAG_ANSWER_SDP + peerId, connectCallback) {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                super.onCreateSuccess(sdp);
                isP2PRecipient = true;
                LogUtils.i(P2PConnectFactory.TAG, "回调：createP2PAnswerSdp onCreateSuccess peerId:"
                    + peerId + ",sdp.type:" + sdp.type + ",sdp.description:" + sdp.description
                    + ", null != connectCallback:" + (null != connectCallback));
                if (!isWebrtcConnected()) {
                    peerConnection.setLocalDescription(new P2PSdpObserver(TAG_LOCAL_SDP + peerId), sdp);
                }
                if (null != connectCallback) {
                    connectCallback.sendAnswerSdp(peerId, getSDPJSONObject(sdp));
                }
//                addP2PConnectPeer(peer);
            }
        }, getAnswerMediaConstraints());
//        setSponsorP2POfferSdp(peer, data, isForceCreation);
//        createP2PAnswerSdp(peer, isForceCreation);
    }

    private MediaConstraints getAnswerMediaConstraints() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "getAnswerMediaConstraints");
        MediaConstraints mediaConstraints = new MediaConstraints();

        mediaConstraints.mandatory.add(
            new MediaConstraints.KeyValuePair("IceRestart", "true"));
        mediaConstraints.mandatory.add(
            new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(
            new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));

        return mediaConstraints;
    }

    /**
     * 接收方 步骤一
     * 创建 createAnswer
     * 设置 setLocalDescription
     * 发送 answer sdp 给发送方(邀请方)  进入 @see #faqifang2
     *
     * @param peer            对方用户信息
     * @param isForceCreation 是否强制创建createAnswer
     */
    public void createP2PAnswerSdp(Peer peer, boolean isForceCreation) {
        mThreadChecker.checkIsOnValidThread();
        isP2PRecipient = true;
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        boolean isCreateAnswer = isCreateAnswer();
        LogUtils.i(TAG, "创建 createP2PAnswerSdp peerId:" + peerId + ",isConnected:" + isConnected()
            + ", isForceCreation:" + isForceCreation + ", isCreateAnswer:" + isCreateAnswer);
        if (!isConnected()) {
            return;
        }
        if (!isForceCreation && isCreateAnswer) {
            return;
        }
        mP2PConnectState = RoomConstant.P2PConnectState.ANSWERSDP;
        PeerConnection peerConnection = getOrCreatePeerConnection(peer, isForceCreation);
        peerConnection.createAnswer(new P2PSdpObserver(TAG_ANSWER_SDP + peerId, connectCallback) {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                super.onCreateSuccess(sdp);
                isP2PRecipient = true;
                LogUtils.i(P2PConnectFactory.TAG, "回调：createP2PAnswerSdp onCreateSuccess peerId:"
                    + peerId + ",sdp.type:" + sdp.type + ",sdp.description:" + sdp.description
                    + ", null != connectCallback:" + (null != connectCallback));
                if (!isWebrtcConnected()) {
                    peerConnection.setLocalDescription(new P2PSdpObserver(TAG_LOCAL_SDP + peerId), sdp);
                }
                if (null != connectCallback) {
                    connectCallback.sendAnswerSdp(peerId, getSDPJSONObject(sdp));
                }
//                addP2PConnectPeer(peer);
            }
        }, getAnswerMediaConstraints());
    }

    /**
     * 接收方（邀请方）步骤二：接收发起方Offer 创建连接
     * 设置setRemoteDescription
     *
     * @param peer            对方用户信息
     * @param data            Offer SDP  数据
     * @param isForceCreation 是否强制创建createAnswer
     */
    public void setSponsorP2POfferSdp(Peer peer, JSONObject data, boolean isForceCreation) {
        mThreadChecker.checkIsOnValidThread();
        isP2PRecipient = true;
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        LogUtils.i(TAG, "设置 setSponsorP2POfferSdp peerId:" + peerId + ",data:"
            + (null == data ? "null" : data.toString()) + ",isConnecting():" + isConnecting()
            + ",isForceCreation:" + isForceCreation);
        if (null == data) {
            return;
        }
        if (!isConnecting()) {
            return;
        }
        mP2PConnectState = RoomConstant.P2PConnectState.SETOFFER;
        PeerConnection peerConnection = getOrCreatePeerConnection(peer, isForceCreation);
        peerConnection.setRemoteDescription(new P2PSdpObserver(TAG_REMOTE_SDP + peerId),
            new SessionDescription(SessionDescription.Type.OFFER, data.optString(KEY_SDP_CONTENT)));
    }

    /**
     * 发起方 步骤二：接收接收方（邀请方）Answer 创建连接
     * 设置setRemoteDescription
     *
     * @param peer 对方用户信息
     * @param data Answer SDP  数据
     */
    public void setResponderP2PAnswerSdp(Peer peer, JSONObject data) {
        mThreadChecker.checkIsOnValidThread();
        isP2PRecipient = false;
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        LogUtils.i(TAG, "接收 setResponderP2PAnswerSdp peerId:" + peerId + ",data:"
            + (null == data ? "null" : data.toString()) + ",isConnecting():" + isConnecting());
        if (null == data) {
            return;
        }
        if (!isConnecting()) {
            return;
        }
        mP2PConnectState = RoomConstant.P2PConnectState.SETANSWER;
        PeerConnection peerConnection = getOrCreatePeerConnection(peer, false);
        peerConnection.setRemoteDescription(new P2PSdpObserver(TAG_REMOTE_SDP + peerId),
            new SessionDescription(/*SessionDescription.Type.ANSWER*/SessionDescription.Type.PRANSWER, data.optString(KEY_SDP_CONTENT)));
    }

    /**
     * ice 设置
     * 接收到对方发送的IceCandidate
     * 添加addIceCandidate
     * 自己createPeerConnection回调中 onIceCandidate 收到IceCandidate数据  发送给对方
     *
     * @param peer 对方用户信息
     */
    public void addP2PIceCandidate(Peer peer, JSONObject data) {
        mThreadChecker.checkIsOnValidThread();
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        LogUtils.i(TAG, "接收：addP2PIceCandidate peerId:" + peerId + ",data:"
            + (null == data ? "null" : data.toString()) + ",isConnecting():" + isConnected());
        if (null == data) {
            return;
        }
        if (!isConnected()) {
            isNeedAddIce = true;
            this.sdpPeer = peer;
            iceJSONList.add(data);
            return;
        }
        PeerConnection peerConnection = getOrCreatePeerConnection(peer, false);
        peerConnection.addIceCandidate(new IceCandidate(
            data.optString(KEY_ICE_ID),//描述协议的id
            data.optInt(KEY_ICE_LABEL),//描述协议的行索引
            data.optString(KEY_ICE_CANDIDATE)//会话描述协议
        ));
    }

    /**
     * 获取 PeerConnection
     *
     * @param peer
     * @return
     */
    private synchronized PeerConnection getOrCreatePeerConnection(Peer peer, boolean isForceCreation) {
        mThreadChecker.checkIsOnValidThread();
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        PeerConnection peerConnection = getPeerConnection(peerId);
        if (peerConnection != null/* && !isForceCreation*/) {
            LogUtils.i(TAG, "获取 getOrCreatePeerConnection peerId:" + peerId + ",isForceCreation:" + isForceCreation);
            return peerConnection;
        }
        LogUtils.i(TAG, "创建 getOrCreatePeerConnection peerId:" + peerId + ",isForceCreation:" + isForceCreation);
        if (null != peerConnection) {
            LogUtils.e(TAG, "getOrCreatePeerConnection create new PeerConnection peerId:" + peerId + ",isForceCreation:" + isForceCreation);
            peerConnectionMap.remove(peerId);
//            peerConnection.dispose();
        }
        //RTCConfiguration.iceTransportsType 属性：enum IceTransportsType
        // NONE	不收集策略信息，目前作用未知
        //  RELAY	只使用服务器的策略信息，简言之就是不通过P2P，只走服务端流量，如果想要保证客户端的联通率，那么RELAY是最好的选择
        //  NOHOST	不收集host类的策略信息
        //  ALL	全部收集，如果想减少流量，那么就用ALL，WebRTC会在能打通P2P情况下使用P2P
        peerConnection = mPeerConnectionUtils.getPeerConnectionFactory().createPeerConnection(P2PConnectUtils.getConnectIceServers(), new P2PConnectionObserver(peerId, connectCallback) {

            /**
             * 等待自己createPeerConnection回调中 onIceCandidate 收到IceCandidate数据  发送给对方
             * @param iceCandidate
             */
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                LogUtils.i(P2PConnectFactory.TAG, "回调 getOrCreatePeerConnection onIceCandidate peerId:"
                    + peerId + ",iceCandidate:" + iceCandidate.toString() + ", null != connectCallback:"
                    + (null != connectCallback));
                if (null != connectCallback) {
                    connectCallback.sendIceCandidate(peerId, getIceJSONObject(iceCandidate));
                }
            }

            /**
             * 完成连接，等待自己createPeerConnection回调中 onAddStream 方法 添加remoteVideoTrack 到SurfaceViewRenderer
             * @param mediaStream
             */
            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                addP2PConnectPeer(peer);
                mP2PConnectState = RoomConstant.P2PConnectState.CONNECTED;
                boolean audioEnabled = null == mLocalAudioTrack ? false : mLocalAudioTrack.enabled();
                boolean videoEnabled = null == mLocalVideoTrack ? false : mLocalVideoTrack.enabled();
                List<AudioTrack> audioTracks = mediaStream.audioTracks;
                List<VideoTrack> videoTracks = mediaStream.videoTracks;
                final AudioTrack remoteAudioTrack = audioTracks.isEmpty() ? null : audioTracks.get(0);
                final VideoTrack remoteVideoTrack = videoTracks.isEmpty() ? null : videoTracks.get(0);
                boolean remoteAudioEnabled = null == remoteAudioTrack ? false : remoteAudioTrack.enabled();
                boolean remoteVideoEnabled = null == remoteVideoTrack ? false : remoteVideoTrack.enabled();
                LogUtils.i(P2PConnectFactory.TAG, "回调 getOrCreatePeerConnection onAddStream peerId:" + peerId
                    + ", mediaStream:" + mediaStream.toString() + ", audioEnabled:" + audioEnabled
                    + ", remoteAudioEnabled:" + remoteAudioEnabled + ", videoEnabled:" + videoEnabled
                    + ", remoteVideoEnabled:" + remoteVideoEnabled + ", mediaStream.preservedVideoTracks:"
                    + mediaStream.preservedVideoTracks.size() + ", null != connectCallback:" + (null != connectCallback));
                addRemoteTrack(peerId, remoteAudioTrack, remoteVideoTrack);
                if (null != connectCallback) {
                    connectCallback.onJoinSuc(1);
                }
            }
        });
        peerConnection.addStream(mPeerConnectionUtils.getMediaStream());
        peerConnectionMap.put(peerId, peerConnection);
        return peerConnection;
    }

    private PeerConnection getPeerConnection(String peerId) {
        if (Utils.isEmptyString(peerId)) {
            LogUtils.e(TAG, "有错 getPeerConnection peerId == null");
            return null;
        }
        if (peerConnectionMap.containsKey(peerId)) {
            return peerConnectionMap.get(peerId);
        }
        return null;
    }

    /**
     * 是否连接
     *
     * @return
     */
    private boolean isWebrtcConnected() {
        return null != mP2PConnectState && mP2PConnectState == RoomConstant.P2PConnectState.CONNECTED;
    }

    /**
     * 是否创建过CreateOffer
     *
     * @return
     */
    private boolean isCreateOffer() {
        return !isP2PRecipient && null != mP2PConnectState
            && (mP2PConnectState == RoomConstant.P2PConnectState.CONNECTED
            || mP2PConnectState == RoomConstant.P2PConnectState.OFFERSDP
            || mP2PConnectState == RoomConstant.P2PConnectState.SETANSWER
        );
    }

    /**
     * 是否创建过CreateAnswer
     *
     * @return
     */
    private boolean isCreateAnswer() {
        return isP2PRecipient && null != mP2PConnectState
            && (mP2PConnectState == RoomConstant.P2PConnectState.CONNECTED
            || mP2PConnectState == RoomConstant.P2PConnectState.SETOFFER
            || mP2PConnectState == RoomConstant.P2PConnectState.ANSWERSDP
        );
    }

    private JSONObject getSDPJSONObject(SessionDescription sdp) {
        if (null == sdp) {
            LogUtils.e(TAG, "生成 getSDPJSONObject sdp == null");
            return null;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put(KEY_SDP_CONTENT, sdp.description);
            payload.put(KEY_SDP_TYPE, sdp.type.canonicalForm());
            return payload;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getIceJSONObject(IceCandidate iceCandidate) {
        if (null == iceCandidate) {
            LogUtils.e(TAG, "生成 getIceJSONObject iceCandidate == null");
            return null;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put(KEY_ICE_ID, iceCandidate.sdpMid);//描述协议的id
            payload.put(KEY_ICE_LABEL, iceCandidate.sdpMLineIndex);//描述协议的行索引
            payload.put(KEY_ICE_CANDIDATE, iceCandidate.sdp);//会话描述协议
            return payload;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 添加本地的track
     */
    private void addLocalTrack() {
        LogUtils.i(TAG, "添加 addLocalTrack mSelfId:" + mSelfId + ",videoType:" + getVideoType()
            + ", null == mLocalAudioTrack:" + (null == mLocalAudioTrack) + ", null == mLocalVideoTrack:"
            + (null == mLocalVideoTrack));
        mStore.addP2PSelfAudioTrack(mSelfId, mLocalAudioTrack);
        mStore.addP2PSelfVideoTrack(mSelfId, getVideoType(), mLocalVideoTrack);
    }

    /**
     * p2p下添加 对方信息
     *
     * @param peer
     */
    private void addP2PConnectPeer(Peer peer) {
//        mThreadChecker.checkIsOnValidThread();
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        LogUtils.i(TAG, "添加 addP2PConnectPeer===peerId:" + peerId);
        if (Utils.isEmptyString(peerId)) {
            return;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put(Peer.KEY_PEER_ID, peerId);
            payload.put(Peer.KEY_PEER_NAME, peerName);
            payload.put(Peer.KEY_PEER_P2PMODE, isP2PMode);
            mStore.addPeer(peerId, payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加远程的 tarck
     *
     * @param peerId
     * @param remoteAudioTrack
     * @param remoteVideoTrack
     */
    private void addRemoteTrack(String peerId, AudioTrack remoteAudioTrack, VideoTrack remoteVideoTrack) {
        LogUtils.i(TAG, "添加 addRemoteTrack mSelfId:" + mSelfId + ",peerId:" + peerId
            + ",(null == remoteAudioTrack):" + (null == remoteAudioTrack) + ",(null == remoteVideoTrack):"
            + (null == remoteVideoTrack));
        mStore.addP2POtherAudioTrack(peerId, remoteAudioTrack);
        mStore.addP2POtherVideoTrack(peerId, remoteVideoTrack);
    }

    public void resume() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "resume===");
    }

    public void pause() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "pause===");
    }

    /**
     * 释放mLocalAudioTrack
     */
    private void releaseAudioTrack(boolean isFinish) {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "releaseVideoTrack==isFinish==" + isFinish);
        if (mLocalAudioTrack != null && null != mPeerConnectionUtils) {
            mPeerConnectionUtils.removeAudioTrackMediaStream(mLocalAudioTrack);
        }
        if (mLocalAudioTrack != null) {
            mLocalAudioTrack.setEnabled(false);
            if (isFinish) {
                mLocalAudioTrack.dispose();
                mLocalAudioTrack = null;
            }
        }
        if (isFinish && null != mPeerConnectionUtils) {
            mPeerConnectionUtils.releaseAudioSource();
        }
    }

    /**
     * 释放mLocalVideoTrack
     */
    private void releaseVideoTrack(boolean isFinish) {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "releaseVideoTrack==isFinish==" + isFinish);
        if (mLocalVideoTrack != null && null != mPeerConnectionUtils) {
            mPeerConnectionUtils.removeVideoTrackMediaStream(mLocalVideoTrack);
        }
        if (mLocalVideoTrack != null) {
            mLocalVideoTrack.setEnabled(false);
            if (isFinish) {
                mLocalVideoTrack.dispose();
                mLocalVideoTrack = null;
            }
        }
        if (isFinish && null != mPeerConnectionUtils) {
            mPeerConnectionUtils.releaseVideoCapturer();
        }
    }

    public void destroy() {
        LogUtils.i(TAG, "destroy==isP2PRecipient:" + isP2PRecipient);
        mThreadChecker.checkIsOnValidThread();
        mP2PConnectState = RoomConstant.P2PConnectState.CLOSED;
        for (Map.Entry<String, PeerConnection> entry : peerConnectionMap.entrySet()) {
            PeerConnection peerConnection = entry.getValue();
            if (null != peerConnection) {
                peerConnection.dispose();
            }
        }
//        releaseAudioTrack(true);
//        releaseVideoTrack(true);
        peerConnectionMap.clear();
        sdpPeer = null;
        sdpJSON = null;
        isP2PRecipient = false;
        isNeedCreateOfferSdp = false;
        isNeedCreateAnswerSdp = false;
        isNeedAddIce = false;
        iceJSONList.clear();
    }

    /**
     * 禁止使用麦克风 录音
     */
    public void disableMicImpl() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "disableMicImpl==1==null == mLocalAudioTrack:" + (null == mLocalAudioTrack));
        if (null == mLocalAudioTrack) {
            return;
        }
        releaseAudioTrack(false);
//        createP2POfferOrAnswer(getConnectPeer(), true);
        mStore.addP2PSelfAudioTrack(mSelfId, mLocalAudioTrack);
    }

    /**
     * 启用麦克风
     */
    public void enableMicImpl() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "enableMicImpl==1==null == mLocalAudioTrack:" + (null == mLocalAudioTrack));
        if (null != mLocalAudioTrack) {
            if (!mLocalAudioTrack.enabled()) {
                mLocalAudioTrack.setEnabled(true);
            }
            return;
        }
        getLocalAudioTrack();
//        createP2POfferOrAnswer(getConnectPeer(), true);
        mStore.addP2PSelfAudioTrack(mSelfId, mLocalAudioTrack);
    }

    /**
     * 静音麦克风
     */
    public void muteMicImpl() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "muteMicImpl==1==null == mLocalAudioTrack:" + (null == mLocalAudioTrack));
//        for (Map.Entry<String, PeerConnection> entry : peerConnectionMap.entrySet()) {
//            PeerConnection peerConnection = entry.getValue();
//            if (null != peerConnection) {
//                peerConnection.setAudioRecording(false);
//            }
//        }
        if (null != mLocalAudioTrack && mLocalAudioTrack.enabled()) {
            mLocalAudioTrack.setEnabled(false);
        }
    }

    /**
     * 取消静音麦克风
     */
    public void unmuteMicImpl() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "unmuteMicImpl==1==null == mLocalAudioTrack:" + (null == mLocalAudioTrack));
//        for (Map.Entry<String, PeerConnection> entry : peerConnectionMap.entrySet()) {
//            PeerConnection peerConnection = entry.getValue();
//            if (null != peerConnection) {
//                peerConnection.setAudioRecording(true);
//            }
//        }

        if (null == mLocalAudioTrack) {
            enableMicImpl();
        } else if (!mLocalAudioTrack.enabled()) {
            mLocalAudioTrack.setEnabled(true);
        }
    }


    /**
     * 启用摄像头
     */
    public void enableCamImpl() {
        mThreadChecker.checkIsOnValidThread();
        RoomConstant.VideoCapturerType capturerType = null != mPeerConnectionUtils ? mPeerConnectionUtils.getCurrentVideoCapturer() : null;
        LogUtils.i(TAG, "enableCamImpl==1==capturerType:" + capturerType + ", null == mLocalVideoTrack:"
            + (null == mLocalVideoTrack));
        if (null != mLocalVideoTrack && capturerType == RoomConstant.VideoCapturerType.CAMERA) {
            if (!mLocalVideoTrack.enabled()) {
                mLocalVideoTrack.setEnabled(true);
            }
            return;
        }
        getLocalVideoTrack(RoomConstant.VideoCapturerType.CAMERA);
//        createP2POfferOrAnswer(getConnectPeer(), true);
        mStore.addP2PSelfVideoTrack(mSelfId, getVideoType(), mLocalVideoTrack);
    }

    /**
     * 禁用摄像头
     */
    public void disableCamImpl() {
        mThreadChecker.checkIsOnValidThread();
        RoomConstant.VideoCapturerType capturerType = null != mPeerConnectionUtils ? mPeerConnectionUtils.getCurrentVideoCapturer() : null;
        LogUtils.i(TAG, "disableCamImpl==1==null == mLocalVideoTrack:" + (null == mLocalVideoTrack));
        if (null == mLocalVideoTrack || capturerType != RoomConstant.VideoCapturerType.CAMERA) {
            return;
        }
        releaseVideoTrack(false);
//        createP2POfferOrAnswer(getConnectPeer(), true);
        mStore.addP2PSelfVideoTrack(mSelfId, getVideoType(capturerType), mLocalVideoTrack);
    }

    /**
     * 启用屏幕共享（功能暂未实现）测试版
     */
    public void enableShareImpl() {
        mThreadChecker.checkIsOnValidThread();
        RoomConstant.VideoCapturerType capturerType = null != mPeerConnectionUtils ? mPeerConnectionUtils.getCurrentVideoCapturer() : null;
        LogUtils.i(TAG, "enableShareImpl==1==capturerType:" + capturerType + ", null == mLocalVideoTrack:"
            + (null == mLocalVideoTrack));
        if (null != mLocalVideoTrack && capturerType == RoomConstant.VideoCapturerType.SCREEN) {
            if (!mLocalVideoTrack.enabled()) {
                mLocalVideoTrack.setEnabled(true);
            }
            return;
        }
        getLocalVideoTrack(RoomConstant.VideoCapturerType.SCREEN);
//        createP2POfferOrAnswer(getConnectPeer(), true);
        mStore.addP2PSelfVideoTrack(mSelfId, getVideoType(), mLocalVideoTrack);
    }

    /**
     * 禁用屏幕共享（功能暂未实现）测试版
     *
     * @param isContinue
     */
    public void disableShareImpl(boolean isContinue) {
        mThreadChecker.checkIsOnValidThread();
        RoomConstant.VideoCapturerType capturerType = null != mPeerConnectionUtils ? mPeerConnectionUtils.getCurrentVideoCapturer() : null;
        LogUtils.i(TAG, "disableShareImpl==1==isContinue:" + isContinue + ", capturerType:" + capturerType
            + ", null == mLocalVideoTrack:" + (null == mLocalVideoTrack));
        if (null == mLocalVideoTrack || capturerType != RoomConstant.VideoCapturerType.SCREEN) {
            return;
        }
        releaseVideoTrack(false);
        //关闭屏幕共享后如果之前是摄像头模式 ，继续启用摄像头
        if (isContinue && mOptions.isEnableVideo()) {
            enableCamImpl();//启用摄像头
        } else {
//            createP2POfferOrAnswer(getConnectPeer(), true);
            mStore.addP2PSelfVideoTrack(mSelfId, getVideoType(capturerType), mLocalVideoTrack);
        }
    }

    /**
     * 设置p2p连接 对方状态
     *
     * @param otherState
     */
    public void setP2POtherState(RoomConstant.P2POtherState otherState) {
        LogUtils.i(TAG, "setP2POtherState==1==otherState:" + otherState);
        mThreadChecker.checkIsOnValidThread();
//        for (Map.Entry<String, PeerConnection> entry : peerConnectionMap.entrySet()) {
//            PeerConnection peerConnection = entry.getValue();
//            if (null != peerConnection) {
//                switch (otherState) {
//                    case VIDEO_RESUME:
//                        break;
//                    case VIDEO_PAUSE:
//                        break;
//                    case AUDIO_RESUME:
//                        peerConnection.setAudioPlayout(true);
//                        break;
//                    case AUDIO_PAUSE:
//                        peerConnection.setAudioPlayout(false);
//                        break;
//                }
//            }
//        }
    }

    /**
     * 重新启动ice
     */
    public void restartP2PIce() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "restartP2PIce==1==");
    }
}
