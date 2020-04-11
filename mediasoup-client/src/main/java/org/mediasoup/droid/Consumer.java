package org.mediasoup.droid;


import android.support.annotation.Nullable;

import org.webrtc.CalledByNative;
import org.webrtc.MediaStreamTrack;
import org.webrtc.RTCUtils;

/**
 * 消费值 消息信息管理类
 */
public class Consumer {

    public interface Listener {

        @CalledByNative
        void onTransportClose(Consumer consumer);//消费者关闭监听
    }

    private long mNativeConsumer;//类似于消费者 native  id

    @Nullable
    private MediaStreamTrack mCachedTrack;

    @CalledByNative
    public Consumer(long nativeProducer) {
        mNativeConsumer = nativeProducer;
        long nativeTrack = getNativeTrack(mNativeConsumer);
        mCachedTrack = RTCUtils.createMediaStreamTrack(nativeTrack);
    }

    public String getId() {
        return getNativeId(mNativeConsumer);
    }

    public String getProducerId() {
        return getNativeProducerId(mNativeConsumer);
    }

    public boolean isClosed() {
        return isNativeClosed(mNativeConsumer);
    }

    public boolean isPaused() {
        return isNativePaused(mNativeConsumer);
    }

    /**
     * 获取当前模式
     * video 视频
     * audio 音频
     *
     * @return
     */
    public String getKind() {
        return getNativeKind(mNativeConsumer);
    }

    public MediaStreamTrack getTrack() {
        return mCachedTrack;
    }

    public String getRtpParameters() {
        return getNativeRtpParameters(mNativeConsumer);
    }

    public String getAppData() {
        return getNativeAppData(mNativeConsumer);
    }

    public void resume() {
        nativeResume(mNativeConsumer);
    }

    public void pause() {
        nativePause(mNativeConsumer);
    }

    public String getStats() throws MediasoupException {
        return getNativeStats(mNativeConsumer);
    }

    public void close() {
        nativeClose(mNativeConsumer);
    }

    private static native String getNativeId(long nativeConsumer);

    private static native String getNativeProducerId(long nativeConsumer);

    private static native boolean isNativeClosed(long nativeConsumer);

    private static native boolean isNativePaused(long nativeConsumer);

    /**
     * 获取当前模式
     * video 视频
     * audio 音频
     *
     * @param nativeConsumer
     * @return
     */
    private static native String getNativeKind(long nativeConsumer);

    private static native long getNativeTrack(long nativeConsumer);

    private static native String getNativeRtpParameters(long nativeConsumer);

    private static native String getNativeAppData(long nativeConsumer);

    private static native void nativeResume(long nativeConsumer);

    private static native void nativePause(long nativeConsumer);

    private static native String getNativeStats(long nativeConsumer) throws MediasoupException;

    private static native void nativeClose(long nativeConsumer);
}
