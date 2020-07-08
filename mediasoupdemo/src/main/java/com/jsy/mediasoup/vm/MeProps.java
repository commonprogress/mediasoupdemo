package com.jsy.mediasoup.vm;

import android.app.Application;
import androidx.lifecycle.LifecycleOwner;
import androidx.databinding.BaseObservable;
import androidx.databinding.Observable;
import androidx.databinding.ObservableField;
import androidx.annotation.NonNull;

import com.jsy.mediasoup.utils.LogUtils;

import org.mediasoup.droid.Producer;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Me;
import org.mediasoup.droid.lib.model.Producers;
import org.webrtc.AudioTrack;
import org.webrtc.VideoTrack;

/**
 * 自己的配置信息操作类
 */
public class MeProps extends PeerViewProps {
    private static final String TAG = MeProps.class.getSimpleName();

    public enum DeviceState {
        UNSUPPORTED,
        ON,
        OFF
    }

    private final ObservableField<Boolean> mConnected;//是否已经连接
    private final ObservableField<Me> mMe;
    private final ObservableField<DeviceState> mMicState;//录音状态，麦克风状态
    private final ObservableField<DeviceState> mCamState;//摄像头状态，视频状态
    private final ObservableField<DeviceState> mChangeCamState;//摄像头状态，视频状态 改变
    // TODO: support screen share
    private final ObservableField<DeviceState> mShareState;//是否支持屏幕共享
    private final StateComposer mStateComposer;//自己的消费状态（连接状态已经改变）

    public MeProps(@NonNull Application application, RoomClient roomClient, @NonNull RoomStore roomStore) {
        super(application, roomStore);
        setMe(true);
        this.mRoomClient = roomClient;
        mConnected = new ObservableField<>(Boolean.FALSE);
        mMe = new ObservableField<>();
        mMicState = new ObservableField<>(DeviceState.UNSUPPORTED);
        mCamState = new ObservableField<>(DeviceState.UNSUPPORTED);
        mChangeCamState = new ObservableField<>(DeviceState.UNSUPPORTED);
        mShareState = new ObservableField<>(DeviceState.UNSUPPORTED);
        mStateComposer = new StateComposer();
        //自己的消费状态（连接状态已经改变）监听
        mStateComposer.addOnPropertyChangedCallback(
            new Observable.OnPropertyChangedCallback() {
                @Override
                public void onPropertyChanged(Observable sender, int propertyId) {
                    boolean isConnecting = null == mRoomClient ? true : mRoomClient.isConnecting();
                    if (!isConnecting) {
                        LogUtils.e(TAG, "MeProps, onPropertyChanged isConnecting=false");
                    }
                    Me me = mStateComposer.mMe;
                    Producers.ProducersWrapper audioPW = mStateComposer.mAudioPW;
                    Producer audioProducer = !isConnecting ? null : (audioPW != null ? audioPW.getProducer() : null);
                    Producers.ProducersWrapper videoPW = mStateComposer.mVideoPW;
                    Producer videoProducer = !isConnecting ? null : (videoPW != null ? videoPW.getProducer() : null);

                    mAudioProducerId.set(audioProducer != null ? audioProducer.getId() : null);
                    mVideoProducerId.set(videoProducer != null ? videoProducer.getId() : null);
                    mAudioRtpParameters.set(
                        audioProducer != null ? audioProducer.getRtpParameters() : null);
                    mVideoRtpParameters.set(
                        videoProducer != null ? videoProducer.getRtpParameters() : null);
                    mAudioTrack.set(audioProducer != null ? (AudioTrack) audioProducer.getTrack() : null);
                    mVideoTrack.set(videoProducer != null ? (VideoTrack) videoProducer.getTrack() : null);
                    // TODO(HaiyangWu) : support codec property
                    // mAudioCodec.set(audioProducer != null ? audioProducer.getCodec() : null);
                    // mVideoCodec.set(videoProducer != null ? videoProducer.getCodec() : null);
                    mAudioScore.set(audioPW != null ? audioPW.getScore() : null);
                    mVideoScore.set(videoPW != null ? videoPW.getScore() : null);

                    LogUtils.i(TAG, "MeProps, onPropertyChanged mAudioProducerId:" + mAudioProducerId.get()
                            + ", mVideoProducerId:" + mVideoProducerId.get()
                            + ", audioPW.getType():" + (audioPW != null ? audioPW.getType() : "null")
                            + ", videoPW.getType():" + (videoPW != null ? videoPW.getType() : "null")
                            + ", \nmAudioScore:" + mAudioScore.get()
                            + ", \nmVideoScore:" + mVideoScore.get()
                            + ", \nmAudioTrack:" + mAudioTrack.get()
                            + ", \nmVideoTrack:" + mVideoTrack.get()
                            + ", \nmAudioRtpParameters:" + mAudioRtpParameters.get() + "\n"
                            + ", \nmVideoRtpParameters:" + mVideoRtpParameters.get() + "\n");

                    DeviceState micState;
                    if (me == null || !me.isCanSendMic()) {
                        micState = DeviceState.UNSUPPORTED;
                    } else if (audioProducer == null) {
                        micState = DeviceState.UNSUPPORTED;
                    } else if (!audioProducer.isPaused()) {
                        micState = DeviceState.ON;
                    } else {
                        micState = DeviceState.OFF;
                    }
                    mMicState.set(micState);

                    DeviceState camState;
                    if (me == null || !me.isCanSendMic()) {
                        camState = DeviceState.UNSUPPORTED;
                    } else if (videoPW != null
                        && !Producers.ProducersWrapper.TYPE_SHARE.equals(videoPW.getType())) {
                        camState = DeviceState.ON;
                    } else {
                        camState = DeviceState.OFF;
                    }
                    mCamState.set(camState);

                    DeviceState changeCamState;
                    if (me == null) {
                        changeCamState = DeviceState.UNSUPPORTED;
                    } else if (videoPW != null
                        && !Producers.ProducersWrapper.TYPE_SHARE.equals(videoPW.getType())
                        && me.isCanChangeCam()) {
                        changeCamState = DeviceState.ON;
                    } else {
                        changeCamState = DeviceState.OFF;
                    }
                    mChangeCamState.set(changeCamState);

                    DeviceState shareState;
                    if (me == null) {
                        shareState = DeviceState.UNSUPPORTED;
                    } else if (videoPW != null
                        && Producers.ProducersWrapper.TYPE_SHARE.equals(videoPW.getType())) {
                        shareState = DeviceState.ON;
                    } else {
                        shareState = DeviceState.OFF;
                    }
                    mShareState.set(shareState);

                    if (null != mPropsLiveDataChange) {
                        mPropsLiveDataChange.onDataChanged(MeProps.this);
                    }
                }
            });
    }

