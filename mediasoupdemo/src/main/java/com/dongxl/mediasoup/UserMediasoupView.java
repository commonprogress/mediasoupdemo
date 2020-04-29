package com.dongxl.mediasoup;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.jsy.mediasoup.MediasoupLoaderUtils;
import com.jsy.mediasoup.PropsChangeAndNotify;
import com.jsy.mediasoup.view.MeView;
import com.jsy.mediasoup.view.PeerView;
import com.jsy.mediasoup.vm.MeProps;
import com.jsy.mediasoup.vm.PeerProps;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Me;
import org.mediasoup.droid.lib.model.Peer;

public abstract class UserMediasoupView extends FrameLayout {
    protected Context mContext;
    protected PropsChangeAndNotify changeAndNotify;
    protected LifecycleOwner lifecycleOwner;

    public UserMediasoupView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public UserMediasoupView(@NonNull Context context, LifecycleOwner lifecycleOwner, PropsChangeAndNotify changeAndNotify) {
        this(context);
        this.lifecycleOwner = lifecycleOwner;
        this.changeAndNotify = changeAndNotify;
    }

    public UserMediasoupView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public UserMediasoupView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    protected void init(Context context) {
        this.mContext = context;
    }

    protected View addChildView() {
        View v = registerHandler();
        v.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(v, 0);
        return this;
    }

    protected abstract View registerHandler();

    static class SelfMediasoupView extends UserMediasoupView {

        private Me me;

        public SelfMediasoupView(@NonNull Context context) {
            super(context);
        }

        public SelfMediasoupView(@NonNull Context context, LifecycleOwner lifecycleOwner, Me me, PropsChangeAndNotify changeAndNotify) {
            super(context, lifecycleOwner, changeAndNotify);
            this.me = me;
        }

        public SelfMediasoupView(@NonNull Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
        }

        public SelfMediasoupView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
        }

        @Override
        protected View registerHandler() {
            this.removeAllViews();
            MeView view = new MeView(mContext);
            view.setNeatView(true);
            boolean isMediasoupReady = MediasoupLoaderUtils.getInstance().isMediasoupReady();
            if (isMediasoupReady) {
                RoomClient roomClient = MediasoupLoaderUtils.getInstance().getRoomClient();
                RoomStore roomStore = MediasoupLoaderUtils.getInstance().getRoomStore();
                MeProps meProps = changeAndNotify.getMePropsAndChange(lifecycleOwner, roomClient, roomStore);
                view.setProps(meProps, roomClient, roomStore);
            }
            return view;
        }
    }

    static class OtherMediasoupView extends UserMediasoupView {

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
            this.removeAllViews();
            PeerView peerView = new PeerView(mContext);
            peerView.setNeatView(true);
            boolean isMediasoupReady = MediasoupLoaderUtils.getInstance().isMediasoupReady();
            if (isMediasoupReady) {
                RoomClient roomClient = MediasoupLoaderUtils.getInstance().getRoomClient();
                RoomStore roomStore = MediasoupLoaderUtils.getInstance().getRoomStore();
                PeerProps peerProps = changeAndNotify.getPeerPropsAndChange(lifecycleOwner, roomClient, roomStore, peer);
                peerView.setProps(peerProps, roomClient);
            }
            return peerView;
        }
    }
}
