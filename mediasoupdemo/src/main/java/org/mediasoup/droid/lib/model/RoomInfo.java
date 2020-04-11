package org.mediasoup.droid.lib.model;

import static org.mediasoup.droid.lib.RoomClient.ConnectionState;
import static org.mediasoup.droid.lib.RoomClient.ConnectionState.NEW;

/**
 * 房间实体信息
 */
public class RoomInfo {

  private String mUrl;//房间url
  private String mRoomId;//房间id
  private ConnectionState mConnectionState = NEW;//连接状态
  private String mActiveSpeakerId;//
  private String mStatsPeerId;//
  private boolean mFaceDetection = false;

  public String getUrl() {
    return mUrl;
  }

  public void setUrl(String url) {
    this.mUrl = url;
  }

  public String getRoomId() {
    return mRoomId;
  }

  public void setRoomId(String roomId) {
    this.mRoomId = roomId;
  }

  public ConnectionState getConnectionState() {
    return mConnectionState;
  }

  public void setConnectionState(ConnectionState connectionState) {
    this.mConnectionState = connectionState;
  }

  public String getActiveSpeakerId() {
    return mActiveSpeakerId;
  }

  public void setActiveSpeakerId(String activeSpeakerId) {
    this.mActiveSpeakerId = activeSpeakerId;
  }

  public String getStatsPeerId() {
    return mStatsPeerId;
  }

  public void setStatsPeerId(String statsPeerId) {
    this.mStatsPeerId = statsPeerId;
  }

  public boolean isFaceDetection() {
    return mFaceDetection;
  }

  public void setFaceDetection(boolean faceDetection) {
    this.mFaceDetection = faceDetection;
  }
}
