package org.mediasoup.droid.lib.model;


import android.support.annotation.NonNull;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * 连接的用户信息
 */
@SuppressWarnings("WeakerAccess")
public class Peer extends Info {

  private String mId;//PeerId 类似于userid
  private String mDisplayName;//用户名
  private DeviceInfo mDevice;//设备信息

  private Set<String> consumers;//消费状态集合

  public Peer(@NonNull JSONObject info) {
    mId = info.optString("id");
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
    consumers = new HashSet<>();
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

  /**
   *
   * @return
   */
  public Set<String> getConsumers() {
    return consumers;
  }
}
