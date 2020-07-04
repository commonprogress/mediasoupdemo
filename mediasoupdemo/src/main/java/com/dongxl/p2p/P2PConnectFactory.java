package com.dongxl.p2p;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.WorkerThread;

import com.dongxl.p2p.utils.P2PConnectUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.Async;
import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.RoomConstant;
import org.webrtc.AudioTrack;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.List;

public class P2PConnectFactory {
    public static final String TAG = P2PConnectFactory.class.getSimpleName();
    // Android context.
    private Context mContext;
    // Closed flag.
    private volatile boolean mClosed;
    // PeerConnection util.
    private PeerConnectionUtils mPeerConnectionUtils;
    private SurfaceViewRenderer localSurface, remoteSurface;
    // Local Audio Track for mic.
    private AudioTrack mLocalAudioTrack;
    // local Video Track for cam.
    private VideoTrack mLocalVideoTrack;

    private HashMap<String, PeerConnection> peerConnectionMap;
    // jobs worker handler.
    private Handler mWorkHandler;
    // main looper handler.
    private Handler mMainHandler;

    public P2PConnectFactory(Context context) {
        this.mContext = context;
    }

    public void init(Context context, EglBase.Context eglBaseContext, SurfaceViewRenderer localSurface, SurfaceViewRenderer remoteSurface) {
        this.mContext = context;
        this.remoteSurface = remoteSurface;
        this.localSurface = localSurface;
        // init worker handler.
        HandlerThread handlerThread = new HandlerThread("worker");
        handlerThread.start();
        mWorkHandler = new Handler(handlerThread.getLooper());
        mMainHandler = new Handler(Looper.getMainLooper());
        mWorkHandler.post(() -> mPeerConnectionUtils = new PeerConnectionUtils());

    }

    private AudioTrack getLocalAudioTrack() {
        if (mLocalAudioTrack == null) {
            mLocalAudioTrack = mPeerConnectionUtils.createAudioTrack(mContext);
            mLocalAudioTrack.setEnabled(true);
            mPeerConnectionUtils.addAudioTrackMediaStream(mLocalAudioTrack);
        }
        return mLocalAudioTrack;
    }

    private VideoTrack getLocalVideoTrack(RoomConstant.VideoCapturerType capturerType) {
        if (mLocalVideoTrack == null || mPeerConnectionUtils.getCurrentVideoCapturer() != capturerType) {
            releaseVideoTrack();
            mLocalVideoTrack = mPeerConnectionUtils.createVideoTrack(mContext, capturerType);
            mLocalVideoTrack.setEnabled(true);
            mPeerConnectionUtils.addVideoTrackMediaStream(mLocalVideoTrack);
        }
        return mLocalVideoTrack;
    }

    /**
     * 创建房间成功
     */
    public void onCreateRoom() {

    }

    private synchronized PeerConnection getOrCreatePeerConnection(String socketId) {
        PeerConnection peerConnection = peerConnectionMap.get(socketId);
        if (peerConnection != null) {
            return peerConnection;
        }
        peerConnection = mPeerConnectionUtils.getPeerConnectionFactory().createPeerConnection(P2PConnectUtils.getConnectIceServers(), new P2PConnectionObserver(socketId) {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                super.onIceCandidate(iceCandidate);
//                JSONObject payload = new JSONObject();
//                payload.put("label", iceCandidate.sdpMLineIndex);
//                payload.put("id", iceCandidate.sdpMid);
//                payload.put("candidate", iceCandidate.sdp);

//                SignalingClient.get().sendIceCandidate(iceCandidate, socketId);
                //P2PConnectUtils.sendIceCandidate(iceCandidate, socketId);
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {
                super.onAddStream(mediaStream);
                VideoTrack remoteVideoTrack = mediaStream.videoTracks.get(0);
//                runOnUiThread(() -> {
//                    remoteVideoTrack.addSink(remoteViews[remoteViewsIndex++]);
//                });
            }
        });
        peerConnection.addStream(mPeerConnectionUtils.getMediaStream());
//        peerConnection.addTrack(mVideoTrack);
//        peerConnection.addTrack(mAudioTrack);

        peerConnectionMap.put(socketId, peerConnection);
        return peerConnection;
    }

    /**
     * 有人加入
     *
     * @param socketId
     */
    public void onPeerJoined(String socketId) {
        final PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
        peerConnection.createOffer(new P2PSdpObserver("createOfferSdp:" + socketId) {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                super.onCreateSuccess(sessionDescription);
                peerConnection.setLocalDescription(new P2PSdpObserver("setLocalSdp:" + socketId), sessionDescription);
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
            }
        }, new MediaConstraints());
    }

    /**
     * 自己加入
     */
    public void onSelfJoined() {

    }

    /**
     * 有人离开
     *
     * @param msg
     */
    public void onPeerLeave(String msg) {

    }

    /**
     * 响应邀请
     *
     * @param data
     */
    public void onOfferReceived(JSONObject data) {
//        runOnUiThread(() -> {
        final String socketId = data.optString("from");
        PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
        peerConnection.setRemoteDescription(new P2PSdpObserver("setRemoteSdp:" + socketId),
                new SessionDescription(SessionDescription.Type.OFFER, data.optString("sdp")));
        peerConnection.createAnswer(new P2PSdpObserver("localAnswerSdp") {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                super.onCreateSuccess(sdp);
                peerConnectionMap.get(socketId).setLocalDescription(new P2PSdpObserver("setLocalSdp:" + socketId), sdp);
//                    SignalingClient.get().sendSessionDescription(sdp, socketId);
//                P2PConnectUtils.sendSessionDescription(sdp,socketId);
            }
        }, new MediaConstraints());

//        });
    }

    /**
     * 回答邀请
     *
     * @param data
     */
    public void onAnswerReceived(JSONObject data) {
        String socketId = data.optString("from");
        PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
        peerConnection.setRemoteDescription(new P2PSdpObserver("setRemoteSdp:" + socketId),
                new SessionDescription(SessionDescription.Type.ANSWER, data.optString("sdp")));
    }

    /**
     * ice 响应
     *
     * @param data
     */
    public void onIceCandidateReceived(JSONObject data) {
        String socketId = data.optString("from");
        PeerConnection peerConnection = getOrCreatePeerConnection(socketId);
//        if(null != peerConnection.getRemoteDescription()) {
        peerConnection.addIceCandidate(new IceCandidate(
                data.optString("id"),
                data.optInt("label"),
                data.optString("candidate")
        ));
//        }
    }

    public void resume() {

    }

    public void pause() {

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

    @Async
    public void connectClose() {
        if (this.mClosed) {
            return;
        }
        this.mClosed = true;
        mWorkHandler.post(
                () -> {
                    Logger.e(TAG, "close() start mClosed：" + mClosed);
                    // Close mProtoo Protoo
//                    if (mProtoo != null) {
//                        mProtoo.close();
//                        mProtoo = null;
//                    }

                    // dispose all transport and device.
                    disposeTransportDevice();
                    Logger.e(TAG, "close() mid mClosed：" + mClosed);
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
                    Logger.e(TAG, "close() end mClosed：" + mClosed);
                });
    }

    @WorkerThread
    private void disposeTransportDevice() {
        Logger.e(TAG, "disposeTransportDevice()");
    }

    public void destroy() {
        connectClose();
//        PeerConnectionFactory.stopInternalTracingCapture()
//        PeerConnectionFactory.shutdownInternalTracer()
    }
}
