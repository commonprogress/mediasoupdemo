package org.mediasoup.droid.lib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.media.projection.MediaProjection;
import android.os.Build;
import androidx.annotation.MainThread;
import androidx.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.AndroidException;
import android.view.Surface;
import android.view.WindowManager;

import com.jsy.mediasoup.MediasoupConstant;
import com.jsy.mediasoup.utils.LogUtils;
import org.mediasoup.droid.Logger;
import org.webrtc.*;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.IOException;
import java.util.Arrays;

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
  private static final String MEDIA_STREAM_ID = "ARDAMS";
  private static final String VIDEO_TRACK_ID = "ARDAMSv0";
  private static final String AUDIO_TRACK_ID = "ARDAMSa0";
  private static String mPreferCameraFace = "front";
  private static EglBase mEglBase = EglBase.create();

    private int curVideoSize;

    private static final int VIDEO_SIZE_HIGH = 2;
    private static final int VIDEO_SIZE_MID = 4;
    private static final int VIDEO_SIZE_LOW = 8;

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
  private MediaStream mMediaStream;
  private AudioSource mAudioSource;
  private VideoSource mVideoSource;
  private VideoCapturer mVideoCapturer;

  public PeerConnectionUtils() {
    mThreadChecker = new ThreadUtils.ThreadChecker();
  }

    public PeerConnectionFactory getPeerConnectionFactory() {
        return mPeerConnectionFactory;
    }

    public MediaStream getMediaStream() {
        return mMediaStream;
    }

    private static final VideoEncoder.Settings ENCODER_SETTINGS =
            new VideoEncoder.Settings(1 /* core */, 640 /* width */, 480 /* height */, 300 /* kbps */,
                    30 /* fps */, 1 /* numberOfSimulcastStreams */, true /* automaticResizeOn */);

    private static final VideoDecoder.Settings DECODER_SETTINGS =
            new VideoDecoder.Settings(/* numberOfCores= */ 1, /* width= */ 640, /* height= */ 480);
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

//      VideoCodecInfo[] encoderInfos = encoderFactory.getSupportedCodecs();
//      int lentch = encoderInfos == null ? 0 : encoderInfos.length;
//      for (int i = 0; i < lentch; i++) {
//          int index = i;
//          VideoCodecInfo videoCodecInfo = encoderInfos[i];
//          VideoEncoder videoEncoder = encoderFactory.createEncoder(videoCodecInfo);
//          boolean isHardwareEncoder = null == videoEncoder ? false : videoEncoder.isHardwareEncoder();
//          long encoderId = null != videoEncoder ? -1L : videoEncoder.createNativeVideoEncoder();
//          Map<String, String> params = videoCodecInfo.params;
//          for (Map.Entry<String, String> entry : params.entrySet()) {
//              Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoEncoderFactory  index:" + i + ", videoEncoder:" + videoEncoder + ", encoderId:" + encoderId + ", isHardwareEncoder:" + isHardwareEncoder + ",videoCodecInfo.name:" + videoCodecInfo.name + ", entry.getKey:" + entry.getKey() + ", entry.getValue:" + entry.getValue());
//          }
//          if (null == params || params.isEmpty()) {
//              Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoEncoderFactory index:" + i + ", videoEncoder:" + videoEncoder + ", encoderId:" + encoderId + ", isHardwareEncoder:" + isHardwareEncoder + ",videoCodecInfo.name:" + videoCodecInfo.name + ", params == null");
//          }
//          VideoCodecStatus videoCodecStatus = null;
//          String levelId = (null != params && params.containsKey("profile-level-id")) ? params.get("profile-level-id") : "";
//          if (null != videoEncoder && isHardwareEncoder /*&& !(videoEncoder instanceof VideoEncoderFallback) && "H264".equals(videoCodecInfo.name) && "42e01f".equals(levelId)*/) {
//              if(videoEncoder instanceof VideoEncoderFallback){
//                  Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoEncoderFactory getHardwareEncoder 1 onEncodedFrame ");
//                  videoCodecStatus = encoderFactory.getHardwareEncoder(videoCodecInfo).initEncode(ENCODER_SETTINGS, new VideoEncoder.Callback() {
//                      @Override
//                      public void onEncodedFrame(EncodedImage encodedImage, VideoEncoder.CodecSpecificInfo info) {
//                          Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoEncoderFactory getHardwareEncoder onEncodedFrame index:" + index + ", videoEncoder:" + videoEncoder + ", encodedImage:" + encodedImage + ", info:" + info);
//                          if (null != videoEncoder) {
//
//                          }
//                      }
//                  });
//                  Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoEncoderFactory getSoftwareEncoder 2 onEncodedFrame ");
////                  videoCodecStatus = encoderFactory.getSoftwareEncoder(videoCodecInfo).initEncode(ENCODER_SETTINGS, new VideoEncoder.Callback() {
////                      @Override
////                      public void onEncodedFrame(EncodedImage encodedImage, VideoEncoder.CodecSpecificInfo info) {
////                          Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoEncoderFactory getSoftwareEncoder onEncodedFrame index:" + index + ", videoEncoder:" + videoEncoder + ", encodedImage:" + encodedImage + ", info:" + info);
////                          if (null != videoEncoder) {
////
////                          }
////                      }
////                  });
//
//              }else {
//                  videoCodecStatus = videoEncoder.initEncode(ENCODER_SETTINGS, new VideoEncoder.Callback() {
//                      @Override
//                      public void onEncodedFrame(EncodedImage encodedImage, VideoEncoder.CodecSpecificInfo info) {
//                          Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoEncoderFactory onEncodedFrame index:" + index + ", videoEncoder:" + videoEncoder + ", encodedImage:" + encodedImage + ", info:" + info);
//                          if (null != videoEncoder) {
//
//                          }
//                      }
//                  });
//              }
//          }
//      }

    //视频的解码格式
    VideoDecoderFactory decoderFactory =
        new DefaultVideoDecoderFactory(mEglBase.getEglBaseContext());
