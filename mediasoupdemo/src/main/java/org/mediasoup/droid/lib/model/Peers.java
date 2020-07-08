package org.mediasoup.droid.lib.model;


import androidx.annotation.NonNull;
import android.text.TextUtils;

import org.json.JSONObject;
import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 连接用户信息集合
 */
public class Peers {

  private static final String TAG = "Peers";

  /**
   * 连接用户信息集合
   */
  private Map<String, Peer> mPeersInfo;

  public Peers() {
    mPeersInfo = Collections.synchronizedMap(new LinkedHashMap<>());
  }

  /**
   * 添加连接用户
   * @param peerId
   * @param peerInfo
   */
  public void addPeer(String peerId, @NonNull JSONObject peerInfo) {
    mPeersInfo.put(peerId, new Peer(peerInfo));
  }

  /**
   * 移除连接用户
   * @param peerId
   */
  public void removePeer(String peerId) {
    if(isContainsCurPeer(peerId)) {
      mPeersInfo.remove(peerId);
    }
  }

  /**
   * 设置用户名
   * @param peerId
   * @param displayName
   */
  public void setPeerDisplayName(String peerId, String displayName) {
    Peer peer = getPeer(peerId);
    if (peer == null) {
      Logger.e(TAG, "setPeerDisplayName no Protoo found");
      return;
    }
    peer.setDisplayName(displayName);
  }

  /**
   * 更新当前peer 视频音频状态
   * @param peerId
   * @param isVideoVisible
   * @param isAudioEnabled
   */
  public void updatePeerVideoAudioState(String peerId, boolean isVideoVisible, boolean isAudioEnabled) {
    Peer peer = getPeer(peerId);
    if (peer == null) {
      Logger.e(TAG, "updatePeerVideoAudioState no Peer found for update Video Audio state");
      return;
    }
    peer.setVideoVisible(isVideoVisible);
    peer.setAudioEnabled(isAudioEnabled);
  }

  /**
   * 添加 其他用户相关的 consumer
   * @param peerId
   * @param consumer
   */
  public void addConsumer(String peerId, Consumer consumer) {
    Peer peer = getPeer(peerId);
    if (peer == null) {
      Logger.e(TAG, "addConsumer no Peer found for new Consumer");
      return;
    }
    peer.getConsumers().add(consumer.getId());
  }

  /**
   * 移除 其他用户相关的 consumer
   * @param peerId
   * @param consumerId
   */
  public void removeConsumer(String peerId, String consumerId) {
    Peer peer = getPeer(peerId);
    if (peer == null) {
      Logger.e(TAG, "removeConsumer no Peer found for new Consumer");
      return;
    }
    peer.getConsumers().remove(consumerId);
  }

  /**
   * 根据peerId 获取连接用户
   * @param peerId userid
   * @return
   */
  public Peer getPeer(String peerId) {
    return isContainsCurPeer(peerId) ? mPeersInfo.get(peerId) : null;
  }

  /**
   * 是否包含peerId
   * @param peerId
   * @return
   */
  public boolean isContainsCurPeer(String peerId) {
    return !TextUtils.isEmpty(peerId) && mPeersInfo.containsKey(peerId);
  }

  /**
   * 获取连接用户的集合
   * @return
   */
  public List<Peer> getAllPeers() {
    List<Peer> peers = new ArrayList<>();
    for (Map.Entry<String, Peer> info : mPeersInfo.entrySet()) {
      peers.add(info.getValue());
    }
    return peers;
  }

  /**
   * 清空连接用户信息
   */
  public void clear() {
    mPeersInfo.clear();
  }
}
