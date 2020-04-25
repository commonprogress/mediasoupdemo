package org.mediasoup.droid.lib;

import android.content.Context;
import android.support.annotation.MainThread;
import android.text.TextUtils;
import android.util.Log;

import org.mediasoup.droid.Logger;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

/**
 * PeerConnection通信建立流程 （@link org.mediasoup.droid.PeerConnection (没有用？)）
 * WebRTC Peerconnection通信过程中的四种角色：
 * Signaling Server
 * ICE/TURN/STUN Server
 * Remote Peer
 * Local Peer
 * https://blog.csdn.net/aflyeaglenku/article/details/76603615?depth_1-utm_source=distribute.pc_relevant.none-task&utm_source=distribute.pc_relevant.none-task
 */
@SuppressWarnings("WeakerAccess")
public class PeerConnectionUtils {

  private static final String TAG = "PeerConnectionUtils";

  private static String mPreferCameraFace;
  private static EglBase mEglBase = EglBase.create();

    /**
     * 获得渲染器
     * @return
     */
  public static EglBase.Context getEglContext() {
    return mEglBase.getEglBaseContext();
  }

    /**
     * 设置摄像头信息 前置 后置摄像头
     * @param preferCameraFace
     */
  public static void setPreferCameraFace(String preferCameraFace) {
    mPreferCameraFace = preferCameraFace;
  }

  private final ThreadUtils.ThreadChecker mThreadChecker;
  private PeerConnectionFactory mPeerConnectionFactory;

  private AudioSource mAudioSource;
  private VideoSource mVideoSource;
  private CameraVideoCapturer mCamCapture;

  public PeerConnectionUtils() {
    mThreadChecker = new ThreadUtils.ThreadChecker();
  }

    /**
     * PeerConnection factory creation.
     * 创建 PeerConnection实体
     * @param context
     */
  private void createPeerConnectionFactory(Context context) {
    Logger.d(TAG, "createPeerConnectionFactory()");
    mThreadChecker.checkIsOnValidThread();
    PeerConnectionFactory.Builder builder = PeerConnectionFactory.builder();//初始化PeerConnectionFactory
    builder.setOptions(null);//初始化PeerConnectionFactory

    AudioDeviceModule adm = createJavaAudioDevice(context);
    //视频的编码格式
    VideoEncoderFactory encoderFactory =
        new DefaultVideoEncoderFactory(
            mEglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, true);
    //视频的解码格式
    VideoDecoderFactory decoderFactory =
        new DefaultVideoDecoderFactory(mEglBase.getEglBaseContext());

    mPeerConnectionFactory =
        builder
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory();
  }

    /**
     * 创建java层 音频设备数据
     * @param appContext
     * @return
     */
  private AudioDeviceModule createJavaAudioDevice(Context appContext) {
    Logger.d(TAG, "createJavaAudioDevice()");
    mThreadChecker.checkIsOnValidThread();
    // Enable/disable OpenSL ES playback.
    // Set audio record error callbacks.
      //录音错误回调
    JavaAudioDeviceModule.AudioRecordErrorCallback audioRecordErrorCallback =
        new JavaAudioDeviceModule.AudioRecordErrorCallback() {
          @Override
          public void onWebRtcAudioRecordInitError(String errorMessage) {
            Logger.e(TAG, "onWebRtcAudioRecordInitError: " + errorMessage);
          }

          @Override
          public void onWebRtcAudioRecordStartError(
              JavaAudioDeviceModule.AudioRecordStartErrorCode errorCode, String errorMessage) {
            Logger.e(TAG, "onWebRtcAudioRecordStartError: " + errorCode + ". " + errorMessage);
          }

          @Override
          public void onWebRtcAudioRecordError(String errorMessage) {
            Logger.e(TAG, "onWebRtcAudioRecordError: " + errorMessage);
          }
        };

    //采集错误回调
    JavaAudioDeviceModule.AudioTrackErrorCallback audioTrackErrorCallback =
        new JavaAudioDeviceModule.AudioTrackErrorCallback() {
          @Override
          public void onWebRtcAudioTrackInitError(String errorMessage) {
            Log.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
          }

          @Override
          public void onWebRtcAudioTrackStartError(
              JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
            Log.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
          }

          @Override
          public void onWebRtcAudioTrackError(String errorMessage) {
            Log.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
          }
        };

    return JavaAudioDeviceModule.builder(appContext)
        .setAudioRecordErrorCallback(audioRecordErrorCallback)
        .setAudioTrackErrorCallback(audioTrackErrorCallback)
        .createAudioDeviceModule();
  }

    /**
     * Audio source creation.
     * 创建音频资源
     * @param context
     */
  private void createAudioSource(Context context) {
    Logger.d(TAG, "createAudioSource()");
    mThreadChecker.checkIsOnValidThread();
    if (mPeerConnectionFactory == null) {
      createPeerConnectionFactory(context);
    }

    mAudioSource = mPeerConnectionFactory.createAudioSource(new MediaConstraints());
  }

