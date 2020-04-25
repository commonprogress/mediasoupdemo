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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jsy.mediasoup.BindingAdapters;
import com.jsy.mediasoup.R;
import com.jsy.mediasoup.vm.PeerProps;
import com.jsy.mediasoup.utils.LogUtils;

import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.RoomClient;
import org.webrtc.SurfaceViewRenderer;

/**
 * 其他user view 信息
 */
public class PeerView extends FrameLayout {
    private static final String TAG = PeerView.class.getSimpleName();
    private boolean isNeat;

    public PeerView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public PeerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PeerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PeerView(
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

    private LinearLayout controls_state;
    private ImageView mic_off;
    private ImageView cam_off;

    public void setNeatView(boolean isNeat) {
        this.isNeat = isNeat;
        if (isNeat) {
            controls_state.setVisibility(GONE);
            box.setVisibility(GONE);
            peer.setVisibility(GONE);
            icons.setVisibility(GONE);
        }
    }

    private void init(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_peer, this, true);
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

        controls_state = view.findViewById(R.id.controls_state);
        mic_off = view.findViewById(R.id.mic_off);
        cam_off = view.findViewById(R.id.cam_off);

        videoRenderer.init(PeerConnectionUtils.getEglContext(), null);//
//        videoRenderer.setMirror(true);
//        videoRenderer.setEnableHardwareScaler(true);
    }

    public void setProps(PeerProps props, RoomClient roomClient) {
        // set view model into included layout
        setPeerViewProps(props);

        props.setOnPropsLiveDataChange(ediasProps -> {
            if(roomClient.isConnecting()) {
                // set view model.
                setPeerViewProps(props);
                // set view model.
                setPeerProps(props);
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
        setPeerProps(props);
    }


    private void setPeerViewProps(PeerProps props) {
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

    private void setPeerProps(PeerProps props) {
        LogUtils.i(TAG, "setPeerProps,mediasoup null == props:" + (null == props));
        if (null == props) {
            return;
        }
        mic_off.setVisibility(!props.getAudioEnabled().get() ? View.VISIBLE : View.GONE);
        cam_off.setVisibility(!props.getVideoVisible().get() ? View.VISIBLE : View.GONE);
    }
}
