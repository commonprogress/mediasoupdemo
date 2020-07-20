package com.jsy.mediasoup.services;

import android.app.Service;

import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleService;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;

import com.jsy.mediasoup.MediasoupConstant;
import com.jsy.mediasoup.MediasoupFWMannager;
import com.jsy.mediasoup.MediasoupLoaderUtils;
import com.jsy.mediasoup.PropsChangeAndNotify;
import com.jsy.mediasoup.RoomManagement;
import com.jsy.mediasoup.interfaces.RoomManagementCallback;
import com.jsy.mediasoup.interfaces.RoomStoreObserveCallback;
import com.jsy.mediasoup.utils.LogUtils;
import com.jsy.mediasoup.vm.MeProps;
import com.jsy.mediasoup.vm.PeerProps;
import com.jsy.mediasoup.vm.RoomProps;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mediasoup.droid.lib.RoomConstant;
import org.mediasoup.droid.lib.model.Notify;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.model.Peers;
import org.threeten.bp.Instant;

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
    private MeProps mMeProps;
    private boolean isBindService;
    private boolean isStartJoin;
    private int roomMode;
    private Handler mHandler = new Handler();
    /**
     * 当前显示悬浮窗的用户id或者SHOW_FLOAT_MODEO
     */
    private MediasoupFWMannager floatWindowMannager;

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
        public String getCurRegister() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder getCurRegister==");
            return MediasoupService.this.getCurRegister();
        }

        @Override
        public boolean isBindService() throws RemoteException {
            LogUtils.e(TAG, "==mediasoupBinder isBindService==isBindService:" + isBindService + ", null==roomBinder:" + (null == roomBinder));
            return null != roomBinder && isBindService;
        }

        @Override
        public boolean isMediasoupReady() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder isMediasoupReady==");
            return MediasoupLoaderUtils.getInstance().isMediasoupReady(getCurRegister());
        }

        @Override
        public boolean isReceiveCall() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder isReceiveCall==");
            return MediasoupLoaderUtils.getInstance().isReceiveCall(getCurRegister());
        }

        @Override
        public boolean isSelfCalling() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder isSelfCalling==");
            return MediasoupLoaderUtils.getInstance().isSelfCalling(getCurRegister());
        }

        @Override
        public boolean isOneOnOneCall() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder isGroupConv==");
            return MediasoupLoaderUtils.getInstance().isOneOnOneCall(getCurRegister());
        }

        @Override
        public boolean isRoomConnecting() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder isRoomConnecting==");
            return MediasoupService.this.isRoomConnecting();
        }

        @Override
        public boolean isRoomConnected() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder isRoomConnected==");
            return MediasoupService.this.isRoomConnected();
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
                roomManagement.callSelfEnd(MediasoupConstant.ClosedReason.Normal);
            }
        }

        @Override
        public void callSelfCancel() throws RemoteException {
            LogUtils.i(TAG, "==mediasoupBinder callSelfCancel==");
            if (null != roomManagement) {
                roomManagement.callSelfCancel(MediasoupConstant.ClosedReason.Canceled);
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
            if (isVisible) {
                hideMediasoupFloatWindow();
            } else {
                showMediasoupFloatWindow(null);
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

    private String getCurRegister() {
        return null != roomManagement ? roomManagement.getCurRegister() : MediasoupLoaderUtils.getInstance().getCurRegister();
    }

    private boolean isRoomConnected() {
        return null != roomManagement && roomManagement.isRoomConnected();
    }

    private boolean isRoomConnecting() {
        return null != roomManagement && roomManagement.isRoomConnecting();
    }

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
        if (!createMediasoupRoom() && isStartJoin) {
            joinMediasoupRoom();
        }
        return Service.START_REDELIVER_INTENT;
    }


    private void getIntentData(Intent intent) {
        if (null == intent) {
            return;
        }
        isStartJoin = intent.getBooleanExtra(MediasoupConstant.key_service_join, false);
        roomMode = intent.getIntExtra(MediasoupConstant.key_intent_roommode, MediasoupConstant.roommode_video);
    }

    private void initRoomManagement() {
        if (null == roomManagement) {
            roomManagement = new RoomManagement(this, this);
            MediasoupLoaderUtils.getInstance().setRoomManagement(roomManagement);
            roomManagement.create();
        } else {
            MediasoupLoaderUtils.getInstance().setRoomManagement(roomManagement);
        }
    }

    /**
     * 创建房间
     */
    private boolean createMediasoupRoom() {
        LogUtils.i(TAG, "==createMediasoupRoom==null == roomBinder:" + (null == roomBinder));
        if (isRoomConnecting()) {
            LogUtils.i(TAG, "createMediasoupRoom RoomConnecting");
            return true;
        }
        initRoomManagement();
        if (null == changeAndNotify) {
            changeAndNotify = new PropsChangeAndNotify(this, this);
        }
        boolean isMediasoupReady = MediasoupLoaderUtils.getInstance().isMediasoupReady(getCurRegister());
        if (isMediasoupReady && !roomManagement.isRoomClosed()) {
            return true;
        }
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
        if (null == roomManagement || isRoomConnecting()) {
            LogUtils.e(TAG, "joinMediasoupRoom not init roomManagement is null:" + (null == roomManagement));
            return;
        }
        MediasoupLoaderUtils.getInstance().setRoomManagement(roomManagement);
        roomManagement.joinMediasoupRoom();
    }

    /**
     * 房间内连接用户的人数变化
     *
     * @param peerSize
     * @param peer
     */
    private void curConnectPeers(int peerSize, Peer peer) {
        if (!isRoomConnecting()) {
            LogUtils.i(TAG, "curConnectPeers not init :" + (null == roomManagement) + ", peerSize:" + peerSize);
            return;
        }
        LogUtils.i(TAG, "curConnectPeers null==roomBinder:" + (null == roomBinder) + ", peerSize:" + peerSize + ", isOtherJoin:" + roomManagement.isOtherJoin());
        try {
            if (roomManagement.isOtherJoin()) {
                if (null != roomBinder) {
                    roomBinder.onOtherUpdate(peerSize);
                }
                if (peerSize <= 0) {
                    MediasoupLoaderUtils.getInstance().clearAllVideoState(getCurRegister());
                    onAllLeaveRoom();
                    allLeaveDelayedWait(MediasoupLoaderUtils.getInstance().isOneOnOneCall(getCurRegister()) ? MediasoupConstant.mediasoup_connect_wait / 3 : MediasoupConstant.mediasoup_connect_wait);
                }
            } else {
                if (peerSize > 0) {
                    if (!roomManagement.isOtherJoin()) {
                        roomManagement.establishedJoinMediasoup();
                        roomManagement.setOtherJoin(true);
                        if (MediasoupLoaderUtils.getInstance().isOneOnOneCall(getCurRegister())) {
                            MediasoupLoaderUtils.getInstance().setIncomingUser(getCurRegister(), peer.getId(), peer.getClientId(), peer.getDisplayName());
                        }
                        if (null != roomBinder) {
                            roomBinder.onOtherJoin();
                        }
                    }
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 所有人离开后延迟坚持是否重连中
     */
    private void allLeaveDelayedWait(long delayMillis) {
//        if (!MediasoupLoaderUtils.getInstance().isOneOnOneCall(getCurRegister())) {
//            return;
//        }
        //                        roomManagement.callSelfEnd();
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                boolean isConnected = null != roomManagement && roomManagement.isConnected();
                int peerSize = null != roomManagement ? roomManagement.getCurRoomPeerSize() : 0;
                LogUtils.i(TAG, "allLeaveDelayedWait roomManagement==roomManagement:" + (null == roomManagement) + ", isConnected:" + isConnected + ", peerSize:" + peerSize + ",delayMillis:" + delayMillis);
                if (!isConnected || peerSize <= 0) {
                    roomManagement.rejectEndCancelCall();
                }
            }
        }, delayMillis);
    }

    @Override
    public void onMediasoupReady(boolean isReady) {
        mediasoupReady(isReady);
        mediasoupReadyBinder(isReady);
    }

    @Override
    public void onConnectSuc(boolean isJoinLast) {
        LogUtils.i(TAG, "onConnectSuc isJoinLast:" + isJoinLast);
    }

    @Override
    public void onConnectFail(boolean isJoinLast) {
        LogUtils.i(TAG, "onConnectFail isJoinLast:" + isJoinLast);
    }

    @Override
    public void onConnectDisconnected(boolean isJoinLast) {
        LogUtils.i(TAG, "onConnectDisconnected isJoinLast:" + isJoinLast);
    }

    @Override
    public void onConnectClose(boolean isJoinLast) {
        LogUtils.i(TAG, "onConnectClose isJoinLast:" + isJoinLast);
        hideMediasoupFloatWindow();
    }

    @Override
    public void onJoinSuc(int existPeer) {
        LogUtils.i(TAG, "onJoinSuc existPeer:" + existPeer);
        if (MediasoupLoaderUtils.getInstance().isReceiveCall(getCurRegister())) {
            if (existPeer <= 0) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (null == roomManagement || !roomManagement.isOtherJoin()) {
                            onAllLeaveRoom();
                        }
                    }
                }, MediasoupConstant.mediasoup_connect_wait);
            }
        }
    }

    /**
     * p2p 连接模式 加入或者连接失败
     */
    @Override
    public void onP2PJoinFail() {
        LogUtils.i(TAG, "onP2PJoinFail");
    }

    /**
     * mediasoup 连接模式 加入房间失败
     */
    @Override
    public void onMediasoupJoinFail() {
        LogUtils.i(TAG, "onMediasoupJoinFail");
    }

    @Override
    public RoomProps getRoomProps() {
        return mRoomProps;
    }

    @Override
    public MeProps getMeProps() {
        return mMeProps;
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

    @Override
    public void onDelayedCheckRoom() {
        allLeaveDelayedWait(MediasoupLoaderUtils.getInstance().isOneOnOneCall(getCurRegister()) ? MediasoupConstant.mediasoup_delayed_check : MediasoupConstant.mediasoup_connect_wait);
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

    /**
     * @param isReady 房间数据是否准备好
     */
    private void mediasoupReadyBinder(boolean isReady) {
        LogUtils.i(TAG, "mediasoupReadyBinder null==roomBinder:" + (null == roomBinder));

        boolean isReceiveCall = MediasoupLoaderUtils.getInstance().isReceiveCall(getCurRegister());
        try {
            if (null != roomBinder) {
                roomBinder.onMediasoupReady(isReady, isReceiveCall, isRoomConnecting());
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
                RoomConstant.ConnectionState connectionState = null == roomProps ? RoomConstant.ConnectionState.NEW : roomProps.getConnectionState().get();
                MediasoupConstant.NetworkMode networkMode = null == roomProps ? MediasoupConstant.NetworkMode.UNKNOWN : roomProps.getNetworkMode().get();
                String callTiming = null == roomProps ? "" : roomProps.getCallTiming().get();
                roomManagement.setConnectionState(connectionState);
                roomManagement.setNetworkMode(networkMode);
                try {
                    if (null != roomBinder) {
                        roomBinder.setConnectionState(connectionState.getIndex());
                        roomBinder.setNetworkMode(networkMode.getIndex());
                        roomBinder.setCallTiming(callTiming);
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        mMeProps = changeAndNotify.getMePropsAndChange(this, roomManagement.getRoomClient(), roomManagement.getRoomStore(), ediasProps -> {
            MeProps meProps = (MeProps) ediasProps;
            if (null != meProps && isRoomConnected()) {
                MeProps.DeviceState camState = meProps.getCamState().get();
                boolean isVideoBeingSent = MeProps.DeviceState.ON.equals(camState) ? true : false;//判断 摄像头打开或关闭
                boolean isVideo = MediasoupLoaderUtils.getInstance().isMediasoupVideoState(getCurRegister());
                MediasoupConstant.VideoState videoState = isVideoBeingSent ? MediasoupConstant.VideoState.Started : (isVideo ? (!isRoomConnected() ? MediasoupConstant.VideoState.Paused : MediasoupLoaderUtils.getInstance().getMediasoupVideoState(getCurRegister())) : MediasoupConstant.VideoState.Stopped);
                if (!roomManagement.isOtherJoin() && MeProps.DeviceState.UNSUPPORTED.equals(camState)) {
                    videoState = isVideo ? MediasoupConstant.VideoState.Started : MediasoupConstant.VideoState.Stopped;
                }
                MediasoupLoaderUtils.getInstance().onVideoReceiveStateChanged(getCurRegister(), roomManagement.getSelfPeerId(), roomManagement.getSelfClientId(), roomManagement.getSelfName(), videoState);
                boolean isAudioBeingSent = MeProps.DeviceState.ON.equals(meProps.getMicState().get()) ? true : false;//判断 麦克风打开或关闭
                LogUtils.i(TAG, "changeAndNotify.getMePropsAndChange isOtherJoin:" + roomManagement.isOtherJoin() + ", videoState:" + videoState + ", isVideo:" + isVideo + ", me CamState:" + camState + ", isVideoBeingSent:" + isVideoBeingSent + ", isAudioBeingSent:" + isAudioBeingSent);
                checkVideoPeersSize(roomManagement.getCurRoomPeerList(), isVideoBeingSent);
                roomManagement.cameraOpenState(null == meProps.getMe() ? true : meProps.getMe().get().isCameraChangeFail());
            }
        });

        changeAndNotify.observePeersAndNotify(this, roomManagement.getRoomClient(), roomManagement.getRoomStore(), new RoomStoreObserveCallback() {

            @Override
            public void onObservePeers(Peers peers) {
                List<Peer> peersList = peers.getAllPeers();
                int peerSize = null == peersList ? 0 : peersList.size();
                LogUtils.d(TAG, "changeAndNotify.observePeersAndNotify onObservePeers isBindService：" + isBindService + ", peerSize: " + peerSize);
                curConnectPeers(peerSize, peerSize > 0 ? peersList.get(0) : null);
                updateMediasoupUser(peerSize, peersList);
                showMediasoupFloatWindow(peersList);
                if (null != mMeProps) {
                    boolean isVideoBeingSent = MeProps.DeviceState.ON.equals(mMeProps.getCamState().get()) ? true : false;//判断 摄像头打开或关闭
                    checkVideoPeersSize(peersList, isVideoBeingSent);
                }
            }

            @Override
            public void onObserveNotify(Notify notify) {
                LogUtils.d(TAG, "changeAndNotify.observePeersAndNotify notifyObserver isBindService：" + isBindService + ", notify.getType(): " + (null == notify ? "" : notify.getType()) + ",notify.getText():" + (null == notify ? "" : notify.getText()));
                if (!isBindService && !isRoomConnected()) {
                    stopSelf(-1);
                }
            }
        });
    }

    /**
     * 判断视频个数 改变摄像头采集的分辨率
     */
    private void checkVideoPeersSize(List<Peer> peersList, boolean isVideoBeingSent) {
        int peerSize = null == peersList ? 0 : peersList.size();
        int videoSize = isVideoBeingSent ? 1 : 0;
        for (int i = 0; i < peerSize; i++) {
            Peer peer = peersList.get(i);
            if (null != peer && (peer.isVideoVisible() || MediasoupLoaderUtils.getInstance().getPeerVideoState(getCurRegister(), peer.getId()))) {
                videoSize++;
            }
        }
        LogUtils.d(TAG, "checkVideoPeersSize, peerSize: " + peerSize + ",videoSize:" + videoSize + ",isVideoBeingSent:" + isVideoBeingSent);
        if (null != roomManagement) {
            roomManagement.changeCaptureFormat(videoSize);
        }
    }

    /**
     * @param peerSize
     * @param peersList
     */
    private void updateMediasoupUser(int peerSize, List<Peer> peersList) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (int i = 0; i < peerSize; i++) {
                Peer peer = peersList.get(i);
                if (null != peer) {
                    videoReceiveState(peer);
                    JSONObject itemObject = new JSONObject();
                    itemObject.put("userid", peer.getId());
                    itemObject.put("clientid", peer.getClientId());
                    jsonArray.put(itemObject);
                }
            }
            MediasoupLoaderUtils.getInstance().mediasoupUserChanged(getCurRegister(), jsonArray);
            LogUtils.d(TAG, "updateMediasoupUser, peerSize: " + peerSize + ",jsonPeers:" + jsonArray.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 同步视频状态
     *
     * @param peer
     */
    private void videoReceiveState(Peer peer) {
        if (null == changeAndNotify || !isRoomConnecting() || null == peer) {
            return;
        }
        changeAndNotify.getPeerPropsAndChange(this, roomManagement.getRoomClient(), roomManagement.getRoomStore(), peer.getId(), ediasProps -> {
            PeerProps peerProps = (PeerProps) ediasProps;
            if (null != peerProps && isRoomConnecting()) {
                Peer curPeer = null == peerProps ? null : (Peer) peerProps.getPeer().get();
                String peerId = null != curPeer ? curPeer.getId() : (null != peer ? peer.getId() : "");
                String clientId = null != curPeer ? curPeer.getClientId() : (null != peer ? peer.getClientId() : "");
                String displayName = null != curPeer ? curPeer.getDisplayName() : (null != peer ? peer.getDisplayName() : "");
                if (null != roomManagement && roomManagement.isContainsCurPeer(peerId)) {
                    MediasoupConstant.VideoState videoState = peerVideoVisible(peerProps) ? MediasoupConstant.VideoState.Started : MediasoupConstant.VideoState.Stopped;
                    LogUtils.i(TAG, "changeAndNotify.getPeerPropsAndChange 1 peerId:" + peerId + ", displayName:" + displayName + ", videoState:" + videoState + ", curPeer==null:" + (curPeer == null));
                    MediasoupLoaderUtils.getInstance().onVideoReceiveStateChanged(getCurRegister(), peerId, clientId, displayName, videoState);
                    updatePeerVideoAudioState(peerProps, curPeer);
                } else {
                    LogUtils.i(TAG, "changeAndNotify.getPeerPropsAndChange 2 peerid:" + peerId + ", displayName:" + displayName + ", curPeer==null:" + (curPeer == null));
                    MediasoupLoaderUtils.getInstance().onVideoReceiveStateChanged(getCurRegister(), peerId, clientId, displayName, MediasoupConstant.VideoState.Stopped);
                }
            } else {
                if (null != peer) {
                    MediasoupLoaderUtils.getInstance().onVideoReceiveStateChanged(getCurRegister(), peer.getId(), peer.getClientId(), peer.getDisplayName(), MediasoupConstant.VideoState.Stopped);
                }
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
        if (null == peer) {
            return false;
        }
        boolean isVideoVisible = peerVideoVisible(peerProps);
        boolean isAudioEnabled = peerAudioVisible(peerProps);
        boolean isSameVideoState = isVideoVisible == peer.isVideoVisible();
        boolean isSameAudioState = isAudioEnabled == peer.isAudioEnabled();
        if (!isSameVideoState || !isSameAudioState) {
            LogUtils.i(TAG, "getPeerPropsAndChange updatePeerVideoAudioState isVideoVisible:" + isVideoVisible + ",isSameVideoState:" + isSameVideoState + ",isAudioEnabled:" + isAudioEnabled + ",isSameAudioState:" + isSameAudioState);
            roomManagement.updatePeerVideoAudioState(peer.getId(), isVideoVisible, isAudioEnabled);
            return true;
        }
        return false;
    }

    /**
     * 判断视频是否可见
     *
     * @param peerProps
     * @return
     */
    private boolean peerVideoVisible(PeerProps peerProps) {
//        LogUtils.i(TAG, "getPeerPropsAndChange peerVideoVisible getVideoVisible:" + peerProps.getVideoVisible().get() + ", getVideoTrack:" + peerProps.getVideoTrack().get() + ", getVideoScore:" + peerProps.getVideoScore().get()
//            + ", getVideoRtpParameters:" + peerProps.getVideoRtpParameters().get() + ", getVideoProducerId:" + peerProps.getVideoProducerId().get());
        return peerProps.getVideoVisible().get()
            || null != peerProps.getVideoTrack().get()
//            || null != peerProps.getVideoScore().get()
            || null != peerProps.getVideoRtpParameters().get()
            || null != peerProps.getVideoProducerId().get();
    }

    /**
     * 判断音频是否可用
     *
     * @param peerProps
     * @return
     */
    private boolean peerAudioVisible(PeerProps peerProps) {
//        LogUtils.i(TAG, "getPeerPropsAndChange peerAudioVisible getAudioEnabled:" + peerProps.getAudioEnabled().get() + ", getAudioTrack:" + peerProps.getAudioTrack().get() + ", getVideoScore:" + peerProps.getAudioScore().get()
//            + ", getAudioRtpParameters:" + peerProps.getAudioRtpParameters().get() + ", getAudioProducerId:" + peerProps.getAudioProducerId().get());
        return peerProps.getAudioEnabled().get()
            || null != peerProps.getAudioTrack().get()
//            || null != peerProps.getAudioScore().get()
            || null != peerProps.getAudioRtpParameters().get()
            || null != peerProps.getAudioProducerId().get();
    }

    /**
     * socket连接成功或者响应成功
     */
    @Override
    public void onAnsweredState() {

    }

    /**
     * 建立通话或者已经有用户加入房间
     *
     * @param estabTime
     */
    @Override
    public void onEstablishedState(Instant estabTime) {

    }

    /**
     * 所以人已经离开
     */
    @Override
    public void onAllLeaveRoom() {
        if (null != roomBinder) {
            try {
                roomBinder.onAllLeaveRoom();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            roomManagement.rejectEndCancelCall();
        }
    }

    /**
     * 关闭了通话
     */
    @Override
    public void onClosedState() {

    }

    @Override
    public void onFinishServiceActivity() {
        LogUtils.i(TAG, "onFinishServiceActivity null==roomBinder:" + (null == roomBinder));
        try {
            hideMediasoupFloatWindow();
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
     * 销毁房间
     */
    private void destroyMediasoupRoom() {
        hideMediasoupFloatWindow();
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
        mHandler.removeCallbacksAndMessages(null);
        destroy();
        System.gc();
        LogUtils.e(TAG, "==onDestroy 4 end close==");
    }

    private void destroy() {
        if (null != changeAndNotify) {
            changeAndNotify.destroy();
            changeAndNotify = null;
            mRoomProps = null;
            mMeProps = null;
        }
        if (null != roomManagement) {
            roomManagement.destroy();
            roomManagement = null;
        }
        hideMediasoupFloatWindow();
        if(null != floatWindowMannager){
            floatWindowMannager.destroy();
        }
    }

    /**
     * 显示悬浮窗
     */
    private void showMediasoupFloatWindow(List<Peer> peersList) {
        LogUtils.i(TAG, "==showMediasoupFloatWindow start peersList==" + (null != peersList ? peersList.size() : "null"));
        if (null == floatWindowMannager) {
            floatWindowMannager = new MediasoupFWMannager(this, this, roomManagement, changeAndNotify);
        }
        floatWindowMannager.showMediasoupFloatWindow(peersList);
    }

    /**
     * 消除悬浮窗
     */
    private void hideMediasoupFloatWindow() {
        LogUtils.i(TAG, "==hideMediasoupFloatWindow==");
        if (null != floatWindowMannager) {
            floatWindowMannager.hideMediasoupFloatWindow();
        }
    }
}
