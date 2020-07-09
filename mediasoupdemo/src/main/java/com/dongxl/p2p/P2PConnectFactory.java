package com.dongxl.p2p;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.dongxl.p2p.utils.P2PConnectUtils;
import com.jsy.mediasoup.utils.LogUtils;

import org.json.JSONObject;
import org.mediasoup.droid.lib.Async;
import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomConstant;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.Map;

public class P2PConnectFactory {
    public static final String TAG = P2PConnectFactory.class.getSimpleName();
    // Android context.
    @NonNull
    private final Context mContext;
    @NonNull
    final RoomStore mRroomStore;
    private final P2PConnectCallback p2PConnectCallback;
    // Closed flag.
    private volatile boolean mClosed;
    // PeerConnection util.
    private PeerConnectionUtils mPeerConnectionUtils;
    private SurfaceViewRenderer localSurface, remoteSurface;
    // Local Audio Track for mic.
    private AudioTrack mLocalAudioTrack;
    // local Video Track for cam.
    private VideoTrack mLocalVideoTrack;
    private RoomConstant.ConnectionState mRoomState;
    private Map<String, PeerConnection> peerConnectionMap = new HashMap<>();
    // jobs worker handler.
    private Handler mWorkHandler;
    // main looper handler.
    private Handler mMainHandler;

    public P2PConnectFactory(Context context, P2PConnectCallback p2PConnectCallback, RoomStore roomStore, SurfaceViewRenderer localSurface, SurfaceViewRenderer remoteSurface) {
        LogUtils.i(TAG, "supe P2PConnectFactory");
        this.mContext = context;
        this.mRroomStore = roomStore;
        this.remoteSurface = remoteSurface;
        this.localSurface = localSurface;
        this.p2PConnectCallback = p2PConnectCallback;
//        this.mRroomStore.setMe(peerId, displayName, this.mOptions.getDevice());
        //设置房间的url
//        this.mRroomStore.setRoomUrl(roomId, UrlFactory.getInvitationLink(roomId, forceH264, forceVP9));
        // init worker handler.
        HandlerThread handlerThread = new HandlerThread("worker");
        handlerThread.start();
        mWorkHandler = new Handler(handlerThread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());
        mWorkHandler.post(() -> mPeerConnectionUtils = new PeerConnectionUtils());
    }

    @Async
    public void initRoom(int type) {
        LogUtils.i(TAG, "initRoom type:" + type);
        mWorkHandler.post(
                () -> {
                    getLocalAudioTrack();
                    getLocalVideoTrack(RoomConstant.VideoCapturerType.CAMERA);
                    mLocalVideoTrack.addSink(localSurface);
                });
    }

    @WorkerThread
    private AudioTrack getLocalAudioTrack() {
        if (mLocalAudioTrack == null) {
            mLocalAudioTrack = mPeerConnectionUtils.createAudioTrack(mContext);
            mLocalAudioTrack.setEnabled(true);
            mPeerConnectionUtils.addAudioTrackMediaStream(mLocalAudioTrack);
//            mRroomStore.addProducer(mLocalAudioTrack);
        }
        return mLocalAudioTrack;
    }

    @WorkerThread
    private VideoTrack getLocalVideoTrack(RoomConstant.VideoCapturerType capturerType) {
        if (mLocalVideoTrack == null || mPeerConnectionUtils.getCurrentVideoCapturer() != capturerType) {
            releaseVideoTrack();
            mLocalVideoTrack = mPeerConnectionUtils.createVideoTrack(mContext, capturerType);
            mLocalVideoTrack.setEnabled(true);
            mPeerConnectionUtils.addVideoTrackMediaStream(mLocalVideoTrack);
//            mRroomStore.addProducer(mLocalVideoTrack, capturerType);
        }
        return mLocalVideoTrack;
    }

    /**
     * 创建房间成功
     */
    public void onCreateRoom() {
        LogUtils.i(TAG, "onCreateRoom mRoomState:" + mRoomState);
        this.mRoomState = RoomConstant.ConnectionState.NEW;
        mRoomState = RoomConstant.ConnectionState.CONNECTING;
        mRoomState = RoomConstant.ConnectionState.CONNECTED;
        mRroomStore.setRoomState(RoomClient.ConnectionState.CONNECTING);
        mRroomStore.setMediaCapabilities(true, true);
    }

