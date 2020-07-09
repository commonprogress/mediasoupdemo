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
    // Android context.
    @NonNull
    private final Context mContext;
    @NonNull
    private final RoomStore mStore;
    private final RoomOptions mOptions;
    private final String mRoomId;
    private final String mSelfId;
    private final String mDisplayName;
    private final String mClientId;
    private final boolean mP2PMode;
    private PeerConnectionUtils mPeerConnectionUtils;
    private AudioTrack mLocalAudioTrack;
    private VideoTrack mLocalVideoTrack;
    private Map<String, PeerConnection> peerConnectionMap = new HashMap<>();
    private final MediasoupConnectCallback connectCallback;
    private final ThreadUtils.ThreadChecker mThreadChecker;

    public P2PConnectFactory(Context context, RoomStore roomStore,
                             boolean p2PMode, String roomId, String peerId,
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
        this.mP2PMode = p2PMode;
        this.mRoomId = roomId;
        this.mSelfId = peerId;
        this.mClientId = clientId;
        this.mDisplayName = displayName;
        this.mPeerConnectionUtils = peerConnectionUtils;
        mThreadChecker = new ThreadUtils.ThreadChecker();
    }

    public void joinRoom(int type) {
        LogUtils.i(TAG, "initRoom type:" + type);
        mThreadChecker.checkIsOnValidThread();
        mStore.setRoomState(RoomConstant.ConnectionState.CONNECTING);


        mStore.setRoomState(RoomConstant.ConnectionState.CONNECTED);//设置状态 已经连接
        mStore.addNotify("You are in the room!", 3000);//添加一个已经加入房间通知
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
     * @param peerId 对方id
     */
    public void faqifang1(String peerId) {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "faqifang1 peerId:" + peerId);
        PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
        peerConnection.createOffer(new P2PSdpObserver("createOfferSdp:" + peerId) {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                LogUtils.i(P2PConnectFactory.TAG, "faqifang1 onCreateSuccess peerId:" + peerId + ",sessionDescription.type:" + sessionDescription.type + ",sessionDescription.description:" + sessionDescription.description);
                peerConnection.setLocalDescription(new P2PSdpObserver("setLocalSdp:" + peerId), sessionDescription);
//                if (null != connectCallback) {
//                    connectCallback.sendOfferSdpToReceive(peerId, sessionDescription.type.canonicalForm(), sessionDescription.description);
//                }
            }
        }, new MediaConstraints());
//        if (null != connectCallback) {
//            connectCallback.onSendSelfState(peerId);
//        }
    }

    /**
     * 接收方 步骤一 接收发送方的 Offer 创建Answer
     * 接收到发送方发送的 Offer SDP 设置setRemoteDescription
     * 创建 createAnswer
     * 设置 setLocalDescription
     * 发送 answer sdp 给发送方(邀请方)  进入 @see #faqifang2
     *
     * @param peerId 对方id
     */
    public void jieshoufang1(String peerId, JSONObject data) {
        mThreadChecker.checkIsOnValidThread();
        if (null == data) {
            return;
        }
        LogUtils.i(TAG, "jieshoufang1 peerId:" + peerId + ",data:" + data.toString());
        addP2PConnectPeer(peerId);
        PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
        peerConnection.setRemoteDescription(new P2PSdpObserver("setRemoteSdp:" + peerId),
                new SessionDescription(SessionDescription.Type.OFFER, data.optString("sdp")));
        peerConnection.createAnswer(new P2PSdpObserver("localAnswerSdp") {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                super.onCreateSuccess(sdp);
                LogUtils.i(P2PConnectFactory.TAG, "jieshoufang1 onCreateSuccess peerId:" + peerId + ",sdp.type:" + sdp.type + ",sdp.description:" + sdp.description);
                peerConnectionMap.get(peerId).setLocalDescription(new P2PSdpObserver("setLocalSdp:" + peerId), sdp);
//                if (null != connectCallback) {
//                    connectCallback.sendAnswerSdpToSend(peerId, sdp.type.canonicalForm(), sdp.description);
//                }
            }
        }, new MediaConstraints());
