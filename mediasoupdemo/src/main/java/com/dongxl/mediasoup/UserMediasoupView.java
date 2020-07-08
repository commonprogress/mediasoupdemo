package com.dongxl.mediasoup;

import androidx.lifecycle.LifecycleOwner;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.jsy.mediasoup.PropsChangeAndNotify;

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
