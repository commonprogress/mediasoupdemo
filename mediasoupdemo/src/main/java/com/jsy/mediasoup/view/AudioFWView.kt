package com.jsy.mediasoup.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.jsy.mediasoup.MediasoupLoaderUtils
import com.jsy.mediasoup.R
import com.jsy.mediasoup.utils.LogUtils
import com.jsy.mediasoup.vm.RoomProps
import org.mediasoup.droid.lib.RoomClient
import org.mediasoup.droid.lib.lv.RoomStore

class AudioFWView : BaseFrameLayout {
    private var callTimingText: TextView? = null
    private var mRoomProps: RoomProps? = null
    private var mRoomClient: RoomClient? = null
    private var mRoomStore: RoomStore? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun addChildRootView(): View? {
        return LayoutInflater.from(mContext).inflate(R.layout.view_fw_audio, this, true)
    }

    override fun initView() {
        callTimingText = rootView.findViewById(R.id.call_timing_text)
    }

    override fun loadViewData(isAgain: Boolean) {
        if (isReleaseView) {
            return
        }
        callTimingText?.setText(R.string.system_notification__ongoing)
    }

    fun setProps(roomProps: RoomProps?) {
        setProps(
            roomProps,
            MediasoupLoaderUtils.getInstance().roomClient,
            MediasoupLoaderUtils.getInstance().roomStore
        )
    }

    fun setProps(props: RoomProps?, roomClient: RoomClient?, roomStore: RoomStore?) {
        LogUtils.i(TAG, "setProps,mediasoup")
        this.mRoomProps = props
        this.mRoomClient = roomClient
        this.mRoomStore = roomStore
        updateCallTiming(
            props,
            roomClient?.isConnected ?: false
        )
        props?.setOnPropsLiveDataChange {
            if (!isReleaseView) {
                val roomProps: RoomProps = it as RoomProps
                updateCallTiming(
                    roomProps,
                    roomClient?.isConnected ?: false
                )
            }
        }
    }

    private fun updateCallTiming(props: RoomProps?, isConnected: Boolean) {
//        LogUtils.i(TAG, "updateCallTiming,mediasoup  isConnected:$isConnected")
        callTimingText?.text = props?.callTiming?.get() ?: ""
    }

    override fun releaseViewData() {

    }

    companion object {
        private val TAG: String by lazy { AudioFWView::class.java.simpleName }
    }
}
