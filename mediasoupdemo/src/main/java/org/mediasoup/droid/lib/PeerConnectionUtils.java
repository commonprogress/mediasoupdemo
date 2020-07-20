package org.mediasoup.droid.lib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.Build;

import androidx.annotation.MainThread;
import androidx.annotation.RequiresApi;

import com.jsy.mediasoup.MediasoupConstant;
import com.jsy.mediasoup.utils.LogUtils;

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
import org.webrtc.FileVideoCapturer;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.ThreadUtils;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoDecoderFactory;
import org.webrtc.VideoEncoderFactory;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.io.IOException;

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

    private static final String TAG = PeerConnectionUtils.class.getSimpleName();
    private static final String MEDIA_STREAM_ID = "ARDAMS";
    private static final String VIDEO_TRACK_ID = "ARDAMSv0";
    private static final String AUDIO_TRACK_ID = "ARDAMSa0";
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
    private static String mPreferCameraFace;
    private static EglBase mEglBase = EglBase.create();
    private int curVideoSize;

    private static final int VIDEO_SIZE_HIGH = 2;
    private static final int VIDEO_SIZE_MID = 4;
    private static final int VIDEO_SIZE_LOW = 8;

    /**
     * 获得渲染器
     *
     * @return
     */
    public static EglBase.Context getEglContext() {
        return mEglBase.getEglBaseContext();
    }

    /**
     * 设置摄像头信息 前置 后置摄像头
     *
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

    /**
     * PeerConnection factory creation.
     * 创建 PeerConnection实体
     *
     * @param context
     */
    private void createPeerConnectionFactory(Context context) {
        Logger.d(TAG, "createPeerConnectionFactory()");
        mThreadChecker.checkIsOnValidThread();
        PeerConnectionFactory.Builder builder = PeerConnectionFactory.builder();//初始化PeerConnectionFactory
        builder.setOptions(null);//初始化PeerConnectionFactory

        AudioDeviceModule audioDeviceModule = createJavaAudioDevice(context);
        //视频的编码格式
        VideoEncoderFactory encoderFactory =
            new DefaultVideoEncoderFactory(
                mEglBase.getEglBaseContext(), true /* enableIntelVp8Encoder */, true);
        //视频的解码格式
        VideoDecoderFactory decoderFactory =
            new DefaultVideoDecoderFactory(mEglBase.getEglBaseContext());

        mPeerConnectionFactory =
            builder
                .setAudioDeviceModule(audioDeviceModule)
                .setVideoEncoderFactory(encoderFactory)
                .setVideoDecoderFactory(decoderFactory)
                .createPeerConnectionFactory();
        mMediaStream = mPeerConnectionFactory.createLocalMediaStream(MEDIA_STREAM_ID);
    }

    /**
     * 创建java层 音频设备数据
     *
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
     *
     * @param context
     */
    private void createAudioSource(Context context) {
        Logger.d(TAG, "createAudioSource()");
        mThreadChecker.checkIsOnValidThread();
        if (mPeerConnectionFactory == null) {
            createPeerConnectionFactory(context);
        }
//    WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(true);
//    WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);
        mAudioSource = mPeerConnectionFactory.createAudioSource(getAudioMediaConstraints());
    }

    private MediaConstraints getAudioMediaConstraints() {
        Logger.d(TAG, "getAudioMediaConstraints()");
        mThreadChecker.checkIsOnValidThread();
        MediaConstraints mediaConstraints = new MediaConstraints();

        mediaConstraints.mandatory.add(
            new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));
        mediaConstraints.mandatory.add(
            new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "true"));
        mediaConstraints.mandatory.add(
            new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "true"));
        mediaConstraints.mandatory.add(
            new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));

        return mediaConstraints;
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

            if (!Utils.isEmptyString(selectedDeviceName)) {
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
                                boolean isFrontCamera = !Utils.isEmptyString(cameraName) && cameraName.toLowerCase().contains("front");
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

        if (mVideoCapturer == null) {
            throw new IllegalStateException("Failed to create Camera Capture");
        }
    }

    /**
     * 切换摄像头
     *
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
        mThreadChecker.checkIsOnValidThread();
        if (null != audioTrack) {
            mMediaStream.addTrack(audioTrack);
        }
    }

    public void removeAudioTrackMediaStream(AudioTrack audioTrack) {
        mThreadChecker.checkIsOnValidThread();
        if (null != audioTrack) {
            mMediaStream.removeTrack(audioTrack);
        }
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
        mThreadChecker.checkIsOnValidThread();
        if (null != videoTrack) {
            mMediaStream.addTrack(videoTrack);
        }
    }

    public void removeVideoTrackMediaStream(VideoTrack videoTrack) {
        mThreadChecker.checkIsOnValidThread();
        if (null != videoTrack) {
            mMediaStream.removeTrack(videoTrack);
        }
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

//        if(null != mMediaStream){
//            mMediaStream.dispose();
//            mMediaStream = null;
//        }

        releaseAudioSource();
        releaseVideoCapturer();

//        if (mVideoCapturer != null) {
//            mVideoCapturer.dispose();
//            mVideoCapturer = null;
//        }
//
//        if (mVideoSource != null) {
//            mVideoSource.dispose();
//            mVideoSource = null;
//        }
//
//        if (mAudioSource != null) {
//            mAudioSource.dispose();
//            mAudioSource = null;
//        }

        if (mPeerConnectionFactory != null) {
            mPeerConnectionFactory.dispose();
            PeerConnectionFactory.stopInternalTracingCapture();
            PeerConnectionFactory.shutdownInternalTracer();
            mPeerConnectionFactory = null;
        }
    }
}
