package com.dongxl.mediasoup;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayout;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;

import com.jsy.mediasoup.MediasoupConstant;
import com.jsy.mediasoup.MediasoupLoaderUtils;
import com.jsy.mediasoup.MediasoupManagement;
import com.jsy.mediasoup.PropsChangeAndNotify;
import com.jsy.mediasoup.R;
import com.jsy.mediasoup.RoomActivity;
import com.jsy.mediasoup.services.MediasoupAidlInterface;
import com.jsy.mediasoup.services.MediasoupService;
import com.jsy.mediasoup.services.RoomAidlInterface;
import com.jsy.mediasoup.utils.LogUtils;

public class MediasoupMainActivity extends AppCompatActivity {
    private static final String TAG = MediasoupMainActivity.class.getSimpleName();
    private EditText roomNameEdit;
    private GridLayout gridLayout;
    private CardView cardView;
    private RecyclerView recyclerView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!MediasoupManagement.mediasoupInit(this)) {
            MediasoupManagement.mediasoupCreate(this);
        }

        setContentView(R.layout.activity_mediasoup_main);
        recyclerView = findViewById(R.id.othervideo_grid);
        cardView = findViewById(R.id.selfvideo_view);
        gridLayout = findViewById(R.id.otheruser_recycler);
        roomNameEdit = findViewById(R.id.roomname_edit);
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

    }

    /**
     * 挂断
     *
     * @param view
     */
    public void endClick(View view) {

    }

    /**
     * 静音
     *
     * @param view
     */
    public void muteClick(View view) {

    }

    /**
     * 视频
     *
     * @param view
     */
    public void videoAndAudioClick(View view) {

    }

    /**
     * 切换
     *
     * @param view
     */
    public void switchClick(View view) {

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
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            unbindMediasoupService();
        }
        if (null != changeAndNotify) {
            changeAndNotify.destroy();
            changeAndNotify = null;
        }
//        MediasoupLoaderUtils.getInstance().stopMediasoupService(this);
    }
}