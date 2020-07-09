package org.mediasoup.droid.lib.p2p;

import android.media.AudioManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.ArrayList;
import java.util.List;

public class P2PConnectUtils {

    /**
     * 获取IceServer 列表 包括 stun和trun
     * @return
     */
    public static List<PeerConnection.IceServer> getConnectIceServers() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        PeerConnection.IceServer.Builder stunIceServer1 = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302");
        iceServers.add(stunIceServer1.createIceServer());

        PeerConnection.IceServer.Builder stunIceServer3 = PeerConnection.IceServer.builder("stun:stun.xten.com:3478");
        iceServers.add(stunIceServer3.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer4 = PeerConnection.IceServer.builder("stun:stun.voipbuster.com:3478");
        iceServers.add(stunIceServer4.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer5 = PeerConnection.IceServer.builder("stun:stun.voxgratia.org:3478");
        iceServers.add(stunIceServer5.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer6 = PeerConnection.IceServer.builder("stun:stun.sipgate.net:10000");
        iceServers.add(stunIceServer6.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer7 = PeerConnection.IceServer.builder("stun:stun.ekiga.net:3478");
        iceServers.add(stunIceServer7.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer8 = PeerConnection.IceServer.builder("stun:stun.ideasip.com:3478");
        iceServers.add(stunIceServer8.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer9 = PeerConnection.IceServer.builder("stun:stun.schlund.de:3478");
        iceServers.add(stunIceServer9.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer10 = PeerConnection.IceServer.builder("stun:stun.voiparound.com:3478");
        iceServers.add(stunIceServer10.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer11 = PeerConnection.IceServer.builder("stun:stun.voipbuster.com:3478");
        iceServers.add(stunIceServer11.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer12 = PeerConnection.IceServer.builder("stun:stun.voipstunt.com:3478");
        iceServers.add(stunIceServer12.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer13 = PeerConnection.IceServer.builder("stun:numb.viagenie.ca:3478");
        iceServers.add(stunIceServer13.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer14 = PeerConnection.IceServer.builder("stun:stun.counterpath.com:3478");
        iceServers.add(stunIceServer14.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer15 = PeerConnection.IceServer.builder("stun:stun.1und1.de:3478");
        iceServers.add(stunIceServer15.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer16 = PeerConnection.IceServer.builder("stun:stun.gmx.net:3478");
        iceServers.add(stunIceServer16.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer17 = PeerConnection.IceServer.builder("stun:stun.callwithus.com:3478");
        iceServers.add(stunIceServer17.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer18 = PeerConnection.IceServer.builder("stun:stun.counterpath.net:3478");
        iceServers.add(stunIceServer18.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer19 = PeerConnection.IceServer.builder("stun:stun.internetcalls.com:3478");
        iceServers.add(stunIceServer19.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer20 = PeerConnection.IceServer.builder("stun:stun.voip.aebc.com:3478");
        iceServers.add(stunIceServer20.createIceServer());
        PeerConnection.IceServer.Builder stunIceServer21 = PeerConnection.IceServer.builder("stun:numb.viagenie.ca:3478");
        iceServers.add(stunIceServer21.createIceServer());

        PeerConnection.IceServer.Builder stunIceServer2 = PeerConnection.IceServer.builder("stun:47.93.186.97:3478?transport=udp");
        iceServers.add(stunIceServer2.createIceServer());

        PeerConnection.IceServer.Builder turnIceServer1 = PeerConnection.IceServer.builder("turn:47.93.186.97:3478?transport=udp")
                .setUsername("ddssingsong")
                .setPassword("123456");
        iceServers.add(turnIceServer1.createIceServer());

        PeerConnection.IceServer.Builder turnIceServer2 = PeerConnection.IceServer.builder("turn:47.93.186.97:3478?transport=tcp")
                .setUsername("ddssingsong")
                .setPassword("123456");
        iceServers.add(turnIceServer2.createIceServer());

        return iceServers;
    }

    /**
     * 自己是否静音
     *
     * @param audioTrack
     * @param enable
     * @return
     */
    public boolean muteAudio(AudioTrack audioTrack, boolean enable) {
        if (audioTrack != null) {
            audioTrack.setEnabled(false);
            return true;
        }
        return false;
    }

    /**
     * 扬声器和听筒切换
     *
     * @param audioManager
     * @param enable
     * @return
     */
    public boolean toggleSpeaker(AudioManager audioManager, boolean enable) {
        if (audioManager != null) {
            if (enable) {
                audioManager.setSpeakerphoneOn(true);
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                        AudioManager.STREAM_VOICE_CALL);
            } else {
                audioManager.setSpeakerphoneOn(false);
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), AudioManager.STREAM_VOICE_CALL);
            }
            return true;
        }
        return false;
    }

    /**
     * 重置AudioManager 管理器
     * @param audioManager
     */
    public static void audioRelease(AudioManager audioManager) {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
    }


    public void sendIceCandidate(IceCandidate iceCandidate, String to) {
        JSONObject jo = new JSONObject();
//        try {
//            jo.put("type", "candidate");
//            jo.put("label", iceCandidate.sdpMLineIndex);
//            jo.put("id", iceCandidate.sdpMid);
//            jo.put("candidate", iceCandidate.sdp);
//            jo.put("from", socket.id());
//            jo.put("to", to);
//
//            socket.emit("message", jo);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    public void sendSessionDescription(SessionDescription sdp, String to) {
        JSONObject jo = new JSONObject();
//        try {
//            jo.put("type", sdp.type.canonicalForm());
//            jo.put("sdp", sdp.description);
//            jo.put("from", socket.id());
//            jo.put("to", to);
//            socket.emit("message", jo);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }
}