//"packetization-mode":1,"level-asymmetry-allowed":1,"profile-level-id":"4d0032","x-google-start-bitrate":1000
//      Map<String, String> addParams = new HashMap<>();
//      addParams.put("level-asymmetry-allowed", "1");
//      addParams.put("profile-level-id", "4d0032");
//      addParams.put("packetization-mode", "1");
//      addParams.put("x-google-start-bitrate", "1000");
//      VideoCodecInfo addCodecInfo = new VideoCodecInfo("H264", addParams);
//      VideoDecoder addDecoder = decoderFactory.createDecoder(addCodecInfo);
//      long addDecoderId = null == addDecoder ? -1L : addDecoder.createNativeVideoDecoder();
//      Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory index: add addDecoder:" + addDecoder + ", addDecoderId:" + addDecoderId);
//      VideoCodecStatus addCodecStatus = null;
//      if (null != addDecoder /*&& !(addDecoder instanceof VideoDecoderFallback)*/) {
//          if (addDecoder instanceof VideoDecoderFallback) {
//              Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory getHardwareDecoder 3 ");
//              addCodecStatus = decoderFactory.getHardwareDecoder(addCodecInfo).initDecode(DECODER_SETTINGS, new VideoDecoder.Callback() {
//                  @Override
//                  public void onDecodedFrame(VideoFrame videoFrame, Integer decodeTimeMs, Integer qp) {
//                      Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory getHardwareDecoder onDecodedFrame index: add addDecoder:" + addDecoder + ", videoFrame:" + videoFrame + ", decodeTimeMs:" + decodeTimeMs + ", qp:" + qp);
//                      if (null != addDecoder) {
//                      }
//                  }
//              });
//              Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory getSoftwareDecoder 4 ");
////              addCodecStatus = decoderFactory.getSoftwareDecoder(addCodecInfo).initDecode(DECODER_SETTINGS, new VideoDecoder.Callback() {
////                  @Override
////                  public void onDecodedFrame(VideoFrame videoFrame, Integer decodeTimeMs, Integer qp) {
////                      Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory getSoftwareDecoder onDecodedFrame index: add addDecoder:" + addDecoder + ", videoFrame:" + videoFrame + ", decodeTimeMs:" + decodeTimeMs + ", qp:" + qp);
////                      if (null != addDecoder) {
////                      }
////                  }
////              });
//          } else {
//              addCodecStatus = addDecoder.initDecode(DECODER_SETTINGS, new VideoDecoder.Callback() {
//                  @Override
//                  public void onDecodedFrame(VideoFrame videoFrame, Integer decodeTimeMs, Integer qp) {
//                      Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory onDecodedFrame index: add addDecoder:" + addDecoder + ", videoFrame:" + videoFrame + ", decodeTimeMs:" + decodeTimeMs + ", qp:" + qp);
//                      if (null != addDecoder) {
//                      }
//                  }
//              });
//          }
//      }