    /**
     * 发起方 步骤一：发起Offer
     * 创建 createOffer
     * 设置 setLocalDescription
     * 发送 offer sdp 给接收方id  进入 @see #jieshoufang1
     *
     * @param peerId 对方id
     */
    @Async
    public void faqifang1(String peerId) {
        mWorkHandler.post(
                () -> {
                    LogUtils.i(TAG, "faqifang1 peerId:" + peerId);
                    PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
                    peerConnection.createOffer(new P2PSdpObserver("createOfferSdp:" + peerId) {
                        @Override
                        public void onCreateSuccess(SessionDescription sessionDescription) {
                            super.onCreateSuccess(sessionDescription);
                            LogUtils.i(P2PConnectFactory.TAG, "faqifang1 onCreateSuccess peerId:" + peerId + ",sessionDescription.type:" + sessionDescription.type + ",sessionDescription.description:" + sessionDescription.description);
                            peerConnection.setLocalDescription(new P2PSdpObserver("setLocalSdp:" + peerId), sessionDescription);
//                SignalingClient.get().sendSessionDescription(sessionDescription, socketId);
//                P2PConnectUtils.sendSessionDescription(sessionDescription, socketId);

//                try {
//                    JSONObject payload = new JSONObject();
//                    payload.put("type", sessionDescription.type.canonicalForm());
//                    payload.put("sdp", sessionDescription.description);
//                    SocketManager.instance.sendMessage(socketId, sessionDescription.type.canonicalForm(), payload);
//                    peerConnection.setLocalDescription(this, sessionDescription);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }

                            if (null != p2PConnectCallback) {
                                p2PConnectCallback.sendOfferSdpToReceive(peerId, sessionDescription.type.canonicalForm(), sessionDescription.description);
                            }
                        }
                    }, new MediaConstraints());
                    if (null != p2PConnectCallback) {
                        p2PConnectCallback.onSendSelfState(peerId);
                    }
                });
    }

    /**
     * 发起方 步骤二：接收接收方（邀请方）Answer 创建连接
     * 设置setRemoteDescription
     * 自己在createPeerConnection回调中 onIceCandidate 收到IceCandidate数据  发送给接收方
     *
     * @param peerId 对方id
     */
    @Async
    public void faqifang2(String peerId, JSONObject data) {
        if (null == data) {
            return;
        }
        mWorkHandler.post(
                () -> {
                    LogUtils.i(TAG, "faqifang2 peerId:" + peerId + ",data:" + data.toString());
                    mRroomStore.addPeer(peerId, data);
                    PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
                    peerConnection.setRemoteDescription(new P2PSdpObserver("setRemoteSdp:" + peerId),
                            new SessionDescription(SessionDescription.Type.ANSWER, data.optString("sdp")));
                });
    }

