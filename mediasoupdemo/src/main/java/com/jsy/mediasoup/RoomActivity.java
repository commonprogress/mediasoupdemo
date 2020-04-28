package com.jsy.mediasoup;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.jsy.mediasoup.adapter.PeerAdapter;
import com.jsy.mediasoup.interfaces.RoomStoreObserveCallback;
import com.jsy.mediasoup.services.MediasoupAidlInterface;
import com.jsy.mediasoup.services.MediasoupService;
import com.jsy.mediasoup.services.RoomAidlInterface;
import com.jsy.mediasoup.utils.ClipboardCopy;
import com.jsy.mediasoup.view.MeView;
import com.jsy.mediasoup.vm.MeProps;
import com.jsy.mediasoup.vm.RoomProps;
import com.jsy.mediasoup.utils.LogUtils;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import org.mediasoup.droid.Logger;
import org.mediasoup.droid.MediasoupClient;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Me;
import org.mediasoup.droid.lib.model.Notify;
import org.mediasoup.droid.lib.model.Peer;
import org.mediasoup.droid.lib.model.Peers;

import java.util.List;

public class RoomActivity extends AppCompatActivity {
    private static final String TAG = RoomActivity.class.getSimpleName();
    private static final int REQUEST_CODE_SETTING = 1;

    private PeerAdapter mPeerAdapter;//连接用户的适配器

    private RecyclerView remotePeers;
    private ImageView roomState;
    private TextView invitationLink;
    private ImageView hideVideos;
    private ImageView muteAudio;
    private ImageView restartIce;
    private TextView speakerMute;
    private MeView me;
    private TextView text_reject, text_end, text_cancel, text_accept, text_name;
    private PropsChangeAndNotify changeAndNotify;
    private int roomMode;

