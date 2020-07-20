package com.jsy.mediasoup.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jsy.mediasoup.utils.LogUtils;

public abstract class BaseFrameLayout extends FrameLayout {
    private static final String TAG = BaseFrameLayout.class.getSimpleName();
    protected Context mContext;
    protected View rootView;
    private boolean isFirstLoad = true;
    private boolean isInitView;
    private boolean isReleaseView;

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        LogUtils.i(TAG, "onAttachedToWindow isInitView:" + isInitView + ", isFirstLoad:" + isFirstLoad + ", current:" + this);
        isReleaseView = false;
        if (isInitView && !isFirstLoad) {
            if (null == mContext) {
                mContext = getContext();
            }
            int childCount = this.getChildCount();
            if (childCount <= 0 || null == rootView) {
                rootView = addChildRootView();
                initView();
            }
            loadViewData(true);
        }
        isFirstLoad = false;
    }

    public BaseFrameLayout(@NonNull Context context) {
        super(context);
        init(context);
    }

    public BaseFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BaseFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public BaseFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        LogUtils.i(TAG, "init isInitView:" + isInitView + ", isFirstLoad:" + isFirstLoad + ", current:" + this);
        this.mContext = context;
        rootView = addChildRootView();
        initView();
        loadViewData(false);
        isInitView = true;
    }

    protected abstract View addChildRootView();

    protected abstract void initView();

    protected abstract void loadViewData(boolean isAgain);

    public abstract void releaseViewData();

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        LogUtils.i(TAG, "onDetachedFromWindow isInitView:" + isInitView + ", isFirstLoad:" + isFirstLoad + ", current:" + this);
        isReleaseView = true;
        releaseViewData();
    }

    public boolean isReleaseView() {
        return isReleaseView;
    }
}
