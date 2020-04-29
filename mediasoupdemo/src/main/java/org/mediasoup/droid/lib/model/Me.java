package org.mediasoup.droid.lib.model;

import java.util.Objects;

/**
 * 自己当前信息
 */
public class Me extends Info {

  private String mId;//peerid 类似于userid
  private String mClientId;
  private String mDisplayName;
  private boolean mDisplayNameSet;
  private DeviceInfo mDevice;

  private boolean mCanSendMic;
  private boolean mCanSendCam;
  private boolean mCanChangeCam;

  private boolean mCamInProgress;
  private boolean mShareInProgress;

  private boolean mAudioOnly;
  private boolean mAudioOnlyInProgress;
  private boolean mAudioMuted;
  private boolean mRestartIceInProgress;

  private boolean mSpeakerMute;
    private boolean mFrontCamera = true;

    @Override
  public String getId() {
    return mId;
  }

  public void setId(String id) {
    this.mId = id;
  }

  @Override
  public String getClientId() {
    return mClientId;
  }

  public void setClientId(String clientId) {
    this.mClientId = clientId;
  }

    @Override
  public String getDisplayName() {
    return mDisplayName;
  }

  public void setDisplayName(String displayName) {
    this.mDisplayName = displayName;
  }

  public boolean isDisplayNameSet() {
    return mDisplayNameSet;
  }

  public void setDisplayNameSet(boolean displayNameSet) {
    this.mDisplayNameSet = displayNameSet;
  }

  @Override
  public DeviceInfo getDevice() {
    return mDevice;
  }

  public void setDevice(DeviceInfo device) {
    this.mDevice = device;
  }

  public boolean isCanSendMic() {
    return mCanSendMic;
  }

  public void setCanSendMic(boolean canSendMic) {
    this.mCanSendMic = canSendMic;
  }

  public boolean isCanSendCam() {
    return mCanSendCam;
  }

  public void setCanSendCam(boolean canSendCam) {
    this.mCanSendCam = canSendCam;
  }

  public boolean isCanChangeCam() {
    return mCanChangeCam;
  }

  public void setCanChangeCam(boolean canChangeCam) {
    this.mCanChangeCam = canChangeCam;
  }

  public boolean isCamInProgress() {
    return mCamInProgress;
  }

  public void setCamInProgress(boolean camInProgress) {
    this.mCamInProgress = camInProgress;
  }

  public boolean isShareInProgress() {
    return mShareInProgress;
  }

  public void setShareInProgress(boolean shareInProgress) {
    this.mShareInProgress = shareInProgress;
  }

  public boolean isAudioOnly() {
    return mAudioOnly;
  }

  public void setAudioOnly(boolean audioOnly) {
    this.mAudioOnly = audioOnly;
  }

  public boolean isAudioOnlyInProgress() {
    return mAudioOnlyInProgress;
  }

  public void setAudioOnlyInProgress(boolean audioOnlyInProgress) {
    this.mAudioOnlyInProgress = audioOnlyInProgress;
  }

  public boolean isAudioMuted() {
    return mAudioMuted;
  }

  public void setAudioMuted(boolean audioMuted) {
    this.mAudioMuted = audioMuted;
  }

  public boolean isRestartIceInProgress() {
    return mRestartIceInProgress;
  }

  public void setRestartIceInProgress(boolean restartIceInProgress) {
    this.mRestartIceInProgress = restartIceInProgress;
  }

    public boolean isSpeakerMute() {
        return mSpeakerMute;
    }

    public void setSpeakerMute(boolean speakerMute) {
        this.mSpeakerMute = speakerMute;
    }

    public void setFrontCamera(boolean frontCamera) {
        this.mFrontCamera = frontCamera;
    }

    public boolean isFrontCamera() {
        return mFrontCamera;
    }

    public void clear() {
    mCamInProgress = false;
    mShareInProgress = false;
    mAudioOnly = false;
    mAudioOnlyInProgress = false;
    mAudioMuted = false;
    mRestartIceInProgress = false;
      mSpeakerMute = false;
      mFrontCamera = true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Me me = (Me) o;
    return Objects.equals(mId, me.mId);
  }
}
