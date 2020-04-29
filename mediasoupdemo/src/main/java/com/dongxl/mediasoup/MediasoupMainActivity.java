package com.dongxl.mediasoup;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.jsy.mediasoup.MediasoupConstant;
import com.jsy.mediasoup.MediasoupLoaderUtils;
import com.jsy.mediasoup.MediasoupManagement;
import com.jsy.mediasoup.PropsChangeAndNotify;
import com.jsy.mediasoup.R;
import com.jsy.mediasoup.interfaces.PropsLiveDataChange;
import com.jsy.mediasoup.interfaces.RoomStoreObserveCallback;
import com.jsy.mediasoup.services.MediasoupAidlInterface;
import com.jsy.mediasoup.services.MediasoupService;
import com.jsy.mediasoup.services.RoomAidlInterface;
import com.jsy.mediasoup.utils.LogUtils;
import com.jsy.mediasoup.vm.EdiasProps;
import com.jsy.mediasoup.vm.MeProps;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.Utils;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Info;
import org.mediasoup.droid.lib.model.Me;
import org.mediasoup.droid.lib.model.Notify;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.model.Peers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediasoupMainActivity extends AppCompatActivity {
    private static final String TAG = MediasoupMainActivity.class.getSimpleName();
    private EditText roomNameEdit;
    private GridLayout gridLayout;
    private CardView cardView;
    private RecyclerView recyclerView;
    private PropsChangeAndNotify changeAndNotify;
    private int roomMode;
    private MediasoupAidlInterface mediasoupBinder;
    private ConnectPeerAdapter connectPeerAdapter;

    ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mediasoupBinder = MediasoupAidlInterface.Stub.asInterface(service);
            LogUtils.i(TAG, "==onServiceConnected mediasoup null == mediasoupBinder:" + (null == mediasoupBinder));
            try {
                mediasoupBinder.onRegisterRoom(roomBinder);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtils.i(TAG, "==onServiceDisconnected mediasoup null == mediasoupBinder:" + (null == mediasoupBinder));
            try {
                if (null != mediasoupBinder) {
                    mediasoupBinder.onUnRegisterRoom();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            mediasoupBinder = null;
        }
    };

    RoomAidlInterface.Stub roomBinder = new RoomAidlInterface.Stub() {

        @Override
        public void onMediasoupReady(boolean isReady, boolean isReceiveCall, boolean isConnecting) throws RemoteException {
            LogUtils.i(TAG, "==roomBinder mediasoup null == mediasoupBinder:" + (null == mediasoupBinder) + ",isReady:" + isReady + ",isReceiveCall:" + isReceiveCall + ",isConnecting:" + isConnecting);
            if (isReceiveCall && !isConnecting) { //接收邀请 且不是通话连接中
            } else { //通话连接中 或者发起邀请
                onSelfAcceptOrJoin();
            }
        }

        @Override
        public void onSelfAcceptOrJoin() throws RemoteException {
            LogUtils.i(TAG, "==roomBinder mediasoup onSelfAccept==");
            initMediasoupView();
            checkPermissionAndJoin();
        }

        @Override
        public void onOtherJoin() throws RemoteException {
            LogUtils.i(TAG, "==roomBinder mediasoup onOtherJoin==");
        }

        @Override
        public void onOtherLeave() throws RemoteException {
            LogUtils.i(TAG, "==roomBinder mediasoup onOtherLeave==");
        }

        @Override
        public void onFinishServiceActivity() throws RemoteException {
            LogUtils.i(TAG, "==roomBinder mediasoup onEndCall==");
            finishServiceActivity(true);
        }
    };

    private List<Info> curPeerUsers;
    private Map<String, View> viewMap = new HashMap<>();

    private void initMediasoupView() {
        if (null == changeAndNotify) {
            changeAndNotify = new PropsChangeAndNotify(this, this);
        }
        getViewModelStore().clear();
        boolean isMediasoupReady = MediasoupLoaderUtils.getInstance().isMediasoupReady();
        if (!isMediasoupReady || null == changeAndNotify) {
            return;
        }
        RoomClient mRoomClient = MediasoupLoaderUtils.getInstance().getRoomClient();
        RoomStore mRoomStore = MediasoupLoaderUtils.getInstance().getRoomStore();
        MeProps mMeProps = changeAndNotify.getMePropsAndChange(this, mRoomClient, mRoomStore, new PropsLiveDataChange() {
            @Override
            public void onDataChanged(EdiasProps ediasProps) {
                MeProps meProps = (MeProps) ediasProps;
                if (MediasoupLoaderUtils.getInstance().isRoomConnecting()) {
                    boolean isAudioBeingSent = MeProps.DeviceState.ON.equals(meProps.getMicState().get()) ? true : false;
                    boolean isVideoBeingSent = MeProps.DeviceState.ON.equals(meProps.getCamState().get()) ? true : false;
                    List<Info> peerUsers = (null == curPeerUsers || curPeerUsers.isEmpty()) ? getMeInfo(meProps) : curPeerUsers;
                    refreshMediasoupView(meProps.getMe().get().getId(), isVideoBeingSent, peerUsers);
                }
            }
        });
        changeAndNotify.observePeersAndNotify(this, mRoomClient, mRoomStore, new RoomStoreObserveCallback() {
            @Override
            public void onObservePeers(Peers peers) {
                if (MediasoupLoaderUtils.getInstance().isRoomConnecting()) {
                    List<Peer> peersList = null != peers ? peers.getAllPeers() : null;
                    int size = null == peersList ? 0 : peersList.size();
                    List<Info> peerUsers = getMeInfo(mMeProps);
                    if (size > 0) {
                        peerUsers.addAll(peersList);
                    }
                    boolean isVideoBeingSent = MeProps.DeviceState.ON.equals(mMeProps.getCamState().get()) ? true : false;
                    refreshMediasoupView(mMeProps.getMe().get().getId(), isVideoBeingSent, peerUsers);
                    setConnectPeer(peersList, isVideoBeingSent);
                }
            }

            @Override
            public void onObserveNotify(Notify notify) {
                if (notify != null) {
                    if ("error".equals(notify.getType())) {
                        Toast toast = Toast.makeText(MediasoupMainActivity.this, notify.getText(), notify.getTimeout());
                        TextView toastMessage = toast.getView().findViewById(android.R.id.message);
                        toastMessage.setTextColor(Color.RED);
                        toast.show();
                    } else {
                        Toast.makeText(MediasoupMainActivity.this, notify.getText(), notify.getTimeout()).show();
                    }
                }
            }
        });
    }

    public List<Info> getMeInfo(MeProps meProps) {
        List<Info> infos = new ArrayList<>();
        infos.add(meProps.getMe().get());
        return infos;
    }

    private View createMediasoupView(Info info, boolean isSelf) {
        if (isSelf) {
            return new UserMediasoupView.SelfMediasoupView(this, this, (Me) info, changeAndNotify).addChildView();
        } else {
            return new UserMediasoupView.OtherMediasoupView(this, this, (Peer) info, changeAndNotify).addChildView();
        }
    }

    private void refreshMediasoupView(String selfId, boolean isVideoBeingSent, List<Info> peerUsers) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                curPeerUsers = peerUsers;
                List<Info> videoUsers = new ArrayList<>();
                int size = null == peerUsers ? 0 : peerUsers.size();
                for (int i = 0; i < size; i++) {
                    Info info = peerUsers.get(i);
                    if (info instanceof Me) {
                        if (isVideoBeingSent) {
                            videoUsers.add(info);
                        }
                    } else if (info instanceof Peer) {
//                        if (MediasoupLoaderUtils.getInstance().getPeerVideoState(info)) {
                            videoUsers.add(info);
//                        }
                    }
                }
                List<View> views = new ArrayList<>();
                int videoSize = null == videoUsers ? 0 : videoUsers.size();
                for (int i = 0; i < videoSize; i++) {
                    Info info = videoUsers.get(i);
                    View view = viewMap.containsKey(info.getId()) ? viewMap.get(info.getId()) : null;
                    if (null == view) {
                        view = createMediasoupView(info, info.getId().equals(selfId));
                        viewMap.put(info.getId(), view);
                    }
                    views.add(view);
                }
                int viewSize = null == views ? 0 : views.size();
                if (viewMap.containsKey(selfId)) {
                    View selfView = viewMap.get(selfId);
                    if (viewSize == 2 && isVideoBeingSent) {
                        cardView.removeAllViews();
                        gridLayout.removeView(selfView);
                        selfView.setLayoutParams(
                                new ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                )
                        );
                        cardView.addView(selfView);
                        cardView.setVisibility(View.VISIBLE);
                    } else {
                        cardView.removeAllViews();
                        cardView.setVisibility(View.GONE);
                    }
                }
                List<View> gridViews = new ArrayList<>();
                for (int i = 0; i < viewSize; i++) {
                    View view = views.get(i);
                    if (view instanceof UserMediasoupView.SelfMediasoupView) {
                        if (viewSize == 2 && isVideoBeingSent) {
                            continue;
                        }
                        if (viewSize > 1 && !isVideoBeingSent) {
                            continue;
                        }
                    }
                    gridViews.add(view);
                }

                int gridSize = null == gridViews ? 0 : gridViews.size();
                for (int i = 0; i < gridSize; i++) {
                    int row, col, span;
                    switch (i) {
                        case 0:
                            if (!isVideoBeingSent && gridSize == 2) {
                                row = 0;
                                col = 0;
                                span = 2;
                            } else {
                                row = 0;
                                col = 0;
                                span = 1;
                            }
                            showGridChildView(row, col, span, gridViews.get(i));
                            break;
                        case 1:
                            if (!isVideoBeingSent && gridSize == 2) {
                                row = 1;
                                col = 0;
                                span = 2;
                            } else {
                                row = 0;
                                col = 1;
                                span = 1;
                            }
                            showGridChildView(row, col, span, gridViews.get(i));
                            break;
                        case 2:
                            if (gridSize == 3) {
                                row = 1;
                                col = 0;
                                span = 2;
                            } else {
                                row = 1;
                                col = 0;
                                span = 1;
                            }
                            showGridChildView(row, col, span, gridViews.get(i));
                            break;
                        case 3:
                            row = 1;
                            col = 1;
                            span = 1;
                            showGridChildView(row, col, span, gridViews.get(i));
                            break;
                        default:

                            break;
                    }


                }

                List<View> removeViews = new ArrayList<>();
                List<String> removeUsers = new ArrayList<>();
                for (Map.Entry<String, View> entry : viewMap.entrySet()) {
                    System.out.println("key= " + entry.getKey() + " and value= " + entry.getValue());
                    View view = entry.getValue();
                    String userId = entry.getKey();
                    if (selfId.equals(userId)) {
                        if (!gridViews.contains(view)) {
                            removeViews.add(view);
                            removeUsers.add(userId);
                        }
                    } else {
                        if (!videoUsers.contains(userId)) {
                            removeViews.add(view);
                            removeUsers.add(userId);
                        }
                    }
                }

                int removeSize = null == removeViews ? 0 : removeViews.size();
                for (int i = 0; i < removeSize; i++) {
                    View view = removeViews.get(i);
                    gridLayout.removeView(view);
                }

                int removeUserSize = null == removeUsers ? 0 : removeUsers.size();
                for (int i = 0; i < removeUserSize; i++) {
                    if (viewMap.containsKey(removeUsers.get(i))) {
                        viewMap.remove(removeUsers.get(i));
                    }
                }

            }
        });
    }

    private void showGridChildView(int row, int col, int span, View view) {
        if (view.getParent() == null || view.getParent() instanceof GridLayout) {
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = GridLayout.LayoutParams.MATCH_PARENT;
            params.height = GridLayout.LayoutParams.MATCH_PARENT;
            params.rowSpec = GridLayout.spec(row, 1, GridLayout.FILL, 1f);
            params.columnSpec = GridLayout.spec(col, span, GridLayout.FILL, 1f);
            view.setLayoutParams(params);
        }
        if (view.getParent() == null) {
            gridLayout.addView(view);
        }
    }

    private void setConnectPeer(List<Peer> peersList, boolean isVideoBeingSent) {
        if (null == connectPeerAdapter) {
            connectPeerAdapter = new ConnectPeerAdapter(this);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(connectPeerAdapter);
        }
        int size = null == peersList ? 0 : peersList.size();
        if (size > 1 || !isVideoBeingSent) {
            connectPeerAdapter.setPeers(peersList);
        } else {
            connectPeerAdapter.clearAll();
        }

    }

    private void checkPermissionAndJoin() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        String rationale = "Please provide permissions";
        Permissions.Options options =
                new Permissions.Options().setRationaleDialogTitle("Info").setSettingsDialogTitle("Warning");
        Permissions.check(this, permissions, rationale, options, new PermissionHandler() {
            @Override
            public void onGranted() {
                try {
                    if (null != mediasoupBinder) {
                        mediasoupBinder.onJoinMediasoupRoom();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    private boolean curIsMuted = true;
    private boolean curIsVideo = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!MediasoupManagement.isInitMediasoup()) {
            MediasoupManagement.mediasoupInit(this);
            String userId = Utils.getRandomString(8);
            String displayName = Utils.getRandomString(8);
            String clientId = Utils.getRandomString(16);
            MediasoupManagement.mediasoupCreate(this, userId, clientId, displayName, mediasoupHandler);
        }
        MediasoupManagement.setUserChangedHandler(userChangedHandler);

        setContentView(R.layout.activity_mediasoup_main);
        recyclerView = findViewById(R.id.otheruser_recycler);
        cardView = findViewById(R.id.selfvideo_view);
        gridLayout = findViewById(R.id.othervideo_grid);
        roomNameEdit = findViewById(R.id.roomname_edit);
    }


    MediasoupManagement.MediasoupHandler mediasoupHandler = new MediasoupManagement.MediasoupHandler() {

        @Override
        public String getCurAccountId() {
            return Utils.getRandomString(8);
        }

        @Override
        public String getCurClientId() {
            return Utils.getRandomString(16);
        }

        @Override
        public String getCurDisplayName() {
            return Utils.getRandomString(8);
        }

        @Override
        public void onReady(boolean isReady) {

        }

        @Override
        public int onSend(String rConvId, String userid_self, String clientid_self, String userid_dest, String clientid_dest, String data, boolean transients) {
            return 0;
        }

        @Override
        public void onIncomingCall(String rConvId, long msg_time, String userId, boolean video_call, boolean should_ring) {

        }

        @Override
        public void onMissedCall(String rConvId, long msg_time, String userId, boolean video_call) {

        }

        @Override
        public void onAnsweredCall(String rConvId) {

        }

        @Override
        public void onEstablishedCall(String rConvId, String userId) {

        }

        @Override
        public void onClosedCall(int reasonCode, String rConvId, long msg_time, String userId) {

        }

        @Override
        public void onMetricsReady(String rConvId, String metricsJson) {

        }

        @Override
        public int onConfigRequest(boolean isRegister) {
            return 0;
        }

        @Override
        public void onBitRateStateChanged(String userId, boolean enabled) {

        }

        @Override
        public void onVideoReceiveStateChanged(String rConvId, String userId, String clientId, int state) {

        }

        @Override
        public void joinMediasoupState(int state) {

        }

        @Override
        public void rejectEndCancelCall() {

        }
    };

    MediasoupManagement.UserChangedHandler userChangedHandler = new MediasoupManagement.UserChangedHandler() {

        @Override
        public void onUserChanged(String convId, String data) {

        }
    };

    private void bindMediasoupService() {
        Intent intent = new Intent(this, MediasoupService.class);
        intent.putExtra(MediasoupConstant.key_intent_roommode, roomMode);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    private void finishServiceActivity(boolean isStop) {
        unbindMediasoupService();
        if (isStop) {
            MediasoupLoaderUtils.getInstance().stopMediasoupService(MediasoupMainActivity.this);
        }
        MediasoupMainActivity.this.finish();
    }

    private void unbindMediasoupService() {
        unbindService(serviceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (null != mediasoupBinder) {
                mediasoupBinder.setVisibleCall(true);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (null != mediasoupBinder) {
                mediasoupBinder.setVisibleCall(false);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     * 加入
     *
     * @param view
     */
    public void joinClick(View view) {
        String roomName = roomNameEdit.getText().toString();
        if (TextUtils.isEmpty(roomName)) {
            roomName = Utils.getRandomString(8);
            roomNameEdit.setText(roomName);
        }
        MediasoupManagement.mediasoupStartCall(this, roomName, 1, 1, true);
        try {
            if (null == mediasoupBinder || !mediasoupBinder.isBindService()) {
                bindMediasoupService();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /**
     * 挂断
     *
     * @param view
     */
    public void endClick(View view) {
        MediasoupManagement.endCall(MediasoupLoaderUtils.getInstance().getCurRConvId(), 3);
    }

    /**
     * 静音
     *
     * @param view
     */
    public void muteClick(View view) {
        boolean muted = !curIsMuted;
        MediasoupManagement.setCallMuted(muted);
        curIsMuted = muted;
    }

    /**
     * 视频
     *
     * @param view
     */
    public void videoAndAudioClick(View view) {
        boolean isVideo = !curIsVideo;
        MediasoupManagement.setVideoSendState(MediasoupLoaderUtils.getInstance().getCurRConvId(), 3);
        curIsVideo = isVideo;
    }

    /**
     * 切换
     *
     * @param view
     */
    public void switchClick(View view) {
        MediasoupManagement.switchCam();
    }

    /**
     * 扬声器
     *
     * @param view
     */
    public void columeClick(View view) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (null != mediasoupBinder && mediasoupBinder.isBindService()) {
                unbindMediasoupService();
                MediasoupLoaderUtils.getInstance().stopMediasoupService(this);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            unbindMediasoupService();
        }
        if (null != changeAndNotify) {
            changeAndNotify.destroy();
            changeAndNotify = null;
        }
    }
}