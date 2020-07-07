package com.waz.media.manager;

import com.waz.media.manager.player.MediaPlayer;

public class MediamgrPlay {
    private final String mediaName;
    private final MediaPlayer player;
    private final boolean mixing;
    private final boolean incall;
    private final int intensity;

    public MediamgrPlay(String mediaName, MediaPlayer player, boolean mixing, boolean incall, int intensity) {
        this.mediaName = mediaName;
        this.player = player;
        this.mixing = mixing;
        this.incall = incall;
        this.intensity = intensity;
    }

    public void playMedia(String name) {
        if (null == player || player.getIsPlaying()) {
            return;
        }
        player.play(true);
    }

    public void stopMedia(String name) {
        if (null == player || !player.getIsPlaying()) {
            return;
        }
        player.stop();
    }

    public void unregisterMedia() {
        if (null == player || !player.getIsPlaying()) {
            return;
        }
        player.stop();
    }

    public String getMediaName() {
        return mediaName;
    }

    public MediaPlayer getPlayer() {
        return player;
    }

    public boolean isMixing() {
        return mixing;
    }

    public boolean isIncall() {
        return incall;
    }

    public int getIntensity() {
        return intensity;
    }
}