    /**
     * 发起方 步骤三：收到接收方 发送的IceCandidate
     * 添加 addIceCandidate
     * 完成连接，等待自己createPeerConnection回调中 onAddStream 方法 添加remoteVideoTrack 到SurfaceViewRenderer
     *
     * @param peerId 对方id
     */
    @Async
    public void faqifang3(String peerId, JSONObject data) {
        if (null == data) {
            return;
        }
        mWorkHandler.post(
                () -> {
                    LogUtils.i(TAG, "faqifang3 peerId:" + peerId + ",data:" + data.toString());
                    mRroomStore.setRoomState(RoomClient.ConnectionState.CONNECTED);//设置状态 已经连接
                    PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
                    peerConnection.addIceCandidate(new IceCandidate(
                            data.optString("id"),
                            data.optInt("label"),
                            data.optString("candidate")
                    ));
                });
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
    @Async
    public void jieshoufang1(String peerId, JSONObject data) {
        if (null == data) {
            return;
        }
        mWorkHandler.post(
                () -> {
                    LogUtils.i(TAG, "jieshoufang1 peerId:" + peerId + ",data:" + data.toString());
                    PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
                    peerConnection.setRemoteDescription(new P2PSdpObserver("setRemoteSdp:" + peerId),
                            new SessionDescription(SessionDescription.Type.OFFER, data.optString("sdp")));
                    peerConnection.createAnswer(new P2PSdpObserver("localAnswerSdp") {
                        @Override
                        public void onCreateSuccess(SessionDescription sdp) {
                            super.onCreateSuccess(sdp);
                            LogUtils.i(P2PConnectFactory.TAG, "jieshoufang1 onCreateSuccess peerId:" + peerId + ",sdp.type:" + sdp.type + ",sdp.description:" + sdp.description);
                            mRroomStore.addPeer(peerId, data);
                            peerConnectionMap.get(peerId).setLocalDescription(new P2PSdpObserver("setLocalSdp:" + peerId), sdp);
//                    SignalingClient.get().sendSessionDescription(sdp, socketId);
//                P2PConnectUtils.sendSessionDescription(sdp,socketId);
                            if (null != p2PConnectCallback) {
                                p2PConnectCallback.sendAnswerSdpToSend(peerId, sdp.type.canonicalForm(), sdp.description);
                            }
                        }
                    }, new MediaConstraints());
                    if (null != p2PConnectCallback) {
                        p2PConnectCallback.onReceiveSelfState(peerId);
                    }
                });
    }

    /**
     * 接收方 步骤二 收到发送方 发送的IceCandidate
     * 添加addIceCandidate
     * 自己createPeerConnection回调中 onIceCandidate 收到IceCandidate数据  发送给发送方(邀请方)
     * 完成连接，等待自己createPeerConnection回调中 onAddStream 方法 添加remoteVideoTrack 到SurfaceViewRenderer
     *
     * @param peerId 对方id
     */
    @Async
    public void jieshoufang2(String peerId, JSONObject data) {
        if (null == data) {
            return;
        }
        mWorkHandler.post(
                () -> {
                    LogUtils.i(TAG, "jieshoufang2 peerId:" + peerId + ",data:" + data.toString());
                    mRroomStore.setRoomState(RoomClient.ConnectionState.CONNECTED);//设置状态 已经连接
                    PeerConnection peerConnection = getOrCreatePeerConnection(peerId);
                    peerConnection.addIceCandidate(new IceCandidate(
                            data.optString("id"),
                            data.optInt("label"),
                            data.optString("candidate")
                    ));
                });
    }

    @WorkerThread
    private synchronized PeerConnection getOrCreatePeerConnection(String socketId) {
        LogUtils.i(TAG, "getOrCreatePeerConnection peerId:" + socketId);
        PeerConnection peerConnection = peerConnectionMap.get(socketId);
        if (peerConnection != null) {
            return peerConnection;
        }
        peerConnection = mPeerConnectionUtils.getPeerConnectionFactory().createPeerConnection(P2PConnectUtils.getConnectIceServers(), new P2PConnectionObserver(socketId) {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
                LogUtils.i(P2PConnectFactory.TAG, "getOrCreatePeerConnection onIceCandidate peerId:" + socketId + ",iceCandidate:" + iceCandidate.toString());
//                JSONObject payload = new JSONObject();
//                payload.put("label", iceCandidate.sdpMLineIndex);
//                payload.put("id", iceCandidate.sdpMid);
//                payload.put("candidate", iceCandidate.sdp);

//                SignalingClient.get().sendIceCandidate(iceCandidate, socketId);
                //P2PConnectUtils.sendIceCandidate(iceCandidate, socketId);
                if (null != p2PConnectCallback) {
                    p2PConnectCallback.sendIceCandidateOtherSide(socketId, iceCandidate.sdpMid, iceCandidate.sdpMLineIndex, iceCandidate.sdp, iceCandidate.serverUrl, iceCandidate.adapterType.bitMask);
                }
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                LogUtils.i(P2PConnectFactory.TAG, "getOrCreatePeerConnection onAddStream peerId:" + socketId + ",mediaStream:" + mediaStream.toString() + ",mediaStream.preservedVideoTracks:" + mediaStream.preservedVideoTracks.size());
                final VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
//                mRroomStore.addConsumer(socketId, type, consumer, producerPaused);
                mWorkHandler.post(
                        () -> {
                            remoteVideoTrack.addSink(remoteSurface);
                        });
                if (null != p2PConnectCallback) {
                    p2PConnectCallback.onP2PConnectSuc(socketId);
                }
            }
        });
        peerConnection.addStream(mPeerConnectionUtils.getMediaStream());
//        peerConnection.addTrack(mVideoTrack);
//        peerConnection.addTrack(mAudioTrack);

        peerConnectionMap.put(socketId, peerConnection);
        return peerConnection;
    }

    public void resume() {
        LogUtils.i(TAG, "resume===");
    }

    public void pause() {
        LogUtils.i(TAG, "pause===");
    }

    /**
     * 释放mLocalVideoTrack
     */
    @WorkerThread
    private void releaseVideoTrack() {
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

    @Async
    public void connectClose() {
        LogUtils.i(TAG, "connectClose==mClosed:" + mClosed);
        mRoomState = RoomConstant.ConnectionState.CLOSED;
        if (this.mClosed) {
            return;
        }
        this.mClosed = true;
        mWorkHandler.post(
                () -> {
                    LogUtils.e(TAG, "close() start mClosed：" + mClosed);
                    // Close mProtoo Protoo
//                    if (mProtoo != null) {
//                        mProtoo.close();
//                        mProtoo = null;
//                    }

                    // dispose all transport and device.
                    disposeTransportDevice();
                    LogUtils.e(TAG, "close() mid mClosed：" + mClosed);
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

                    // quit worker handler thread.
                    mWorkHandler.getLooper().quit();
                    LogUtils.e(TAG, "close() end mClosed：" + mClosed);
                });
        mRroomStore.setRoomState(RoomClient.ConnectionState.CLOSED);
    }

    @WorkerThread
    private void disposeTransportDevice() {
        LogUtils.e(TAG, "disposeTransportDevice()");
    }

    public void destroy() {
        LogUtils.i(TAG, "destroy==mClosed:" + mClosed);
        connectClose();
        for (Map.Entry<String, PeerConnection> entry : peerConnectionMap.entrySet()) {
            PeerConnection peerConnection = entry.getValue();
            if (null != peerConnection) {
                peerConnection.dispose();
            }
        }
        peerConnectionMap.clear();
//        PeerConnectionFactory.stopInternalTracingCapture()
//        PeerConnectionFactory.shutdownInternalTracer()
    }
}
