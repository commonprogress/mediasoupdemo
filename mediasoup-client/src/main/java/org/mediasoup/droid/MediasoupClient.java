package org.mediasoup.droid;

import android.content.Context;

import org.webrtc.PeerConnectionFactory;
import org.webrtc.PeerConnectionFactory.InitializationOptions;

/**
 * Mediasoup 初始化管理类
 */
public class MediasoupClient {

  static {
    System.loadLibrary("mediasoupclient_so");
  }

  public static void initialize(Context appContext) {
    initialize(appContext, null);
  }

  /**
   * 初始化 PeerConnection
   * @param appContext
   * @param fieldTrials
   */
  public static void initialize(Context appContext, String fieldTrials) {
    InitializationOptions options =
        InitializationOptions.builder(appContext)
            .setFieldTrials(fieldTrials)
            .setEnableInternalTracer(true)
            .setNativeLibraryName("mediasoupclient_so")
            .createInitializationOptions();
    PeerConnectionFactory.initialize(options);//初始化 PeerConnection
  }

  public static String version() {
    return nativeVersion();
  }

  /**
   * 获取 Mediasoup 版本
   * @return
   */
  private static native String nativeVersion();
}
