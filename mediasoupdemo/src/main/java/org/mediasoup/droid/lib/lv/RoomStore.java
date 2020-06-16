package org.mediasoup.droid.lib.lv;

import android.arch.lifecycle.MutableLiveData;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.Producer;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.model.Consumers;
import org.mediasoup.droid.lib.model.DeviceInfo;
import org.mediasoup.droid.lib.model.Me;
import org.mediasoup.droid.lib.model.Notify;
import org.mediasoup.droid.lib.model.Peers;
import org.mediasoup.droid.lib.model.Producers;
import org.mediasoup.droid.lib.model.RoomInfo;

/**
 * Room state.
 * 房间数据状态操作刷新相关
 * <p>Just like mediasoup-demo/app/lib/redux/stateActions.js
 */
@SuppressWarnings("unused")
public class RoomStore {

  private static final String TAG = "RoomStore";

  // room
  // mediasoup-demo/app/lib/redux/reducers/room.js
  private SupplierMutableLiveData<RoomInfo> roomInfo = new SupplierMutableLiveData<>(RoomInfo::new);

  // me
  // mediasoup-demo/app/lib/redux/reducers/me.js
  private SupplierMutableLiveData<Me> me = new SupplierMutableLiveData<>(Me::new);

  // producers
  // mediasoup-demo/app/lib/redux/reducers/producers.js
  private SupplierMutableLiveData<Producers> producers =
      new SupplierMutableLiveData<>(Producers::new);

  // peers
  // mediasoup-demo/app/lib/redux/reducers/peer.js
  private SupplierMutableLiveData<Peers> peers = new SupplierMutableLiveData<>(Peers::new);

  // consumers
  // mediasoup-demo/app/lib/redux/reducers/consumers.js
  private SupplierMutableLiveData<Consumers> consumers =
      new SupplierMutableLiveData<>(Consumers::new);

  // notify
  // mediasoup-demo/app/lib/redux/reducers/notifications.js
  private MutableLiveData<Notify> notify = new MutableLiveData<>();

  /**
   * 设置房间信息
   * @param roomId
   * @param url
   */
  public void setRoomUrl(String roomId, String url) {
    roomInfo.postValue(
        roomInfo -> {
          roomInfo.setRoomId(roomId);
          roomInfo.setUrl(url);
        });
  }

  /**
   * 设置房间状态
   * @param state
   */
  public void setRoomState(RoomClient.ConnectionState state) {
    roomInfo.postValue(roomInfo -> roomInfo.setConnectionState(state));

    if (RoomClient.ConnectionState.CLOSED.equals(state)) {
      Logger.e(TAG, "setRoomState RoomClient.ConnectionState.CLOSED");
      peers.postValue(Peers::clear);
      me.postValue(Me::clear);
      producers.postValue(Producers::clear);
      consumers.postValue(Consumers::clear);
    }
  }

//  public RoomClient.ConnectionState getRoomState(){
//
//  }

    /**
     * 设置有声音room？
     * @param peerId
     */
  public void setRoomActiveSpeaker(String peerId) {
    roomInfo.postValue(roomInfo -> roomInfo.setActiveSpeakerId(peerId));
  }

  public void setRoomStatsPeerId(String peerId) {
    roomInfo.postValue(roomInfo -> roomInfo.setStatsPeerId(peerId));
  }

  public void setRoomFaceDetection(boolean enable) {
    roomInfo.postValue(roomInfo -> roomInfo.setFaceDetection(enable));
  }

  public void setMe(String peerId, String displayName, DeviceInfo device) {
    me.postValue(
        me -> {
          me.setId(peerId);
          me.setDisplayName(displayName);
          me.setDevice(device);
        });
  }

    /**
     * 设置摄像头和麦克风状态
     * @param canSendMic
     * @param canSendCam
     */
  public void setMediaCapabilities(boolean canSendMic, boolean canSendCam) {
    me.postValue(
        me -> {
          me.setCanSendMic(canSendMic);
          me.setCanSendCam(canSendCam);
        });
  }

  public void setCanChangeCam(boolean canChangeCam) {
    me.postValue(me -> me.setCanSendCam(canChangeCam));
  }

    /**
     * 改变显示的名字
     * @param displayName
     */
  public void setDisplayName(String displayName) {
    me.postValue(me -> me.setDisplayName(displayName));
  }

    /**
     * 只有音频
     * @param enabled
     */
  public void setAudioOnlyState(boolean enabled) {
    me.postValue(me -> me.setAudioOnly(enabled));
  }

    /**
     * 设置只有音频 进行中
     * @param enabled 进行中
     */
  public void setAudioOnlyInProgress(boolean enabled) {
    me.postValue(me -> me.setAudioOnlyInProgress(enabled));
  }

    /**
     * 设置静音状态
     * @param enabled
     */
  public void setAudioMutedState(boolean enabled) {
    me.postValue(me -> me.setAudioMuted(enabled));
  }

  /**
   * 重新启动ice 进度
   * @param restartIceInProgress 是否进行中
   */
  public void setRestartIceInProgress(boolean restartIceInProgress) {
    me.postValue(me -> me.setRestartIceInProgress(restartIceInProgress));
  }

    /**
     * 设置听筒和扬声器切换
     * @param enabled
     */
    public void setSpeakerMuteState(boolean enabled) {
        me.postValue(me -> me.setSpeakerMute(enabled));
    }

  /**
   * 启用摄像头 进度
   * @param inProgress 是否进行中
   */
  public void setCamInProgress(boolean inProgress) {
    me.postValue(me -> me.setCamInProgress(inProgress));
  }