    /**
     * 创建相机普获的画面
     *
     * @param context
     */
  private void createCamCapture(Context context) {
    Logger.d(TAG, "createCamCapture()");
    mThreadChecker.checkIsOnValidThread();
    boolean isCamera2Supported = Camera2Enumerator.isSupported(context);//是否支持Camera2
    CameraEnumerator cameraEnumerator;

    if (isCamera2Supported) {
      cameraEnumerator = new Camera2Enumerator(context);
    } else {
      cameraEnumerator = new Camera1Enumerator();
    }
    final String[] deviceNames = cameraEnumerator.getDeviceNames();
    for (String deviceName : deviceNames) {
      boolean needFrontFacing = "front".endsWith(mPreferCameraFace);
      String selectedDeviceName = null;
      if (needFrontFacing) {
        if (cameraEnumerator.isFrontFacing(deviceName)) {
          selectedDeviceName = deviceName;
        }
      } else {
        if (!cameraEnumerator.isFrontFacing(deviceName)) {
          selectedDeviceName = deviceName;
        }
      }

      if (!TextUtils.isEmpty(selectedDeviceName)) {
        mCamCapture =
            cameraEnumerator.createCapturer(
                selectedDeviceName,
                new CameraVideoCapturer.CameraEventsHandler() {
                  @Override
                  public void onCameraError(String errorDescription) {
                    Logger.e(TAG, "onCameraError, errorDescription:" + errorDescription);
                  }

                  @Override
                  public void onCameraDisconnected() {
                    Logger.w(TAG, "onCameraDisconnected");
                  }

                  @Override
                  public void onCameraFreezed(String errorDescription) {
                    Logger.w(TAG, "onCameraFreezed, errorDescription:" + errorDescription);
                  }

                  @Override
                  public void onCameraOpening(String cameraName) {
                      boolean isFrontCamera = !TextUtils.isEmpty(cameraName) && cameraName.toLowerCase().contains("front");
                      Logger.d(TAG, "onCameraOpening,isFrontCamera:" + isFrontCamera + ", cameraName:" + cameraName);
                    //cameraName:Camera 1, Facing front, Orientation 270
                  }

                  @Override
                  public void onFirstFrameAvailable() {
                    Logger.d(TAG, "onFirstFrameAvailable");
                  }

                  @Override
                  public void onCameraClosed() {
                    Logger.d(TAG, "onCameraClosed");
                  }
                });
        break;
      }
    }

    if (mCamCapture == null) {
      throw new IllegalStateException("Failed to create Camera Capture");
    }
  }

    /**
     * 切换摄像头
     * @param switchHandler
     */
  public void switchCam(CameraVideoCapturer.CameraSwitchHandler switchHandler) {
    Logger.d(TAG, "switchCam()");
    mThreadChecker.checkIsOnValidThread();
    if (mCamCapture != null) {
      mCamCapture.switchCamera(switchHandler);
    }
  }

    /**
     * Video source creation.
     * 创建视频源
     *
     * @param context
     */
    @MainThread
    private void createVideoSource(Context context) {
        Logger.d(TAG, "createVideoSource()");
        mThreadChecker.checkIsOnValidThread();
        if (mPeerConnectionFactory == null) {
            createPeerConnectionFactory(context);
        }
        if (mCamCapture == null) {
            createCamCapture(context);
        }

        mVideoSource = mPeerConnectionFactory.createVideoSource(false);
        SurfaceTextureHelper surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", mEglBase.getEglBaseContext());

        mCamCapture.initialize(surfaceTextureHelper, context, mVideoSource.getCapturerObserver());
        mCamCapture.startCapture(640, 480, 15);
    }


    /**
     * Audio track creation.
     * 创建音频轨迹
     * @param context
     * @param id
     * @return
     */
  public AudioTrack createAudioTrack(Context context, String id) {
    Logger.d(TAG, "createAudioTrack()");
    mThreadChecker.checkIsOnValidThread();
    if (mAudioSource == null) {
      createAudioSource(context);
    }
    return mPeerConnectionFactory.createAudioTrack(id, mAudioSource);
  }

    /**
     * Video track creation.
     * 创建视频轨迹
     * @param context
     * @param id
     * @return
     */
  public VideoTrack createVideoTrack(Context context, String id) {
    Logger.d(TAG, "createVideoTrack()");
    mThreadChecker.checkIsOnValidThread();
    if (mVideoSource == null) {
      createVideoSource(context);
    }

    return mPeerConnectionFactory.createVideoTrack(id, mVideoSource);
  }

    /**
     * 销毁PeerConnectionUtils 中相关的
     * 相机
     * 音视频
     * PeerConnection
     */
  public void dispose() {
    Logger.w(TAG, "dispose()");
    mThreadChecker.checkIsOnValidThread();
    if (mCamCapture != null) {
      mCamCapture.dispose();
      mCamCapture = null;
    }

    if (mVideoSource != null) {
      mVideoSource.dispose();
      mVideoSource = null;
    }

    if (mAudioSource != null) {
      mAudioSource.dispose();
      mAudioSource = null;
    }

    if (mPeerConnectionFactory != null) {
      mPeerConnectionFactory.dispose();
      mPeerConnectionFactory = null;
    }
  }
}
