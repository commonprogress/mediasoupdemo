package org.mediasoup.droid;

import org.webrtc.CalledByNative;

public class RecvTransport extends Transport {

  public interface Listener extends Transport.Listener {}

  private long mNativeTransport;

  @CalledByNative
  public RecvTransport(long nativeTransport) {
    mNativeTransport = nativeTransport;
  }

  public void dispose() {
    checkTransportExists();
    nativeFreeTransport(mNativeTransport);
    mNativeTransport = 0;
  }

  @Override
  public long getNativeTransport() {
    return nativeGetNativeTransport(mNativeTransport);
  }

  private void checkTransportExists() {
    if (mNativeTransport == 0) {
      throw new IllegalStateException("RecvTransport has been disposed.");
    }
  }

  /**
   * 添加一个Consumer（消费者）到C 层
   * @param listener
   * @param id
   * @param producerId
   * @param kind
   * @param rtpParameters
   * @return
   * @throws MediasoupException
   */
  public Consumer consume(
      Consumer.Listener listener, String id, String producerId, String kind, String rtpParameters)
      throws MediasoupException {
    return consume(listener, id, producerId, kind, rtpParameters, null);
  }

  /**
   * 添加一个Consumer（消费者）到C 层
   * @param listener
   * @param id
   * @param producerId
   * @param kind
   * @param rtpParameters
   * @param appData
   * @return
   * @throws MediasoupException
   */
  public Consumer consume(
      Consumer.Listener listener,
      String id,
      String producerId,
      String kind,
      String rtpParameters,
      String appData)
      throws MediasoupException {
    checkTransportExists();
    return nativeConsume(mNativeTransport, listener, id, producerId, kind, rtpParameters, appData);
  }

  private static native long nativeGetNativeTransport(long nativeTransport);

  /**
   * 添加一个Consumer（消费者）到C 层
   * @param mNativeTransport
   * @param listener
   * @param id
   * @param producerId
   * @param kind
   * @param rtpParameters
   * @param appData
   * @return
   * @throws MediasoupException
   */
  private static native Consumer nativeConsume(
      long mNativeTransport,
      Consumer.Listener listener,
      String id,
      String producerId,
      String kind,
      String rtpParameters,
      String appData)
      throws MediasoupException;

  private static native void nativeFreeTransport(long nativeTransport);
}
