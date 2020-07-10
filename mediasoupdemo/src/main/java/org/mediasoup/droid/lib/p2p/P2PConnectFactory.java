package org.mediasoup.droid.lib.p2p;

import android.content.Context;

import androidx.annotation.NonNull;

import com.jsy.mediasoup.utils.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.RoomConstant;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.interfaces.MediasoupConnectCallback;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.model.Producers;
import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoTrack;

import java.util.HashMap;
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
    private Map<String, PeerConnection> peerConnectionMap = new HashMap<>();
    private PeerConnectionUtils mPeerConnectionUtils;
    private final MediasoupConnectCallback connectCallback;
    private final ThreadUtils.ThreadChecker mThreadChecker;

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
        mThreadChecker = new ThreadUtils.ThreadChecker();
    }

    public void joinImpl() {
        LogUtils.i(TAG, "joinImpl:");
        mThreadChecker.checkIsOnValidThread();

        mStore.setMediaCapabilities(true, true);
        if (mOptions.isEnableAudio()) {
            getLocalAudioTrack();//启动麦克风 录音
        }
        if (mOptions.isEnableVideo()) {
            getLocalVideoTrack(RoomConstant.VideoCapturerType.CAMERA);//启用摄像头
        }
    }

    private AudioTrack getLocalAudioTrack() {
        if (mLocalAudioTrack == null) {
            mLocalAudioTrack = mPeerConnectionUtils.createAudioTrack(mContext);
            mLocalAudioTrack.setEnabled(true);
            mPeerConnectionUtils.addAudioTrackMediaStream(mLocalAudioTrack);
            mStore.addP2PSelfAudioTrack(mSelfId, mLocalAudioTrack);
        }
        return mLocalAudioTrack;
    }

    private VideoTrack getLocalVideoTrack(RoomConstant.VideoCapturerType capturerType) {
        if (mLocalVideoTrack == null || mPeerConnectionUtils.getCurrentVideoCapturer() != capturerType) {
            releaseVideoTrack();
            mLocalVideoTrack = mPeerConnectionUtils.createVideoTrack(mContext, capturerType);
            mLocalVideoTrack.setEnabled(true);
            mPeerConnectionUtils.addVideoTrackMediaStream(mLocalVideoTrack);
            mStore.addP2PSelfVideoTrack(mSelfId, Producers.ProducersWrapper.TYPE_CAM, mLocalVideoTrack);
        }
        return mLocalVideoTrack;
    }

    /**
     * 发起方 步骤一：发起Offer
     * 创建 createOffer
     * 设置 setLocalDescription
     * 发送 offer sdp 给接收方id  进入 @see #jieshoufang1
     *
     * @param peer 对方用户信息
     */
    public void createP2POfferSdp(Peer peer) {
        mThreadChecker.checkIsOnValidThread();
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        LogUtils.i(TAG, "createP2POfferSdp peerId:" + peer.getId());
        PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
        peerConnection.createOffer(new P2PSdpObserver(TAG_OFFER_SDP + peerId) {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                LogUtils.i(P2PConnectFactory.TAG, "createP2POfferSdp onCreateSuccess peerId:" + peerId + ",sessionDescription.type:" + sessionDescription.type + ",sessionDescription.description:" + sessionDescription.description);
                peerConnection.setLocalDescription(new P2PSdpObserver(TAG_LOCAL_SDP + peerId), sessionDescription);
                if (null != connectCallback) {
                    connectCallback.sendOfferSdp(peerId, getSDPJSONObject(sessionDescription));
                }
            }
        }, new MediaConstraints());
    }

    /**
     * 接收方 步骤一 接收发送方的 Offer 创建Answer
     * 接收到发送方发送的 Offer SDP 设置setRemoteDescription
     * 创建 createAnswer
     * 设置 setLocalDescription
     * 发送 answer sdp 给发送方(邀请方)  进入 @see #faqifang2
     *
     * @param peer 对方用户信息
     */
    public void createP2PAnswerSdp(Peer peer, JSONObject data) {
        mThreadChecker.checkIsOnValidThread();
        if (null == data) {
            return;
        }
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        LogUtils.i(TAG, "createP2PAnswerSdp peerId:" + peerId + ",data:" + data.toString());
        addP2PConnectPeer(peer);
        PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
        peerConnection.setRemoteDescription(new P2PSdpObserver(TAG_REMOTE_SDP + peerId),
                new SessionDescription(SessionDescription.Type.OFFER, data.optString(KEY_SDP_CONTENT)));
        peerConnection.createAnswer(new P2PSdpObserver(TAG_ANSWER_SDP + peerId) {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                super.onCreateSuccess(sdp);
                LogUtils.i(P2PConnectFactory.TAG, "createP2PAnswerSdp onCreateSuccess peerId:" + peerId + ",sdp.type:" + sdp.type + ",sdp.description:" + sdp.description);
                peerConnectionMap.get(peerId).setLocalDescription(new P2PSdpObserver(TAG_LOCAL_SDP + peerId), sdp);
                if (null != connectCallback) {
                    connectCallback.sendAnswerSdp(peerId, getSDPJSONObject(sdp));
                }
            }
        }, new MediaConstraints());
    }

    /**
     * 发起方 步骤二：接收接收方（邀请方）Answer 创建连接
     * 设置setRemoteDescription
     *
     * @param peer 对方用户信息
     */
    public void setP2PAnswerSdp(Peer peer, JSONObject data) {
        mThreadChecker.checkIsOnValidThread();
        if (null == data) {
            return;
        }
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        LogUtils.i(TAG, "setP2PAnswerSdp peerId:" + peerId + ",data:" + data.toString());
        addP2PConnectPeer(peer);
        PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
        peerConnection.setRemoteDescription(new P2PSdpObserver(TAG_REMOTE_SDP + peerId),
                new SessionDescription(SessionDescription.Type.ANSWER, data.optString(KEY_SDP_CONTENT)));
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
        if (null == data) {
            return;
        }
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        LogUtils.i(TAG, "addP2PIceCandidate peerId:" + peerId + ",data:" + data.toString());
        mStore.setRoomState(RoomConstant.ConnectionState.CONNECTED);//设置状态 已经连接
        mStore.addNotify("You are in the room!", 3000);//添加一个已经加入房间通知
        PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
        peerConnection.addIceCandidate(new IceCandidate(
                data.optString(KEY_ICE_ID),
                data.optInt(KEY_ICE_LABEL),
                data.optString(KEY_ICE_CANDIDATE)
        ));
    }

    /**
     * 获取 PeerConnection
     *
     * @param peerId
     * @return
     */
    private synchronized PeerConnection getOrCreatePeerConnection(String peerId) {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "getOrCreatePeerConnection peerId:" + peerId);
        PeerConnection peerConnection = peerConnectionMap.get(peerId);
        if (peerConnection != null) {
            return peerConnection;
        }
        peerConnection = mPeerConnectionUtils.getPeerConnectionFactory().createPeerConnection(P2PConnectUtils.getConnectIceServers(), new P2PConnectionObserver(peerId) {

            /**
             * 等待自己createPeerConnection回调中 onIceCandidate 收到IceCandidate数据  发送给对方
             * @param iceCandidate
             */
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                LogUtils.i(P2PConnectFactory.TAG, "getOrCreatePeerConnection onIceCandidate peerId:" + peerId + ",iceCandidate:" + iceCandidate.toString());
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
                LogUtils.i(P2PConnectFactory.TAG, "getOrCreatePeerConnection onAddStream peerId:" + peerId + ",mediaStream:" + mediaStream.toString() + ",mediaStream.preservedVideoTracks:" + mediaStream.preservedVideoTracks.size());
                final AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                final VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                mStore.addP2POtherAudioTrack(peerId, remoteAudioTrack);
                mStore.addP2POtherVideoTrack(peerId, remoteVideoTrack);
                if (null != connectCallback) {
                    connectCallback.onJoinSuc();
                }
            }
        });
        peerConnection.addStream(mPeerConnectionUtils.getMediaStream());
        peerConnectionMap.put(peerId, peerConnection);
        return peerConnection;
    }

    private JSONObject getSDPJSONObject(SessionDescription sdp) {
        if (null == sdp) {
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
            return null;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put(KEY_ICE_ID, iceCandidate.sdpMid);
            payload.put(KEY_ICE_LABEL, iceCandidate.sdpMLineIndex);
            payload.put(KEY_ICE_CANDIDATE, iceCandidate.sdp);
            return payload;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * p2p下添加 对方信息
     *
     * @param peer
     */
    private void addP2PConnectPeer(Peer peer) {
        mThreadChecker.checkIsOnValidThread();
        String peerId = null == peer ? "" : peer.getId();
        String peerName = null == peer ? "" : peer.getDisplayName();
        LogUtils.i(TAG, "addP2PConnectPeer===peerId:" + peerId);
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
    private void releaseAudioTrack() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "releaseVideoTrack===");
        if (mLocalAudioTrack != null) {
            mLocalAudioTrack.setEnabled(false);
            mLocalAudioTrack.dispose();
            mLocalAudioTrack = null;
        }
        if (null != mPeerConnectionUtils) {
            mPeerConnectionUtils.releaseAudioSource();
        }
    }

    /**
     * 释放mLocalVideoTrack
     */
    private void releaseVideoTrack() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "releaseVideoTrack===");
        if (mLocalVideoTrack != null) {
            mLocalVideoTrack.setEnabled(false);
            mLocalVideoTrack.dispose();
            mLocalVideoTrack = null;
        }
        if (null != mPeerConnectionUtils) {
            mPeerConnectionUtils.releaseVideoCapturer();
        }
    }

    public void connectClose() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "connectClose==mClosed:");

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
        LogUtils.e(TAG, "close() end mClosed：");
    }

    public void destroy() {
        LogUtils.i(TAG, "destroy==mClosed:");
        mThreadChecker.checkIsOnValidThread();
        connectClose();
        for (Map.Entry<String, PeerConnection> entry : peerConnectionMap.entrySet()) {
            PeerConnection peerConnection = entry.getValue();
            if (null != peerConnection) {
                peerConnection.dispose();
            }
        }
        peerConnectionMap.clear();
    }

    /**
     * 禁止使用麦克风 录音
     */
    public void disableMicImpl() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.e(TAG, "disposeTransportDevice()");
        releaseAudioTrack();
    }

    /**
     * 启用麦克风
     */
    public void enableMicImpl() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.e(TAG, "disposeTransportDevice()");
        getLocalAudioTrack();
    }

    /**
     * 静音麦克风
     */
    public void muteMicImpl() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.e(TAG, "disposeTransportDevice()");
        for (Map.Entry<String, PeerConnection> entry : peerConnectionMap.entrySet()) {
            PeerConnection peerConnection = entry.getValue();
            if (null != peerConnection) {
                peerConnection.setAudioRecording(false);
            }
        }
    }

    /**
     * 取消静音麦克风
     */
    public void unmuteMicImpl() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.e(TAG, "disposeTransportDevice()");
        for (Map.Entry<String, PeerConnection> entry : peerConnectionMap.entrySet()) {
            PeerConnection peerConnection = entry.getValue();
            if (null != peerConnection) {
                peerConnection.setAudioRecording(true);
            }
        }
    }


    /**
     * 启用摄像头
     */
    public void enableCamImpl() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.e(TAG, "disposeTransportDevice()");
        getLocalVideoTrack(RoomConstant.VideoCapturerType.CAMERA);
    }

    /**
     * 禁用摄像头
     */
    public void disableCamImpl() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.e(TAG, "disposeTransportDevice()");
        RoomConstant.VideoCapturerType capturerType = null != mPeerConnectionUtils ? mPeerConnectionUtils.getCurrentVideoCapturer() : null;
        if (capturerType != RoomConstant.VideoCapturerType.CAMERA) {
            return;
        }
        releaseVideoTrack();
    }

    /**
     * 启用屏幕共享（功能暂未实现）测试版
     */
    public void enableShareImpl() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.e(TAG, "disposeTransportDevice()");
        if (null != mLocalVideoTrack) {
            RoomConstant.VideoCapturerType capturerType = mPeerConnectionUtils.getCurrentVideoCapturer();
            if (capturerType == RoomConstant.VideoCapturerType.CAMERA) {
                disableCamImpl();
            } else if (capturerType == RoomConstant.VideoCapturerType.FILE) {

            } else {
                return;
            }
        }
        getLocalVideoTrack(RoomConstant.VideoCapturerType.SCREEN);
    }

    /**
     * disableShareImpl
     *
     * @param isContinue
     */
    public void disableShareImpl(boolean isContinue) {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.e(TAG, "disposeTransportDevice() isContinue:" + isContinue);
        RoomConstant.VideoCapturerType capturerType = null != mPeerConnectionUtils ? mPeerConnectionUtils.getCurrentVideoCapturer() : null;
        if (capturerType != RoomConstant.VideoCapturerType.SCREEN) {
            return;
        }
        releaseVideoTrack();

        //关闭屏幕共享后如果之前是摄像头模式 ，继续启用摄像头
        if (isContinue && mOptions.isEnableVideo()) {
            enableCamImpl();//启用摄像头
        }
    }

    /**
     * 设置p2p连接 对方状态
     *
     * @param otherState
     */
    public void setP2POtherState(RoomConstant.P2POtherState otherState) {
        LogUtils.i(TAG, "setP2POtherState===otherState:" + otherState);
        mThreadChecker.checkIsOnValidThread();
        for (Map.Entry<String, PeerConnection> entry : peerConnectionMap.entrySet()) {
            PeerConnection peerConnection = entry.getValue();
            if (null != peerConnection) {
                switch (otherState) {
                    case VIDEO_RESUME:
                        break;
                    case VIDEO_PAUSE:
                        break;
                    case AUDIO_RESUME:
                        peerConnection.setAudioPlayout(true);
                        break;
                    case AUDIO_PAUSE:
                        peerConnection.setAudioPlayout(false);
                        break;
                }
            }
        }
    }

    /**
     * 重新启动ice
     */
    public void restartP2PIce() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "restartP2PIce===");
    }
}
