package com.jsy.mediasoup.services;

import android.app.Service;
import android.arch.lifecycle.LifecycleService;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.jsy.mediasoup.MediasoupConstant;
import com.jsy.mediasoup.MediasoupLoaderUtils;
import com.jsy.mediasoup.PropsChangeAndNotify;
import com.jsy.mediasoup.RoomManagement;
import com.jsy.mediasoup.interfaces.RoomManagementCallback;
import com.jsy.mediasoup.interfaces.RoomStoreObserveCallback;
import com.jsy.mediasoup.vm.MeProps;
import com.jsy.mediasoup.vm.PeerProps;
import com.jsy.mediasoup.vm.RoomProps;
import com.jsy.mediasoup.utils.LogUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.model.Notify;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.model.Peers;

import java.util.List;

/**
 * Mediasoup 连接服务
 */
public class MediasoupService extends LifecycleService implements RoomManagementCallback {
    private static final String TAG = MediasoupService.class.getSimpleName();
    private RoomAidlInterface roomBinder;
    private RoomManagement roomManagement;
    private PropsChangeAndNotify changeAndNotify;
    private RoomProps mRoomProps;
    private boolean isBindService;
    private int roomMode;

    public MediasoupService() {
    }

    MediasoupAidlInterface.Stub mediasoupBinder = new MediasoupAidlInterface.Stub() {

        @Override
        public void onRegisterRoom(RoomAidlInterface roomInterface) throws RemoteException {
            isBindService = true;
            roomBinder = roomInterface;
            LogUtils.i(TAG, "==mediasoupBinder onRegisterRoom=null == roomBinder:" + (null == roomBinder));
            setVisibleCall(true);
            if (createMediasoupRoom()) {
                mediasoupReadyBinder(true);
            }
        }

        @Override
        public void onUnRegisterRoom() throws RemoteException {
            LogUtils.e(TAG, "==mediasoupBinder onUnRegisterRoom==isBindService:" + isBindService + ", null == roomBinder:" + (null == roomBinder));
            setVisibleCall(false);
            isBindService = false;
            roomBinder = null;
        }

        @Override
        public void onCreateMediasoupRoom() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder onCreateMediasoupRoom==");
            createMediasoupRoom();
        }

