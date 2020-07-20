package org.mediasoup.droid.lib.model;

import org.json.JSONArray;
import org.mediasoup.droid.Consumer;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消费状态（连接状态相关）
 */
public class Consumers {

  public static class ConsumerWrapper {

    private String mType;
    private boolean mLocallyPaused;
    private boolean mRemotelyPaused;
    private int mSpatialLayer;
    private int mTemporalLayer;
    private Consumer mConsumer;
    private JSONArray mScore;
    private int mPreferredSpatialLayer;
    private int mPreferredTemporalLayer;
    private P2PTrack mP2PTrack;

    ConsumerWrapper(String type, boolean remotelyPaused, Consumer consumer) {
      mType = type;
      mLocallyPaused = false;
      mRemotelyPaused = remotelyPaused;
      mSpatialLayer = -1;
      mTemporalLayer = -1;
      mConsumer = consumer;
      mPreferredSpatialLayer = -1;
      mPreferredTemporalLayer = -1;
    }

    public ConsumerWrapper(P2PTrack p2PTrack) {
      this.mP2PTrack = p2PTrack;
    }

    public String getType() {
      return mType;
    }

    public boolean isLocallyPaused() {
      return mLocallyPaused;
    }

    public boolean isRemotelyPaused() {
      return mRemotelyPaused;
    }

    public int getSpatialLayer() {
      return mSpatialLayer;
    }

    public int getTemporalLayer() {
      return mTemporalLayer;
    }

    public Consumer getConsumer() {
      return mConsumer;
    }

    public JSONArray getScore() {
      return mScore;
    }

    public int getPreferredSpatialLayer() {
      return mPreferredSpatialLayer;
    }

    public int getPreferredTemporalLayer() {
      return mPreferredTemporalLayer;
    }

      public P2PTrack getP2PTrack() {
          return mP2PTrack;
      }

      public void setP2PTrack(P2PTrack p2PTrack) {
          this.mP2PTrack = p2PTrack;
      }
  }

  private final Map<String, ConsumerWrapper> consumers;

  public Consumers() {
    consumers = new ConcurrentHashMap<>();
  }

  public void addConsumer(String type, Consumer consumer, boolean remotelyPaused) {
    consumers.put(consumer.getId(), new ConsumerWrapper(type, remotelyPaused, consumer));
  }

    public void addP2PTrack(P2PTrack p2PTrack) {
        consumers.put(p2PTrack.getPeerId(), new ConsumerWrapper(p2PTrack));
    }

    public void addP2PAudioTrack(String peerId, AudioTrack audioTrack) {
        ConsumerWrapper wrapper = consumers.get(peerId);
        if (wrapper == null || null == wrapper.mP2PTrack) {
            addP2PTrack(new P2PTrack(peerId));
        }
        ConsumerWrapper wrapper1 = consumers.get(peerId);
        P2PTrack p2PTrack = wrapper1.mP2PTrack;
        p2PTrack.setAudioTrack(audioTrack);
        wrapper1.setP2PTrack(p2PTrack);
    }

    public void addP2PVideoTrack(String peerId, VideoTrack videoTrack) {
        ConsumerWrapper wrapper = consumers.get(peerId);
        if (wrapper == null || null == wrapper.mP2PTrack) {
            addP2PTrack(new P2PTrack(peerId));
        }
        ConsumerWrapper wrapper1 = consumers.get(peerId);
        P2PTrack p2PTrack = wrapper1.mP2PTrack;
        p2PTrack.setVideoTrack(videoTrack);
        wrapper1.setP2PTrack(p2PTrack);
    }

  public void removeConsumer(String consumerId) {
    consumers.remove(consumerId);
  }

  public void setConsumerPaused(String consumerId, String originator) {
    ConsumerWrapper wrapper = consumers.get(consumerId);
    if (wrapper == null) {
      return;
    }

    if ("local".equals(originator)) {
      wrapper.mLocallyPaused = true;
    } else {
      wrapper.mRemotelyPaused = true;
    }
  }

  public void setConsumerResumed(String consumerId, String originator) {
    ConsumerWrapper wrapper = consumers.get(consumerId);
    if (wrapper == null) {
      return;
    }

    if ("local".equals(originator)) {
      wrapper.mLocallyPaused = false;
    } else {
      wrapper.mRemotelyPaused = false;
    }
  }

  public void setConsumerCurrentLayers(String consumerId, int spatialLayer, int temporalLayer) {
    ConsumerWrapper wrapper = consumers.get(consumerId);
    if (wrapper == null) {
      return;
    }
    wrapper.mSpatialLayer = spatialLayer;
    wrapper.mTemporalLayer = temporalLayer;
  }

  public void setConsumerType(String consumerId, String type) {
    ConsumerWrapper wrapper = consumers.get(consumerId);
    if (wrapper == null) {
      return;
    }
    wrapper.mType = type;
  }

  public void setConsumerScore(String consumerId, JSONArray score) {
    ConsumerWrapper wrapper = consumers.get(consumerId);
    if (wrapper == null) {
      return;
    }

    wrapper.mScore = score;
  }

  public ConsumerWrapper getConsumer(String consumerId) {
    return consumers.get(consumerId);
  }

  public void clear() {
    consumers.clear();
  }
}
