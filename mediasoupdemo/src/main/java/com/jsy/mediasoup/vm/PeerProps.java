package com.jsy.mediasoup.vm;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.databinding.ObservableField;
import android.support.annotation.NonNull;
import android.databinding.BaseObservable;
import android.databinding.Observable;

import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Consumers;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.model.Peers;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

import java.util.Set;

/**
 * 连接用户的配置信息操作类
 */
public class PeerProps extends PeerViewProps {

  private static final String TAG = "PeerProps";

  private final ObservableField<Boolean> mAudioEnabled;
  private final ObservableField<Boolean> mVideoVisible;
  private final StateComposer mStateComposer;

  public PeerProps(@NonNull Application application, @NonNull RoomStore roomStore) {
    super(application, roomStore);
    setMe(false);
    mAudioEnabled = new ObservableField<>();
    mVideoVisible = new ObservableField<>();
    mStateComposer = new StateComposer();
    mStateComposer.addOnPropertyChangedCallback(
        new Observable.OnPropertyChangedCallback() {
          @Override
          public void onPropertyChanged(Observable sender, int propertyId) {
            Consumers.ConsumerWrapper audioCW = mStateComposer.getConsumer("audio");
            Consumers.ConsumerWrapper videoCW = mStateComposer.getConsumer("video");
            Consumer audioConsumer = audioCW != null ? audioCW.getConsumer() : null;
            Consumer videoConsumer = videoCW != null ? videoCW.getConsumer() : null;

            mPeer.set(mStateComposer.mPeer);
            mAudioProducerId.set(audioConsumer != null ? audioConsumer.getId() : null);
            mVideoProducerId.set(videoConsumer != null ? videoConsumer.getId() : null);
            mAudioRtpParameters.set(
                audioConsumer != null ? audioConsumer.getRtpParameters() : null);
            mVideoRtpParameters.set(
                videoConsumer != null ? videoConsumer.getRtpParameters() : null);
            mAudioTrack.set(audioConsumer != null ? (AudioTrack) audioConsumer.getTrack() : null);
            mVideoTrack.set(videoConsumer != null ? (VideoTrack) videoConsumer.getTrack() : null);
            // TODO(HaiyangWu) : support codec property
            // mAudioCodec.set(videoConsumer != null ? videoConsumer.getCodec() : null);
            // mVideoCodec.set(videoConsumer != null ? videoConsumer.getCodec() : null);
            mAudioScore.set(audioCW != null ? audioCW.getScore() : null);
            mVideoScore.set(videoCW != null ? videoCW.getScore() : null);

            mAudioEnabled.set(
                audioCW != null && !audioCW.isLocallyPaused() && !audioCW.isRemotelyPaused());
            mVideoVisible.set(
                videoCW != null && !videoCW.isLocallyPaused() && !videoCW.isRemotelyPaused());
          }
        });
  }

  /**
   * 音频是否可用
   * @return
   */
  public ObservableField<Boolean> getAudioEnabled() {
    return mAudioEnabled;
  }

  /**
   * 视屏是否可见
   * @return
   */
  @Override
  public ObservableField<Boolean> getVideoVisible() {
    return mVideoVisible;
  }

  public void connect(LifecycleOwner owner, @NonNull String peerId) {
    getRoomStore().getMe().observe(owner, me -> mAudioMuted.set(me.isAudioMuted()));
    getRoomStore()
        .getRoomInfo()
        .observe(owner, roomInfo -> mFaceDetection.set(roomInfo.isFaceDetection()));
    mStateComposer.connect(owner, getRoomStore(), peerId);
  }

  @Override
  public void connect(LifecycleOwner lifecycleOwner) {
    throw new IllegalAccessError("use connect with peer Id");
  }

  public static class StateComposer extends BaseObservable {

    private String mPeerId;
    private Peer mPeer;
    private Consumers mConsumers;
    private Observer<Peers> mPeersObservable =
        peers -> {
          mPeer = peers.getPeer(mPeerId);
          Logger.w(
              TAG,
              "onChanged() id: "
                  + mPeerId
                  + ", name:"
                  + (mPeer != null ? mPeer.getDisplayName() : ""));
          // TODO(HaiyangWu): check whether need notify change.
          notifyChange();
        };

    private Observer<Consumers> mConsumersObserver =
        consumers -> {
          mConsumers = consumers;
          // TODO(HaiyangWu): check whether need notify change.
          notifyChange();
        };

    void connect(@NonNull LifecycleOwner owner, RoomStore store, String peerId) {
      mPeerId = peerId;
      store.getPeers().removeObserver(mPeersObservable);
      store.getPeers().observe(owner, mPeersObservable);

      store.getConsumers().removeObserver(mConsumersObserver);
      store.getConsumers().observe(owner, mConsumersObserver);
    }

    Consumers.ConsumerWrapper getConsumer(String kind) {
      if (mPeer == null || mConsumers == null) {
        return null;
      }

      Set<String> consumerIds = mPeer.getConsumers();
      for (String consumerId : consumerIds) {
        Consumers.ConsumerWrapper wp = mConsumers.getConsumer(consumerId);
        if (wp == null || wp.getConsumer() == null) {
          continue;
        }
        if (kind.equals(wp.getConsumer().getKind())) {
          return wp;
        }
      }
      return null;
    }
  }
}
