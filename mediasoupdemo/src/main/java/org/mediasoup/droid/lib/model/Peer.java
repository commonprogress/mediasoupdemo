package org.mediasoup.droid.lib.model;


import android.support.annotation.NonNull;

import org.json.JSONObject;
import org.mediasoup.droid.lib.Utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 连接的用户信息
 */
@SuppressWarnings("WeakerAccess")
public class Peer extends Info {

    private String mId;//PeerId 类似于userid
    private String mClientId;
    private String mDisplayName;//用户名
    private DeviceInfo mDevice;//设备信息

    private boolean mVideoVisible;
    private boolean mAudioEnabled;

    private Set<String> consumers;//消费状态集合

    public Peer(@NonNull JSONObject info) {
        mId = info.optString("id");
        mClientId = Utils.getRandomString(16);
        mDisplayName = info.optString("displayName");
        JSONObject deviceInfo = info.optJSONObject("device");
        if (deviceInfo != null) {
            mDevice =
                new DeviceInfo()
                    .setFlag(deviceInfo.optString("flag"))
                    .setName(deviceInfo.optString("name"))
                    .setVersion(deviceInfo.optString("version"));
        } else {
            mDevice = DeviceInfo.unknownDevice();
        }
        consumers = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public String getId() {
        return mId;
    }

    @Override
    public String getDisplayName() {
        return mDisplayName;
    }

    @Override
    public DeviceInfo getDevice() {
        return mDevice;
    }

    public void setDisplayName(String displayName) {
        this.mDisplayName = displayName;
    }

    public void setDevice(DeviceInfo device) {
        this.mDevice = device;
    }

    @Override
    public String getClientId() {
        return mClientId;
    }

    public boolean isVideoVisible() {
        return mVideoVisible;
    }

    public void setVideoVisible(boolean videoVisible) {
        this.mVideoVisible = videoVisible;
    }

    public boolean isAudioEnabled() {
        return mAudioEnabled;
    }

    public void setAudioEnabled(boolean audioEnabled) {
        this.mAudioEnabled = audioEnabled;
    }

    /**
     * @return
     */
    public Set<String> getConsumers() {
        return consumers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Peer peer = (Peer) o;
        return Objects.equals(mId, peer.mId);
    }
}
