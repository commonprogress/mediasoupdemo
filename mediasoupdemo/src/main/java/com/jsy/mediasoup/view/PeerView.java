package com.jsy.mediasoup.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.jsy.mediasoup.BindingAdapters;
import com.jsy.mediasoup.MediasoupLoaderUtils;
import com.jsy.mediasoup.R;
import com.jsy.mediasoup.vm.PeerProps;
import com.jsy.mediasoup.utils.LogUtils;

import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomConstant;
import org.mediasoup.droid.lib.Utils;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Info;
import org.mediasoup.droid.lib.model.Peer;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

/**
 * 其他user view 信息
 */
public class PeerView extends BaseFrameLayout {
    private static final String TAG = PeerView.class.getSimpleName();
    private static final long REFRESH_TRACK_INTERVAL = 10 * 60 * 1000;
    private boolean isNeat;
    private boolean isAddVideoTrack;
    private PeerProps mPeerProps;
    private RoomClient mRoomClient;
    private RoomStore mRoomStore;
    private Peer curPeer;
    private long lastAddTrackTime;

    public PeerView(@NonNull Context context) {
        super(context);
    }

    public PeerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PeerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PeerView(
        @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private FrameLayout peerView;
    private SurfaceViewRenderer videoRenderer;
    private LinearLayout video_hidden;
    private LinearLayout icons;
    private ImageView stats;
    private ImageView info;

    private LinearLayout box;
    private LinearLayout peer;

    private TextView audio_producer;
    private TextView audio_consumer;
    private TextView video_producer;
    private TextView video_consumer;

    private EditText meDisplayName;
    private TextView peer_display_name;
    private TextView device_version;

    private LinearLayout controls_state;
    private ImageView mic_off;
    private ImageView cam_off;

    @Override
    protected View addChildRootView() {
        return LayoutInflater.from(mContext).inflate(R.layout.view_peer, this, true);
    }

    @Override
    protected void initView() {
        lastAddTrackTime = System.currentTimeMillis();
        if (null == rootView) {
            LogUtils.e(TAG, "initView null == rootView");
        }

        peerView = rootView.findViewById(R.id.peer_view);

        video_hidden = peerView.findViewById(R.id.video_hidden);
        icons = peerView.findViewById(R.id.icons);
        stats = peerView.findViewById(R.id.stats);
        info = peerView.findViewById(R.id.info);

        box = peerView.findViewById(R.id.box);
        peer = peerView.findViewById(R.id.peer);

        audio_producer = peerView.findViewById(R.id.audio_producer);
        audio_consumer = peerView.findViewById(R.id.audio_consumer);
        video_producer = peerView.findViewById(R.id.video_producer);
        video_consumer = peerView.findViewById(R.id.video_consumer);

        meDisplayName = peerView.findViewById(R.id.me_display_name);
        device_version = peerView.findViewById(R.id.device_version);
        peer_display_name = peerView.findViewById(R.id.peer_display_name);

        controls_state = rootView.findViewById(R.id.controls_state);
        mic_off = rootView.findViewById(R.id.mic_off);
        cam_off = rootView.findViewById(R.id.cam_off);
    }

    public void setNeatView(boolean isNeat) {
        this.isNeat = isNeat;
        if (isNeat) {
            controls_state.setVisibility(GONE);
            box.setVisibility(GONE);
            peer.setVisibility(GONE);
            icons.setVisibility(GONE);
        }
    }

    @Override
    protected void loadViewData(boolean isAgain) {
        LogUtils.i(TAG, "loadViewData,mediasoup isAddVideoTrack:" + isAddVideoTrack + ", isAgain:" + isAgain);
        if (isReleaseView()) {
            return;
        }
        initSurfaceRenderer();
        if (isAgain) {
            // set view model into included layout
            setPeerViewProps(mPeerProps, null != mRoomClient ? mRoomClient.isConnected() : false);
            // set view model
            setPeerProps(mPeerProps, null != mRoomClient ? mRoomClient.isConnected() : false);
        }
    }

    private void initSurfaceRenderer() {
        if (null == peerView) {
            LogUtils.e(TAG, "initSurfaceRenderer null == peerView");
        }
        if (null != videoRenderer) {
            LogUtils.e(TAG, "initSurfaceRenderer null != videoRenderer");
            return;
        }
        isAddVideoTrack = false;
        try {
            videoRenderer = peerView.findViewById(R.id.video_renderer);
            videoRenderer.init(PeerConnectionUtils.getEglContext(), null);//
        } catch (Exception e) {
            e.printStackTrace();
            videoRenderer.release();
            videoRenderer = peerView.findViewById(R.id.video_renderer);
            videoRenderer.init(PeerConnectionUtils.getEglContext(), null);//
        }
//        videoRenderer.setMirror(true);
        videoRenderer.setEnableHardwareScaler(true);
        videoRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_BALANCED);
    }

    public void setProps(PeerProps props) {
        setProps(props, MediasoupLoaderUtils.getInstance().getRoomClient(), MediasoupLoaderUtils.getInstance().getRoomStore());
    }

    public void setProps(PeerProps props, RoomClient roomClient, RoomStore roomStore) {
        LogUtils.i(TAG, "setProps,mediasoup isAddVideoTrack:" + isAddVideoTrack);
        isAddVideoTrack = false;
        this.mPeerProps = props;
        this.mRoomClient = roomClient;
        this.mRoomStore = roomStore;
        // set view model into included layout
        setPeerViewProps(props, null != roomClient ? roomClient.isConnected() : false);

        props.setOnPropsLiveDataChange(ediasProps -> {
            if (!isReleaseView()) {
                PeerProps peerProps = (PeerProps) ediasProps;
                // set view model.
                setPeerViewProps(peerProps, null != roomClient ? roomClient.isConnected() : false);
                // set view model.
                setPeerProps(peerProps, null != roomClient ? roomClient.isConnected() : false);
            }
        });

        // register click listener.
        info.setOnClickListener(
            view -> {
                Boolean showInfo = props.getShowInfo().get();
                props.getShowInfo().set(showInfo != null && showInfo ? Boolean.FALSE : Boolean.TRUE);
            });

        stats.setOnClickListener(
            view -> {
                // TODO(HaiyangWU): Handle inner click event;
            });

        // set view model
        setPeerProps(props, null != roomClient ? roomClient.isConnected() : false);
    }


