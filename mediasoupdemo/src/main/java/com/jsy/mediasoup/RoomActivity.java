package com.jsy.mediasoup;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import android.databinding.DataBindingUtil;

import org.mediasoup.droid.lib.Utils;
import com.jsy.mediasoup.databinding.ActivityRoomBinding;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;

import org.mediasoup.droid.Logger;
import com.jsy.mediasoup.adapter.PeerAdapter;
import com.jsy.mediasoup.vm.EdiasProps;
import com.jsy.mediasoup.vm.MeProps;
import com.jsy.mediasoup.vm.RoomProps;
import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.RoomOptions;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Me;
import org.mediasoup.droid.lib.model.Notify;
import org.mediasoup.droid.lib.model.Peer;

import java.util.List;

import static com.jsy.mediasoup.utils.ClipboardCopy.clipboardCopy;

public class RoomActivity extends AppCompatActivity {

  private static final String TAG = RoomActivity.class.getSimpleName();
  private static final int REQUEST_CODE_SETTING = 1;

  //房间id 自己userid 自己名字
  private String mRoomId, mPeerId, mDisplayName;
  //视频编码
  private boolean mForceH264, mForceVP9;

  private RoomOptions mOptions;//房间的配置信息
  private RoomStore mRoomStore;
  private RoomClient mRoomClient;//房间操作类

