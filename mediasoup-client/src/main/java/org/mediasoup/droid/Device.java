package org.mediasoup.droid;

/**
 *
 */
public class Device {

  /**
   * 设备数量
   */
  private long mNativeDevice;

  public Device() {
    //WebSocket连接成功 创建一个设备
    mNativeDevice = nativeNewDevice();
  }

  /**
   * 销毁时 释放设备
   */
  public void dispose() {
    checkDeviceExists();
    nativeFreeDevice(mNativeDevice);
    mNativeDevice = 0;
  }

  /**
   * 加载设备 路由rtp
   * @param routerRtpCapabilities
   * @throws MediasoupException
   */
  public void load(String routerRtpCapabilities) throws MediasoupException {
    checkDeviceExists();
    nativeLoad(mNativeDevice, routerRtpCapabilities);
  }

  public boolean isLoaded() {
    checkDeviceExists();
    return nativeIsLoaded(mNativeDevice);
  }

  /**
   * 获取MediasoupDevice rtp
   * @return
   * @throws MediasoupException
   */
  public String getRtpCapabilities() throws MediasoupException {
    checkDeviceExists();
    return nativeGetRtpCapabilities(mNativeDevice);
  }

  public boolean canProduce(String kind) throws MediasoupException {
    checkDeviceExists();
    return nativeCanProduce(mNativeDevice, kind);
  }

  /**
   * 创建发送的 音视频 参数和ice相关
   * @param listener
   * @param id
   * @param iceParameters
   * @param iceCandidates
   * @param dtlsParameters
   * @return
   * @throws MediasoupException
   */
  public SendTransport createSendTransport(
      SendTransport.Listener listener,
      String id,
      String iceParameters,
      String iceCandidates,
      String dtlsParameters)
      throws MediasoupException {
    return createSendTransport(
        listener, id, iceParameters, iceCandidates, dtlsParameters, null, null);
  }

  /**
   * 创建发送的 音视频 参数和ice相关
   * @param listener
   * @param id
   * @param iceParameters
   * @param iceCandidates
   * @param dtlsParameters
   * @param options
   * @param appData
   * @return
   * @throws MediasoupException
   */
  public SendTransport createSendTransport(
      SendTransport.Listener listener,
      String id,
      String iceParameters,
      String iceCandidates,
      String dtlsParameters,
      PeerConnection.Options options,
      String appData)
      throws MediasoupException {
    checkDeviceExists();
    return nativeCreateSendTransport(
        mNativeDevice,
        listener,
        id,
        iceParameters,
        iceCandidates,
        dtlsParameters,
        options,
        appData);
  }

  /**
   * 创建接收 音视频 参数和ice相关
   * @param listener
   * @param id
   * @param iceParameters
   * @param iceCandidates
   * @param dtlsParameters
   * @return
   * @throws MediasoupException
   */
  public RecvTransport createRecvTransport(
      RecvTransport.Listener listener,
      String id,
      String iceParameters,
      String iceCandidates,
      String dtlsParameters)
      throws MediasoupException {
    return createRecvTransport(
        listener, id, iceParameters, iceCandidates, dtlsParameters, null, null);
  }

  /**
   * 创建接收 音视频 参数和ice相关
   * @param listener
   * @param id
   * @param iceParameters
   * @param iceCandidates
   * @param dtlsParameters
   * @param options
   * @param appData
   * @return
   * @throws MediasoupException
   */
  public RecvTransport createRecvTransport(
      RecvTransport.Listener listener,
      String id,
      String iceParameters,
      String iceCandidates,
      String dtlsParameters,
      PeerConnection.Options options,
      String appData)
      throws MediasoupException {
    checkDeviceExists();
    return nativeCreateRecvTransport(
        mNativeDevice,
        listener,
        id,
        iceParameters,
        iceCandidates,
        dtlsParameters,
        options,
        appData);
  }

  /**
   * 检测设备是否存在
   */
  private void checkDeviceExists() {
    if (mNativeDevice == 0) {
      throw new IllegalStateException("Device has been disposed.");
    }
  }

  /**
   * 创建一个新设备
   * @return
   */
  private static native long nativeNewDevice();

  /**
   * 释放一个设备
   * @param device
   */
  private static native void nativeFreeDevice(long device);

  /**
   * 加载设备 路由rtp
   * @param device
   * @param routerRtpCapabilities
   * @throws MediasoupException
   */
  private static native void nativeLoad(long device, String routerRtpCapabilities)
      throws MediasoupException;

  private static native boolean nativeIsLoaded(long device);

  /**
   * 获取MediasoupDevice rtp
   * @param device
   * @return
   * @throws MediasoupException
   */
  private static native String nativeGetRtpCapabilities(long device) throws MediasoupException;

  private static native boolean nativeCanProduce(long device, String kind)
      throws MediasoupException;

  /**
   * 创建发送的 音视频 参数和ice相关
   * @param device
   * @param listener
   * @param id
   * @param iceParameters
   * @param iceCandidates
   * @param dtlsParameters
   * @param options
   * @param appData
   * @return
   * @throws MediasoupException
   */
  private static native SendTransport nativeCreateSendTransport(
      long device,
      SendTransport.Listener listener,
      String id,
      String iceParameters,
      String iceCandidates,
      String dtlsParameters,
      PeerConnection.Options options,
      String appData)
      throws MediasoupException;

  /**
   * 接收 音视频 参数和ice相关
   * @param device
   * @param listener
   * @param id
   * @param iceParameters
   * @param iceCandidates
   * @param dtlsParameters
   * @param options
   * @param appData
   * @return
   * @throws MediasoupException
   */
  private static native RecvTransport nativeCreateRecvTransport(
      long device,
      RecvTransport.Listener listener,
      String id,
      String iceParameters,
      String iceCandidates,
      String dtlsParameters,
      PeerConnection.Options options,
      String appData)
      throws MediasoupException;
}
