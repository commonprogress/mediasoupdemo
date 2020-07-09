package org.mediasoup.droid.lib.model;


import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.mediasoup.droid.Producer;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 音视频 屏幕共享状态
 */
public class Producers {

  public static class ProducersWrapper {

    public static final String TYPE_CAM = "cam";
    public static final String TYPE_SHARE = "share";
    private P2PTrack mP2PTrack;
    private Producer mProducer;
    private JSONArray mScore;
    private String mType;

    ProducersWrapper(Producer producer) {
      this.mProducer = producer;
    }

    ProducersWrapper(P2PTrack p2PTrack) {
      this.mP2PTrack = p2PTrack;
    }

    public Producer getProducer() {
      return mProducer;
    }

    public void setProducer(Producer producer) {
      this.mProducer = producer;
    }

    public JSONArray getScore() {
      return mScore;
    }

    public void setScore(JSONArray score) {
      this.mScore = score;
    }

    public String getType() {
      return mType;
    }

    public void setType(String type) {
      mType = type;
    }

    public P2PTrack getP2PTrack() {
      return mP2PTrack;
    }

    public void setP2PTrack(P2PTrack p2PTrack) {
      this.mP2PTrack = p2PTrack;
    }
  }

  private final Map<String, ProducersWrapper> mProducers;

  public Producers() {
    mProducers = new ConcurrentHashMap<>();
  }

  public void addProducer(Producer producer) {
    mProducers.put(producer.getId(), new ProducersWrapper(producer));
  }

  public void addP2PTrack(P2PTrack p2PTrack) {
    mProducers.put(p2PTrack.getPeerId(), new ProducersWrapper(p2PTrack));
  }

  public void addP2PAudioTrack(String peerId, AudioTrack audioTrack) {
    ProducersWrapper wrapper = mProducers.get(peerId);
    if (wrapper == null || null == wrapper.mP2PTrack) {
      addP2PTrack(new P2PTrack(peerId));
    }
    P2PTrack p2PTrack = wrapper.mP2PTrack;
    p2PTrack.setAudioTrack(audioTrack);
    wrapper.setP2PTrack(p2PTrack);
  }

  public void addP2PVideoTrack(String peerId, VideoTrack videoTrack) {
    ProducersWrapper wrapper = mProducers.get(peerId);
    if (wrapper == null || null == wrapper.mP2PTrack) {
      addP2PTrack(new P2PTrack(peerId));
    }
    P2PTrack p2PTrack = wrapper.mP2PTrack;
    p2PTrack.setVideoTrack(videoTrack);
    wrapper.setP2PTrack(p2PTrack);
  }

  public void removeProducer(String producerId) {
    mProducers.remove(producerId);
  }

  public void setProducerPaused(String producerId) {
    ProducersWrapper wrapper = mProducers.get(producerId);
    if (wrapper == null) {
      return;
    }
    wrapper.mProducer.pause();
  }

  public void setProducerResumed(String producerId) {
    ProducersWrapper wrapper = mProducers.get(producerId);
    if (wrapper == null) {
      return;
    }
    wrapper.mProducer.resume();
  }

  public void setProducerType(String producerId, String type) {
    ProducersWrapper wrapper = mProducers.get(producerId);
    if (wrapper == null) {
      return;
    }
    wrapper.mType = type;
  }

  public void setProducerScore(String producerId, JSONArray score) {
    ProducersWrapper wrapper = mProducers.get(producerId);
    if (wrapper == null) {
      return;
    }
    wrapper.mScore = score;
  }

  public ProducersWrapper filter(@NonNull String kind) {
    for (ProducersWrapper wrapper : mProducers.values()) {
      if (wrapper.mProducer == null) {
        continue;
      }
      if (wrapper.mProducer.getTrack() == null) {
        continue;
      }
      if (kind.equals(wrapper.mProducer.getTrack().kind())) {
        return wrapper;
      }
    }
    return null;
  }

  public ProducersWrapper getP2PTrack() {
    for (ProducersWrapper wrapper : mProducers.values()) {
      if (wrapper.mP2PTrack == null) {
        continue;
      }
      return wrapper;
    }
    return null;
  }

  public void clear() {
    mProducers.clear();
  }
}
