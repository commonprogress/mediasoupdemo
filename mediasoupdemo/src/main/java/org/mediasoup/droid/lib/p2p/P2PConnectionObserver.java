package org.mediasoup.droid.lib.p2p;

import com.jsy.mediasoup.utils.LogUtils;

import org.mediasoup.droid.lib.interfaces.MediasoupConnectCallback;
import org.webrtc.AudioTrack;
import org.webrtc.CandidatePairChangeEvent;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.VideoTrack;

import java.util.List;

public class P2PConnectionObserver implements PeerConnection.Observer {
    public static final String TAG = P2PConnectionObserver.class.getSimpleName();

    private String peerTag;
    private final MediasoupConnectCallback connectCallback;
    private PeerConnection.SignalingState curSignalingState;
    private PeerConnection.IceConnectionState curIceConnectionState;
    private PeerConnection.IceGatheringState curIceGatheringState;
    private PeerConnection.PeerConnectionState curPeerConnectionState;

    public P2PConnectionObserver() {
        this(TAG);
    }

    public P2PConnectionObserver(String peerTag) {
        this(peerTag, null);
    }

    public P2PConnectionObserver(String peerTag, MediasoupConnectCallback connectCallback) {
        this.peerTag = peerTag;
        this.connectCallback = connectCallback;
    }

    //SignalingState变化时触发
    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        //STABLE	没有正在进行的SDP offer/answer交换，这也是连接的初始状态
        //HAVE_LOCAL_OFFER	本地已调用PeerConnection.setLocalDescription()，传入代表offer的SDP（调用PeerConnection.createOffer()创建），并且offer已成功保存到本地
        //HAVE_REMOTE_OFFER	远端创建了一个offer，并通过信令服务器将其发送到本地，本地通过调用PeerConnection.setRemoteDescription()将远端offer保存到本地
        //HAVE_LOCAL_PRANSWER	收到远端发送的offer并保存在本地，然后创建了answer（调用PeerConnection.createAnswer()），然后调用PeerConnection.setLocalDescription()保存answer
        //HAVE_REMOTE_PRANSWER	已收到并成功保存answer
        //CLOSED	连接关闭

        //信令状态改变。
        //产生/设置 SDP 后，会触发 signaling state 变化，常见的变化是 stable -> have-local-offer -> stable 或 stable -> have-remote-offer -> stable ，具体可以查看 SPEC 4.3 State Definitions ；
        LogUtils.i(TAG, "onSignalingChange " + peerTag + ", signalingState:" + signalingState
            + ",curSignalingState:" + curSignalingState);
        if (null != connectCallback) {

        }
        this.curSignalingState = signalingState;
    }

    //IceConnectionState变化时触发
    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        // 符合标准的ICE连接状态改变。
        //ICE 连接状态变化后回调；
        //"new": ICE 代理正在搜集地址或者等待远程候选可用。
        //"checking": ICE 代理已收到至少一个远程候选，并进行校验，无论此时是否有可用连接。同时可能在继续收集候选。
        //"connected": ICE代理至少对每个候选发现了一个可用的连接，此时仍然会继续测试远程候选以便发现更优的连接。同时可能在继续收集候选。
        //"completed": ICE代理已经发现了可用的连接，不再测试远程候选。
        //"failed": ICE候选测试了所有远程候选没有发现匹配的候选。也可能有些候选中发现了一些可用连接。
        //"disconnected": 测试不再活跃，这可能是一个暂时的状态，可以自我恢复。
        //"closed": ICE代理关闭，不再应答任何请求
        LogUtils.i(TAG, "onIceConnectionChange " + peerTag + ", iceConnectionState:" + iceConnectionState
            + ", curIceConnectionState:" + curIceConnectionState);
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

        if (null != connectCallback) {
            if (curIceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED
                && iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                connectCallback.onP2PConnectionFailed();
            }
        }
        this.curIceConnectionState = iceConnectionState;
    }

    //ICE连接接收状态发生变化时触发
    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {
        LogUtils.i(TAG, "onIceConnectionReceivingChange " + peerTag + ", receiving：" + receiving);
    }

    //IceGatheringState 变化时触发
    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        //ICE收集状态改变
        //ICE candidate 收集状态变化后回调；

        //NEW	刚刚创建
        //GATHERING	正在收集
        //COMPLETE	完成收集
        LogUtils.i(TAG, "onIceGatheringChange " + peerTag + ", iceGatheringState:" + iceGatheringState
            + ", curIceGatheringState:" + curIceGatheringState);
        if (null != connectCallback) {

        }
        this.curIceGatheringState = iceGatheringState;
    }

    //重要的方法，当有新的IceCandidate加入时，就会发送事件，并触发该方法
    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        //收集到一个新的ICE候选项时触发。
        //收集到本地 ICE candidate 后回调；
