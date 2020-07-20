package com.dongxl.mediasoup;

import androidx.lifecycle.LifecycleOwner;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.AttributeSet;
import android.view.View;

import com.jsy.mediasoup.MediasoupLoaderUtils;
import com.jsy.mediasoup.PropsChangeAndNotify;
import com.jsy.mediasoup.utils.LogUtils;
import com.jsy.mediasoup.view.MeView;
import com.jsy.mediasoup.vm.MeProps;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Me;

public class SelfMediasoupView extends UserMediasoupView {
    private static final String TAG = SelfMediasoupView.class.getSimpleName();
    private Me me;
    private String curRegister;

    public SelfMediasoupView(@NonNull Context context) {
        super(context);
    }

    public SelfMediasoupView(@NonNull Context context, LifecycleOwner lifecycleOwner, Me me, PropsChangeAndNotify changeAndNotify, String curRegister) {
        super(context, lifecycleOwner, changeAndNotify);
        this.me = me;
        this.curRegister = curRegister;
    }

    public SelfMediasoupView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SelfMediasoupView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected View registerHandler() {
        MeView view = new MeView(mContext);
//        view.setNeatView(true);
        boolean isMediasoupReady = MediasoupLoaderUtils.getInstance().isMediasoupReady(curRegister);
        LogUtils.e(TAG, "registerHandler() isMediasoupReady:" + isMediasoupReady);
        if (isMediasoupReady) {
            RoomClient roomClient = MediasoupLoaderUtils.getInstance().getRoomClient();
            RoomStore roomStore = MediasoupLoaderUtils.getInstance().getRoomStore();
            MeProps meProps = changeAndNotify.getMePropsAndChange(lifecycleOwner, roomClient, roomStore);
            view.setProps(meProps, roomClient, roomStore);
        }
        return view;
    }
}
