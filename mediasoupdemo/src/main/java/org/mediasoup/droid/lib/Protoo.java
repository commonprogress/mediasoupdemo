package org.mediasoup.droid.lib;


import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import org.json.JSONObject;
import org.mediasoup.droid.Logger;
import org.mediasoup.droid.lib.socket.WebSocketTransport;
import org.protoojs.droid.Peer;
import org.protoojs.droid.ProtooException;

import io.reactivex.Observable;

/**
 * 连接WebSocket 和请求返回相关
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class Protoo extends org.protoojs.droid.Peer {

  private static final String TAG = Protoo.class.getSimpleName();

  interface RequestGenerator {
    void request(JSONObject req);
  }

  public Protoo(@NonNull WebSocketTransport transport, @NonNull Peer.Listener listener) {
    super(transport, listener);//初始化Protoo ， 相关监听 并连接WebSocket
  }

  public Observable<String> request(String method) {
    return request(method, new JSONObject());
  }

  public Observable<String> request(String method, @NonNull RequestGenerator generator) {
    JSONObject req = new JSONObject();
    generator.request(req);
    return request(method, req);
  }

  /**
   *     发送一个请求
   *     @param method 方法名
   *     @param data 请求body （json）
   * @return
   */
  private Observable<String> request(String method, @NonNull JSONObject data) {
    Logger.d(TAG, "request(), method: " + method + ", data:" + (null != data ? data.toString() : "null"));
    return Observable.create(
        emitter ->
            request(
                method,
                data,
                new ClientRequestHandler() {
                  @Override
                  public void resolve(String data) {
                      Logger.d(TAG, "request(), resolve method: " + method + ", data: " + data);
                    if (!emitter.isDisposed()) {
                      emitter.onNext(Utils.isEmptyString(data) ? "" : data);
                    }
                  }

                  @Override
                  public void reject(long error, String errorReason) {
                    Logger.e(TAG, "request(), reject method: " + method + ", error:" + error + ",errorReason:"+errorReason);
                    if (!emitter.isDisposed()) {
                      emitter.onError(new ProtooException(error, errorReason));
                    }
                  }
                }));
  }

  /**
   * 同步一个请求
   * @param method 方法名
   * @return
   * @throws ProtooException
   */
  @WorkerThread
  public String syncRequest(String method) throws ProtooException {
    return syncRequest(method, new JSONObject());
  }

  /**
   *  同步一个请求
   * @param method 方法名
   * @param generator
   * @return
   * @throws ProtooException
   */
  @WorkerThread
  public String syncRequest(String method, @NonNull RequestGenerator generator)
      throws ProtooException {
    JSONObject req = new JSONObject();
    generator.request(req);
    return syncRequest(method, req);
  }

  /**
   * 同步一个请求
   * @param method 方法名
   * @param data 请求body （json）
   * @return
   * @throws ProtooException
   */
  @WorkerThread
  private String syncRequest(String method, @NonNull JSONObject data) throws ProtooException {
    try {
      return request(method, data).blockingFirst();
    } catch (Throwable throwable) {
      Logger.e(TAG, "syncRequest(),fail method: " + method + ", throwable:" + throwable.getMessage());
      throw new ProtooException(-1, throwable.getMessage());
    }
  }
}
