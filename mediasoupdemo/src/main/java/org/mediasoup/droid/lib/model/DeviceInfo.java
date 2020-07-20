package org.mediasoup.droid.lib.model;

import android.os.Build;

import org.json.JSONObject;

import static org.mediasoup.droid.lib.JsonUtils.jsonPut;

/**
 * 设备信息
 */
@SuppressWarnings("WeakerAccess")
public class DeviceInfo {
    public static final String KEY_DEVICE_FLAG = "flag";
    public static final String KEY_DEVICE_NAME = "name";
    public static final String KEY_DEVICE_VERSION = "version";

    private String mFlag;
    private String mName;
    private String mVersion;

    public String getFlag() {
        return mFlag;
    }

    public DeviceInfo setFlag(String flag) {
        this.mFlag = flag;
        return this;
    }

    public String getName() {
        return mName;
    }

    public DeviceInfo setName(String name) {
        this.mName = name;
        return this;
    }

    public String getVersion() {
        return mVersion;
    }

    public DeviceInfo setVersion(String version) {
        this.mVersion = version;
        return this;
    }

    public static DeviceInfo androidDevice() {
        return new DeviceInfo()
            .setFlag("android")
            .setName("Android " + Build.DEVICE)
            .setVersion(Build.VERSION.CODENAME);
    }

    public static DeviceInfo unknownDevice() {
        return new DeviceInfo().setFlag("unknown").setName("unknown").setVersion("unknown");
    }

    public JSONObject toJSONObject() {
        JSONObject deviceInfo = new JSONObject();
        jsonPut(deviceInfo, KEY_DEVICE_FLAG, getFlag());
        jsonPut(deviceInfo, KEY_DEVICE_NAME, getName());
        jsonPut(deviceInfo, KEY_DEVICE_VERSION, getVersion());
        return deviceInfo;
    }
}