//        LogUtils.i(TAG, "onIceCandidate " + peerTag + ",iceCandidate: " + iceCandidate.toString());
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

    //当有IceCandidate移除时，就会发送事件，并触发该方法
    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        //当候选项被移除时触发。
        //本地 ICE candidate 被移除后回调；
        LogUtils.i(TAG, "onIceCandidatesRemoved " + peerTag + ", iceCandidates:" + getIceCandidateStr(iceCandidates));
//        mPeerConnection?.removeIceCandidates(candidates)
        if (null != connectCallback) {

        }
    }

    //重要的方法，该动作是由对端的Peerconnection的addstream动作产生的事件触发，即发现新的媒体流时
    @Override
    public void onAddStream(MediaStream mediaStream) {
        //收到远端Peer的一个新stream。
//        LogUtils.i(TAG, "onAddStream " + peerTag + ", audioTracks:" + mediaStream.audioTracks.size()
//            + ", videoTracks:" + mediaStream.videoTracks.size());
    }

    //媒体流被关闭时触发
    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        //收到远端Peer移出一个stream。
        LogUtils.i(TAG, "onRemoveStream " + peerTag + ", audioTracks:" + mediaStream.audioTracks.size()
            + ", videoTracks:" + mediaStream.videoTracks.size());

        List<AudioTrack> audioTracks = mediaStream.audioTracks;
        List<VideoTrack> videoTracks = mediaStream.videoTracks;
        final AudioTrack remoteAudioTrack = audioTracks.isEmpty() ? null : audioTracks.get(0);
        final VideoTrack remoteVideoTrack = videoTracks.isEmpty() ? null : videoTracks.get(0);
        boolean remoteAudioEnabled = null == remoteAudioTrack ? false : remoteAudioTrack.enabled();
        boolean remoteVideoEnabled = null == remoteVideoTrack ? false : remoteVideoTrack.enabled();
        LogUtils.i(P2PConnectFactory.TAG, "onRemoveStream " + peerTag
            + ", mediaStream:" + mediaStream.toString() + ", remoteAudioEnabled:" + remoteAudioEnabled
            + ", remoteVideoEnabled:" + remoteVideoEnabled + ", mediaStream.preservedVideoTracks:"
            + mediaStream.preservedVideoTracks.size());

