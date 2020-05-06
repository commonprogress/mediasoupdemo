package com.dongxl.mediasoup;

import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.jsy.mediasoup.MediasoupLoaderUtils;
import com.jsy.mediasoup.PropsChangeAndNotify;
import com.jsy.mediasoup.utils.LogUtils;
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
        this.removeAllViews();
        addView(v);
        return this;
    }

    protected abstract View registerHandler();
}
