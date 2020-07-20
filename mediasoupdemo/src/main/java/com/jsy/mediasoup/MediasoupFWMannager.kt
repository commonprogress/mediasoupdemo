package com.jsy.mediasoup

import android.content.Context
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.jsy.mediasoup.fw.FloatingWindowListener
import com.jsy.mediasoup.fw.FloatingWindowManager
import com.jsy.mediasoup.fw.FloatingWindowUtils
import com.jsy.mediasoup.view.AudioFWView
import com.jsy.mediasoup.view.BaseFrameLayout
import com.jsy.mediasoup.view.MeView
import com.jsy.mediasoup.view.PeerView
import com.jsy.mediasoup.vm.MeProps
import com.jsy.mediasoup.vm.PeerProps
import com.jsy.mediasoup.vm.RoomProps
import com.jsy.mediasoup.utils.LogUtils
import org.mediasoup.droid.lib.Utils
import org.mediasoup.droid.lib.model.Peer

class MediasoupFWMannager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val roomManagement: RoomManagement?,
    private val changeAndNotify: PropsChangeAndNotify?
) {
    private val TAG by lazy {
        MediasoupFWMannager::class.java.simpleName
    }

    /**
     * 当前显示悬浮窗的用户id或者SHOW_FLOAT_MODEO
     */
    var showFloatPeerId: String? = null

    /**
     * 获取悬浮窗显示的用户id
     *
     * @param peersList
     * @return
     */
    private fun getShowPeerId(peersList: List<Peer?>?): String? {
        var peersList: List<Peer?>? = peersList
        if (null == roomManagement) {
            return null
        }
        if (roomManagement.isContainsCurPeer(showFloatPeerId) && MediasoupLoaderUtils.getInstance()
                .getPeerVideoState(roomManagement.curRegister, showFloatPeerId)
        ) {
            return showFloatPeerId
        }
        if (null == peersList || peersList.isEmpty()) {
            peersList = roomManagement.curRoomPeerList
        }
        val size = peersList?.size ?: 0
        for (i in 0 until size) {
            val peer = peersList!![i]
            if (null != peer && (peer.isVideoVisible || MediasoupLoaderUtils.getInstance()
                    .getPeerVideoState(roomManagement.curRegister, peer.id))
            ) {
                return peer.id
            }
        }
        val selfId = roomManagement.selfPeerId
        if (MediasoupLoaderUtils.getInstance()
                .getPeerVideoState(roomManagement.curRegister, selfId)
        ) {
            return selfId
        }
        return if (size > 0) {
            MediasoupConstant.SHOW_FLOAT_WINDOW_MODEO
        } else null
    }

    /**
     * 判断悬浮窗是否显示中
     *
     * @return
     */
    private fun isShowFloatWindow(): Boolean {
        return FloatingWindowManager.instance.isShowFloating()
    }

    /**
     * 显示悬浮窗
     */
    fun showMediasoupFloatWindow(peersList: List<Peer?>?) {
        LogUtils.i(TAG, "==showMediasoupFloatWindow start peersList==$peersList")
        if (null != roomManagement && null != changeAndNotify
            && roomManagement!!.isRoomConnected && !roomManagement!!.isVisibleCall
            && FloatingWindowUtils.checkFloatPermission(context)
        ) {
            val showPeerId = getShowPeerId(peersList)
            LogUtils.i(
                TAG,
                "==showMediasoupFloatWindow mid showFloatPeerId==$showFloatPeerId, showPeerId:$showPeerId, selfPeerId:${roomManagement.selfPeerId}"
            )
            if (Utils.isEmptyString(showPeerId)) {
                hideMediasoupFloatWindow()
            } else if (!isShowFloatWindow() || showPeerId != showFloatPeerId) {
                if (showPeerId == MediasoupConstant.SHOW_FLOAT_WINDOW_MODEO) {
                    val roomProps: RoomProps? = changeAndNotify.getRoomPropsAndChange(
                        lifecycleOwner,
                        roomManagement.roomClient,
                        roomManagement.roomStore
                    )
                    if (null != roomProps) {
                        val audioFloatWindow = AudioFWView(context)
                        audioFloatWindow.setProps(
                            roomProps,
                            roomManagement.roomClient,
                            roomManagement.roomStore
                        )
                        FloatingWindowManager.instance.showUpdateFloatWindow(
                            context,
                            audioFloatWindow,
                            floatingWindowListener
                        )
                    }
                } else if (showPeerId == roomManagement.selfPeerId) {
                    val meProps: MeProps? = changeAndNotify.getMePropsAndChange(
                        lifecycleOwner,
                        roomManagement.roomClient,
                        roomManagement.roomStore
                    )
                    if (null != meProps) {
                        val meFloat = MeView(context)
                        meFloat.setNeatView(true)
                        meFloat.setProps(
                            meProps,
                            roomManagement.roomClient,
                            roomManagement.roomStore
                        )
                        FloatingWindowManager.instance.showVideoFloatWindow(
                            context,
                            meFloat,
                            floatingWindowListener
                        )
                    } else {
                        LogUtils.e(
                            TAG,
                            "==showMediasoupFloatWindow 2 end showFloatPeerId==$showFloatPeerId, showPeerId:$showPeerId"
                        )
                    }
                } else {
                    val peerProps: PeerProps? = changeAndNotify.getPeerPropsAndChange(
                        lifecycleOwner,
                        roomManagement.roomClient,
                        roomManagement.roomStore,
                        showPeerId
                    )
                    if (null != peerProps) {
                        val peerFloat = PeerView(context)
                        peerFloat.setNeatView(true)
                        peerFloat.setProps(
                            peerProps,
                            roomManagement.roomClient,
                            roomManagement.roomStore
                        )
                        FloatingWindowManager.instance.showVideoFloatWindow(
                            context,
                            peerFloat,
                            floatingWindowListener
                        )
                    } else {
                        LogUtils.e(
                            TAG,
                            "==showMediasoupFloatWindow 3 end showFloatPeerId==$showFloatPeerId, showPeerId:$showPeerId"
                        )
                    }
                }
                showFloatPeerId = showPeerId
            }
        }
    }

    /**
     * 悬浮窗点击监听
     */
    private val floatingWindowListener: FloatingWindowListener = object : FloatingWindowListener {
        override fun onFWClick() {
            roomManagement?.startIfCallIsActive()
        }

        override fun onDestroy(view: View?) {
            if(view is BaseFrameLayout){
                view.releaseViewData()
            }
        }
    }

    /**
     * 消除悬浮窗
     */
    fun hideMediasoupFloatWindow() {
        LogUtils.i(TAG, "==hideMediasoupFloatWindow start showFloatPeerId==$showFloatPeerId")
        showFloatPeerId = null
        if (isShowFloatWindow() && FloatingWindowUtils.checkFloatPermission(context)) {
            LogUtils.i(TAG, "==hideMediasoupFloatWindow mid==")
            FloatingWindowManager.instance.removeFloatWindowView()
        }
    }

    fun destroy() {
        FloatingWindowManager.instance.destroy()
    }
}
