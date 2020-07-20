package com.jsy.mediasoup.fw

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.view.*
import com.jsy.mediasoup.R
import com.jsy.mediasoup.utils.DisplayUtils
import com.jsy.mediasoup.utils.LogUtils

class FloatingWindowManager private constructor() {
    companion object {
        val instance: FloatingWindowManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            FloatingWindowManager()
        }
    }

    private val TAG: String = FloatingWindowManager::class.java.simpleName
    private val LONG_CLICK_LIMIT: Long = 100
    private lateinit var mContext: Context
    private var mWindowManager: WindowManager? = null
    private var mLayoutParams: WindowManager.LayoutParams? = null
    private var mFloatView: View? = null
    private var isShowFloating: Boolean = false
    private val mHandler: Handler by lazy { Handler() }
    private var mFWListener: FloatingWindowListener? = null

    private val navigationBarHeight: Int by lazy { DisplayUtils.getNavigationBarHeight(mContext) }
    private val screenWidth: Int by lazy { DisplayUtils.getScreenWidth(mContext) }
    private val screenHeight: Int by lazy { DisplayUtils.getScreenHeight(mContext) }

    // 控制的变量
    private var lastDownX: Float = 0f  // 控制的变量
    private var lastDownY: Float = 0f
    private var lastParamX: Int = 0
    private var lastParamY: Int = 0
    private var isTouching: Boolean = false//是否触摸中
    private var isTouchMoveing: Boolean = false//是否移动中
    private var isLongTouch: Boolean = false//是否长按中
    private var lastDownTime: Long = 0

    private fun initWindowManager() {
        if (null == mWindowManager) {
            mWindowManager =
                mContext.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager?
        }
    }

    private fun initLayoutParams() {
        if (null != mLayoutParams) {
            return
        }
        mLayoutParams = WindowManager.LayoutParams()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mLayoutParams!!.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            mLayoutParams!!.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        //刘海屏延伸到刘海里面
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mLayoutParams!!.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        mLayoutParams!!.packageName = mContext.applicationContext.packageName
        mLayoutParams!!.format = PixelFormat.RGBA_8888
        // FLAG_NOT_FOCUSABLE 设置浮动窗口不可聚焦（实现操作除浮动窗口外的其他可见窗口的操作）
        mLayoutParams!!.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        // 当悬浮窗显示的时候可以获取到焦点
//        mLayoutParams!!.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
//                or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
//                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)

        mLayoutParams!!.width = WindowManager.LayoutParams.WRAP_CONTENT
        mLayoutParams!!.height = WindowManager.LayoutParams.WRAP_CONTENT

        mLayoutParams!!.gravity = Gravity.START or Gravity.TOP //默认剧中
        mLayoutParams!!.x =
            screenWidth - mContext.resources.getDimensionPixelSize(R.dimen.float_window_video_width) //设置gravity之后的再次偏移的显示的位置
        mLayoutParams!!.y = navigationBarHeight * 2
        //
    }

    private fun updateVideoLayoutParams() {
        if (null != mLayoutParams) {
            mLayoutParams!!.width =
                mContext.resources.getDimensionPixelSize(R.dimen.float_window_video_width)
            mLayoutParams!!.height =
                mContext.resources.getDimensionPixelSize(R.dimen.float_window_video_height)
            mLayoutParams!!.flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        }
    }

    private fun initFloatWindowView(layoutResId: Int) {
        mFloatView = LayoutInflater.from(mContext).inflate(layoutResId, null)
        mFloatView?.setOnTouchListener(OnDragTouchListener())
    }

    private fun initFloatWindowView(rootView: View?) {
        this.mFloatView = rootView
        mFloatView?.setOnTouchListener(OnDragTouchListener())
    }

    fun isShowFloating(): Boolean {
        return null != mWindowManager && null != mLayoutParams && null != mFloatView && isShowFloating
    }

    fun showUpdateFloatWindow(
        context: Context,
        layoutResId: Int,
        listener: FloatingWindowListener?
    ): View? {
        this.mContext = context.applicationContext
        LogUtils.v(
            TAG,
            "showUpdateFloatWindow 10,layoutResId:$layoutResId, mFloatView:${mFloatView?.id ?: 0}"
        )
        if (!context.isRestricted && !mContext.isRestricted && FloatingWindowUtils.checkFloatPermission(
                mContext
            )
        ) {
            this.mFWListener = listener
            if (isShowFloating() && (layoutResId > 0 && mFloatView?.id ?: 0 == layoutResId)) {
                updateFloatWindowView()
            } else {
                removeFloatWindowView()
                initWindowManager()
                initLayoutParams()
                initFloatWindowView(layoutResId)
                mWindowManager?.addView(mFloatView, mLayoutParams)
                LogUtils.v(TAG, "showUpdateFloatWindow 12,addView")
            }
            isShowFloating = true
            return mFloatView
        }
        isShowFloating = false
        return null
    }

    fun showUpdateFloatWindow(
        context: Context,
        rootView: View?,
        listener: FloatingWindowListener?
    ): View? {
        this.mContext = context.applicationContext
        LogUtils.v(TAG, "showUpdateFloatWindow 20,rootView:$rootView, mFloatView:$mFloatView")
        if (!context.isRestricted && !mContext.isRestricted && FloatingWindowUtils.checkFloatPermission(
                mContext
            )
        ) {
            this.mFWListener = listener
            if (isShowFloating() && (null != mFloatView && mFloatView == rootView)) {
                updateFloatWindowView()
            } else {
                removeFloatWindowView()
                initWindowManager()
                initLayoutParams()
                initFloatWindowView(rootView)
                mWindowManager?.addView(mFloatView, mLayoutParams)
                LogUtils.v(TAG, "showUpdateFloatWindow 22,addView")
            }
            isShowFloating = true
            return mFloatView
        }
        isShowFloating = false
        return null
    }

    fun showVideoFloatWindow(
        context: Context,
        rootView: View?,
        listener: FloatingWindowListener?
    ): View? {
        this.mContext = context.applicationContext
        LogUtils.v(
            TAG,
            "showUpdateFloatWindow 30,rootView:$rootView, mFloatView:$mFloatView, 1:${mContext.isRestricted},2: ${context.isRestricted}"
        )
        if (!context.isRestricted && !mContext.isRestricted && FloatingWindowUtils.checkFloatPermission(
                mContext
            )
        ) {
            this.mFWListener = listener
            if (isShowFloating() && (null != mFloatView && mFloatView == rootView)) {
                updateFloatWindowView()
            } else {
                removeFloatWindowView()
                initWindowManager()
                initLayoutParams()
                updateVideoLayoutParams()
                initFloatWindowView(rootView)
                mWindowManager?.addView(mFloatView, mLayoutParams)
                LogUtils.v(TAG, "showUpdateFloatWindow 32,addView")
            }
            isShowFloating = true
            return mFloatView
        }
        isShowFloating = false
        return null
    }

    private fun updateFloatWindowView() {
        LogUtils.v(TAG, "updateFloatWindowView,isShowFloating:${isShowFloating()}")
        if (isShowFloating()) {
            mWindowManager?.updateViewLayout(mFloatView, mLayoutParams)
        }
    }

    fun removeFloatWindowView() {
        LogUtils.v(TAG, "removeFloatWindowView mFloatView:$mFloatView")
        mHandler.removeCallbacksAndMessages(null)
        if (isShowFloating()) {
            mWindowManager?.removeView(mFloatView)
            mFWListener?.onDestroy(mFloatView)
            isShowFloating = false
        }
    }

    internal inner class OnDragTouchListener : View.OnTouchListener {
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
//            Log.d(
//                TAG,
//                "onTouch event?.actionMasked:${event?.actionMasked}, event.rawX:${event?.rawX} ,downX:$lastDownX, event.rawY:${event?.rawY}  ,downY:$lastDownY,"
//            )
            if (null == mLayoutParams) {
                return true
            }
            when (event?.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    isTouching = true
                    lastDownTime = System.currentTimeMillis()
                    //手指按下的位置
                    lastDownX = event.rawX
                    lastDownY = event.rawY
                    //记录手指按下时,悬浮窗的位置
                    lastParamX = mLayoutParams!!.x
                    lastParamY = mLayoutParams!!.y
                    mHandler.postDelayed({
                        if (isLongTouch()) {
                            isLongTouch = true
                        }
                    }, LONG_CLICK_LIMIT)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!isLongTouch && isTouchSlop(event)) {
                        return true
                    }
                    if (isLongTouch) {
                        var moveX = event.rawX - lastDownX
                        var moveY = event.rawY - lastDownY
                        updateFloatPosition(moveX, moveY)
                        isTouchMoveing = true
                    }
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    isTouching = false
                    if (isLongTouch) {
                        isLongTouch = false
                    }
                    isTouchMoveing = false
                    //当手指按下的位置和手指抬起来的位置距离小于10像素时,将此次触摸归结为点击事件,
                    if (isTouchSlop(event)) {
                        callOnClick()
                    }
                }
            }
            return true
        }
    }

    private fun isLongTouch(): Boolean {
        val time = System.currentTimeMillis()
        return isTouching && !isTouchMoveing && time - lastDownTime >= LONG_CLICK_LIMIT
    }

    private fun isTouchSlop(event: MotionEvent): Boolean {
        val x = event.rawX
        val y = event.rawY
        val mTouchSlop = 10
        return Math.abs(x - lastDownX) < mTouchSlop && Math.abs(y - lastDownY) < mTouchSlop
    }

    private fun callOnClick() {
        LogUtils.d(TAG, "callOnClick ")
        mFWListener?.onFWClick()
    }

    private fun updateFloatPosition(moveX: Float, moveY: Float) {
        if (null != mLayoutParams && null != mWindowManager && null != mFloatView) {
            mLayoutParams!!.x = lastParamX + moveX.toInt()
            mLayoutParams!!.y = lastParamY + moveY.toInt()
            mWindowManager!!.updateViewLayout(mFloatView, mLayoutParams)
        }
    }

    fun destroy() {
        mHandler.removeCallbacksAndMessages(null)
        isShowFloating = false
        mWindowManager = null
        mLayoutParams = null
        mFloatView = null
        mFWListener = null
    }

}
