package org.mediasoup.droid.lib.p2p;

import com.jsy.mediasoup.utils.LogUtils;

import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;

public class P2PConnectionObserver implements PeerConnection.Observer {
    public static final String TAG = P2PConnectionObserver.class.getSimpleName();

    private String tag;

    public P2PConnectionObserver() {
        this("");
    }

    public P2PConnectionObserver(String tag) {
        this.tag = "dongxl:" + tag;
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        LogUtils.i(TAG, "onSignalingChange " + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        LogUtils.i(TAG, "onIceConnectionChange " + iceConnectionState);
        if (iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
//            isCall = false;
        }
        if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
//            isCall = true;
        }
        if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
//            peers.keys.forEach {
//                if (!it.equals(uuid)) {
//                    peers.remove(it);
//                }
//            }
//            isCall = false;
        }
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        LogUtils.i(TAG, "onIceConnectionReceivingChange " + b);
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        LogUtils.i(TAG, "onIceGatheringChange " + iceGatheringState);
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        LogUtils.i(TAG, "onIceCandidate " + iceCandidate.toString());
    }

    private String getIceCandidateStr(IceCandidate[] iceCandidates) {
        int length = null != iceCandidates ? iceCandidates.length : 0;
        if (length > 0) {
            String str = "length:" + length + "[";
            for (IceCandidate iceCandidate : iceCandidates) {
                str += (iceCandidate.toString() + "],[");
            }
            return str;
        }
        return "length=null";
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        LogUtils.i(TAG, "onIceCandidatesRemoved iceCandidates:" + getIceCandidateStr(iceCandidates));
//        mPeerConnection?.removeIceCandidates(candidates)
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        LogUtils.i(TAG, "onAddStream audioTracks:" + mediaStream.audioTracks.size() + ", videoTracks:" + mediaStream.videoTracks.size());
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        LogUtils.i(TAG, "onRemoveStream audioTracks:" + mediaStream.audioTracks.size() + ", videoTracks:" + mediaStream.videoTracks.size());
//        remoteSurface?.release()
//        mPeerConnection?.close()
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        LogUtils.i(TAG, "onDataChannel label:" + dataChannel.label() + ",id:" + dataChannel.id() + ",state:" + dataChannel.state());
    }

    @Override
    public void onRenegotiationNeeded() {
        LogUtils.i(TAG, "onRenegotiationNeeded ");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        LogUtils.i(TAG, "onAddTrack rtpReceiver:" + rtpReceiver.id() + ",getParameters:" + rtpReceiver.getParameters() + ",mediaStreams:" + mediaStreams.length);
//        isCall = true
//        val track = rtpReceiver?.track()
//        if (track is VideoTrack) {
//            val remoteVideoTrack = track
//            remoteVideoTrack.setEnabled(true)
//            val videoSink = ProxyVideoSink()
//            remoteSurface?.let { videoSink.setTarget(it) }
//            remoteVideoTrack.addSink(videoSink)
//        }
    }
}