//      VideoCodecInfo[] videoCodecInfos = decoderFactory.getSupportedCodecs();
//      int le = videoCodecInfos == null ? 0 : videoCodecInfos.length;
//      for (int i = 0; i < le; i++) {
//          int index = i;
//          VideoCodecInfo videoCodecInfo = videoCodecInfos[i];
//          VideoDecoder videoDecoder = decoderFactory.createDecoder(videoCodecInfo);
//          long decoderId = null != videoDecoder ? -1L : videoDecoder.createNativeVideoDecoder();
//          Map<String, String> params = videoCodecInfo.params;
//          for (Map.Entry<String, String> entry : params.entrySet()) {
//              Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory index:" + i + ", videoDecoder:" + videoDecoder + ", decoderId:" + decoderId + ",videoCodecInfo.name:" + videoCodecInfo.name + ", entry.getKey:" + entry.getKey() + ", entry.getValue:" + entry.getValue());
//          }
//          if (null == params || params.isEmpty()) {
//              Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory index:" + i + ", videoDecoder:" + videoDecoder + ", decoderId:" + decoderId +  ",videoCodecInfo.name:" + videoCodecInfo.name + ", params == null");
//          }
//          VideoCodecStatus videoCodecStatus = null;
//          String levelId = (null != params && params.containsKey("profile-level-id")) ? params.get("profile-level-id") : "";
//          if (null != videoDecoder /*&& !(videoDecoder instanceof VideoDecoderFallback)&& "H264".equals(videoCodecInfo.name) && "42e01f".equals(levelId)*/) {
//              if (videoDecoder instanceof VideoDecoderFallback) {
//                  Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory getHardwareDecoder 5 ");
//                  videoCodecStatus = decoderFactory.getHardwareDecoder(videoCodecInfo).initDecode(DECODER_SETTINGS, new VideoDecoder.Callback() {
//                      @Override
//                      public void onDecodedFrame(VideoFrame videoFrame, Integer decodeTimeMs, Integer qp) {
//                          Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory getHardwareDecoder onDecodedFrame index: videoDecoder:" + videoDecoder + ", videoFrame:" + videoFrame + ", decodeTimeMs:" + decodeTimeMs + ", qp:" + qp);
//                          if (null != videoDecoder) {
//                          }
//                      }
//                  });
//                  Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory getSoftwareDecoder 6 "+videoCodecStatus);
////                  videoCodecStatus = decoderFactory.getSoftwareDecoder(videoCodecInfo).initDecode(DECODER_SETTINGS, new VideoDecoder.Callback() {
////                      @Override
////                      public void onDecodedFrame(VideoFrame videoFrame, Integer decodeTimeMs, Integer qp) {
////                          Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory getSoftwareDecoder onDecodedFrame index: videoDecoder:" + videoDecoder + ", videoFrame:" + videoFrame + ", decodeTimeMs:" + decodeTimeMs + ", qp:" + qp);
////                          if (null != videoDecoder) {
////                          }
////                      }
////                  });
//              } else {
//                  videoCodecStatus = videoDecoder.initDecode(DECODER_SETTINGS, new VideoDecoder.Callback() {
//                      @Override
//                      public void onDecodedFrame(VideoFrame videoFrame, Integer decodeTimeMs, Integer qp) {
//                          Logger.d(TAG, "dongxl createPeerConnectionFactory() VideoDecoderFactory onDecodedFrame index:" + index + ", videoDecoder:" + videoDecoder + ", videoFrame:" + videoFrame + ",decodeTimeMs:" + decodeTimeMs + ", qp:" + qp);
//                          if (null != videoDecoder) {
//                          }
//                      }
//                  });
//              }
//          }
//      }