    private void setPeerViewProps(PeerProps props, boolean isConnected) {
        Peer user;
        Info curInfo = null == props ? null : (null == props.getPeer() ? null : props.getPeer().get());
        if (null != curInfo && !(curInfo instanceof Peer)) {
            LogUtils.e(TAG, "setPeerViewProps,mediasoup  setProps curInfo is not Peer:" + curInfo);
//            user = null;
            return;
        } else {
            user = null == curInfo ? null : (Peer) curInfo;
        }

        VideoTrack videoTrack = null == props ? null : (null == props.getVideoTrack() ? null : props.getVideoTrack().get());
        boolean isPropVideoVisible = null == props ? false : (null == props.getVideoVisible() ? false : props.getVideoVisible().get());
        boolean isPeerVideoVisible = null == user ? false : user.isVideoVisible();
        RoomConstant.PeerState peerState = null == props ? RoomConstant.PeerState.NEW : (null == props.getPeerState() ? RoomConstant.PeerState.NEW : props.getPeerState().get());
        int step = 0;
        if (null == videoRenderer || null == user || null == videoTrack || !isConnected) {
            isAddVideoTrack = BindingAdapters.render(videoRenderer, videoTrack, peerState, isConnected);
            BindingAdapters.renderEmpty(video_hidden, videoTrack, peerState, isConnected);
            step = 1;
        } else {
            if (!isAddVideoTrack || isNeedRefreshVideoTrack() || videoRenderer.getVisibility() != VISIBLE || video_hidden.getVisibility() == VISIBLE || null == curPeer || Utils.isEmptyString(user.getId()) || !user.getId().equals(curPeer.getId())) {
                isAddVideoTrack = BindingAdapters.render(videoRenderer, videoTrack, peerState, isConnected);
                BindingAdapters.renderEmpty(video_hidden, videoTrack, peerState, isConnected);
                step = 2;
            } else {
                step = 3;
            }
        }
        LogUtils.i(TAG, "setPeerViewProps,mediasoup null == videoTrack:" + (null == videoTrack) + ", step:" + step + ", null == videoRenderer:" + (null == videoRenderer) + ", isConnected:" + isConnected + ", isAddVideoTrack:" + isAddVideoTrack + ", isPropVideoVisible:" + isPropVideoVisible + ", isPeerVideoVisible:" + isPeerVideoVisible + ", null == user:" + (null == user) + ", null == curPeer:" + (null == curPeer) + ", null == props:" + (null == props) + ",peerState:" + peerState);
        this.curPeer = user;

//        audio_producer.setVisibility(!Utils.isEmptyString(props.getAudioProducerId().get()) ? View.VISIBLE : View.GONE);
//        audio_producer.setText(props.getAudioProducerId().get());
//        audio_consumer.setVisibility(!Utils.isEmptyString(props.getAudioConsumerId().get()) ? View.VISIBLE : View.GONE);
//        audio_consumer.setText(props.getAudioConsumerId().get());
//        video_producer.setVisibility(!Utils.isEmptyString(props.getVideoProducerId().get()) ? View.VISIBLE : View.GONE);
//        video_producer.setText(props.getVideoProducerId().get());
//        video_consumer.setVisibility(!Utils.isEmptyString(props.getVideoConsumerId().get()) ? View.VISIBLE : View.GONE);
//        video_consumer.setText(props.getVideoConsumerId().get());
//
//        if (null != props.getPeer().get()) {
//            meDisplayName.setText(props.getPeer().get().getDisplayName());
//            peer_display_name.setText(props.getPeer().get().getDisplayName());
//            BindingAdapters.deviceInfo(device_version, props.getPeer().get().getDevice());
//        } else {
//            LogUtils.i(TAG, "setPeerViewProps,mediasoup null == props.getPeer().get()");
//        }
//        meDisplayName.setVisibility(props.isMe() ? View.VISIBLE : View.GONE);
//        peer_display_name.setVisibility(!props.isMe() ? View.VISIBLE : View.GONE);

    }

    private void setPeerProps(PeerProps props, boolean isConnected) {
        LogUtils.i(TAG, "setPeerProps,mediasoup null == props:" + (null == props) + ", isConnected:" + isConnected);
        if (null == props) {
            return;
        }
//        mic_off.setVisibility(!props.getAudioEnabled().get() ? View.VISIBLE : View.GONE);
//        cam_off.setVisibility(!props.getVideoVisible().get() ? View.VISIBLE : View.GONE);
    }

    private boolean isNeedRefreshVideoTrack() {
        long curTime = System.currentTimeMillis();
        if (curTime - lastAddTrackTime > REFRESH_TRACK_INTERVAL) {
            lastAddTrackTime = curTime;
            return true;
        }
        return false;
    }

    public void clearEglRendererImage() {
        if (null != videoRenderer) {
            videoRenderer.clearImage();
        }
    }

    @Override
    public void releaseViewData() {
        LogUtils.i(TAG, "releaseViewData,mediasoup null == videoRenderer:" + (null == videoRenderer));
        if (null != videoRenderer) {
            videoRenderer.release();
        }
        videoRenderer = null;
        curPeer = null;
    }
}