//        if (null != connectCallback) {
//            connectCallback.onReceiveSelfState(peerId);
//        }
    }

    /**
     * 发起方 步骤二：接收接收方（邀请方）Answer 创建连接
     * 设置setRemoteDescription
     * 自己在createPeerConnection回调中 onIceCandidate 收到IceCandidate数据  发送给接收方
     *
     * @param peerId 对方id
     */
    public void faqifang2(String peerId, JSONObject data) {
        mThreadChecker.checkIsOnValidThread();
        if (null == data) {
            return;
        }
        LogUtils.i(TAG, "faqifang2 peerId:" + peerId + ",data:" + data.toString());
        addP2PConnectPeer(peerId);
        PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
        peerConnection.setRemoteDescription(new P2PSdpObserver("setRemoteSdp:" + peerId),
                new SessionDescription(SessionDescription.Type.ANSWER, data.optString("sdp")));
    }

    /**
     * 接收方 步骤二 收到发送方 发送的IceCandidate
     * 添加addIceCandidate
     * 自己createPeerConnection回调中 onIceCandidate 收到IceCandidate数据  发送给发送方(邀请方)
     * 完成连接，等待自己createPeerConnection回调中 onAddStream 方法 添加remoteVideoTrack 到SurfaceViewRenderer
     *
     * @param peerId 对方id
     */
    public void jieshoufang2(String peerId, JSONObject data) {
        mThreadChecker.checkIsOnValidThread();
        if (null == data) {
            return;
        }
        LogUtils.i(TAG, "jieshoufang2 peerId:" + peerId + ",data:" + data.toString());
        mStore.setRoomState(RoomConstant.ConnectionState.CONNECTED);//设置状态 已经连接
        PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
        peerConnection.addIceCandidate(new IceCandidate(
                data.optString("id"),
                data.optInt("label"),
                data.optString("candidate")
        ));
    }

    /**
     * 发起方 步骤三：收到接收方 发送的IceCandidate
     * 添加 addIceCandidate
     * 完成连接，等待自己createPeerConnection回调中 onAddStream 方法 添加remoteVideoTrack 到SurfaceViewRenderer
     *
     * @param peerId 对方id
     */
    public void faqifang3(String peerId, JSONObject data) {
        mThreadChecker.checkIsOnValidThread();
        if (null == data) {
            return;
        }
        LogUtils.i(TAG, "faqifang3 peerId:" + peerId + ",data:" + data.toString());
        mStore.setRoomState(RoomConstant.ConnectionState.CONNECTED);//设置状态 已经连接
        PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
        peerConnection.addIceCandidate(new IceCandidate(
                data.optString("id"),
                data.optInt("label"),
                data.optString("candidate")
        ));
    }

    private synchronized PeerConnection getOrCreatePeerConnection(String peerId) {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.i(TAG, "getOrCreatePeerConnection peerId:" + peerId);
        PeerConnection peerConnection = peerConnectionMap.get(peerId);
        if (peerConnection != null) {
            return peerConnection;
        }
        peerConnection = mPeerConnectionUtils.getPeerConnectionFactory().createPeerConnection(P2PConnectUtils.getConnectIceServers(), new P2PConnectionObserver(peerId) {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                LogUtils.i(P2PConnectFactory.TAG, "getOrCreatePeerConnection onIceCandidate peerId:" + peerId + ",iceCandidate:" + iceCandidate.toString());
//                if (null != connectCallback) {
//                    connectCallback.sendIceCandidateOtherSide(peerId, iceCandidate.sdpMid, iceCandidate.sdpMLineIndex, iceCandidate.sdp, iceCandidate.serverUrl, iceCandidate.adapterType.bitMask);
//                }
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                LogUtils.i(P2PConnectFactory.TAG, "getOrCreatePeerConnection onAddStream peerId:" + peerId + ",mediaStream:" + mediaStream.toString() + ",mediaStream.preservedVideoTracks:" + mediaStream.preservedVideoTracks.size());
                final AudioTrack remoteAudioTrack = mediaStream.audioTracks.get(0);
                final VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
                mStore.addP2POtherAudioTrack(peerId, remoteAudioTrack);
                mStore.addP2POtherVideoTrack(peerId, remoteVideoTrack);
//                if (null != connectCallback) {
//                    connectCallback.onP2PConnectSuc(peerId);
//                }
            }
        });
        peerConnection.addStream(mPeerConnectionUtils.getMediaStream());
        peerConnectionMap.put(peerId, peerConnection);
        return peerConnection;
    }

    /**
     * p2p下添加 对方信息
     *
     * @param peerId
     */
    private void addP2PConnectPeer(String peerId) {
        mThreadChecker.checkIsOnValidThread();
        try {
            JSONObject payload = new JSONObject();
            payload.put("id", peerId);
            payload.put("isP2PMode", mP2PMode);
            payload.put("displayName", "dufangname");
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
        // dispose all transport and device.
        disposeTransportDevice();
        LogUtils.e(TAG, "close() mid mClosed：");
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

        // dispose peerConnection.
        if (null != mPeerConnectionUtils) {
            mPeerConnectionUtils.dispose();
        }

        LogUtils.e(TAG, "close() end mClosed：");
        mStore.setRoomState(RoomConstant.ConnectionState.CLOSED);
    }

    private void disposeTransportDevice() {
        mThreadChecker.checkIsOnValidThread();
        LogUtils.e(TAG, "disposeTransportDevice()");
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
}
