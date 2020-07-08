package com.jsy.mediasoup.view;

import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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
import org.mediasoup.droid.lib.Utils;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Me;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

/**
 * 自己view 信息
 */
public class MeView extends BaseFrameLayout {
    private static final String TAG = MeView.class.getSimpleName();
    private boolean isNeat;
    private MeProps mMeProps;
    private RoomClient mRoomClient;
    private RoomStore mRoomStore;
    private boolean isAddVideoTrack;
    private Me curMe;

    public MeView(@NonNull Context context) {
        super(context);
    }

    public MeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public MeView(
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

    private LinearLayout controls;
    private ImageView mic;
    private ImageView cam;
    private ImageView changeCam;
    private ImageView share;


    @Override
    protected View addChildRootView() {
        return LayoutInflater.from(mContext).inflate(R.layout.view_me, this, true);
    }

    @Override
    protected void initView() {
        if (null == rootView) {
            LogUtils.e(TAG, "initView null == rootView ,mediasoup ");
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

        controls = rootView.findViewById(R.id.controls);
        mic = rootView.findViewById(R.id.mic);
        cam = rootView.findViewById(R.id.cam);
        changeCam = rootView.findViewById(R.id.change_cam);
        share = rootView.findViewById(R.id.share);
        share.setVisibility(VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    private void initSurfaceRenderer() {
        if (null == peerView) {
            LogUtils.e(TAG, "initSurfaceRenderer null == peerView , mediasoup ");
        }
        if (null != videoRenderer) {
            LogUtils.e(TAG, "initSurfaceRenderer null != videoRenderer ,mediasoup ");
            return;
        }
        isAddVideoTrack = false;
        try {
            videoRenderer = peerView.findViewById(R.id.video_renderer);
            videoRenderer.init(PeerConnectionUtils.getEglContext(), null);//设置摄像头信息的渲染
        } catch (Exception e) {
            e.printStackTrace();
            videoRenderer.release();
            videoRenderer = peerView.findViewById(R.id.video_renderer);
            videoRenderer.init(PeerConnectionUtils.getEglContext(), null);//设置摄像头信息的渲染
        }
        videoRenderer.setEnableHardwareScaler(true);
        //SurfaceView 层次覆盖关系
        videoRenderer.setZOrderMediaOverlay(true);
        videoRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
    }

    public void setNeatView(boolean isNeat) {
        this.isNeat = isNeat;
        if (isNeat) {
            controls.setVisibility(GONE);
            box.setVisibility(GONE);
            peer.setVisibility(GONE);
            icons.setVisibility(GONE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    @Override
    protected void loadViewData(boolean isAgain) {
        LogUtils.i(TAG, "loadViewData,mediasoup isAddVideoTrack:" + isAddVideoTrack + ", isAgain:" + isAgain);
        initSurfaceRenderer();
        if (isAgain) {
            setPeerViewProps(mMeProps, null != mRoomClient ? mRoomClient.isConnected() : false);
            setMeCameraFace(mMeProps, null != mRoomClient ? mRoomClient.isConnected() : false);
            // set view model.
            setMeProps(mMeProps, null != mRoomClient ? mRoomClient.isConnected() : false);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    public void setProps(MeProps props, final RoomClient roomClient, RoomStore roomStore) {
        LogUtils.i(TAG, "loadViewData,mediasoup  setProps isAddVideoTrack:" + isAddVideoTrack);
        this.mMeProps = props;
        this.mRoomClient = roomClient;
        this.mRoomStore = roomStore;
        isAddVideoTrack = false;
        // set view model.
        setPeerViewProps(props, null != roomClient ? roomClient.isConnected() : false);
        setMeCameraFace(props, null != roomClient ? roomClient.isConnected() : false);
        props.setOnPropsLiveDataChange(ediasProps -> {
            if (!isReleaseView() && roomClient.isConnecting()) {
                // set view model.
                setPeerViewProps(props, null != roomClient ? roomClient.isConnected() : false);
                setMeCameraFace(props, null != roomClient ? roomClient.isConnected() : false);
                // set view model.
                setMeProps(props, null != roomClient ? roomClient.isConnected() : false);
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
                    LogUtils.i(TAG, "getShareState() ,mediasoup :" + (props.getShareState().get()) + ",isShareInProgress:" + props.getMe().get().isShareInProgress() + ", isCamInProgress:"+props.getMe().get().isCamInProgress());
                    if (MeProps.DeviceState.ON.equals(props.getShareState().get())) {
                        roomClient.disableShare();
                    } else {
                        roomClient.enableShare();
                    }
                });
    }

    private void setMeCameraFace(MeProps props, boolean isConnected) {
        LogUtils.i(TAG, "setMeCameraFace,mediasoup null == props:" + (null == props) + ", isConnected:" + isConnected);
        if (null == props) {
            return;
        }
//        isCamFront = "front".endsWith(MediasoupLoaderUtils.getInstance().getCurCameraFace());
//        boolean isFrontCamera = null == props.getMe().get() ? true : props.getMe().get().isFrontCamera();
//        videoRenderer.setMirror(isFrontCamera);
//        videoRenderer.setEnableHardwareScaler(true);
    }

    private void setPeerViewProps(MeProps props, boolean isConnected) {
        LogUtils.i(TAG, "setPeerViewProps,mediasoup null == props:" + (null == props) + ", isConnected:" + isConnected);
        if (null == props) {
            return;
        }

        Me me = null == props ? null : (null == props.getMe() ? null : props.getMe().get());
        VideoTrack videoTrack = null == props ? null : (null == props.getVideoTrack() ? null : props.getVideoTrack().get());
        MeProps.DeviceState camState = null == props ? null : (null == props.getCamState() ? null : props.getCamState().get());
        int step = 0;
        if (null == videoRenderer || null == me || null == videoTrack || !isConnected) {
            isAddVideoTrack = BindingAdapters.render(videoRenderer, videoTrack, isConnected);
            BindingAdapters.renderEmpty(video_hidden, videoTrack, isConnected);
            step = 1;
        } else {
            if (!isAddVideoTrack || videoRenderer.getVisibility() != VISIBLE || video_hidden.getVisibility() == VISIBLE || null == curMe || Utils.isEmptyString(me.getId()) || !me.getId().equals(curMe.getId())) {
                isAddVideoTrack = BindingAdapters.render(videoRenderer, videoTrack, isConnected);
                BindingAdapters.renderEmpty(video_hidden, videoTrack, isConnected);
                step = 2;
            } else {
                step = 3;
            }
        }
        LogUtils.i(TAG, "setPeerViewProps,mediasoup null == videoTrack:" + (null == videoTrack) + ",step:" + step + ", isConnected:" + isConnected + ", isAddVideoTrack:" + isAddVideoTrack + ",camState:" + camState + ", null == me:" + (null == me) + ", null == curMe:" + (null == curMe) + ",null == props:" + (null == props));
        this.curMe = me;

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

    private void setMeProps(MeProps props, boolean isConnected) {
        LogUtils.i(TAG, "setPeerViewProps,mediasoup null == props:" + (null == props) + ", isConnected:" + isConnected);
        if (null == props) {
            return;
        }
        controls.setVisibility(props.getConnected().get() && !isNeat ? View.VISIBLE : View.GONE);
        BindingAdapters.deviceMicState(mic, props.getMicState().get());
        BindingAdapters.deviceCamState(cam, props.getCamState().get());
        BindingAdapters.changeCamState(changeCam, props.getCamState().get());
        BindingAdapters.shareState(share, props.getShareState().get());
        if (null != props.getMe().get()) {
            LogUtils.i(TAG, "props.getMe().get() ,mediasoup :" + (props.getShareState().get()) + ",isShareInProgress:" + props.getMe().get().isShareInProgress() + ", isCamInProgress:"+props.getMe().get().isCamInProgress());
            cam.setClickable(!(props.getMe().get().isCamInProgress() || props.getMe().get().isShareInProgress()));
            changeCam.setClickable(!(props.getMe().get().isCamInProgress() || props.getMe().get().isShareInProgress()));
            share.setClickable(!(props.getMe().get().isCamInProgress() || props.getMe().get().isShareInProgress()));
        } else {
            LogUtils.i(TAG, "setMeProps,mediasoup null == props.getMe().get()");
        }
    }


    @Override
    protected void releaseViewData() {
        LogUtils.i(TAG, "releaseViewData ,mediasoup  :" + this);
        releaseRenderer();
    }

    public void clearEglRendererImage() {
        if (null != videoRenderer) {
            videoRenderer.clearImage();
        }
    }

    public void releaseRenderer() {
        if (null != videoRenderer) {
            videoRenderer.release();
        }
        videoRenderer = null;
    }
}
