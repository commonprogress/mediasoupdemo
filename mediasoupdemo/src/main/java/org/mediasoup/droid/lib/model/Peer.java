package org.mediasoup.droid.lib.model;


import androidx.annotation.NonNull;

import org.json.JSONObject;
import org.mediasoup.droid.lib.RoomConstant;
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

    public static final String KEY_PEER_ID = "id";
    public static final String KEY_PEER_CLIENTID = "clientId";
    public static final String KEY_PEER_NAME = "displayName";
    public static final String KEY_PEER_P2PMODE = "isP2PMode";
    public static final String KEY_PEER_DEVICE = "device";

    private String mId;//PeerId 类似于userid
    private String mClientId;
    private String mDisplayName;//用户名
    private DeviceInfo mDevice;//设备信息
    private boolean isP2PMode;
    private boolean mVideoVisible;
    private boolean mAudioEnabled;
    private RoomConstant.PeerState mPeerState = RoomConstant.PeerState.NEW;//当前peer状态
    private Set<String> consumers;//消费状态集合

    public Peer() {
        mClientId = Utils.getRandomString(16);
        mDevice = DeviceInfo.unknownDevice();
        consumers = Collections.synchronizedSet(new HashSet<>());
    }

    public Peer(String peerId, String peerName) {
        mId = peerId;
        mClientId = Utils.getRandomString(16);
        mDisplayName = peerName;
        mDevice = DeviceInfo.unknownDevice();
        consumers = Collections.synchronizedSet(new HashSet<>());
    }

    public Peer(@NonNull JSONObject info) {
        mId = info.optString(KEY_PEER_ID);
        mClientId = Utils.getRandomString(16);
        mDisplayName = info.optString(KEY_PEER_NAME);
        isP2PMode = info.optBoolean(KEY_PEER_P2PMODE, false);
        JSONObject deviceInfo = info.optJSONObject(KEY_PEER_DEVICE);
        if (deviceInfo != null) {
            mDevice =
                new DeviceInfo()
                    .setFlag(deviceInfo.optString(DeviceInfo.KEY_DEVICE_FLAG))
                    .setName(deviceInfo.optString(DeviceInfo.KEY_DEVICE_NAME))
                    .setVersion(deviceInfo.optString(DeviceInfo.KEY_DEVICE_VERSION));
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
    public boolean isP2PMode() {
        return isP2PMode;
    }

    public void setP2PMode(boolean p2PMode) {
        isP2PMode = p2PMode;
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

    public RoomConstant.PeerState getPeerState() {
        return mPeerState;
    }

    public void setPeerState(RoomConstant.PeerState peerState) {
        this.mPeerState = peerState;
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
