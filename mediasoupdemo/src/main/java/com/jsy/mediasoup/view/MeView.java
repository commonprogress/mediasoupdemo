package com.jsy.mediasoup.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jsy.mediasoup.BindingAdapters;
import com.jsy.mediasoup.R;
import com.jsy.mediasoup.vm.MeProps;
import com.jsy.mediasoup.utils.LogUtils;

import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.webrtc.SurfaceViewRenderer;

/**
 * 自己view 信息
 */
public class MeView extends FrameLayout {
    private static final String TAG = MeView.class.getSimpleName();
    private boolean isNeat;

    public MeView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public MeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MeView(
        @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
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

    private LinearLayout controls;
    private ImageView mic;
    private ImageView cam;
    private ImageView changeCam;
    private ImageView share;

    public void setNeatView(boolean isNeat) {
        this.isNeat = isNeat;
        if (isNeat) {
            controls.setVisibility(GONE);
            box.setVisibility(GONE);
            peer.setVisibility(GONE);
            icons.setVisibility(GONE);
        }
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_me, this, true);
        peerView = view.findViewById(R.id.peer_view);
        videoRenderer = peerView.findViewById(R.id.video_renderer);
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

        controls = view.findViewById(R.id.controls);
        mic = view.findViewById(R.id.mic);
        cam = view.findViewById(R.id.cam);
        changeCam = view.findViewById(R.id.change_cam);
        share = view.findViewById(R.id.share);
        videoRenderer.init(PeerConnectionUtils.getEglContext(), null);//设置摄像头信息的渲染
    }

    public void setProps(MeProps props, final RoomClient roomClient, RoomStore roomStore) {
        // set view model.
        setPeerViewProps(props);
        setMeCameraFace(props);
        props.setOnPropsLiveDataChange(ediasProps -> {
            if(roomClient.isConnecting()) {
                // set view model.
                setPeerViewProps(props);
                setMeCameraFace(props);
                // set view model.
                setMeProps(props);
            }
        });

        // register click listener.
        info.setOnClickListener(
            view -> {
                Boolean showInfo = props.getShowInfo().get();
                props.getShowInfo().set(showInfo != null && showInfo ? Boolean.FALSE : Boolean.TRUE);
            });

        meDisplayName.setOnEditorActionListener(
            (textView, actionId, keyEvent) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    //修改自己名字
                    roomClient.changeDisplayName(textView.getText().toString().trim());
                    return true;
                }
                return false;
            });
        stats.setOnClickListener(
            view -> {
                // TODO(HaiyangWU): Handle inner click event;
            });

        //SurfaceView 层次覆盖关系
        videoRenderer.setZOrderMediaOverlay(true);

        // set view model.
        setMeProps(props);

        // register click listener. 是否静音
        mic.setOnClickListener(
            view -> {
                if (MeProps.DeviceState.ON.equals(props.getMicState().get())) {
                    roomClient.muteMic();
                } else {
                    roomClient.unmuteMic();
                }
            });
        //是否是否打开摄像头
        cam.setOnClickListener(
            view -> {
                if (MeProps.DeviceState.ON.equals(props.getCamState().get())) {
                    roomClient.disableCam();
                } else {
                    roomClient.enableCam();
                }
            });
        //前后摄像头切换
        changeCam.setOnClickListener(view -> roomClient.changeCam());
        //屏幕共享 （功能暂未实现）
        share.setOnClickListener(
            view -> {
                if (MeProps.DeviceState.ON.equals(props.getShareState().get())) {
                    roomClient.disableShare();
                } else {
                    roomClient.enableShare();
                }
            });
    }

    private void setMeCameraFace(MeProps props) {
        LogUtils.i(TAG, "setMeCameraFace,mediasoup null == props:" + (null == props));
        if (null == props) {
            return;
        }
//        isCamFront = "front".endsWith(MediasoupLoaderUtils.getInstance().getCurCameraFace());
        boolean isFrontCamera = null == props.getMe().get() ? true : props.getMe().get().isFrontCamera();
        videoRenderer.setMirror(isFrontCamera);
//        videoRenderer.setEnableHardwareScaler(true);
    }

    private void setPeerViewProps(MeProps props) {
        LogUtils.i(TAG, "setPeerViewProps,mediasoup null == props:" + (null == props));
        if (null == props) {
            return;
        }
        BindingAdapters.render(videoRenderer, props.getVideoTrack().get());
        BindingAdapters.renderEmpty(video_hidden, props.getVideoTrack().get());

        audio_producer.setVisibility(!TextUtils.isEmpty(props.getAudioProducerId().get()) ? View.VISIBLE : View.GONE);
        audio_producer.setText(props.getAudioProducerId().get());
        audio_consumer.setVisibility(!TextUtils.isEmpty(props.getAudioConsumerId().get()) ? View.VISIBLE : View.GONE);
        audio_consumer.setText(props.getAudioConsumerId().get());
        video_producer.setVisibility(!TextUtils.isEmpty(props.getVideoProducerId().get()) ? View.VISIBLE : View.GONE);
        video_producer.setText(props.getVideoProducerId().get());
        video_consumer.setVisibility(!TextUtils.isEmpty(props.getVideoConsumerId().get()) ? View.VISIBLE : View.GONE);
        video_consumer.setText(props.getVideoConsumerId().get());
        if (null != props.getPeer().get()) {
            meDisplayName.setText(props.getPeer().get().getDisplayName());
            peer_display_name.setText(props.getPeer().get().getDisplayName());
            BindingAdapters.deviceInfo(device_version, props.getPeer().get().getDevice());
        } else {
            LogUtils.i(TAG, "setPeerViewProps,mediasoup null == props.getPeer().get()");
        }
        meDisplayName.setVisibility(props.isMe() ? View.VISIBLE : View.GONE);
        peer_display_name.setVisibility(!props.isMe() ? View.VISIBLE : View.GONE);
    }

    private void setMeProps(MeProps props) {
        LogUtils.i(TAG, "setPeerViewProps,mediasoup null == props:" + (null == props));
        if (null == props) {
            return;
        }
        controls.setVisibility(props.getConnected().get() && !isNeat ? View.VISIBLE : View.GONE);
        BindingAdapters.deviceMicState(mic, props.getMicState().get());
        BindingAdapters.deviceCamState(cam, props.getCamState().get());
        BindingAdapters.changeCamState(changeCam, props.getCamState().get());
        BindingAdapters.shareState(share, props.getShareState().get());
        if (null != props.getMe().get()) {
            cam.setClickable(!(props.getMe().get().isCamInProgress() || props.getMe().get().isShareInProgress()));
            changeCam.setClickable(!(props.getMe().get().isCamInProgress() || props.getMe().get().isShareInProgress()));
            share.setClickable(!(props.getMe().get().isCamInProgress() || props.getMe().get().isShareInProgress()));
        } else {
            LogUtils.i(TAG, "setMeProps,null == props.getMe().get()");
        }
    }

}