        @Override
        public void onJoinMediasoupRoom() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder onJoinMediasoupRoom==");
            joinMediasoupRoom();
        }

        @Override
        public void onResetMediasoupRoom() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder onResetMediasoupRoom==");
            resetMediasoupRoom();
        }

        @Override
        public void onDestroyMediasoupRoom() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder onDestroyMediasoupRoom==");
            destroyMediasoupRoom();
        }

        @Override
        public boolean isBindService() throws RemoteException {
            LogUtils.e(TAG, "==mediasoupBinder isBindService==isBindService:" + isBindService + ", null==roomBinder:" + (null == roomBinder));
            return null != roomBinder && isBindService;
        }

        @Override
        public boolean isReceiveCall() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder isReceiveCall==");
            return MediasoupLoaderUtils.getInstance().isReceiveCall();
        }

        @Override
        public boolean isSelfCalling() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder isSelfCalling==");
            return MediasoupLoaderUtils.getInstance().isSelfCalling();
        }

        @Override
        public boolean isOneOnOneCall() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder isGroupConv==");
            return MediasoupLoaderUtils.getInstance().isOneOnOneCall();
        }

        @Override
        public boolean isRoomConnecting() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder isRoomConnecting==");
            return null == roomManagement ? false : roomManagement.isRoomConnecting();
        }

        @Override
        public void callSelfAccept() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder callSelfAccept==");
            if (null != roomManagement) {
                roomManagement.callSelfAccept();
            }
        }

        @Override
        public void callSelfEnd() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder callSelfEnd==");
            if (null != roomManagement) {
                roomManagement.callSelfEnd();
            }
        }

        @Override
        public void callSelfCancel() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder callSelfCancel==");
            if (null != roomManagement) {
                roomManagement.callSelfCancel();
            }
        }

        @Override
        public void callSelfReject() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder callSelfReject==");
            if (null != roomManagement) {
                roomManagement.callSelfReject();
            }
        }

        @Override
        public void rejectEndCancelCall() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder callSelfReject==");
            if (null != roomManagement) {
                roomManagement.rejectEndCancelCall();
            }
        }

        @Override
        public void setVisibleCall(boolean isVisible) throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder setVisibleCall==" + isVisible);
            if (null != roomManagement) {
                roomManagement.setVisibleCall(isVisible);
            }
        }

        @Override
        public void setShareScreenIntentData(boolean isReqSuc) throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder setShareScreenIntentData==");
            if (null != roomManagement) {
                roomManagement.setShareScreenIntentData(isReqSuc);
            }
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        LogUtils.i(TAG, "==onBind==");
        isBindService = true;
        getIntentData(intent);
        return mediasoupBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        LogUtils.i(TAG, "==onRebind==null==mediasoupBinder:" + (null == mediasoupBinder) + ",null==roomBinder" + (null == roomBinder));
        isBindService = true;
        getIntentData(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.i(TAG, "==onCreate==");
        initRoomManagement();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        LogUtils.i(TAG, "==onStartCommand==");
        getIntentData(intent);
        createMediasoupRoom();
        return Service.START_REDELIVER_INTENT;
    }


    private void getIntentData(Intent intent) {
        if (null == intent) {
            return;
        }
        roomMode = intent.getIntExtra(MediasoupConstant.key_intent_roommode, MediasoupConstant.roommode_video);
    }

    private void initRoomManagement() {
        if (null == roomManagement) {
            roomManagement = new RoomManagement(this, this);
            MediasoupLoaderUtils.getInstance().setRoomManagement(roomManagement);
            roomManagement.create();
        }
    }

    /**
     * 创建房间
     */
    private boolean createMediasoupRoom() {
        LogUtils.i(TAG, "==createMediasoupRoom==null == roomBinder:" + (null == roomBinder));
        if (null != roomManagement && roomManagement.isRoomConnecting()) {
            LogUtils.i(TAG, "createMediasoupRoom RoomConnecting");
            return true;
        }
        initRoomManagement();
        if (null == changeAndNotify) {
            changeAndNotify = new PropsChangeAndNotify(this, this);
        }
        boolean isMediasoupReady = MediasoupLoaderUtils.getInstance().isMediasoupReady();
        if (isMediasoupReady && !roomManagement.isRoomClosed()) {
            return true;
        }
        roomManagement.acceptAnsweredTime();
        roomManagement.setRoomMode(roomMode);
        roomManagement.createInitRoom();
        return false;
    }

    /**
     * 重置房间
     */
    private void resetMediasoupRoom() {
        if (null == roomManagement) {
            LogUtils.i(TAG, "joinMediasoupRoom not init");
            return;
        }
        //close, dispose room related and clear store.
        destroyMediasoupRoom();
        // local config and reCreate room related.
        roomManagement.createInitRoom();

    }

    /**
     * 加入房间
     */
    private void joinMediasoupRoom() {
        if (null == roomManagement) {
            LogUtils.i(TAG, "joinMediasoupRoom not init");
            return;
        }
        if (roomManagement.isRoomConnecting()) {
            LogUtils.i(TAG, "joinMediasoupRoom RoomConnecting");
            return;
        }
        roomManagement.joinMediasoupRoom();
    }

    private void curConnectPeers(int size, Peer peer) {
        LogUtils.i(TAG, "curConnectPeers null==roomBinder:" + (null == roomBinder) + ", size:" + size);
        if (null == roomManagement) {
            LogUtils.i(TAG, "curConnectPeers not init");
            return;
        }
        if (!roomManagement.isRoomConnecting()) {
            LogUtils.i(TAG, "curConnectPeers no RoomConnecting");
            return;
        }
        if (roomManagement.isOtherJoin()) {
            if (size <= 0) {
                try {
                    MediasoupLoaderUtils.getInstance().clearAllVideoState();
                    if (null != roomBinder) {
                        roomBinder.onOtherLeave();
                    }
                    if (MediasoupLoaderUtils.getInstance().isOneOnOneCall()) {
                        roomManagement.rejectEndCancelCall();
//                        roomManagement.callSelfEnd();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (size > 0) {
                if (!roomManagement.isOtherJoin()) {
                    roomManagement.establishedJoinMediasoup();
                }
                roomManagement.setOtherJoin(true);
                if (MediasoupLoaderUtils.getInstance().isOneOnOneCall()) {
                    MediasoupLoaderUtils.getInstance().setIncomingUser(peer.getId(), peer.getClientId(), peer.getDisplayName());
                }
                try {
                    if (null != roomBinder) {
                        roomBinder.onOtherJoin();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onMediasoupReady(boolean isReady) {
        mediasoupReady(isReady);
        mediasoupReadyBinder(isReady);
    }

    @Override
    public void onConnectSuc() {
    }

    @Override
    public void onConnectFail() {
    }

    @Override
    public void onConnectDisconnected() {
    }

    @Override
    public void onConnectClose() {

    }

    @Override
    public void onJoinSuc() {

    }

    @Override
    public void onJoinFail() {

    }

    @Override
    public RoomProps getRoomProps() {
        return mRoomProps;
    }

    @Override
    public void onSelfAcceptOrJoin() {
        try {
            if (null != roomBinder) {
                roomBinder.onSelfAcceptOrJoin();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取共享屏幕需要参数
     */
    @Override
    public boolean reqShareScreenIntentData() {
        try {
            if (null != roomBinder) {
                return roomBinder.reqShareScreenIntentData();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onFinishServiceActivity() {
        try {
            if (null != roomBinder && isBindService) {
                roomBinder.onFinishServiceActivity();
            } else {
                stopSelf(-1);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param isReady 房间数据是否准备好
     */
    private void mediasoupReadyBinder(boolean isReady) {
        LogUtils.i(TAG, "mediasoupReadyBinder null==roomBinder:" + (null == roomBinder));

        boolean isReceiveCall = MediasoupLoaderUtils.getInstance().isReceiveCall();
        boolean isRoomConnecting = null == roomManagement ? false : roomManagement.isRoomConnecting();
        try {
            if (null != roomBinder) {
                roomBinder.onMediasoupReady(isReady, isReceiveCall, isRoomConnecting);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void mediasoupReady(boolean isReady) {
        if (null == roomManagement) {
            LogUtils.i(TAG, "mediasoupReady not init");
            return;
        }
        if (null == changeAndNotify) {
            changeAndNotify = new PropsChangeAndNotify(this, this);
        }
        mRoomProps = changeAndNotify.getRoomPropsAndChange(this, roomManagement.getRoomClient(), roomManagement.getRoomStore(), ediasProps -> {
            if (null != roomManagement) {
                RoomProps roomProps = (RoomProps) ediasProps;
                roomManagement.setConnectionState(null == roomProps ? RoomClient.ConnectionState.NEW : roomProps.getConnectionState().get());
            }
        });

        changeAndNotify.getMePropsAndChange(this, roomManagement.getRoomClient(), roomManagement.getRoomStore(), ediasProps -> {
            MeProps meProps = (MeProps) ediasProps;
            if (null != roomManagement && roomManagement.isRoomConnecting()) {
                MediasoupConstant.VideoState videoState = MeProps.DeviceState.ON.equals(meProps.getCamState().get()) ? MediasoupConstant.VideoState.Started : MediasoupConstant.VideoState.Stopped;
                if (!roomManagement.isOtherJoin()) {
                    videoState = MeProps.DeviceState.UNSUPPORTED.equals(meProps.getCamState().get()) ? (MediasoupLoaderUtils.getInstance().isVideoIncoming() ? MediasoupConstant.VideoState.Started : MediasoupConstant.VideoState.Stopped) : videoState;
                }
                LogUtils.i(TAG, "changeAndNotify.getMePropsAndChange isOtherJoin:" + roomManagement.isOtherJoin() + ", videoState:" + videoState + ", isVideoIncoming:" + MediasoupLoaderUtils.getInstance().isVideoIncoming() + ", meProps.getCamState:" + meProps.getCamState().get());
                MediasoupLoaderUtils.getInstance().onVideoReceiveStateChanged(roomManagement.getPeerId(), roomManagement.getClientId(), roomManagement.getDisplayName(), videoState);
            }
        });

        changeAndNotify.observePeersAndNotify(this, roomManagement.getRoomClient(), roomManagement.getRoomStore(), new RoomStoreObserveCallback() {

            @Override
            public void onObservePeers(Peers peers) {
                List<Peer> peersList = peers.getAllPeers();
                int size = null == peersList ? 0 : peersList.size();
                curConnectPeers(size, size > 0 ? peersList.get(0) : null);
                try {
                    JSONArray jsonArray = new JSONArray();
                    for (int i = 0; i < size; i++) {
                        Peer peer = peersList.get(i);
                        if (null != peer) {
                            videoReceiveState(peer);
                            JSONObject itemObject = new JSONObject();
                            itemObject.put("userid", peer.getId());
                            itemObject.put("clientid", peer.getClientId());
                            jsonArray.put(itemObject);
                        }
                    }
                    MediasoupLoaderUtils.getInstance().mediasoupUserChanged(jsonArray);
                    Logger.d(TAG, "PeersObserver, peersList.size(): " + size + ",jsonPeers:" + jsonArray.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onObserveNotify(Notify notify) {
                Logger.d(TAG, "notifyObserver, notify.getType(): " + (null == notify ? "" : notify.getType()) + ",notify.getText():" + (null == notify ? "" : notify.getText()));
            }
        });
    }

    /**
     * 同步视频状态
     *
     * @param peer
     */
    private void videoReceiveState(Peer peer) {
        if (null == roomManagement || null == changeAndNotify || !roomManagement.isRoomConnecting()) {
            return;
        }
        changeAndNotify.getPeerPropsAndChange(this, roomManagement.getRoomClient(), roomManagement.getRoomStore(), peer, ediasProps -> {
            if (null != roomManagement && roomManagement.isRoomConnecting()) {
                PeerProps peerProps = (PeerProps) ediasProps;
                Peer curPeer = null == peerProps ? null : (Peer) peerProps.getPeer().get();
                Peers curPeers = null == roomManagement ? null : roomManagement.getCurRoomPeers();
                if (null != curPeer && null != curPeers && curPeers.isContainsCurPeer(curPeer.getId())) {
                    MediasoupConstant.VideoState videoState = peerVideoVisible(peerProps) ? MediasoupConstant.VideoState.Started : MediasoupConstant.VideoState.Stopped;
//                LogUtils.i(TAG, "changeAndNotify.getPeerPropsAndChange peer:" + peer.getId() + ", videoState:" + videoState);
                    MediasoupLoaderUtils.getInstance().onVideoReceiveStateChanged(peer.getId(), peer.getClientId(), peer.getDisplayName(), videoState);
                    updatePeerVideoAudioState(peerProps, curPeer);
                } else {
//                String peerId = null != curPeer ? curPeer.getId() : (null != peer ? peer.getId() : "");
//                String clientId = null != curPeer ? curPeer.getClientId() : (null != peer ? peer.getClientId() : "");
//                MediasoupLoaderUtils.getInstance().removePeerVideoState(peerId, clientId);
                }
            } else {
//                MediasoupLoaderUtils.getInstance().clearAllVideoState();
            }
        });
    }

    /**
     * 更新当前peer 视频音频状态
     *
     * @param peerProps
     * @return
     */
    private boolean updatePeerVideoAudioState(PeerProps peerProps, Peer peer) {
        boolean isVideoVisible = peerVideoVisible(peerProps);
        boolean isAudioEnabled = peerAudioVisible(peerProps);
        boolean isSameVideoState = isVideoVisible == peer.isVideoVisible();
        boolean isSameAudioState = isAudioEnabled == peer.isAudioEnabled();
        if (!isSameVideoState || !isSameAudioState) {
            LogUtils.i(TAG, "getPeerPropsAndChange updatePeerVideoAudioState isVideoVisible:" + isVideoVisible + ",isSameVideoState:" + isSameVideoState + ",isAudioEnabled:" + isAudioEnabled + ",isSameAudioState:" + isSameAudioState);
            roomManagement.getRoomStore().updatePeerVideoAudioState(peer.getId(), isVideoVisible, isAudioEnabled);
            return true;
        }
        return false;
    }

    private boolean peerVideoVisible(PeerProps peerProps) {
//        LogUtils.i(TAG, "getPeerPropsAndChange peerVideoVisible getVideoVisible:" + peerProps.getVideoVisible().get() + ", getVideoTrack:" + peerProps.getVideoTrack().get() + ", getVideoScore:" + peerProps.getVideoScore().get()
//            + ", getVideoRtpParameters:" + peerProps.getVideoRtpParameters().get() + ", getVideoProducerId:" + peerProps.getVideoProducerId().get());
        return peerProps.getVideoVisible().get()
            || null != peerProps.getVideoTrack().get()
            || null != peerProps.getVideoScore().get()
            || null != peerProps.getVideoRtpParameters().get()
            || null != peerProps.getVideoProducerId().get();
    }

    private boolean peerAudioVisible(PeerProps peerProps) {
//        LogUtils.i(TAG, "getPeerPropsAndChange peerAudioVisible getAudioEnabled:" + peerProps.getAudioEnabled().get() + ", getAudioTrack:" + peerProps.getAudioTrack().get() + ", getVideoScore:" + peerProps.getAudioScore().get()
//            + ", getAudioRtpParameters:" + peerProps.getAudioRtpParameters().get() + ", getAudioProducerId:" + peerProps.getAudioProducerId().get());
        return peerProps.getAudioEnabled().get()
            || null != peerProps.getAudioTrack().get()
            || null != peerProps.getAudioScore().get()
            || null != peerProps.getAudioRtpParameters().get()
            || null != peerProps.getAudioProducerId().get();
    }

    /**
     * 销毁房间
     */
    private void destroyMediasoupRoom() {
        if (null != roomManagement) {
            roomManagement.closeWebSocketDestroyRoom(false);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtils.e(TAG, "==onUnbind==");
        if (null != roomManagement) {
            roomManagement.setVisibleCall(false);
        }
        isBindService = false;
        roomBinder = null;
//        return super.onUnbind(intent);
        return true;
    }

    @Override
    public void onDestroy() {
        LogUtils.e(TAG, "==onDestroy 4 start close==");
        super.onDestroy();
        destroy();
        LogUtils.e(TAG, "==onDestroy 4 end close==");
    }

    private void destroy() {
        if (null != changeAndNotify) {
            changeAndNotify.destroy();
            changeAndNotify = null;
            mRoomProps = null;
        }
        if (null != roomManagement) {
            roomManagement.destroy();
            roomManagement = null;
        }
    }
}