    /**
     * 是否已经连接
     *
     * @return
     */
    public ObservableField<Boolean> getConnected() {
        return mConnected;
    }

    /**
     * 获取自己信息
     *
     * @return
     */
    public ObservableField<Me> getMe() {
        return mMe;
    }

    /**
     * 麦克风状态 是否静音
     *
     * @return
     */
    public ObservableField<DeviceState> getMicState() {
        return mMicState;
    }

    /**
     * 摄像头状态 是否启用
     *
     * @return
     */
    public ObservableField<DeviceState> getCamState() {
        return mCamState;
    }

    /**
     * 屏幕共享状态
     *
     * @return
     */
    public ObservableField<DeviceState> getShareState() {
        return mShareState;
    }

    @Override
    public void connect(LifecycleOwner owner) {
        getRoomStore()
            .getMe()
            .observe(
                owner,
                me -> {
                    mMe.set(me);
                    mPeer.set(me);
                });
        getRoomStore()
            .getRoomInfo()
            .observe(
                owner,
                roomInfo -> {
                    mFaceDetection.set(roomInfo.isFaceDetection());
                    mConnected.set(
                        RoomClient.ConnectionState.CONNECTED.equals(roomInfo.getConnectionState()));
                });
        mStateComposer.connect(owner, getRoomStore());
    }

    public static class StateComposer extends BaseObservable {

        private Producers.ProducersWrapper mAudioPW;
        private Producers.ProducersWrapper mVideoPW;
        private Me mMe;

        void connect(@NonNull LifecycleOwner owner, RoomStore store) {
            store
                .getProducers()
                .observe(
                    owner,
                    (producers) -> {
                        mAudioPW = producers.filter("audio");
                        mVideoPW = producers.filter("video");
                        notifyChange();
                    });
            store
                .getMe()
                .observe(
                    owner,
                    (me) -> {
                        mMe = me;
                        notifyChange();
                    });
        }
    }
}