//      VideoDecoderFactory oldDecoderFactory  =  MediaCodecVideoDecoder.createFactory();
//      VideoEncoderFactory oldEncoderFactory = MediaCodecVideoEncoder.createFactory();

    mPeerConnectionFactory =
        builder
            .setAudioDeviceModule(adm)
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .createPeerConnectionFactory();
      mMediaStream = mPeerConnectionFactory.createLocalMediaStream(MEDIA_STREAM_ID);
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
            LogUtils.e(TAG, "onWebRtcAudioTrackInitError: " + errorMessage);
          }

          @Override
          public void onWebRtcAudioTrackStartError(
            JavaAudioDeviceModule.AudioTrackStartErrorCode errorCode, String errorMessage) {
            LogUtils.e(TAG, "onWebRtcAudioTrackStartError: " + errorCode + ". " + errorMessage);
          }

          @Override
          public void onWebRtcAudioTrackError(String errorMessage) {
            LogUtils.e(TAG, "onWebRtcAudioTrackError: " + errorMessage);
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
    WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
    WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
    mAudioSource = mPeerConnectionFactory.createAudioSource(new MediaConstraints());
  }

    /**
     * 共享视频文件
     *
     * @param videoFile 本地视频文件
     */
    private void createFileCapturer(String videoFile) {
        Logger.d(TAG, "createFileCapturer() videoFile:" + videoFile);
        if (!Utils.isEmptyString(videoFile)) {
            try {
                mThreadChecker.checkIsOnValidThread();
                mVideoCapturer = new FileVideoCapturer(videoFile);
            } catch (IOException e) {
                e.printStackTrace();
                Logger.e(TAG, "createFileCapturer: Failed to open video file for emulated camera IOException:" + e.getLocalizedMessage());
            }
        }
    }

    /**
     * 共享屏幕
     *
     * @param mediaProjectionPermissionResultCode
     * @param mediaProjectionPermissionResultData
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void createScreenCapturer(int mediaProjectionPermissionResultCode, Intent mediaProjectionPermissionResultData) {
        Logger.d(TAG, "createScreenCapturer() mediaProjectionPermissionResultCode:" + mediaProjectionPermissionResultCode);
        if (null == mediaProjectionPermissionResultData || mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
            Logger.e(TAG, "createScreenCapturer: User didn't give permission to capture the screen.");
            return;
        }
        mThreadChecker.checkIsOnValidThread();
        mVideoCapturer = new ScreenCapturerAndroid(mediaProjectionPermissionResultData, new MediaProjection.Callback() {
            @Override
            public void onStop() {
                Logger.e(TAG, "createScreenCapturer: User revoked permission to capture the screen.");
            }
        });
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
      Logger.d(TAG, "createCamCapture() deviceNames:" + Arrays.toString(deviceNames));
    for (String deviceName : deviceNames) {
      boolean needFrontFacing = Utils.isEmptyString(mPreferCameraFace) ? true : "front".endsWith(mPreferCameraFace);
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
        mVideoCapturer =
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
                      //cameraName:Camera 1, Facing front, Orientation 270
                      Logger.d(TAG, "onCameraOpening,isFrontCamera:" + isFrontCamera + ", cameraName:" + cameraName);
                      boolean isCamera2Supported = Camera2Enumerator.isSupported(context);//是否支持Camera2
                      boolean isFrontFacing = cameraEnumerator.isFrontFacing(cameraName);
                      String cameraId = cameraSwitchDone(context, isFrontFacing);
                      Logger.d(TAG, "onCameraOpening,isCamera2Supported:" + isCamera2Supported + ", isFrontFacing:" + isFrontFacing + ", cameraId:" + cameraId);
//                      frontFacingRotation(context, isCamera2Supported, isFrontFacing, cameraId);
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

    if (mVideoCapturer == null) {
      throw new IllegalStateException("Failed to create Camera Capture");
    }
  }

    private void frontFacingRotation(Context context, boolean isCamera2Supported, boolean isFrontFacing, String cameraId) {
        if (!isFrontFacing || TextUtils.isEmpty(cameraId)) {
            return;
        }
        if (!isCamera2Supported) {
            try {
                int id = Integer.parseInt(cameraId);
                if (id == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(id, info);
                    final Camera camera;
                    try {
                        camera = Camera.open(id);
                    } catch (RuntimeException e) {
                        return;
                    }
                    if (camera == null) {
                        return;
                    }
                    final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                    int rotation = wm.getDefaultDisplay().getRotation();
                    int degrees = 0;
                    switch (rotation) {
                        case Surface.ROTATION_0:
                            degrees = 0;
                            break;
                        case Surface.ROTATION_90:
                            degrees = 90;
                            break;
                        case Surface.ROTATION_180:
                            degrees = 180;
                            break;
                        case Surface.ROTATION_270:
                            degrees = 270;
                            break;
                    }

                    int result;
                    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        result = (info.orientation + degrees) % 360;
                        result = (360 - result) % 360;  // compensate the mirror
                    } else {  // back-facing
                        result = (info.orientation - degrees + 360) % 360;
                    }
                    camera.setDisplayOrientation(result);
                } else {

                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        } else {
            try {
                CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                if (characteristics != null
                        && characteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraMetadata.LENS_FACING_FRONT) {
                    int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

                } else {

                }
            } catch (/* CameraAccessException */ AndroidException e) {
                e.printStackTrace();
            }
        }
    }

    public String cameraSwitchDone(Context context, boolean isFrontCamera) {
        boolean isCamera2Supported = Camera2Enumerator.isSupported(context);//是否支持Camera2
        Logger.w(TAG, "cameraSwitchDone() onCameraSwitchDone isFrontCamera:" + isFrontCamera + ", isCamera2Supported:" + isCamera2Supported);
        if (!isFrontCamera) {
            return "-1";
        }
        return "-1";
//        if (isCamera2Supported) {
//            String cameraId2 = "";
//            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
//            try {
//                String[] cameraIds = cameraManager.getCameraIdList();
//                for (String cameraInfoId : cameraIds) {
//                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraInfoId);
//                    if (isFrontCamera && characteristics != null
//                            && characteristics.get(CameraCharacteristics.LENS_FACING)
//                            == CameraMetadata.LENS_FACING_FRONT) {
//                        cameraId2 = cameraInfoId;
//                        break;
//                    } else if (characteristics != null
//                            && characteristics.get(CameraCharacteristics.LENS_FACING)
//                            == CameraMetadata.LENS_FACING_BACK) {
//                        cameraId2 = cameraInfoId;
//                        break;
//                    }
//                }
//            } catch (/* CameraAccessException */ AndroidException e) {
//                e.printStackTrace();
//                Logging.e(TAG, "Camera2 access exception: " + e);
//            }
//            return cameraId2;
//        } else {
//            int cameraId = -1;
//            try {
//                int mCameras = Camera.getNumberOfCameras();
//                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
//                for (int cameraInfoId = 0; cameraInfoId < mCameras; cameraInfoId++) {
//                    Camera.getCameraInfo(cameraInfoId, cameraInfo);
//                    if (cameraInfo.facing == (isFrontCamera ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK)) {
//                        cameraId = cameraInfoId;
//                        break;
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                Logging.e(TAG, "Camera access exception: " + e);
//            }
//            return String.valueOf(cameraId);
//        }
    }

    /**
     * 切换摄像头
     * @param switchHandler
     */
  public void switchCam(CameraVideoCapturer.CameraSwitchHandler switchHandler) {
    Logger.d(TAG, "switchCam()");
    if (mVideoCapturer != null && mVideoCapturer instanceof CameraVideoCapturer) {
        mThreadChecker.checkIsOnValidThread();
        ((CameraVideoCapturer) mVideoCapturer).switchCamera(switchHandler);
    }
  }

    /**
     * Video source creation.
     * 创建视频源
     *
     * @param context
     */
    @MainThread
    private void createVideoSource(Context context, RoomConstant.VideoCapturerType capturerType) {
        Logger.d(TAG, "createVideoSource() capturerType:" + capturerType);
        mThreadChecker.checkIsOnValidThread();
        if (mPeerConnectionFactory == null) {
            createPeerConnectionFactory(context);
        }
        releaseVideoCapturer();
        switch (capturerType) {
            case CAMERA:
                createCamCapture(context);
                break;
            case SCREEN:
                createScreenCapturer(MediasoupConstant.mediaProjectionPermissionResultCode, MediasoupConstant.mediaProjectionPermissionResultData);
                break;
            case FILE:
                createFileCapturer(MediasoupConstant.extraVideoFileAsCamera);
                break;
        }

        mVideoSource = mPeerConnectionFactory.createVideoSource(mVideoCapturer.isScreencast());
        SurfaceTextureHelper surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", mEglBase.getEglBaseContext());

        mVideoCapturer.initialize(surfaceTextureHelper, context, mVideoSource.getCapturerObserver());
        this.curVideoSize = VIDEO_SIZE_HIGH;
        mVideoCapturer.startCapture(640, 480, 15);
    }

    /**
     * 改变摄像头采集的分辨率
     *
     * @param videoSize
     */
    public boolean changeCaptureFormat(int videoSize) {
        Logger.d(TAG, "changeCaptureFormat() videoSize:" + videoSize + ",curVideoSize:" + curVideoSize);
        mThreadChecker.checkIsOnValidThread();
        if (null != mVideoCapturer && curVideoSize != videoSize) {
            if (videoSize <= VIDEO_SIZE_HIGH && curVideoSize > VIDEO_SIZE_HIGH) {
                mVideoCapturer.changeCaptureFormat(640, 480, 15);
                this.curVideoSize = videoSize;
                return true;
            } else if (videoSize > VIDEO_SIZE_HIGH && videoSize <= VIDEO_SIZE_MID && (curVideoSize <= VIDEO_SIZE_HIGH || curVideoSize > VIDEO_SIZE_MID)) {
                mVideoCapturer.changeCaptureFormat(480, 360, 15);
                this.curVideoSize = videoSize;
                return true;
            } else if (videoSize > VIDEO_SIZE_MID && curVideoSize <= VIDEO_SIZE_MID) {
                mVideoCapturer.changeCaptureFormat(320, 240, 15);
                this.curVideoSize = videoSize;
                return true;
            }
        }
        this.curVideoSize = videoSize;
        return false;
    }

    /**
     * 停止释放AudioSource相关
     */
    public void releaseAudioSource() {
        Logger.d(TAG, "releaseAudioSource()");
        mThreadChecker.checkIsOnValidThread();
        if (mAudioSource != null) {
            mAudioSource.dispose();
            mAudioSource = null;
        }
    }

    /**
     * 停止释放VideoSource相关
     */
    public void releaseVideoCapturer() {
        Logger.d(TAG, "releaseVideoCapturer()");
        mThreadChecker.checkIsOnValidThread();
        if (mVideoCapturer != null) {
            try {
                mVideoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (!(mVideoCapturer instanceof CameraVideoCapturer)) {
                mVideoCapturer.dispose();
            }
            mVideoCapturer = null;
        }

        if (mVideoSource != null) {
            mVideoSource.dispose();
            mVideoSource = null;
        }
    }

    /**
     * Audio track creation.
     * 创建音频轨迹
     *
     * @param context
     * @return
     */
    public AudioTrack createAudioTrack(Context context) {
        Logger.d(TAG, "createAudioTrack()");
        mThreadChecker.checkIsOnValidThread();
        if (mAudioSource == null) {
            createAudioSource(context);
        }
        return mPeerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, mAudioSource);
    }

    public void addAudioTrackMediaStream(AudioTrack audioTrack) {
     mMediaStream.addTrack(audioTrack);
    }

    /**
     * Video track creation.
     * 创建视频轨迹
     *
     * @param context
     * @param capturerType
     * @return
     */
    public VideoTrack createVideoTrack(Context context, RoomConstant.VideoCapturerType capturerType) {
        Logger.d(TAG, "createVideoTrack() capturerType:" + capturerType);
        mThreadChecker.checkIsOnValidThread();
        if (mVideoSource == null || !isCurrentVideoCapturer(capturerType)) {
            createVideoSource(context, capturerType);
        }
        return mPeerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, mVideoSource);
    }

    public void addVideoTrackMediaStream(VideoTrack videoTrack) {
    mMediaStream.addTrack(videoTrack);
    }

    /**
     * 判断当前存在的VideoCapturer 和想要的是否一致
     *
     * @param capturerType
     * @return
     */
    private boolean isCurrentVideoCapturer(RoomConstant.VideoCapturerType capturerType) {
        if (null == capturerType || mVideoCapturer == null) {
            return false;
        }
        return capturerType == getCurrentVideoCapturer();
    }

    /**
     * 获取当前存在VideoCapturer 类型
     *
     * @return
     */
    public RoomConstant.VideoCapturerType getCurrentVideoCapturer() {
        if (null == mVideoCapturer) {
            return null;
        } else if (mVideoCapturer instanceof CameraVideoCapturer) {
            return RoomConstant.VideoCapturerType.CAMERA;
        } else if (mVideoCapturer instanceof ScreenCapturerAndroid) {
            return RoomConstant.VideoCapturerType.SCREEN;
        } else if (mVideoCapturer instanceof FileVideoCapturer) {
            return RoomConstant.VideoCapturerType.FILE;
        } else {
            return null;
        }
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

//    if(null != mMediaStream){
//       mMediaStream.dispose();
//       mMediaStream = null;
//    }

    releaseVideoCapturer();
    releaseAudioSource();
//    if (mVideoCapturer != null) {
//      mVideoCapturer.dispose();
//      mVideoCapturer = null;
//    }
//
//    if (mVideoSource != null) {
//      mVideoSource.dispose();
//      mVideoSource = null;
//    }
//
//    if (mAudioSource != null) {
//      mAudioSource.dispose();
//      mAudioSource = null;
//    }

    if (mPeerConnectionFactory != null) {
      mPeerConnectionFactory.dispose();
      mPeerConnectionFactory = null;
    }
  }
}