    private MediasoupAidlInterface mediasoupBinder;

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
            rejectViewData();
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
            if (isReceiveCall && !isConnecting) {
//接收邀请 且不是通话连接中
//                initViewModel();
                rejectViewData();
            } else {
                //通话连接中 或者发起邀请
                onSelfAcceptOrJoin();
            }
        }

        @Override
        public void onSelfAcceptOrJoin() throws RemoteException {
            LogUtils.i(TAG, "==roomBinder mediasoup onSelfAccept==");
            initViewData();
        }

        @Override
        public void onOtherJoin() throws RemoteException {
            LogUtils.i(TAG, "==roomBinder mediasoup onOtherJoin==");
            rejectViewData();
        }

        @Override
        public void onOtherLeave() throws RemoteException {
            LogUtils.i(TAG, "==roomBinder mediasoup onOtherLeave==");
//            rejectViewData();
        }

        @Override
        public void onFinishServiceActivity() throws RemoteException {
            LogUtils.i(TAG, "==roomBinder mediasoup onEndCall==");
            finishServiceActivity(true);
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        roomMode = getIntent().getIntExtra(MediasoupConstant.key_intent_roommode, MediasoupConstant.roommode_video);
        changeAndNotify = new PropsChangeAndNotify(this, this);
        initView();
        bindMediasoupService();
    }

    private void bindMediasoupService() {
        Intent intent = new Intent(this, MediasoupService.class);
        intent.putExtra(MediasoupConstant.key_intent_roommode, roomMode);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    private void finishServiceActivity(boolean isStop) {
        unbindMediasoupService();
        if (isStop) {
            MediasoupLoaderUtils.getInstance().stopMediasoupService(RoomActivity.this);
        }
        RoomActivity.this.finish();
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

    private void initView() {
        remotePeers = findViewById(R.id.remote_peers);
        roomState = findViewById(R.id.room_state);
        invitationLink = findViewById(R.id.invitation_link);
        hideVideos = findViewById(R.id.hide_videos);
        muteAudio = findViewById(R.id.mute_audio);
        restartIce = findViewById(R.id.restart_ice);
        speakerMute = findViewById(R.id.speaker_mute);
        me = findViewById(R.id.me);
        text_reject = findViewById(R.id.text_reject);
        text_end = findViewById(R.id.text_end);
        text_cancel = findViewById(R.id.text_cancel);
        text_accept = findViewById(R.id.text_accept);
        text_name = findViewById(R.id.text_name);
        // Display version number.
        ((TextView)findViewById(R.id.version)).setText(String.valueOf(MediasoupClient.version()));
    }

    private void initViewData() {
        rejectViewData();
        initViewModel();
        checkPermission();
    }

    private void rejectViewData() {
        LogUtils.i(TAG, "==initViewData mediasoup null == mediasoupBinder:" + (null == mediasoupBinder));
        if (null == mediasoupBinder) {
            return;
        }
        try {
            boolean isReceiveCall = mediasoupBinder.isReceiveCall();
            boolean isConnecting = mediasoupBinder.isRoomConnecting();
//        private View text_reject, text_end, text_cancel, text_accept;
            if (isConnecting) {
                text_reject.setVisibility(View.GONE);
                text_end.setVisibility(View.VISIBLE);
                text_cancel.setVisibility(View.GONE);
                text_accept.setVisibility(View.GONE);
                text_name.setText("已经连接——他人：" + MediasoupLoaderUtils.getInstance().getIncomingDisplayName());
            } else {
                if (isReceiveCall) {
                    text_reject.setVisibility(View.VISIBLE);
                    text_end.setVisibility(View.GONE);
                    text_cancel.setVisibility(View.GONE);
                    text_accept.setVisibility(View.VISIBLE);
                    text_name.setText("等待自己接受——他人：" + MediasoupLoaderUtils.getInstance().getIncomingDisplayName());
                } else {
                    text_reject.setVisibility(View.GONE);
                    text_end.setVisibility(View.GONE);
                    text_cancel.setVisibility(View.VISIBLE);
                    text_accept.setVisibility(View.GONE);
                    text_name.setText("等待他人接受——自己：" + MediasoupLoaderUtils.getInstance().getIncomingDisplayName());
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void initViewModel() {
        LogUtils.i(TAG, "==initViewModel mediasoup null == mediasoupBinder:" + (null == mediasoupBinder));
        if (null == changeAndNotify) {
            changeAndNotify = new PropsChangeAndNotify(this, this);
        }
        getViewModelStore().clear();
        boolean isMediasoupReady = MediasoupLoaderUtils.getInstance().isMediasoupReady();
        if (!isMediasoupReady) {
            return;
        }
        final RoomClient roomClient = MediasoupLoaderUtils.getInstance().getRoomClient();
        final RoomStore roomStore = MediasoupLoaderUtils.getInstance().getRoomStore();
        final RoomProps roomProps = changeAndNotify.getRoomPropsAndChange(this, roomClient, roomStore, ediasProps -> setRoomProps((RoomProps) ediasProps));

        //邀请连接
        invitationLink.setOnClickListener(
            v -> {
                String linkUrl = roomProps.getInvitationLink().get();
                ClipboardCopy.clipboardCopy(getApplication(), linkUrl, R.string.invite_link_copied);
            });


        // Me. 自己view

        final MeProps meProps = changeAndNotify.getMePropsAndChange(this, roomClient, roomStore);
        me.setProps(meProps, roomClient, roomStore);

        hideVideos.setOnClickListener(
            v -> {
                Me me = meProps.getMe().get();
                if (me != null) {
                    if (me.isAudioOnly()) {
                        //音视频都有
                        roomClient.disableAudioOnly();
                    } else {
                        //仅音频无视频
                        roomClient.enableAudioOnly();
                    }
                }
            });
        muteAudio.setOnClickListener(
            v -> {
                Me me = meProps.getMe().get();
                if (me != null) {
                    if (me.isAudioMuted()) {
                        //取消静音
                        roomClient.unmuteAudio();
                    } else {
                        //静音
                        roomClient.muteAudio();
                    }
                }
            });

        //是否重启ice
        restartIce.setOnClickListener(v -> roomClient.restartIce());

        //听筒和扬声器
        speakerMute.setOnClickListener(
            v -> {
                Me me = meProps.getMe().get();
                if (me != null) {
                    if (me.isSpeakerMute()) {
//关闭扬声器打开听筒
                        roomClient.setSpeakerMute(false);
                    } else {
//打开扬声器 关闭听筒
                        roomClient.setSpeakerMute(true);
                    }
                }
            });

        // Peers.
        mPeerAdapter = new PeerAdapter(this, changeAndNotify, roomClient, roomStore);
        remotePeers.setLayoutManager(new LinearLayoutManager(this));
        remotePeers.setAdapter(mPeerAdapter);

        mPeerAdapter.clearPeers();
//        Peers curPeers = null != roomStore.getPeers() ? roomStore.getPeers().getValue() : null;
//        setRemotePeersForAdapter(curPeers);

        changeAndNotify.observePeersAndNotify(this, roomClient, roomStore, new RoomStoreObserveCallback() {

            @Override
            public void onObservePeers(Peers peers) {
                setRemotePeersForAdapter(peers);
            }

            @Override
            public void onObserveNotify(Notify notify) {
                if (notify == null) {
                    return;
                }
                Logger.d(TAG, "notifyObserver, notify.getType(): " + (null == notify ? "" : notify.getType()) + ",notify.getText():" + (null == notify ? "" : notify.getText()));
                if ("error".equals(notify.getType())) {
                    Toast toast = Toast.makeText(RoomActivity.this, notify.getText(), notify.getTimeout());
                    TextView toastMessage = toast.getView().findViewById(android.R.id.message);
                    toastMessage.setTextColor(Color.RED);
                    toast.show();
                } else {
                    Toast.makeText(RoomActivity.this, notify.getText(), notify.getTimeout()).show();
                }
            }
        });
    }

    private void setRemotePeersForAdapter(Peers peers) {
        List<Peer> peersList = null != peers ? peers.getAllPeers() : null;
        int size = null == peersList ? 0 : peersList.size();
        Logger.d(TAG, "setRemotePeersForAdapter, peersList.size(): " + size);
        if (size > 0) {
            remotePeers.setVisibility(View.VISIBLE);
            roomState.setVisibility(View.GONE);
        } else {
            remotePeers.setVisibility(View.GONE);
            roomState.setVisibility(View.VISIBLE);
        }
        mPeerAdapter.replacePeers(peersList);
    }

    private void setRoomProps(RoomProps roomProps) {
        if (null == roomProps) {
            LogUtils.i(TAG, "setRoomProps,null == roomProps");
            return;
        }
        BindingAdapters.roomState(roomState, roomProps.getConnectionState().get(), roomProps.getConnectingAnimation());
        BindingAdapters.inviteLink(invitationLink, roomProps.getInvitationLink().get());
        BindingAdapters.hideVideos(hideVideos, roomProps.getAudioOnly().get(), roomProps.getAudioOnlyInProgress().get());
        BindingAdapters.audioMuted(muteAudio, roomProps.getAudioMuted().get());
        BindingAdapters.restartIce(restartIce, roomProps.getRestartIceInProgress().get(), roomProps.getRestartIceAnimation());
    }

    private PermissionHandler permissionHandler =
        new PermissionHandler() {
            @Override
            public void onGranted() {
                Logger.d(TAG, "permission granted mediasoup null == mediasoupBinder:" + (null == mediasoupBinder));
                try {
                    if (null != mediasoupBinder) {
                        mediasoupBinder.onJoinMediasoupRoom();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        };

    /**
     * 检测权限
     */
    private void checkPermission() {
        String[] permissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
        String rationale = "Please provide permissions";
        Permissions.Options options =
            new Permissions.Options().setRationaleDialogTitle("Info").setSettingsDialogTitle("Warning");
        Permissions.check(this, permissions, rationale, options, permissionHandler);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_SETTING) {
            Logger.d(TAG, "request config done");
            try {
                if (null != mediasoupBinder) {
                    mediasoupBinder.onResetMediasoupRoom();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            initViewData();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (null != mediasoupBinder && mediasoupBinder.isBindService()) {
                unbindMediasoupService();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            unbindMediasoupService();
        }
        MediasoupLoaderUtils.getInstance().stopMediasoupService(RoomActivity.this);
        if (null != changeAndNotify) {
            changeAndNotify.destroy();
            changeAndNotify = null;
        }
//        MediasoupLoaderUtils.getInstance().stopMediasoupService(this);
    }

    /**
     * 拒绝
     *
     * @param view
     */
    public void callRejectClick(View view) {
        try {
            if (null != mediasoupBinder) {
                mediasoupBinder.rejectEndCancelCall();
                mediasoupBinder.callSelfReject();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 接受
     *
     * @param view
     */
    public void callAcceptClick(View view) {
        try {
            if (null != mediasoupBinder) {
                mediasoupBinder.callSelfAccept();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 结束
     *
     * @param view
     */
    public void callEndClick(View view) {
        try {
            if (null != mediasoupBinder) {
                mediasoupBinder.rejectEndCancelCall();
                mediasoupBinder.callSelfEnd();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消
     *
     * @param view
     */
    public void callCancelClick(View view) {
        try {
            if (null != mediasoupBinder) {
                mediasoupBinder.rejectEndCancelCall();
                mediasoupBinder.callSelfCancel();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