  /**
   * 开启共享屏幕进度
   * @param inProgress
   */
  public void setShareInProgress(boolean inProgress) {
    me.postValue(me -> me.setShareInProgress(inProgress));
  }

    /**
     * 切换摄像头完成
     * @param isFrontCamera 是否前置
     */
    public void cameraSwitchDone(boolean isFrontCamera) {
        me.postValue(me -> me.setFrontCamera(isFrontCamera));
    }

  public void addProducer(Producer producer) {
    producers.postValue(producers -> producers.addProducer(producer));
  }

  public void setProducerPaused(String producerId) {
    producers.postValue(producers -> producers.setProducerPaused(producerId));
  }

  public void setProducerResumed(String producerId) {
    producers.postValue(producers -> producers.setProducerResumed(producerId));
  }

  public void removeProducer(String producerId) {
    producers.postValue(producers -> producers.removeProducer(producerId));
  }

  public void setProducerType(String producerId, String type) {
    producers.postValue(producers -> producers.setProducerType(producerId, type));
  }

  public void setProducerScore(String producerId, JSONArray score) {
    producers.postValue(producers -> producers.setProducerScore(producerId, score));
  }

  public void addDataProducer(Object dataProducer) {
    // TODO(HaiyangWU): support data consumer. Note, new DataConsumer.java
  }

  public void removeDataProducer(String dataProducerId) {
    // TODO(HaiyangWU): support data consumer.
  }

  /**
   * 添加连接用户
   * @param peerId
   * @param peerInfo
   */
  public void addPeer(String peerId, JSONObject peerInfo) {
    peers.postValue(peersInfo -> peersInfo.addPeer(peerId, peerInfo));
  }

  /**
   * 设置用户名
   * @param peerId
   * @param displayName
   */
  public void setPeerDisplayName(String peerId, String displayName) {
    peers.postValue(peersInfo -> peersInfo.setPeerDisplayName(peerId, displayName));
  }

  /**
   * 更新当前peer 视频音频状态
   * @param peerId
   * @param isVideoVisible
   * @param isAudioEnabled
   */
  public void updatePeerVideoAudioState(String peerId, boolean isVideoVisible, boolean isAudioEnabled) {
      peers.postValue(peersInfo -> peersInfo.updatePeerVideoAudioState(peerId, isVideoVisible, isAudioEnabled));
  }

  /**
   * 移除连接用户
   * @param peerId
   */
  public void removePeer(String peerId) {
    roomInfo.postValue(
        roomInfo -> {
          if (!TextUtils.isEmpty(peerId) && peerId.equals(roomInfo.getActiveSpeakerId())) {
            roomInfo.setActiveSpeakerId(null);
          }
          if (!TextUtils.isEmpty(peerId) && peerId.equals(roomInfo.getStatsPeerId())) {
            roomInfo.setStatsPeerId(null);
          }
        });
    peers.postValue(peersInfo -> peersInfo.removePeer(peerId));
  }

  /**
   * 添加 其他用户相关的 consumer
   * @param peerId
   * @param type
   * @param consumer
   * @param remotelyPaused
   */
  public void addConsumer(String peerId, String type, Consumer consumer, boolean remotelyPaused) {
    consumers.postValue(consumers -> consumers.addConsumer(type, consumer, remotelyPaused));
    peers.postValue(peers -> peers.addConsumer(peerId, consumer));
  }

  /**
   * 移除 其他用户相关的 consumer
   * @param peerId
   * @param consumerId
   */
  public void removeConsumer(String peerId, String consumerId) {
    consumers.postValue(consumers -> consumers.removeConsumer(consumerId));
    peers.postValue(peers -> peers.removeConsumer(peerId, consumerId));
  }

  public void setConsumerPaused(String consumerId, String originator) {
    consumers.postValue(consumers -> consumers.setConsumerPaused(consumerId, originator));
  }

  public void setConsumerResumed(String consumerId, String originator) {
    consumers.postValue(consumers -> consumers.setConsumerResumed(consumerId, originator));
  }

  public void setConsumerCurrentLayers(String consumerId, int spatialLayer, int temporalLayer) {
    consumers.postValue(
        consumers -> consumers.setConsumerCurrentLayers(consumerId, spatialLayer, temporalLayer));
  }

  public void setConsumerType(String consumerId, String type) {
    consumers.postValue(consumers -> consumers.setConsumerType(consumerId, type));
  }

  public void setConsumerScore(String consumerId, JSONArray score) {
    consumers.postValue(consumers -> consumers.setConsumerScore(consumerId, score));
  }

  public void addDataConsumer(String peerId, Object dataConsumer) {
    // TODO(HaiyangWU): support data consumer. Note, new DataConsumer.java
  }

  public void removeDataConsumer(String peerId, String dataConsumerId) {
    // TODO(HaiyangWU): support data consumer.
  }

  public void addNotify(String text) {
    notify.postValue(new Notify("info", text));
  }

  public void addNotify(String text, int timeout) {
    notify.postValue(new Notify("info", text, timeout));
  }

  public void addNotify(String type, String text) {
    notify.postValue(new Notify(type, text));
  }

  public void addNotify(String text, Throwable throwable) {
    notify.postValue(new Notify("error", text + throwable.getMessage()));
  }

  public SupplierMutableLiveData<RoomInfo> getRoomInfo() {
    return roomInfo;
  }

  public SupplierMutableLiveData<Me> getMe() {
    return me;
  }

  public MutableLiveData<Notify> getNotify() {
    return notify;
  }

  public SupplierMutableLiveData<Peers> getPeers() {
    return peers;
  }

  public SupplierMutableLiveData<Producers> getProducers() {
    return producers;
  }

  public SupplierMutableLiveData<Consumers> getConsumers() {
    return consumers;
  }
}