//        remoteSurface?.release()
//        mPeerConnection?.close()
        if (null != connectCallback) {

        }
    }

    //对端打开DataChannel
    @Override
    public void onDataChannel(DataChannel dataChannel) {
        //当远端打开data channel通道时触发。
        LogUtils.i(TAG, "onDataChannel " + peerTag + ", label:" + dataChannel.label() + ",id:" + dataChannel.id()
            + ",state:" + dataChannel.state());
        if (null != connectCallback) {

        }
    }

    @Override
    public void onRenegotiationNeeded() {
        //需要重新协商时触发，比如重启ICE时。
        //需要重新协商（重新建立 P2P 连接）时回调，例如 ICE restart 时会回调；
        boolean isRenegotiation = null != curSignalingState && null != curIceConnectionState && null != curIceGatheringState;
        LogUtils.i(TAG, "onRenegotiationNeeded " + peerTag + ",isRenegotiation:" + isRenegotiation
            + ", curSignalingState:" + curSignalingState + ", curIceConnectionState:" + curIceConnectionState
            + ", curPeerConnectionState:" + curPeerConnectionState + ", curIceGatheringState:" + curIceGatheringState);
        if (null != connectCallback) {
            connectCallback.onP2PReExchangeSDP(isRenegotiation);
        }
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
//        当一个receiver和它的track被创建时。Plan B 和 Unified Plan语法下都会被调用，但是Unified Plan语法下更建议使用OnTrack回调，OnAddTrack只是为了兼容之前的Plan B遗留的接口，二者在同样的情况下被回调。
        int length = null == mediaStreams ? 0 : mediaStreams.length;
        LogUtils.i(TAG, "onAddTrack " + peerTag + ", rtpReceiver:" + rtpReceiver.id() + ",getParameters:"
            + (null == rtpReceiver.getParameters() ? "null" : rtpReceiver.getParameters().transactionId)
            + ",mediaStreams.length:" + length);

        for (MediaStream mediaStream : mediaStreams) {
            List<AudioTrack> audioTracks = mediaStream.audioTracks;
            List<VideoTrack> videoTracks = mediaStream.videoTracks;
            final AudioTrack remoteAudioTrack = audioTracks.isEmpty() ? null : audioTracks.get(0);
            final VideoTrack remoteVideoTrack = videoTracks.isEmpty() ? null : videoTracks.get(0);
            boolean remoteAudioEnabled = null == remoteAudioTrack ? false : remoteAudioTrack.enabled();
            boolean remoteVideoEnabled = null == remoteVideoTrack ? false : remoteVideoTrack.enabled();
            LogUtils.i(P2PConnectFactory.TAG, "\nonAddTrack for " + peerTag
                + ", mediaStream:" + mediaStream.toString() + ", remoteAudioEnabled:" + remoteAudioEnabled
                + ", remoteVideoEnabled:" + remoteVideoEnabled + ", mediaStream.preservedVideoTracks:"
                + mediaStream.preservedVideoTracks.size());
        }
//        isCall = true
//        val track = rtpReceiver?.track()
//        if (track is VideoTrack) {
//            val remoteVideoTrack = track
//            remoteVideoTrack.setEnabled(true)
//            val videoSink = ProxyVideoSink()
//            remoteSurface?.let { videoSink.setTarget(it) }
//            remoteVideoTrack.addSink(videoSink)
//        }
        if (null != connectCallback) {

        }
    }

    /**
     * @param newState
     * @ see onIceConnectionChange方法
     */
    @Override
    public void onStandardizedIceConnectionChange(PeerConnection.IceConnectionState newState) {
        //符合标准的ICE连接状态改变。
        LogUtils.i(TAG, "onStandardizedIceConnectionChange " + peerTag + ", newState：" + newState);
        if (null != connectCallback) {

        }
    }

    @Override
    public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
//PeerConnection状态改变。
        //"new": ICE 代理正在搜集地址或者等待远程候选可用。
        //"checking": ICE 代理已收到至少一个远程候选，并进行校验，无论此时是否有可用连接。同时可能在继续收集候选。
        //"connected": ICE代理至少对每个候选发现了一个可用的连接，此时仍然会继续测试远程候选以便发现更优的连接。同时可能在继续收集候选。
        //"completed": ICE代理已经发现了可用的连接，不再测试远程候选。
        //"failed": ICE候选测试了所有远程候选没有发现匹配的候选。也可能有些候选中发现了一些可用连接。
        //"disconnected": 测试不再活跃，这可能是一个暂时的状态，可以自我恢复。
        //"closed": ICE代理关闭，不再应答任何请求
        LogUtils.i(TAG, "onConnectionChange " + peerTag + ", newState：" + newState
            + ", curPeerConnectionState:" + curPeerConnectionState);
        this.curPeerConnectionState = newState;
    }

    @Override
    public void onSelectedCandidatePairChanged(CandidatePairChangeEvent event) {
        //ICE连接所采用的候选者对改变。？
        LogUtils.i(TAG, "onSelectedCandidatePairChanged " + peerTag + ", event："
            + (null == event ? "null" : ("reason:" + event.reason + ", lastDataReceivedMs:"
            + event.lastDataReceivedMs + ", \nevent.local:" + (null == event.local ? "null" : event.local.toString())
            + ", \nevent.remote:" + (null == event.remote ? "null" : event.remote.toString()))));
        if (null != connectCallback) {

        }
    }

    //OnRemoveTrack: 当确定一个 track 不再接收媒体数据后，会回调这个接口，track 不会移除，但 transceiver 的 recv 方向将会被去掉

    @Override
    public void onTrack(RtpTransceiver transceiver) {
        //调用 SetRemoteDescription 后，如果 SDP 表明将会创建接收用的 transceiver，就会回调这个接口；
        //该方法在收到的信令指示一个transceiver将从远端接收媒体时被调用，实际就是在调用SetRemoteDescription时被触发。该接收track可以通过transceiver->receiver()->track()方法被访问到，其关联的streams可以通过transceiver->receiver()->streams()获取。只有在Unified Plan语法下，该回调方法才会被触发
        LogUtils.i(TAG, "onConnectionChange " + peerTag + ", transceiver：" + transceiver);
        if (null != connectCallback) {

        }
    }
}
