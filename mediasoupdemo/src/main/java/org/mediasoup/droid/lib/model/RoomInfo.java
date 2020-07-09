package org.mediasoup.droid.lib.model;

import org.mediasoup.droid.lib.RoomConstant;

/**
 * 房间实体信息
 */
public class RoomInfo {

  private String mUrl;//房间url
  private String mRoomId;//房间id
  private RoomConstant.ConnectionState mConnectionState = RoomConstant.ConnectionState.NEW;//连接状态
  private String mActiveSpeakerId;//
  private String mStatsPeerId;//
  private boolean mFaceDetection = false;
  private boolean isP2PMode;

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

  public RoomConstant.ConnectionState getConnectionState() {
    return mConnectionState;
  }

  public void setConnectionState(RoomConstant.ConnectionState connectionState) {
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

  public boolean isP2PMode() {
    return isP2PMode;
  }

  public void setP2PMode(boolean p2PMode) {
    isP2PMode = p2PMode;
  }
}
