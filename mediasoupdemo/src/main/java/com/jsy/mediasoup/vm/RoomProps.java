package com.jsy.mediasoup.vm;

import android.app.Application;
import androidx.lifecycle.LifecycleOwner;
import androidx.databinding.BaseObservable;
import androidx.databinding.Observable;
import androidx.databinding.ObservableField;
import androidx.annotation.NonNull;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.jsy.mediasoup.R;
import com.jsy.mediasoup.utils.LogUtils;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Me;
import org.mediasoup.droid.lib.model.RoomInfo;

public class RoomProps extends EdiasProps {
    private static final String TAG = RoomProps.class.getSimpleName();
    private final Animation mConnectingAnimation;
    private ObservableField<String> mInvitationLink;//邀请链接
    private ObservableField<RoomClient.ConnectionState> mConnectionState;//连接状态
    private ObservableField<Boolean> mAudioOnly;//是否只有音频
    private ObservableField<Boolean> mAudioOnlyInProgress;//音视频切换中？
    private ObservableField<Boolean> mAudioMuted;//是否静音
    private ObservableField<Boolean> mRestartIceInProgress;//是否重启ice
    private final Animation mRestartIceAnimation;
    private final StateComposer mStateComposer;//自己的消费状态（连接状态已经改变）

    public RoomProps(@NonNull Application application, RoomClient roomClient, @NonNull RoomStore roomStore) {
        super(application, roomStore);
        this.mRoomClient = roomClient;
        mConnectingAnimation = AnimationUtils.loadAnimation(getApplication(), R.anim.ani_connecting);
        mInvitationLink = new ObservableField<>();
        mConnectionState = new ObservableField<>();
        mAudioOnly = new ObservableField<>();
        mAudioOnlyInProgress = new ObservableField<>();
        mAudioMuted = new ObservableField<>();
        mRestartIceInProgress = new ObservableField<>();
        mRestartIceAnimation = AnimationUtils.loadAnimation(getApplication(), R.anim.ani_restart_ice);
        mStateComposer = new StateComposer();
        mStateComposer.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable sender, int propertyId) {
                boolean isConnecting = null == mRoomClient ? true : mRoomClient.isConnecting();
                if (!isConnecting) {
                    LogUtils.e(TAG, "RoomProps, onPropertyChanged isConnecting=false");
                }
                Me me = mStateComposer.mMe;
                RoomInfo roomInfo = mStateComposer.mRoomInfo;
                if (null != me) {
                    mAudioOnly.set(me.isAudioOnly());//设置是否仅音频
                    mAudioOnlyInProgress.set(me.isAudioOnlyInProgress());//设置是否音频？
                    mAudioMuted.set(me.isAudioMuted());//设置是否静音
                    mRestartIceInProgress.set(me.isRestartIceInProgress());//设置是否重制ice 中
                }
                if (null != roomInfo) {
                    mConnectionState.set(roomInfo.getConnectionState());
                    mInvitationLink.set(roomInfo.getUrl());
                }
                if (null != mPropsLiveDataChange) {
                    mPropsLiveDataChange.onDataChanged(RoomProps.this);
                }
            }
        });
    }

    public Animation getConnectingAnimation() {
        return mConnectingAnimation;
    }

    public ObservableField<String> getInvitationLink() {
        return mInvitationLink;
    }

    public ObservableField<RoomClient.ConnectionState> getConnectionState() {
        return mConnectionState;
    }

    public ObservableField<Boolean> getAudioOnly() {
        return mAudioOnly;
    }

    public ObservableField<Boolean> getAudioOnlyInProgress() {
        return mAudioOnlyInProgress;
    }

    public ObservableField<Boolean> getAudioMuted() {
        return mAudioMuted;
    }

    public ObservableField<Boolean> getRestartIceInProgress() {
        return mRestartIceInProgress;
    }

    public Animation getRestartIceAnimation() {
        return mRestartIceAnimation;
    }

    /**
     * 设置mConnectionState 连接状态？
     *
     * @param roomInfo
     */
    private void receiveState(RoomInfo roomInfo) {
        mConnectionState.set(roomInfo.getConnectionState());
        mInvitationLink.set(roomInfo.getUrl());
    }

    /**
     * room连接
     *
     * @param owner
     */
    @Override
    public void connect(LifecycleOwner owner) {
        RoomStore roomStore = getRoomStore();
        roomStore.getRoomInfo().observe(owner, this::receiveState);
        roomStore
            .getMe()
            .observe(
                owner,
                me -> {
                    //设置自己状态
                    mAudioOnly.set(me.isAudioOnly());//设置是否仅音频
                    mAudioOnlyInProgress.set(me.isAudioOnlyInProgress());//设置是否音频？
                    mAudioMuted.set(me.isAudioMuted());//设置是否静音
                    mRestartIceInProgress.set(me.isRestartIceInProgress());//设置是否重制ice 中
                });
        mStateComposer.connect(owner, getRoomStore());
    }

    public static class StateComposer extends BaseObservable {
        private Me mMe;
        private RoomInfo mRoomInfo;

        void connect(LifecycleOwner owner, RoomStore store) {
            store
                .getRoomInfo()
                .observe(owner,
                    (info) -> {
                        mRoomInfo = info;
                        notifyChange();
                    }
                );
            store
                .getMe()
                .observe(owner,
                    (me) -> {
                        mMe = me;
                        notifyChange();
                    }
                );
        }
    }
}
