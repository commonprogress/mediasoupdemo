package com.jsy.mediasoup.vm;

import android.app.Application;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.databinding.BaseObservable;
import android.databinding.Observable;
import android.databinding.ObservableField;
import android.support.annotation.NonNull;

import com.jsy.mediasoup.utils.LogUtils;

import org.mediasoup.droid.Consumer;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.RoomClient;
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
    private static final String TAG = PeerProps.class.getSimpleName();

    private final ObservableField<Boolean> mAudioEnabled;
    private final ObservableField<Boolean> mVideoVisible;
    private final StateComposer mStateComposer;

    public PeerProps(@NonNull Application application, RoomClient roomClient, @NonNull RoomStore roomStore) {
        super(application, roomStore);
        setMe(false);
        this.mRoomClient = roomClient;
        mAudioEnabled = new ObservableField<>();
        mVideoVisible = new ObservableField<>();
        mStateComposer = new StateComposer();
        mStateComposer.addOnPropertyChangedCallback(
            new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    boolean isConnecting = null == mRoomClient ? true : mRoomClient.isConnecting();
                    if (!isConnecting) {
                        LogUtils.e(TAG, "PeerProps, onPropertyChanged isConnecting=false");
                    }
                    Consumers.ConsumerWrapper audioCW = mStateComposer.getConsumer("audio");
                    Consumers.ConsumerWrapper videoCW = mStateComposer.getConsumer("video");
                    Consumer audioConsumer = !isConnecting ? null : (audioCW != null ? audioCW.getConsumer() : null);
                    Consumer videoConsumer = !isConnecting ? null : (videoCW != null ? videoCW.getConsumer() : null);

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

                    if (null != mPropsLiveDataChange) {
                        mPropsLiveDataChange.onDataChanged(PeerProps.this);
                    }
                }
            });
    }

    /**
     * 音频是否可用
     *
     * @return
     */
    public ObservableField<Boolean> getAudioEnabled() {
        return mAudioEnabled;
    }

    /**
     * 视屏是否可见
     *
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

        synchronized Consumers.ConsumerWrapper getConsumer(String kind) {
            final Consumers consumers = mConsumers;
            final Peer peer = mPeer;
            if (peer == null || consumers == null) {
                return null;
            }
            final Set<String> consumerIds = peer.getConsumers();
            if (null != consumerIds) {
                synchronized (consumerIds) {
                    for (String consumerId : consumerIds) {
                        Consumers.ConsumerWrapper wp = consumers.getConsumer(consumerId);
                        if (wp == null || wp.getConsumer() == null) {
                            continue;
                        }
                        if (kind.equals(wp.getConsumer().getKind())) {
                            return wp;
                        }
                    }
                }
            } else {
                LogUtils.i(TAG, "StateComposer getConsumer,null == consumerIds");
            }
            return null;
        }
    }
}
