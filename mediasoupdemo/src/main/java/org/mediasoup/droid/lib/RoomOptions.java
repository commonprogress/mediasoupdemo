package org.mediasoup.droid.lib;

import android.support.annotation.NonNull;

import org.mediasoup.droid.lib.model.DeviceInfo;

/**
 * 房间的配置信息
 */
public class RoomOptions {

    // Device info.
    @NonNull
    private DeviceInfo mDevice = DeviceInfo.androidDevice();
    // Whether we want to force RTC over TCP. 是否强制tcp 否则rtc
    private boolean mForceTcp = false;
    // Whether we want to produce audio/video.
    private boolean mProduce = true;
    // Whether we should consume.
    private boolean mConsume = true;
    // Whether we want DataChannels.
    private boolean mUseDataChannel;

    //是否立即启用摄像头
    private boolean mEnableVideo;
    //是否立即启用麦克风
    private boolean mEnableAudio;

    public RoomOptions setDevice(@NonNull DeviceInfo device) {
        this.mDevice = device;
        return this;
    }

    public RoomOptions setForceTcp(boolean forceTcp) {
        this.mForceTcp = forceTcp;
        return this;
    }

    public RoomOptions setProduce(boolean produce) {
        this.mProduce = produce;
        return this;
    }

    public RoomOptions setConsume(boolean consume) {
        this.mConsume = consume;
        return this;
    }

    public RoomOptions setUseDataChannel(boolean useDataChannel) {
        this.mUseDataChannel = useDataChannel;
        return this;
    }

    public RoomOptions setEnableVideo(boolean enableVideo) {
        this.mEnableVideo = enableVideo;
        return this;
    }

    public RoomOptions setEnableAudio(boolean enableAudio) {
        this.mEnableAudio = enableAudio;
        return this;
    }

    @NonNull
    public DeviceInfo getDevice() {
        return mDevice;
    }

    public boolean isForceTcp() {
        return mForceTcp;
    }

    public boolean isProduce() {
        return mProduce;
    }

    public boolean isConsume() {
        return mConsume;
    }

    public boolean isUseDataChannel() {
        return mUseDataChannel;
    }

    public boolean isEnableVideo() {
        return mEnableVideo;
    }

    public boolean isEnableAudio() {
        return mEnableAudio;
    }
}
