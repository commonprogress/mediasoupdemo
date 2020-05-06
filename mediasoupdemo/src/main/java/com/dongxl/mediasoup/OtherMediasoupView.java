package com.dongxl.mediasoup;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.jsy.mediasoup.MediasoupLoaderUtils;
import com.jsy.mediasoup.PropsChangeAndNotify;
import com.jsy.mediasoup.utils.LogUtils;
import com.jsy.mediasoup.view.PeerView;
import com.jsy.mediasoup.vm.PeerProps;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Peer;

public class OtherMediasoupView extends UserMediasoupView {

    private static final String TAG = OtherMediasoupView.class.getSimpleName();
    private Peer peer;

    public OtherMediasoupView(@NonNull Context context) {
        super(context);
    }

    public OtherMediasoupView(@NonNull Context context, LifecycleOwner lifecycleOwner, Peer peer, PropsChangeAndNotify changeAndNotify) {
        super(context, lifecycleOwner, changeAndNotify);
        this.peer = peer;
    }

    public OtherMediasoupView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OtherMediasoupView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View registerHandler() {
        this.setBackgroundColor(Color.YELLOW);
        PeerView peerView = new PeerView(mContext);
//        peerView.setNeatView(true);
        boolean isMediasoupReady = MediasoupLoaderUtils.getInstance().isMediasoupReady();
        LogUtils.e(TAG, "registerHandler() isMediasoupReady:" + isMediasoupReady);
        if (isMediasoupReady) {
            RoomClient roomClient = MediasoupLoaderUtils.getInstance().getRoomClient();
            RoomStore roomStore = MediasoupLoaderUtils.getInstance().getRoomStore();
            PeerProps peerProps = changeAndNotify.getPeerPropsAndChange(lifecycleOwner, roomClient, roomStore, peer);
            peerView.setProps(peerProps, roomClient);
        }
        return peerView;
    }
}
