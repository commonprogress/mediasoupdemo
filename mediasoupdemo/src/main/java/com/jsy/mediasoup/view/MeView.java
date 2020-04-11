package com.jsy.mediasoup.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.RelativeLayout;
import android.databinding.DataBindingUtil;

import com.jsy.mediasoup.R;
import com.jsy.mediasoup.databinding.ViewMeBindingImpl;
import com.jsy.mediasoup.vm.MeProps;
import org.mediasoup.droid.lib.PeerConnectionUtils;
import org.mediasoup.droid.lib.RoomClient;

/**
 * 自己view 信息
 */
public class MeView extends RelativeLayout {

  public MeView(@NonNull Context context) {
    super(context);
    init(context);
  }

  public MeView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public MeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public MeView(
          @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context);
  }

  ViewMeBindingImpl mBinding;

  private void init(Context context) {
    mBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.view_me, this, true);
    mBinding.peerView.videoRenderer.init(PeerConnectionUtils.getEglContext(), null);//设置摄像头信息的渲染
  }

  public void setProps(MeProps props, final RoomClient roomClient) {

    // set view model.
    mBinding.peerView.setPeerViewProps(props);

    // register click listener.
    mBinding.peerView.info.setOnClickListener(
        view -> {
          Boolean showInfo = props.getShowInfo().get();
          props.getShowInfo().set(showInfo != null && showInfo ? Boolean.FALSE : Boolean.TRUE);
        });

    mBinding.peerView.meDisplayName.setOnEditorActionListener(
        (textView, actionId, keyEvent) -> {
          if (actionId == EditorInfo.IME_ACTION_DONE) {
              //修改自己名字
            roomClient.changeDisplayName(textView.getText().toString().trim());
            return true;
          }
          return false;
        });
    mBinding.peerView.stats.setOnClickListener(
        view -> {
          // TODO(HaiyangWU): Handle inner click event;
        });

    //SurfaceView 层次覆盖关系
    mBinding.peerView.videoRenderer.setZOrderMediaOverlay(true);

    // set view model.
    mBinding.setMeProps(props);

    // register click listener. 是否静音
    mBinding.mic.setOnClickListener(
        view -> {
          if (MeProps.DeviceState.ON.equals(props.getMicState().get())) {
            roomClient.muteMic();
          } else {
            roomClient.unmuteMic();
          }
        });
    //是否是否打开摄像头
    mBinding.cam.setOnClickListener(
        view -> {
          if (MeProps.DeviceState.ON.equals(props.getCamState().get())) {
            roomClient.disableCam();
          } else {
            roomClient.enableCam();
          }
        });
    //前后摄像头切换
    mBinding.changeCam.setOnClickListener(view -> roomClient.changeCam());
    //屏幕共享 （功能暂未实现）
    mBinding.share.setOnClickListener(
        view -> {
          if (MeProps.DeviceState.ON.equals(props.getShareState().get())) {
            roomClient.disableShare();
          } else {
            roomClient.enableShare();
          }
        });
  }
}