  private ActivityRoomBinding mBinding;
  private PeerAdapter mPeerAdapter;//连接用户的适配器

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_room);
    createRoom();
    checkPermission();
  }

  /**
   * 创建房间 初始化房间信息 view
   */
  private void createRoom() {
    mOptions = new RoomOptions();
    loadRoomConfig();//配置room信息

    mRoomStore = new RoomStore();
    initRoomClient();//初始化房间

    getViewModelStore().clear();
    initViewModel();
  }

  /**
   * 加载房间配置room信息
   */
  private void loadRoomConfig() {
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

    // Room initial config.
    mRoomId = preferences.getString("roomId", "");
    mPeerId = preferences.getString("peerId", "");
    mDisplayName = preferences.getString("displayName", "");
    mForceH264 = preferences.getBoolean("forceH264", false);
    mForceVP9 = preferences.getBoolean("forceVP9", false);
    if (TextUtils.isEmpty(mRoomId)) {
      mRoomId = Utils.getRandomString(8);
      preferences.edit().putString("roomId", "mRoomId").apply();
    }
    if (TextUtils.isEmpty(mPeerId)) {
      mPeerId = Utils.getRandomString(8);
      preferences.edit().putString("peerId", mPeerId).apply();
    }
    if (TextUtils.isEmpty(mDisplayName)) {
      mDisplayName = Utils.getRandomString(8);
      preferences.edit().putString("displayName", mDisplayName).apply();
    }

    // Room action config. 房间配置
    mOptions.setProduce(preferences.getBoolean("produce", true));//是否立即打开摄像头和录音？
    mOptions.setConsume(preferences.getBoolean("consume", true));//是否立即连接？
    mOptions.setForceTcp(preferences.getBoolean("forceTcp", false));//是否强制tcp 否则rtc

    mRoomId = "dongxl";
    mOptions.setProduce(true);
    mOptions.setConsume(true);

    // Device config. 获取上传保存的摄像头信息 默认前置摄像头
    String camera = preferences.getString("camera", "front");
    PeerConnectionUtils.setPreferCameraFace(camera);
  }

  /**
   * 初始化房间
   */
  private void initRoomClient() {
    mRoomClient =
        new RoomClient(
            this, mRoomStore, mRoomId, mPeerId, mDisplayName, mForceH264, mForceVP9, mOptions);
  }

  private void initViewModel() {
    EdiasProps.Factory factory = new EdiasProps.Factory(getApplication(), mRoomStore);

    // Room. 连接
    RoomProps roomProps = ViewModelProviders.of(this, factory).get(RoomProps.class);
    roomProps.connect(this);

    //邀请连接
    mBinding.invitationLink.setOnClickListener(
        v -> {
          String linkUrl = roomProps.getInvitationLink().get();
          clipboardCopy(getApplication(), linkUrl, R.string.invite_link_copied);
        });
    mBinding.setRoomProps(roomProps);

    // Me. 自己view
    MeProps meProps = ViewModelProviders.of(this, factory).get(MeProps.class);
    meProps.connect(this);
    mBinding.me.setProps(meProps, mRoomClient);

    mBinding.hideVideos.setOnClickListener(
        v -> {
          Me me = meProps.getMe().get();
          if (me != null) {
            if (me.isAudioOnly()) {
              //音视频都有
              mRoomClient.disableAudioOnly();
            } else {
              //仅音频无视频
              mRoomClient.enableAudioOnly();
            }
          }
        });
    mBinding.muteAudio.setOnClickListener(
        v -> {
          Me me = meProps.getMe().get();
          if (me != null) {
            if (me.isAudioMuted()) {
              //取消静音
              mRoomClient.unmuteAudio();
            } else {
              //静音
              mRoomClient.muteAudio();
            }
          }
        });

    //是否重启ice
    mBinding.restartIce.setOnClickListener(v -> mRoomClient.restartIce());

    // Peers.
    mPeerAdapter = new PeerAdapter(mRoomStore, this, mRoomClient);
    mBinding.remotePeers.setLayoutManager(new LinearLayoutManager(this));
    mBinding.remotePeers.setAdapter(mPeerAdapter);
    mRoomStore
        .getPeers()
        .observe(
            this,
            peers -> {
              List<Peer> peersList = peers.getAllPeers();
              if (peersList.isEmpty()) {
                mBinding.remotePeers.setVisibility(View.GONE);
                mBinding.roomState.setVisibility(View.VISIBLE);
              } else {
                mBinding.remotePeers.setVisibility(View.VISIBLE);
                mBinding.roomState.setVisibility(View.GONE);
              }
              mPeerAdapter.replacePeers(peersList);
            });

    // Notify
    final Observer<Notify> notifyObserver =
        notify -> {
          if (notify == null) {
            return;
          }
          Logger.d(TAG, "notifyObserver, notify.getType(): " + (null == notify ? "" : notify.getType()) + ",notify.getText():" + (null == notify ? "" : notify.getText()));
          if ("error".equals(notify.getType())) {
            Toast toast = Toast.makeText(this, notify.getText(), notify.getTimeout());
            TextView toastMessage = toast.getView().findViewById(android.R.id.message);
            toastMessage.setTextColor(Color.RED);
            toast.show();
          } else {
            Toast.makeText(this, notify.getText(), notify.getTimeout()).show();
          }
        };
    mRoomStore.getNotify().observe(this, notifyObserver);
  }

  private PermissionHandler permissionHandler =
      new PermissionHandler() {
        @Override
        public void onGranted() {
          Logger.d(TAG, "permission granted");
          if (mRoomClient != null) {
            //有权限加入房间
            mRoomClient.join();
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

  /**
   * 销毁房间
   */
  private void destroyRoom() {
    if (mRoomClient != null) {
      mRoomClient.close();
      mRoomClient = null;
    }
    if (mRoomStore != null) {
      mRoomStore = null;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.room_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle item selection
    if (item.getItemId() == R.id.setting) {
      Intent intent = new Intent(this, SettingsActivity.class);
      startActivityForResult(intent, REQUEST_CODE_SETTING);
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (requestCode == REQUEST_CODE_SETTING) {
      Logger.d(TAG, "request config done");
      // close, dispose room related and clear store.
      destroyRoom();
      // local config and reCreate room related.
      createRoom();
      // check permission again. if granted, join room.
      checkPermission();
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    destroyRoom();
  }
}
