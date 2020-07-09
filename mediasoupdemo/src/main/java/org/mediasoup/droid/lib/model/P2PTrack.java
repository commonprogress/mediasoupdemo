package org.mediasoup.droid.lib.model;

import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

public class P2PTrack {
    private String peerId;
    private AudioTrack audioTrack;
    private VideoTrack videoTrack;

    public P2PTrack(String peerId) {
        this.peerId = peerId;
    }

    public String getPeerId() {
        return peerId;
    }

    public void setPeerId(String peerId) {
        this.peerId = peerId;
    }

    public AudioTrack getAudioTrack() {
        return audioTrack;
    }

    public void setAudioTrack(AudioTrack audioTrack) {
        this.audioTrack = audioTrack;
    }

    public VideoTrack getVideoTrack() {
        return videoTrack;
    }

    public void setVideoTrack(VideoTrack videoTrack) {
        this.videoTrack = videoTrack;
    }
}
